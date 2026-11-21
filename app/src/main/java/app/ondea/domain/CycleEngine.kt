package app.ondea.domain

import app.ondea.data.AppData
import app.ondea.data.DayEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class CycleAnalysis(
    val start: LocalDate,
    /** Last day of the cycle, inclusive; null while the cycle is ongoing. */
    val end: LocalDate?,
    /** Days until the next period started; null if ongoing or tracking was paused. */
    val naturalLength: Int?,
    val shift: Shift?,
    val mucus: MucusSign,
    /** First day with a positive ovulation (LH) test. */
    val lhPositiveIndex: Int?,
)

enum class OvulationSource { TEMPERATURE, LH_TEST, MUCUS, PREDICTED }

/** What the fertile phase is still waiting for before it can be considered over. */
enum class Waiting { TEMPERATURE, MUCUS }

/** Fertile window of one cycle, as 0-based cycle-day indices. */
data class Window(
    val ovulationIndex: Int,
    val source: OvulationSource,
    val fertileStart: Int,
    val fertileEnd: Int,
    val peakStart: Int,
    /** The fertile phase is over: temperature confirmed, and mucus too when it is tracked. */
    val closed: Boolean,
    val waitingFor: Waiting?,
)

data class DayInfo(
    val entry: DayEntry?,
    val cycleStart: Boolean,
    val periodPredicted: Boolean,
    val fertile: Boolean,
    val fertilePredicted: Boolean,
    val peak: Boolean,
    val ovulation: Boolean,
    val ovulationPredicted: Boolean,
)

data class ChartData(
    val cycle: CycleAnalysis,
    val days: Int,
    /** Raw readings per day (°C), disturbed ones included. */
    val temps: List<Double?>,
    val disturbed: List<Boolean>,
    val flow: List<Int>,
    val mucus: List<Int>,
    val intercourse: List<Boolean>,
    val window: Window?,
    val todayIndex: Int?,
)

/** Everything about the current cycle that the Today screen shows. */
data class Forecast(
    val cycleDay: Int,
    val window: Window,
    val ovulation: LocalDate,
    val fertileStart: LocalDate,
    val fertileEnd: LocalDate,
    val peakStart: LocalDate,
    val nextPeriod: LocalDate,
    /** > 0 when the period is later than expected. */
    val daysLate: Int,
    /** Days of elevated temperature so far (0 if no shift). */
    val highDays: Int,
    /** Today's mucus observation (0 if none). */
    val mucusToday: Int,
) {
    val ovulationSource get() = window.source
    val closed get() = window.closed

    /** Today relative to the (confirmed or estimated) ovulation day. */
    val offsetFromOvulation get() = (cycleDay - 1) - window.ovulationIndex
}

/**
 * Pure, Android-free analysis of the recorded data. Recreated whenever data or "today" changes.
 *
 * Signs, from strongest to weakest:
 *  1. Temperature: the 3-over-6 rule ([ShiftDetector]) confirms ovulation happened.
 *  2. Ovulation (LH) test: ovulation is estimated the day after the first positive test.
 *  3. Cervical mucus ([MucusDetector]): the first sign opens the fertile window, the peak day
 *     estimates ovulation, and the fertile phase only ends 3 days after the peak.
 *  4. Calendar prediction: ovulation on the user's usual ovulation day if set, otherwise
 *     (average cycle length - luteal phase). The luteal phase is learnt from cycles with a
 *     confirmed temperature rise, 14 days by default.
 *
 * The fertile window spans the 5 days before ovulation through the day after
 * (sperm survive up to ~5 days, the egg ~1 day). Peak days are the 2 days before ovulation
 * and ovulation day.
 */
class CycleEngine(val data: AppData, val today: LocalDate) {
    private val entries: Map<Long, DayEntry> = data.entries.associateBy { it.date }
    private val settings = data.settings

    val cycles: List<CycleAnalysis>
    val avgCycleLength: Int
    val avgLutealLength: Int
    /** 0-based index where ovulation is expected when no sign has shown it yet. */
    val predictedOvulationIndex: Int
    val current: CycleAnalysis?
    val forecast: Forecast?
    private val windows: List<Window?>
    private val futurePeriodStarts: List<LocalDate>

