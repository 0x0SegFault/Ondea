package app.ondea.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class ShiftDetectorTest {
    private val low = listOf(36.40, 36.35, 36.45, 36.30, 36.40, 36.50, 36.35, 36.40, 36.45, 36.40, 36.35, 36.45, 36.40)

    @Test
    fun classicShiftIsDetected() {
        // Coverline = max of days 7..12 (indices 7..12) = 36.45; highs from index 13.
        val temps: List<Double?> = low + listOf(36.55, 36.60, 36.70, 36.75, 36.70)
        val shift = ShiftDetector.detect(temps)!!
        assertEquals(13, shift.firstHighIndex)
        assertEquals(15, shift.confirmIndex)
        assertEquals(12, shift.ovulationIndex)
        assertEquals(36.45, shift.coverlineC, absoluteTolerance = 1e-9)
    }

    @Test
    fun thirdReadingBelow02RiseNeedsAFourth() {
        val pending: List<Double?> = low + listOf(36.50, 36.55, 36.60)
        assertNull(ShiftDetector.detect(pending))
        val shift = ShiftDetector.detect(pending + 36.55)!!
        assertEquals(13, shift.firstHighIndex)
        assertEquals(16, shift.confirmIndex)
    }

    @Test
    fun fourthReadingFallingBackCancelsTheException() {
        assertNull(ShiftDetector.detect(low + listOf(36.50, 36.55, 36.60, 36.40)))
    }

    @Test
    fun disturbedAndMissingDaysAreSkipped() {
        val temps: List<Double?> = low + listOf(36.55, null, 36.60, null, 36.70)
        val shift = ShiftDetector.detect(temps)!!
        assertEquals(13, shift.firstHighIndex)
        assertEquals(17, shift.confirmIndex)
    }

    @Test
    fun flatOrTooFewReadingsGiveNoShift() {
        assertNull(ShiftDetector.detect(low))
        assertNull(ShiftDetector.detect(listOf(36.4, 36.4, 36.9, 36.9, 36.9)))
    }

    @Test
    fun equalToCoverlineIsNotHigh() {
        assertNull(ShiftDetector.detect(low + listOf(36.45, 36.70, 36.70)))
    }
}
