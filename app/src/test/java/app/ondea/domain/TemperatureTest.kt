package app.ondea.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemperatureTest {
    @Test
    fun `parses both decimal separators and converts fahrenheit`() {
        assertEquals(36.55, Temperature.parse("36,55", false)!!, 1e-9)
        assertEquals(36.5, Temperature.parse("97.7", true)!!, 1e-3)
    }

    @Test
    fun `rejects implausible values`() {
        assertNull(Temperature.parse("3.65", false))
        assertNull(Temperature.parse("abc", false))
        assertNull(Temperature.parse("36.5", true))
    }
}
