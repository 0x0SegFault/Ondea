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
}
