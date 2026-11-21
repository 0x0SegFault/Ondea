package app.ondea.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShiftDetectorTest {
    private val low = listOf(36.40, 36.35, 36.45, 36.30, 36.40, 36.50, 36.35, 36.40, 36.45, 36.40, 36.35, 36.45, 36.40)

    @Test
    fun `classic shift is detected`() {
        // Coverline = max of days 7..12 (indices 7..12) = 36.45; highs from index 13.
        val temps: List<Double?> = low + listOf(36.55, 36.60, 36.70, 36.75, 36.70)
        val shift = ShiftDetector.detect(temps)!!
        assertEquals(13, shift.firstHighIndex)
        assertEquals(15, shift.confirmIndex)
        assertEquals(12, shift.ovulationIndex)
        assertEquals(36.45, shift.coverlineC, 1e-9)
    }

    @Test
    fun `third reading below 0_2 rise needs a fourth`() {
        val pending: List<Double?> = low + listOf(36.50, 36.55, 36.60)
        assertNull(ShiftDetector.detect(pending))
        val shift = ShiftDetector.detect(pending + 36.55)!!
        assertEquals(13, shift.firstHighIndex)
        assertEquals(16, shift.confirmIndex)
    }

    @Test
    fun `fourth reading falling back cancels the exception`() {
        assertNull(ShiftDetector.detect(low + listOf(36.50, 36.55, 36.60, 36.40)))
    }

    @Test
    fun `disturbed and missing days are skipped`() {
        val temps: List<Double?> = low + listOf(36.55, null, 36.60, null, 36.70)
        val shift = ShiftDetector.detect(temps)!!
        assertEquals(13, shift.firstHighIndex)
        assertEquals(17, shift.confirmIndex)
    }

    @Test
    fun `flat or too few readings give no shift`() {
        assertNull(ShiftDetector.detect(low))
        assertNull(ShiftDetector.detect(listOf(36.4, 36.4, 36.9, 36.9, 36.9)))
    }

    @Test
    fun `equal to coverline is not high`() {
        assertNull(ShiftDetector.detect(low + listOf(36.45, 36.70, 36.70)))
    }
}
