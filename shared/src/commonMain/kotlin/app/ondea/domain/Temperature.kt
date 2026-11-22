package app.ondea.domain

import kotlin.math.roundToLong

object Temperature {
    val VALID_RANGE_C = 34.0..42.0

    fun cToF(c: Double) = c * 9.0 / 5.0 + 32.0
    fun fToC(f: Double) = (f - 32.0) * 5.0 / 9.0

    fun toDisplay(c: Double, fahrenheit: Boolean) = if (fahrenheit) cToF(c) else c
    fun fromDisplay(v: Double, fahrenheit: Boolean) = round3(if (fahrenheit) fToC(v) else v)


    fun unit(fahrenheit: Boolean) = if (fahrenheit) "°F" else "°C"

    /** Step used by the +/- buttons, in display units. */
    fun step(fahrenheit: Boolean) = if (fahrenheit) 0.1 else 0.05

    /** Parses user input in display units (accepts "36.5" and "36,5"); returns °C or null if implausible. */
    fun parse(text: String, fahrenheit: Boolean): Double? {
        val v = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return fromDisplay(v, fahrenheit).takeIf { it in VALID_RANGE_C }
    }

    private fun round3(v: Double) = (v * 1000).roundToLong() / 1000.0
}
