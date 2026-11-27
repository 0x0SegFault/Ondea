package app.ondea.domain

import app.ondea.data.AppData
import app.ondea.data.DayEntry
import app.ondea.data.Settings
import kotlinx.datetime.LocalDate
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
    /** A temperature rise that has started but is not confirmed yet. */
    val rise: Rise? = null,
    /** Mucus by 0-based day, 0 on bleeding days (mucus cannot be observed then). */
    val mucusDays: List<Int> = emptyList(),
    /** Valid (not disturbed) temperatures by 0-based day. */
    val temps: List<Double?> = emptyList(),
)

/** Whether the temperature and the mucus of the current cycle point to the same ovulation. */
data class SignsCheck(
    val agree: Boolean,
    /** The temperature rise is confirmed (3 high readings), not only starting. */
    val confirmed: Boolean,
    /** Ovulation from the temperature. */
    val temperature: LocalDate,
    /** Ovulation from the mucus peak and the usual gap. */
    val mucus: LocalDate,
    /** The forecast day of this cycle. */
    val forecast: LocalDate,
    /** Days from the forecast day to the agreed ovulation, when they agree and differ by 2 days or more; else 0. */
    val forecastGap: Int,
) {
    /** Text key, shared by Android and iOS. */
    val key: String get() = when {
        !agree -> "signs_disagree"
        forecastGap > 0 -> "signs_agree_later"
        forecastGap < 0 -> "signs_agree_earlier"
        else -> "signs_agree"
    }
    /** Date arguments of [key], in order. */
    val dates: List<LocalDate> get() = when {
        !agree -> listOf(temperature, mucus)
        forecastGap != 0 -> listOf(temperature, forecast)
        else -> listOf(temperature)
    }
}

/** The user's usual temperature levels, learnt from cycles with a confirmed rise. */
data class TempProfile(val lowC: Double, val highC: Double, val cycles: Int) {
    val riseC: Double get() = highC - lowC
}

/** Ondea's learnt ovulation day differs from the day in the settings: the user can choose. Days are 1-based. */
data class OvulationAdvice(
    /** The day the prediction used until now: the user's own day, or a typical cycle when they gave none. */
    val settingDay: Int,
    val learnedDay: Int,
    val cycles: Int,
    /** True when [settingDay] is the user's own day, false when it is the typical-cycle day. */
    val fromUser: Boolean,
)

/**
 * Where the ovulation date of a cycle comes from.
 * Confirmed or strong signs: [TEMPERATURE], [LH_TEST], [MUCUS] (peak). Signs that point to ovulation but are
 * not confirmed yet: [TEMP_RISING], [MUCUS_SIGN]. No sign: [PREDICTED] (calendar), or [LATE] when the
 * calendar day has passed without a sign and the estimate moves later day by day.
 */
enum class OvulationSource {
    TEMPERATURE, LH_TEST, MUCUS, TEMP_RISING, MUCUS_SIGN, PREDICTED, LATE;

    /** Based on the calendar only, not on a sign of this cycle. */
    val calendar: Boolean get() = this == PREDICTED || this == LATE
    /** The ovulation day is known from a sign (not an estimate that can still move). */
    val firm: Boolean get() = this == TEMPERATURE || this == LH_TEST || this == MUCUS
}

