package app.ondea.domain

import app.ondea.data.AppData
import java.time.LocalDate
import java.util.Locale

/** Plain CSV of all entries, e.g. to share with a doctor. Temperatures in °C. */
object CsvExport {
    fun build(data: AppData): String {
        val engine = CycleEngine(data, LocalDate.now())
        val sb = StringBuilder("date,cycle_day,temperature_c,disturbed,period_flow,mucus,lh_test,intercourse,note\n")
        for (e in data.entries) {
            val date = LocalDate.ofEpochDay(e.date)
            val cycleDay = engine.cycleIndexOf(date)?.let { CycleEngine.days(engine.cycles[it].start, date) + 1 }
            sb.append(date).append(',')
                .append(cycleDay ?: "").append(',')
                .append(e.tempC?.let { String.format(Locale.US, "%.2f", it) } ?: "").append(',')
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

    private fun quote(s: String): String =
        if (s.isEmpty()) "" else "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\""
}
