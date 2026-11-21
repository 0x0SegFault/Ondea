package app.ondea.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MucusDetectorTest {
    @Test
    fun `nothing observed`() {
        assertFalse(MucusDetector.analyze(listOf(0, 0, 0)).tracked)
    }

    @Test
    fun `peak is the last best day once quality drops`() {
        val m = MucusDetector.analyze(listOf(0, 1, 1, 2, 3, 4, 4, 2, 1))
        assertEquals(3, m.firstIndex)
        assertEquals(6, m.peakIndex)
    }

    @Test
    fun `peak is unknown while quality is still at its best`() {
        assertNull(MucusDetector.analyze(listOf(1, 2, 3, 4, 4)).peakIndex)
    }

    @Test
    fun `best quality returning after a drop moves the peak`() {
        assertNull(MucusDetector.analyze(listOf(2, 4, 2, 4)).peakIndex)
        assertEquals(3, MucusDetector.analyze(listOf(2, 4, 2, 4, 1)).peakIndex)
    }

    @Test
    fun `dry only gives no peak`() {
        val m = MucusDetector.analyze(listOf(1, 1, 1))
        assertNull(m.firstIndex)
        assertNull(m.peakIndex)
    }
}

class FertilityChanceTest {
    @Test
    fun `wilcox table`() {
        assertEquals(0.33, FertilityChance.probability(0), 1e-9)
        assertEquals(0.10, FertilityChance.probability(-5), 1e-9)
        assertEquals(0.0, FertilityChance.probability(1), 1e-9)
        assertEquals(0.0, FertilityChance.probability(-6), 1e-9)
        assertEquals(FertilityChance.Level.HIGH, FertilityChance.level(-2))
        assertEquals(FertilityChance.Level.MEDIUM, FertilityChance.level(-4))
        assertEquals(FertilityChance.Level.LOW, FertilityChance.level(2))
    }
}