    init {
        val sorted = data.cycles.sortedBy { it.start }
        val todayDay = today.toEpochDay()
        cycles = sorted.mapIndexed { i, c ->
            val next = sorted.getOrNull(i + 1)
            val naturalEnd = next?.let { it.start - 1 }
            val end = listOfNotNull(c.end, naturalEnd).minOrNull()
            val stoppedEarly = c.end != null && (naturalEnd == null || c.end < naturalEnd)
            val natural = if (next != null && !stoppedEarly) (next.start - c.start).toInt() else null
            val lastDay = end ?: maxOf(todayDay, c.start)
            val days = (c.start..lastDay).map { entries[it] }
            CycleAnalysis(
                start = LocalDate.ofEpochDay(c.start),
                end = end?.let(LocalDate::ofEpochDay),
                naturalLength = natural,
                shift = ShiftDetector.detect(days.map { e -> e?.takeIf { !it.disturbed }?.tempC }),
                mucus = MucusDetector.analyze(days.map { it?.mucus ?: 0 }),
                lhPositiveIndex = days.indexOfFirst { it?.lh == 2 }.takeIf { it >= 0 },
            )
        }

        val completed = cycles.mapNotNull { it.naturalLength }.filter { it in 15..60 }.takeLast(6)
        avgCycleLength = if (completed.size >= 2) completed.average().roundToInt() else settings.cycleLength

        val luteal = cycles
            .filter { it.naturalLength != null && it.shift != null }
            .map { it.naturalLength!! - (it.shift!!.ovulationIndex + 1) }
            .filter { it in 7..20 }
            .takeLast(6)
        avgLutealLength = if (luteal.isNotEmpty()) luteal.average().roundToInt().coerceIn(10, 16) else 14

        predictedOvulationIndex = settings.ovulationDay?.let { it - 1 }
            ?: (avgCycleLength - avgLutealLength - 1).coerceAtLeast(settings.periodLength)

        current = cycles.lastOrNull()?.takeIf { it.end == null && !it.start.isAfter(today) }
        windows = cycles.map { windowFor(it, isCurrent = it == current) }
        forecast = current?.let { forecastFor(it, windows.last()!!) }

        futurePeriodStarts = forecast?.let { f ->
            val first = if (f.nextPeriod.isAfter(today)) f.nextPeriod else today.plusDays(1)
            (0 until 6).map { first.plusDays(it.toLong() * avgCycleLength) }
        } ?: emptyList()
    }

    private fun windowFor(c: CycleAnalysis, isCurrent: Boolean): Window? {
        val shift = c.shift
        val m = c.mucus
        val (ov, source) = when {
            shift != null -> shift.ovulationIndex to OvulationSource.TEMPERATURE
            // Ovulation follows the LH surge by about 24-36 h (Direito et al., Fertil Steril 2013).
            c.lhPositiveIndex != null -> c.lhPositiveIndex + 1 to OvulationSource.LH_TEST
            m.peakIndex != null -> m.peakIndex to OvulationSource.MUCUS
            isCurrent -> predictedOvulationIndex to OvulationSource.PREDICTED
            else -> return null
        }
        val mucusClose = m.peakIndex?.plus(MucusDetector.DAYS_AFTER_PEAK)
        // A finished cycle with a confirmed rise is closed even if mucus logging stopped early.
        val closed = shift != null && (!m.tracked || mucusClose != null || !isCurrent)
        val end = when {
            shift != null -> maxOf(shift.confirmIndex, mucusClose ?: 0)
            mucusClose != null -> maxOf(mucusClose, ov + 1)
            else -> ov + 1
        }
        val start = minOf(ov - 5, m.firstIndex ?: Int.MAX_VALUE).coerceAtLeast(0)
        return Window(
            ovulationIndex = ov,
            source = source,
            fertileStart = start,
            fertileEnd = end,
            peakStart = (ov - 2).coerceAtLeast(0),
            closed = closed,
            waitingFor = when {
                closed -> null
                shift == null -> Waiting.TEMPERATURE
                else -> Waiting.MUCUS
            },
        )
    }