/** Basis of a prediction made before any sign of the current cycle. */
enum class PredictionBasis { OWN_DAY, LEARNED, TYPICAL }

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
    /** The user's usual low and high temperature, if learnt. */
    val profile: TempProfile? = null,
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
    /** Ovulation date planned before this cycle's signs (settings or learnt). */
    val plannedOvulation: LocalDate,
    /** High readings so far of a rise that is not yet confirmed (0 if none). */
    val risingDays: Int,
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
 *  4. Calendar prediction: the user's own ovulation day when they choose it. Otherwise Ondea's
 *     estimate: the average confirmed ovulation day of the last finished cycles, else the
 *     user's own day, else a typical cycle (average cycle length - luteal phase). The luteal phase is learnt from cycles with a
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
    /** Earliest recent ovulation; the predicted window starts 5 days before it so an early cycle is not missed. */
    private val earliestOvulationIndex: Int
    /** Confirmed ovulation days (0-based) of the last finished cycles, used for prediction. */
    val pastOvulations: List<Int>
    /** Average of [pastOvulations], or null before the first confirmed cycle. */
    val learnedOvulationIndex: Int?
    /** Cycle length minus luteal phase: used when nothing else is known. */
    val typicalOvulationIndex: Int
    /** What Ondea would predict on its own (learnt, else the user's day, else typical). */
    val ondeaOvulationIndex: Int
    /** Where [predictedOvulationIndex] comes from. */
    val predictionBasis: PredictionBasis
    /** Usual low and high temperature, or null before the first cycle with a confirmed rise. */
    val profile: TempProfile?
    /**
     * Days from the mucus peak to ovulation (temperature), learnt from past cycles; 0 by default
     * (ovulation is most often within a day of the mucus peak: Ecochard et al., BJOG 2001).
     */
    val mucusLag: Int
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
            val temps = days.map { e -> e?.takeIf { !it.disturbed }?.tempC }
            // Mucus cannot be observed during bleeding: those observations do not count.
            val mucus = days.map { e -> if ((e?.flow ?: 0) >= 2) 0 else e?.mucus ?: 0 }
            CycleAnalysis(
                start = epochDayToDate(c.start),
                end = end?.let(::epochDayToDate),
                naturalLength = natural,
                shift = ShiftDetector.detect(temps),
                mucus = MucusDetector.analyze(mucus),
                lhPositiveIndex = days.indexOfFirst { it?.lh == 2 }.takeIf { it >= 0 },
                rise = ShiftDetector.rising(temps),
                mucusDays = mucus,
                temps = temps,
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

        // Learn the ovulation day itself from finished cycles with a confirmed temperature rise.
        // The fertile window varies even in regular cycles (Wilcox et al., BMJ 2000), so the
        // predicted window opens 5 days before the earliest recent ovulation.
        // Every finished cycle with a sign counts: temperature first, then LH test, then mucus peak.
        pastOvulations = cycles.filter { it.end != null }.mapNotNull { c ->
            c.shift?.ovulationIndex ?: c.lhPositiveIndex?.plus(1) ?: c.mucus.peakIndex
        }.takeLast(6)
        typicalOvulationIndex = (avgCycleLength - avgLutealLength - 1).coerceAtLeast(settings.periodLength)
        learnedOvulationIndex = pastOvulations.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val own = settings.ovulationDay?.let { it - 1 }
        // What the signs show: learnt from finished cycles, else the day in the settings, else a typical cycle.
        ondeaOvulationIndex = learnedOvulationIndex ?: own ?: typicalOvulationIndex
        // The forecast always uses the day in the settings. Ondea never changes it alone: it suggests (advice()).
        predictedOvulationIndex = own ?: typicalOvulationIndex
        predictionBasis = when {
            predictedOvulationIndex == learnedOvulationIndex -> PredictionBasis.LEARNED
            own != null -> PredictionBasis.OWN_DAY
            else -> PredictionBasis.TYPICAL
        }
        earliestOvulationIndex = if (predictionBasis == PredictionBasis.LEARNED)
            minOf(predictedOvulationIndex, pastOvulations.min())
        else predictedOvulationIndex

        val confirmed = cycles.filter { it.end != null && it.shift != null }.takeLast(6)
        val levels = confirmed.mapNotNull { c ->
            val first = c.shift!!.firstHighIndex
            val low = c.temps.take(first).filterNotNull()
            val high = c.temps.drop(first).filterNotNull()
            if (low.size >= 3 && high.size >= 3) low.average() to high.average() else null
        }
        profile = levels.takeIf { it.isNotEmpty() }?.let { l ->
            TempProfile(round2(l.map { it.first }.average()), round2(l.map { it.second }.average()), l.size)
        }
        val lags = cycles.filter { it.end != null && it.shift != null && it.mucus.peakIndex != null }
            .map { it.shift!!.ovulationIndex - it.mucus.peakIndex!! }.takeLast(6).sorted()
        mucusLag = lags.takeIf { it.isNotEmpty() }?.let { it[it.size / 2].coerceIn(-2, 2) } ?: 0

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
        val todayIndex = days(c.start, today)
        // Best mucus of the last 3 days (creamy or egg white), if any.
        val recentMucus = (todayIndex downTo (todayIndex - 2).coerceAtLeast(0))
            .firstOrNull { (c.mucusDays.getOrNull(it) ?: 0) >= 3 }
        val rise = c.rise?.takeIf { it.lastIndex >= todayIndex - 1 }
        var signEnd: Int? = null
        val (ov, source) = when {
            shift != null -> shift.ovulationIndex to OvulationSource.TEMPERATURE
            // Ovulation follows the LH surge by about 24-36 h (Direito et al., Fertil Steril 2013).
            c.lhPositiveIndex != null -> c.lhPositiveIndex + 1 to OvulationSource.LH_TEST
            m.peakIndex != null -> m.peakIndex to OvulationSource.MUCUS
            !isCurrent -> return null
            // Day by day, the latest signs move the estimate before they are confirmed.
            rise != null -> {
                signEnd = rise.firstHighIndex + 2
                rise.ovulationIndex to OvulationSource.TEMP_RISING
            }
            recentMucus != null -> {
                // Creamy mucus usually comes a day or so before egg white.
                val extra = if (c.mucusDays[recentMucus] == 3) 1 else 0
                val est = maxOf(recentMucus + mucusLag + extra, todayIndex)
                signEnd = maxOf(est + 1, recentMucus + MucusDetector.DAYS_AFTER_PEAK)
                est to OvulationSource.MUCUS_SIGN
            }
            // The calendar day has passed without a sign while the user keeps recording: ovulation may be
            // late this cycle. Without recent records there is nothing to learn from, so the calendar stays.
            todayIndex > predictedOvulationIndex + 1 && recording(c, todayIndex) ->
                minOf(todayIndex, predictedOvulationIndex + MAX_LATE_DAYS) to OvulationSource.LATE
            else -> predictedOvulationIndex to OvulationSource.PREDICTED
        }
        val mucusClose = m.peakIndex?.plus(MucusDetector.DAYS_AFTER_PEAK)
        // A finished cycle with a confirmed rise is closed even if mucus logging stopped early.
        val closed = shift != null && (!m.tracked || mucusClose != null || !isCurrent)
        val end = when {
            shift != null -> maxOf(shift.confirmIndex, mucusClose ?: 0)
            mucusClose != null -> maxOf(mucusClose, ov + 1)
            signEnd != null -> signEnd
            else -> ov + 1
        }
        val earliest = if (source.firm) ov else minOf(earliestOvulationIndex, ov)
        val start = minOf(earliest - 5, ov - 5, m.firstIndex ?: Int.MAX_VALUE).coerceAtLeast(0)
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
        val calendarPeriod = cycle.start.plusDays(avgCycleLength.toLong())
        val nextPeriod = when (w.source) {
            OvulationSource.PREDICTED -> calendarPeriod
            OvulationSource.LATE -> maxOf(calendarPeriod, ovulation.plusDays(avgLutealLength + 1L))
            else -> ovulation.plusDays(avgLutealLength + 1L)
        }
        val todayIndex = cycleDay - 1
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
            plannedOvulation = cycle.start.plusDays(predictedOvulationIndex.toLong()),
            risingDays = cycle.rise?.takeIf { it.lastIndex >= todayIndex - 1 }?.highDays ?: 0,
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
            val predicted = window.source.calendar
            fertile = inFertile && !predicted
            fertilePredicted = inFertile && predicted
            peak = d in window.peakStart..window.ovulationIndex
            ovulation = d == window.ovulationIndex && window.source.firm
            ovulationPredicted = d == window.ovulationIndex && !window.source.firm
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
                if (date.inRange(p.plusDays(earliestOvulationIndex - 5L), ov.plusDays(1))) fertilePredicted = true
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
            profile = profile,
            todayIndex = days(cycle.start, today).takeIf { it in 0 until shown },
        )
    }

    /** A temperature or mucus observation in the last 3 days. */
    private fun recording(c: CycleAnalysis, todayIndex: Int): Boolean =
        ((todayIndex - 2).coerceAtLeast(0)..todayIndex).any { (c.temps.getOrNull(it) != null) || (c.mucusDays.getOrNull(it) ?: 0) > 0 }

    /**
     * After at least one cycle with a sign, Ondea's learnt ovulation day may differ from the day used so far.
     * Shown until the user chooses; it comes back only if the learnt day changes again.
     */
    fun advice(): OvulationAdvice? {
        val learned = (learnedOvulationIndex ?: return null) + 1
        val own = settings.ovulationDay
        val setting = own ?: (typicalOvulationIndex + 1)
        if (kotlin.math.abs(learned - setting) < ADVICE_MIN_DAYS || settings.adviceDismissedDay == learned) return null
        return OvulationAdvice(setting, learned, pastOvulations.size, fromUser = own != null)
    }

    /**
     * In the current cycle, once there is a temperature rise and a mucus peak: do they point to the same
     * ovulation? The mucus side uses the user's usual gap between peak and ovulation ([mucusLag]).
     */
    fun signsCheck(): SignsCheck? {
        val c = current ?: return null
        if (c.end != null) return null
        val todayIndex = days(c.start, today)
        val confirmed = c.shift != null
        val temp = c.shift?.ovulationIndex ?: c.rise?.takeIf { it.lastIndex >= todayIndex - 1 }?.ovulationIndex ?: return null
        val peak = c.mucus.peakIndex ?: return null
        val fromMucus = peak + mucusLag
        val agree = kotlin.math.abs(temp - fromMucus) <= SIGNS_AGREE_DAYS
        val gap = temp - predictedOvulationIndex
        return SignsCheck(
            agree = agree,
            confirmed = confirmed,
            temperature = c.start.plusDays(temp.toLong()),
            mucus = c.start.plusDays(fromMucus.toLong()),
            forecast = c.start.plusDays(predictedOvulationIndex.toLong()),
            forecastGap = if (agree && kotlin.math.abs(gap) >= ADVICE_MIN_DAYS) gap else 0,
        )
    }

    /** Most recent reading, used as the starting value of the temperature input. */
    fun lastTemperature(): Double? = data.entries.lastOrNull { it.tempC != null }?.tempC

    companion object {
        /** A late ovulation estimate moves at most this many days past the planned day. */
        const val MAX_LATE_DAYS = 10
        /** Advice is shown only when the learnt and the set day differ by at least this. */
        const val ADVICE_MIN_DAYS = 2
        /** Temperature and mucus agree when they point to ovulation days at most this far apart. */
        const val SIGNS_AGREE_DAYS = 2

        fun days(from: LocalDate, to: LocalDate): Int = daysBetween(from, to)
    }
}

private fun LocalDate.inRange(from: LocalDate, to: LocalDate) = !isBefore(from) && !isAfter(to)

private fun round2(v: Double) = kotlin.math.round(v * 100) / 100.0

/** Forecast with the learnt day from now on. */
fun Settings.acceptAdvice(a: OvulationAdvice) = copy(ovulationDay = a.learnedDay, adviceDismissedDay = null)

/** Keep the day used so far; the advice comes back only when the learnt day changes. */
fun Settings.keepOwnDay(a: OvulationAdvice) = copy(adviceDismissedDay = a.learnedDay)
