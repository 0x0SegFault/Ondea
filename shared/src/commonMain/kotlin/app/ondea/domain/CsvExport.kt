package app.ondea.domain

import app.ondea.data.AppData
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/** Plain CSV of all entries, e.g. to share with a doctor. Temperatures in °C. */
object CsvExport {
    fun build(data: AppData, today: LocalDate): String {
        val engine = CycleEngine(data, today)
        val sb = StringBuilder("date,cycle_day,temperature_c,disturbed,period_flow,mucus,lh_test,intercourse,note\n")
        for (e in data.entries) {
            val date = epochDayToDate(e.date)
            val cycleDay = engine.cycleIndexOf(date)?.let { CycleEngine.days(engine.cycles[it].start, date) + 1 }
            sb.append(date).append(',')
                .append(cycleDay ?: "").append(',')
                .append(e.tempC?.let(::twoDecimals) ?: "").append(',')
                .append(e.disturbed).append(',')
                .append(FLOW_NAMES.getOrElse(e.flow) { "" }).append(',')
                .append(MUCUS_NAMES.getOrElse(e.mucus) { "" }).append(',')
                .append(LH_NAMES.getOrElse(e.lh) { "" }).append(',')
                .append(e.intercourse).append(',')
                .append(quote(e.note)).append('\n')
        }
        return sb.toString()
    }

    private val FLOW_NAMES = listOf("", "spotting", "light", "medium", "heavy")
    private val LH_NAMES = listOf("", "negative", "positive")
    private val MUCUS_NAMES = listOf("", "dry", "sticky", "creamy", "egg_white")

    /** "36.45": always a dot, always 2 decimals, the same on every platform. */
    internal fun twoDecimals(v: Double): String {
        val hundredths = (v * 100).roundToLong()
        val sign = if (hundredths < 0) "-" else ""
        val a = abs(hundredths)
        return sign + (a / 100) + "." + (a % 100).toString().padStart(2, '0')
    }

    private fun quote(s: String): String =
        if (s.isEmpty()) "" else "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\""
}