    private fun forecastFor(cycle: CycleAnalysis, w: Window): Forecast {
        val cycleDay = days(cycle.start, today) + 1
        val ovulation = cycle.start.plusDays(w.ovulationIndex.toLong())
        val nextPeriod = if (w.source == OvulationSource.PREDICTED) cycle.start.plusDays(avgCycleLength.toLong())
        else ovulation.plusDays(avgLutealLength + 1L)
        return Forecast(
            cycleDay = cycleDay,
            window = w,
            ovulation = ovulation,
            fertileStart = cycle.start.plusDays(w.fertileStart.toLong()),
            fertileEnd = cycle.start.plusDays(w.fertileEnd.toLong()),
            peakStart = cycle.start.plusDays(w.peakStart.toLong()),
            nextPeriod = nextPeriod,
            daysLate = days(nextPeriod, today).coerceAtLeast(0),
            highDays = cycle.shift?.let { cycleDay - it.firstHighIndex } ?: 0,
            mucusToday = entries[today.toEpochDay()]?.mucus ?: 0,
        )
    }

    fun cycleIndexOf(date: LocalDate): Int? =
        cycles.indexOfLast { !it.start.isAfter(date) && (it.end == null || !it.end.isBefore(date)) }
            .takeIf { it >= 0 }

    fun dayInfo(date: LocalDate): DayInfo {
        val entry = entries[date.toEpochDay()]
        var fertile = false
        var fertilePredicted = false
        var peak = false
        var ovulation = false
        var ovulationPredicted = false
        var periodPredicted = false

        val index = cycleIndexOf(date)
        val window = index?.let { windows[it] }
        if (index != null && window != null) {
            val d = days(cycles[index].start, date)
            val inFertile = d in window.fertileStart..window.fertileEnd
            val predicted = window.source == OvulationSource.PREDICTED
            fertile = inFertile && !predicted
            fertilePredicted = inFertile && predicted
            peak = d in window.peakStart..window.ovulationIndex
            ovulation = d == window.ovulationIndex && !predicted
            ovulationPredicted = d == window.ovulationIndex && predicted
        }

        if (current != null && index != null && cycles[index] == current && date.isAfter(today) &&
            days(current.start, date) < settings.periodLength
        ) {
            periodPredicted = true
        }

        if (date.isAfter(today)) {
            for (p in futurePeriodStarts) {
                if (date.inRange(p, p.plusDays(settings.periodLength - 1L))) periodPredicted = true
                val ov = p.plusDays(predictedOvulationIndex.toLong())
                if (date.inRange(ov.minusDays(5), ov.plusDays(1))) fertilePredicted = true
                if (date.inRange(ov.minusDays(2), ov)) peak = true
                if (date == ov) ovulationPredicted = true
            }
        }

        return DayInfo(
            entry = entry,
            cycleStart = data.cycles.any { it.start == date.toEpochDay() },
            periodPredicted = periodPredicted && (entry?.flow ?: 0) == 0,
            fertile = fertile,
            fertilePredicted = fertilePredicted && !fertile,
            peak = peak,
            ovulation = ovulation,
            ovulationPredicted = ovulationPredicted,
        )
    }

    fun chart(index: Int): ChartData {
        val cycle = cycles[index]
        val shown = when {
            cycle.end != null -> days(cycle.start, cycle.end) + 1
            else -> maxOf(days(cycle.start, today) + 1, avgCycleLength)
        }
        val dayEntries = (0 until shown).map { entries[cycle.start.plusDays(it.toLong()).toEpochDay()] }
        return ChartData(
            cycle = cycle,
            days = shown,
            temps = dayEntries.map { it?.tempC },
            disturbed = dayEntries.map { it?.disturbed == true },
            flow = dayEntries.map { it?.flow ?: 0 },
            mucus = dayEntries.map { it?.mucus ?: 0 },
            intercourse = dayEntries.map { it?.intercourse == true },
            window = windows[index],
            todayIndex = days(cycle.start, today).takeIf { it in 0 until shown },
        )
    }

    /** Most recent reading, used as the starting value of the temperature input. */
    fun lastTemperature(): Double? = data.entries.lastOrNull { it.tempC != null }?.tempC

    companion object {
        fun days(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
    }
}

private fun LocalDate.inRange(from: LocalDate, to: LocalDate) = !isBefore(from) && !isAfter(to)
