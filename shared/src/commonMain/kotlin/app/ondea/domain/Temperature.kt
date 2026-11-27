package app.ondea.domain

import kotlin.math.roundToLong

object Temperature {
    val VALID_RANGE_C = 34.0..42.0

    fun cToF(c: Double) = c * 9.0 / 5.0 + 32.0
    fun fToC(f: Double) = (f - 32.0) * 5.0 / 9.0

    fun toDisplay(c: Double, fahrenheit: Boolean) = if (fahrenheit) cToF(c) else c
    fun fromDisplay(v: Double, fahrenheit: Boolean) = round3(if (fahrenheit) fToC(v) else v)


    fun unit(fahrenheit: Boolean) = if (fahrenheit) "°F" else "°C"

    /** Step used by the +/- buttons, in display units: the resolution of a basal thermometer. */
    fun step(fahrenheit: Boolean) = if (fahrenheit) 0.02 else 0.01

    /** Parses user input in display units (accepts "36.5" and "36,5"); returns °C or null if implausible. */
    fun parse(text: String, fahrenheit: Boolean): Double? {
        val v = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return fromDisplay(v, fahrenheit).takeIf { it in VALID_RANGE_C }
    }

    /** Whole degrees offered by the digit wheels, in display units (typing still accepts the full valid range). */
    fun wholeRange(fahrenheit: Boolean): IntRange = if (fahrenheit) 95..101 else 35..38

    /** A reading in hundredths of the display unit, e.g. 36.47 °C -> 3647. */
    fun toHundredths(c: Double, fahrenheit: Boolean): Int = kotlin.math.round(toDisplay(c, fahrenheit) * 100).toInt()

    /** Inverse of [toHundredths], in °C. */
    fun fromHundredths(h: Int, fahrenheit: Boolean): Double = fromDisplay(h / 100.0, fahrenheit)

    /** The three wheels: whole degrees, tenths, hundredths, with the whole part kept in [wholeRange]. */
    fun digits(h: Int, fahrenheit: Boolean): Triple<Int, Int, Int> {
        val range = wholeRange(fahrenheit)
        val clamped = h.coerceIn(range.first * 100, range.last * 100 + 99)
        return Triple(clamped / 100, clamped / 10 % 10, clamped % 10)
    }

    fun fromDigits(whole: Int, tenths: Int, hundredths: Int): Int = whole * 100 + tenths * 10 + hundredths

    private fun round3(v: Double) = (v * 1000).roundToLong() / 1000.0
}
