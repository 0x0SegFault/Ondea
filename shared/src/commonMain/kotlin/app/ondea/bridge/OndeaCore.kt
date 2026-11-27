package app.ondea.bridge

import app.ondea.domain.acceptAdvice
import app.ondea.domain.keepOwnDay

import app.ondea.data.AppData
import app.ondea.data.AppDataJson
import app.ondea.data.DayEntry
import app.ondea.data.withCycleStart
import app.ondea.data.withEntry
import app.ondea.data.withTrackingStopped
import app.ondea.domain.CycleEngine
import app.ondea.domain.DayInfo
import app.ondea.domain.FertilityChance
import app.ondea.domain.epochDayToDate
import app.ondea.domain.todayStatus
import app.ondea.domain.toEpochDay
import kotlin.math.roundToInt

/**
 * Entry point for the iOS app (and any other non-Android client).
 * Dates are epoch days: whole days since 1970-01-01. Texts are keys of the shared translations.
 * The iOS app stores [json] encrypted and passes it back on the next launch.
 */
class OndeaCore(json: String?) {
    var data: AppData = json?.takeIf { it.isNotBlank() }?.let { AppDataJson.decode(it.encodeToByteArray()) } ?: AppData()
        private set

    fun json(): String = AppDataJson.encode(data).decodeToString()

    val onboarded: Boolean get() = data.settings.onboarded
    val fahrenheit: Boolean get() = data.settings.fahrenheit
    val appLock: Boolean get() = data.settings.appLock

    fun completeOnboarding(lastPeriod: Long, cycleLength: Int, periodLength: Int, ovulationDay: Int, fahrenheit: Boolean) {
        data = data.copy(
            settings = data.settings.copy(
                onboarded = true,
                cycleLength = cycleLength,
                periodLength = periodLength,
                // 0 means "I don't know": Ondea learns it.
                ovulationDay = ovulationDay.takeIf { it > 0 },
                useOwnOvulationDay = false,
                fahrenheit = fahrenheit,
            ),
            cycles = data.cycles.ifEmpty { listOf(app.ondea.data.Cycle(lastPeriod)) },
        )
    }

    fun setFahrenheit(on: Boolean) { data = data.copy(settings = data.settings.copy(fahrenheit = on)) }
    fun setAppLock(on: Boolean) { data = data.copy(settings = data.settings.copy(appLock = on)) }

    fun entry(day: Long): DayEntry = data.entries.firstOrNull { it.date == day } ?: DayEntry(day)

    private fun edit(day: Long, change: (DayEntry) -> DayEntry) { data = data.withEntry(change(entry(day))) }

    fun setTemperature(day: Long, tempC: Double) = edit(day) { it.copy(tempC = tempC) }
    fun clearTemperature(day: Long) = edit(day) { it.copy(tempC = null) }
    fun setFlow(day: Long, flow: Int) = edit(day) { it.copy(flow = flow) }
    fun setMucus(day: Long, mucus: Int) = edit(day) { it.copy(mucus = mucus) }
    fun setLh(day: Long, lh: Int) = edit(day) { it.copy(lh = lh) }
    fun setIntercourse(day: Long, on: Boolean) = edit(day) { it.copy(intercourse = on) }
    fun setDisturbed(day: Long, on: Boolean) = edit(day) { it.copy(disturbed = on) }
    fun setNote(day: Long, note: String) = edit(day) { it.copy(note = note.trim()) }

    fun startCycle(day: Long) { data = data.withCycleStart(day) }
    fun stopTracking(day: Long) { data = data.withTrackingStopped(day) }

    /** Use Ondea's learnt ovulation day (the advice on the Today screen). */
    fun acceptAdvice(today: Long) {
        val a = CycleEngine(data, epochDayToDate(today)).advice() ?: return
        data = data.copy(settings = data.settings.acceptAdvice(a))
    }

    /** Keep the day used so far. */
    fun keepOwnDay(today: Long) {
        val a = CycleEngine(data, epochDayToDate(today)).advice() ?: return
        data = data.copy(settings = data.settings.keepOwnDay(a))
    }

