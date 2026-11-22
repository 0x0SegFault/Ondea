package app.ondea.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.Test

class MucusDetectorTest {
    @Test
    fun nothingObserved() {
        assertFalse(MucusDetector.analyze(listOf(0, 0, 0)).tracked)
    }

    @Test
    fun peakIsTheLastBestDayOnceQualityDrops() {
        val m = MucusDetector.analyze(listOf(0, 1, 1, 2, 3, 4, 4, 2, 1))
        assertEquals(3, m.firstIndex)
        assertEquals(6, m.peakIndex)
    }

    @Test
    fun peakIsUnknownWhileQualityIsStillAtItsBest() {
        assertNull(MucusDetector.analyze(listOf(1, 2, 3, 4, 4)).peakIndex)
    }

    @Test
    fun bestQualityReturningAfterADropMovesThePeak() {
        assertNull(MucusDetector.analyze(listOf(2, 4, 2, 4)).peakIndex)
        assertEquals(3, MucusDetector.analyze(listOf(2, 4, 2, 4, 1)).peakIndex)
    }

    @Test
    fun dryOnlyGivesNoPeak() {
        val m = MucusDetector.analyze(listOf(1, 1, 1))
        assertNull(m.firstIndex)
        assertNull(m.peakIndex)
    }
}

class FertilityChanceTest {
    @Test
    fun wilcoxTable() {
        assertEquals(0.33, FertilityChance.probability(0), absoluteTolerance = 1e-9)
        assertEquals(0.10, FertilityChance.probability(-5), absoluteTolerance = 1e-9)
        assertEquals(0.0, FertilityChance.probability(1), absoluteTolerance = 1e-9)
        assertEquals(0.0, FertilityChance.probability(-6), absoluteTolerance = 1e-9)
        assertEquals(FertilityChance.Level.HIGH, FertilityChance.level(-2))
        assertEquals(FertilityChance.Level.MEDIUM, FertilityChance.level(-4))
        assertEquals(FertilityChance.Level.LOW, FertilityChance.level(2))
    }
}
