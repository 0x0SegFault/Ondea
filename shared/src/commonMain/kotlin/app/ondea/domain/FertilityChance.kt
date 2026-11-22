package app.ondea.domain

/**
 * Probability of conception from intercourse on a single day, relative to ovulation.
 * Wilcox A.J., Weinberg C.R., Baird D.D. "Timing of sexual intercourse in relation to ovulation."
 * N Engl J Med 1995;333:1517-21 (221 women, 625 cycles; ovulation dated by urinary hormones).
 * No pregnancies were attributed to intercourse after the day of ovulation.
 */
object FertilityChance {
    /** Index 0 = 5 days before ovulation, index 5 = ovulation day. */
    val BY_DAY = listOf(0.10, 0.16, 0.14, 0.27, 0.31, 0.33)

    enum class Level { LOW, MEDIUM, HIGH }

    /** [offset] = day minus (estimated) ovulation day. */
    fun probability(offset: Int): Double = BY_DAY.getOrNull(offset + 5) ?: 0.0

    fun level(offset: Int): Level = when (offset) {
        in -2..0 -> Level.HIGH
        in -5..-3 -> Level.MEDIUM
        else -> Level.LOW
    }
}