    /** Most recent temperature in °C, or null. */
    fun lastTemperature(): Double? = data.entries.lastOrNull { it.tempC != null }?.tempC

    fun today(day: Long): TodayView {
        val engine = CycleEngine(data, epochDayToDate(day))
        val f = engine.forecast
        val status = engine.todayStatus()
        if (f == null || status == null || engine.current == null) return TodayView.paused()
        val start = engine.current!!.start
        val length = maxOf(CycleEngine.days(start, f.nextPeriod), f.cycleDay)
        val offset = f.offsetFromOvulation
        val level = if (f.closed && offset > 0) FertilityChance.Level.LOW else FertilityChance.level(offset)
        return TodayView(
            tracking = true,
            cycleDay = f.cycleDay,
            cycleLength = length,
            tone = status.tone.name,
            headlineKey = status.headline.key,
            headlinePlural = status.headline.plural,
            headlineCount = status.headlineCount,
            detailKey = status.detail.key,
            detailNumber = status.detailNumber ?: 0,
            detailDates = status.detailDates.map { it.toEpochDay() },
            sourceKey = status.source.key,
            fertileStart = f.fertileStart.toEpochDay(),
            fertileEnd = f.fertileEnd.toEpochDay(),
            ovulation = f.ovulation.toEpochDay(),
            nextPeriod = f.nextPeriod.toEpochDay(),
            chancePercent = (FertilityChance.probability(offset) * 100).roundToInt(),
            chanceLevel = level.name,
            adviceSettingDay = engine.advice()?.settingDay ?: 0,
            adviceLearnedDay = engine.advice()?.learnedDay ?: 0,
            adviceFromUser = engine.advice()?.fromUser ?: false,
            ringDays = (0 until length).map { i ->
                val date = start.plusDaysCompat(i)
                dayKind(engine.dayInfo(date))
            },
        )
    }

    /** Kind of a calendar day, as a text code (see [dayKind]). */
    fun dayKindOn(day: Long, today: Long): String =
        dayKind(CycleEngine(data, epochDayToDate(today)).dayInfo(epochDayToDate(day)))

    private fun kotlinx.datetime.LocalDate.plusDaysCompat(i: Int) = epochDayToDate(toEpochDay() + i)

    companion object {
        /** Same priority as the Android ring and calendar. */
        fun dayKind(info: DayInfo): String {
            val flow = info.entry?.flow ?: 0
            return when {
                flow > 0 -> "period"
                info.ovulation -> "ovulation"
                info.ovulationPredicted -> "ovulationPredicted"
                info.peak && info.fertile -> "peak"
                info.fertile -> "fertile"
                info.periodPredicted -> "periodPredicted"
                info.peak && info.fertilePredicted -> "peakPredicted"
                info.fertilePredicted -> "fertilePredicted"
                else -> "neutral"
            }
        }
    }
}

/** Everything the iOS Today screen shows. */
data class TodayView(
    val tracking: Boolean,
    val cycleDay: Int,
    val cycleLength: Int,
    /** PERIOD, OVULATION, PEAK, FERTILE or NEUTRAL. */
    val tone: String,
    val headlineKey: String,
    val headlinePlural: Boolean,
    val headlineCount: Int,
    val detailKey: String,
    val detailNumber: Int,
    val detailDates: List<Long>,
    val sourceKey: String,
    val fertileStart: Long,
    val fertileEnd: Long,
    val ovulation: Long,
    val nextPeriod: Long,
    val chancePercent: Int,
    /** HIGH, MEDIUM or LOW. */
    val chanceLevel: String,
    /** Advice: the day used so far and Ondea's learnt day (both 0 when there is no advice). */
    val adviceSettingDay: Int,
    val adviceLearnedDay: Int,
    /** True: [adviceSettingDay] is the user's own day; false: a typical cycle. */
    val adviceFromUser: Boolean,
    /** One kind per cycle day, for the ring. */
    val ringDays: List<String>,
) {
    companion object {
        fun paused() = TodayView(false, 0, 0, "NEUTRAL", "not_tracking_title", false, 0, "not_tracking_text", 0,
            emptyList(), "", 0, 0, 0, 0, 0, "LOW", 0, 0, false, emptyList())
    }
}
