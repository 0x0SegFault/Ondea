package app.ondea.domain

/**
 * A confirmed post-ovulatory temperature rise.
 * Indices are 0-based cycle days (index 0 = cycle day 1).
 */
data class Shift(
    /** First day whose temperature is above the coverline. */
    val firstHighIndex: Int,
    /** Day on which the rise is confirmed; the fertile window is over from that evening. */
    val confirmIndex: Int,
    /** Highest of the six readings before the rise, in °C. */
    val coverlineC: Double,
) {
    /** Ovulation most commonly happens the day before the first high reading. */
    val ovulationIndex: Int get() = (firstHighIndex - 1).coerceAtLeast(0)
}

/**
 * Detects the temperature shift with the "3 over 6" rule used by symptothermal methods
 * (e.g. Sensiplan / NFP):
 *
 *  - The coverline is the highest of the 6 valid readings before a candidate day.
 *  - 3 consecutive valid readings must be strictly above the coverline,
 *    and the 3rd must be at least 0.2 °C above it.
 *  - Exception: if the 3rd is above the coverline but by less than 0.2 °C,
 *    a 4th reading above the coverline confirms the shift.
 *
 * Missing days and readings marked as disturbed are skipped (they are passed as null).
 */
object ShiftDetector {
    const val MIN_RISE_C = 0.2
    private const val EPS = 1e-6

    fun detect(temps: List<Double?>): Shift? {
        val valid = temps.withIndex().mapNotNull { (i, t) -> t?.let { i to it } }
        for (i in 6 until valid.size - 2) {
            val coverline = (i - 6 until i).maxOf { valid[it].second }
            fun high(k: Int) = valid[k].second > coverline + EPS
            if (!high(i) || !high(i + 1) || !high(i + 2)) continue
            if (valid[i + 2].second >= coverline + MIN_RISE_C - EPS) {
                return Shift(valid[i].first, valid[i + 2].first, coverline)
            }
            if (i + 3 < valid.size && high(i + 3)) {
                return Shift(valid[i].first, valid[i + 3].first, coverline)
            }
        }
        return null
    }
}
