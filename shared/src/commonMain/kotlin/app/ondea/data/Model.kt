package app.ondea.data

import kotlinx.serialization.Serializable

/** Everything the app knows. Stored as one encrypted document; dates are epoch days. */
@Serializable
data class AppData(
    val version: Int = 1,
    val settings: Settings = Settings(),
    val cycles: List<Cycle> = emptyList(),
    val entries: List<DayEntry> = emptyList(),
)

@Serializable
data class Settings(
    val onboarded: Boolean = false,
    /** Cycle length the user declared; used until enough cycles have been recorded. */
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    /** Cycle day on which the user usually ovulates, if known. Kept when the user lets Ondea predict. */
    val ovulationDay: Int? = null,
    /** True: always predict with [ovulationDay]. False: Ondea learns. Null (older data): own day if set. */
    val useOwnOvulationDay: Boolean? = null,
    val fahrenheit: Boolean = false,
    val appLock: Boolean = false,
    val secureScreen: Boolean = true,
)

/**
 * A cycle starts on the first day of a period. It ends the day before the next cycle starts,
 * or on [end] when the user paused tracking.
 */
@Serializable
data class Cycle(
    val start: Long,
    val end: Long? = null,
)

@Serializable
data class DayEntry(
    val date: Long,
    /** Basal body temperature, always stored in °C. */
    val tempC: Double? = null,
    /** 0 none, 1 spotting, 2 light, 3 medium, 4 heavy. */
    val flow: Int = 0,
    /** 0 not observed, 1 dry, 2 sticky, 3 creamy, 4 egg white. */
    val mucus: Int = 0,
    /** Reading may be off (illness, alcohol, short sleep, measured late); excluded from analysis. */
    val disturbed: Boolean = false,
    val intercourse: Boolean = false,
    /** Ovulation (LH) test: 0 not done, 1 negative, 2 positive. */
    val lh: Int = 0,
    val note: String = "",
) {
    val isEmpty: Boolean
        get() = tempC == null && flow == 0 && mucus == 0 && lh == 0 && !disturbed && !intercourse && note.isBlank()
}

fun AppData.withEntry(entry: DayEntry): AppData {
    val others = entries.filter { it.date != entry.date }
    val updated = if (entry.isEmpty) others else others + entry
    return copy(entries = updated.sortedBy { it.date })
}

fun AppData.withCycleStart(day: Long): AppData {
    val others = cycles
        .filter { it.start != day }
        // A paused cycle that overlaps the new start now simply ends where the new one begins.
        .map { if (it.start < day && it.end != null && it.end >= day) it.copy(end = null) else it }
    return copy(cycles = (others + Cycle(day)).sortedBy { it.start })
}

fun AppData.withoutCycleStart(day: Long): AppData =
    copy(cycles = cycles.filter { it.start != day })

/** Ends the ongoing cycle on [day] without starting a new one. */
fun AppData.withTrackingStopped(day: Long): AppData {
    val last = cycles.maxByOrNull { it.start } ?: return this
    if (last.end != null || last.start > day) return this
    return copy(cycles = cycles.map { if (it == last) it.copy(end = day) else it })
}

/** Whether predictions use the user's own ovulation day rather than Ondea's learnt estimate. */
val Settings.usesOwnOvulationDay: Boolean
    get() = ovulationDay != null && (useOwnOvulationDay ?: true)
