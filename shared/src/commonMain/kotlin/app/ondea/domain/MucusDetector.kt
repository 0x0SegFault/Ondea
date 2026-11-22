package app.ondea.domain

/**
 * Cervical mucus observations of one cycle, by 0-based cycle day.
 * Values: 0 not observed, 1 dry, 2 sticky, 3 creamy, 4 egg white.
 */
data class MucusSign(
    /** At least one observation was recorded this cycle. */
    val tracked: Boolean,
    /** First day with sticky or better mucus: the fertile window opens at the latest here. */
    val firstIndex: Int?,
    /**
     * Peak day: the last day of the best mucus quality of the cycle, identified once a
     * later observation shows lower quality. Ovulation usually happens within a day or two of it.
     */
    val peakIndex: Int?,
)

/**
 * Mucus part of the symptothermal "double check" (Sensiplan):
 * the fertile window opens with the first sign of mucus and closes only when both
 * the temperature rise is confirmed and the 3rd day after the mucus peak is reached.
 */
object MucusDetector {
    const val DAYS_AFTER_PEAK = 3

    fun analyze(mucus: List<Int>): MucusSign {
        if (mucus.none { it > 0 }) return MucusSign(tracked = false, firstIndex = null, peakIndex = null)
        val first = mucus.indexOfFirst { it >= 2 }.takeIf { it >= 0 }
        val best = mucus.max()
        var peak: Int? = null
        if (best >= 2) {
            val last = mucus.indexOfLast { it == best }
            // Confirmed only when a later observation is of lower quality.
            if ((last + 1 until mucus.size).any { mucus[it] in 1 until best }) peak = last
        }
        return MucusSign(tracked = true, firstIndex = first, peakIndex = peak)
    }
}
