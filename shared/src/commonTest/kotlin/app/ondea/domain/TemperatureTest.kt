package app.ondea.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class TemperatureTest {
    @Test
    fun parsesBothDecimalSeparatorsAndConvertsFahrenheit() {
        assertEquals(36.55, Temperature.parse("36,55", false)!!, absoluteTolerance = 1e-9)
        assertEquals(36.5, Temperature.parse("97.7", true)!!, absoluteTolerance = 1e-3)
    }

    @Test
    fun rejectsImplausibleValues() {
        assertNull(Temperature.parse("3.65", false))
        assertNull(Temperature.parse("abc", false))
        assertNull(Temperature.parse("36.5", true))
    }

    @Test
    fun digitWheels() {
        val h = Temperature.toHundredths(36.47, fahrenheit = false)
        assertEquals(3647, h)
        assertEquals(Triple(36, 4, 7), Temperature.digits(h, fahrenheit = false))
        assertEquals(36.47, Temperature.fromHundredths(Temperature.fromDigits(36, 4, 7), fahrenheit = false))
        // Out of the wheel range: clamped to the nearest value the wheels can show.
        assertEquals(Triple(35, 0, 0), Temperature.digits(3412, fahrenheit = false))
        assertEquals(Triple(97, 7, 0), Temperature.digits(Temperature.toHundredths(36.5, fahrenheit = true), fahrenheit = true))
    }
}
