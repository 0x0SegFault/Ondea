package app.ondea.domain

import app.ondea.data.AppData
import app.ondea.data.Cycle
import app.ondea.data.DayEntry
import app.ondea.data.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/** Signs that move the estimate day by day, and what Ondea learns from finished cycles. */
class LearningTest {
    private val start = LocalDate(2026, 11, 1)
    private val settings = Settings(onboarded = true, cycleLength = 28, periodLength = 5)

    private fun day(d: Int) = start.plusDays(d - 1L)
    private fun entry(d: Int, temp: Double? = null, flow: Int = 0, mucus: Int = 0) =
        DayEntry(day(d).toEpochDay(), tempC = temp, flow = flow, mucus = mucus)
    private fun period() = (1..5).map { entry(it, flow = 3) }
    private fun engine(today: Int, entries: List<DayEntry>, s: Settings = settings, starts: List<LocalDate> = listOf(start)) =
        CycleEngine(AppData(settings = s, cycles = starts.map { Cycle(it.toEpochDay()) }, entries = entries), day(today))

    @Test
    fun mucusOnBleedingDaysDoesNotOpenTheWindow() {
        val e = engine(4, period().take(3) + entry(4, flow = 2, mucus = 2))
        assertEquals(Headline.FERTILE_IN, e.todayStatus()!!.headline)
        assertEquals(day(9), e.forecast!!.fertileStart)
    }

    @Test
    fun eggWhiteMovesTheEstimateAndTheRingAgrees() {
        val e = engine(8, period() + entry(8, mucus = 4))
        val f = e.forecast!!
        assertEquals(OvulationSource.MUCUS_SIGN, f.ovulationSource)
        assertEquals(day(8), f.ovulation)
        assertEquals(day(14), f.plannedOvulation)
        val status = e.todayStatus()!!
        assertEquals(Headline.PEAK_FERTILITY, status.headline)
        assertEquals(Detail.MUCUS_MOVED, status.detail)
        // Today is solid fertile (a sign), and the probable ovulation is drawn as an estimate.
        val info = e.dayInfo(day(8))
        assertTrue(info.fertile)
        assertTrue(info.ovulationPredicted)
        assertFalse(info.ovulation)
    }

    @Test
    fun creamyMucusPointsOneDayLater() {
        val f = engine(10, period() + entry(10, mucus = 3)).forecast!!
        assertEquals(day(11), f.ovulation)
        assertEquals(OvulationSource.MUCUS_SIGN, f.ovulationSource)
    }

    @Test
    fun dryMucusDoesNotMoveTheCalendar() {
        val f = engine(8, period() + entry(8, mucus = 1)).forecast!!
        assertEquals(OvulationSource.PREDICTED, f.ovulationSource)
        assertEquals(day(14), f.ovulation)
    }

    @Test
    fun temperatureRiseShowsBeforeItIsConfirmed() {
        val low = (6..15).map { entry(it, temp = 36.40) }
        val e = engine(17, period() + low + entry(16, temp = 36.62) + entry(17, temp = 36.65))
        val f = e.forecast!!
        assertEquals(OvulationSource.TEMP_RISING, f.ovulationSource)
        assertEquals(day(15), f.ovulation)
        assertEquals(2, f.risingDays)
        assertFalse(f.closed)
        val status = e.todayStatus()!!
        assertEquals(Detail.TEMP_RISING, status.detail)
        assertEquals(2, status.detailNumber)
    }

    @Test
    fun oneSmallHighReadingIsNotARise() {
        val low = (6..15).map { entry(it, temp = 36.40) }
        val f = engine(16, period() + low + entry(16, temp = 36.45)).forecast!!
        assertEquals(OvulationSource.LATE, f.ovulationSource)
        assertEquals(0, f.risingDays)
    }

    @Test
    fun lateOvulationMovesWhileTheUserRecords() {
        val low = (6..18).map { entry(it, temp = 36.40) }
        val e = engine(18, period() + low)
        val f = e.forecast!!
        assertEquals(OvulationSource.LATE, f.ovulationSource)
        assertEquals(day(18), f.ovulation)
        assertEquals(Detail.OVULATION_LATE, e.todayStatus()!!.detail)
        assertEquals(listOf(day(14)), e.todayStatus()!!.detailDates)
    }

    @Test
    fun withoutRecordsTheCalendarStays() {
        val f = engine(18, period()).forecast!!
        assertEquals(OvulationSource.PREDICTED, f.ovulationSource)
        assertEquals(day(14), f.ovulation)
    }

    /** Two finished cycles of 31 days with a rise on day 18 (ovulation day 17), then the current one. */
    private fun history(): Pair<List<LocalDate>, List<DayEntry>> {
        val starts = listOf(start, start.plusDays(31), start.plusDays(62))
        val entries = starts.take(2).flatMap { s ->
            (0 until 31).map { i ->
                DayEntry(s.plusDays(i.toLong()).toEpochDay(), tempC = if (i + 1 < 18) 36.35 else 36.75, mucus = if (i + 1 == 16) 4 else if (i + 1 == 17) 2 else 0)
            }
        }
        return starts to entries
    }

    @Test
    fun learnsProfileMucusLagAndAdvisesANewDay() {
        val (starts, entries) = history()
        val own = settings.copy(ovulationDay = 14)
        val e = CycleEngine(AppData(settings = own, cycles = starts.map { Cycle(it.toEpochDay()) }, entries = entries), starts[2].plusDays(2))
        val p = assertNotNull(e.profile)
        assertEquals(36.35, p.lowC)
        assertEquals(36.75, p.highC)
        assertEquals(2, p.cycles)
        // Mucus peak on day 16, ovulation on day 17.
        assertEquals(1, e.mucusLag)
        val a = assertNotNull(e.advice())
        assertEquals(14, a.settingDay)
        assertEquals(17, a.learnedDay)
        assertTrue(a.fromUser)

        val accepted = CycleEngine(e.data.copy(settings = own.acceptAdvice(a)), e.today)
        assertNull(accepted.advice())
        assertEquals(16, accepted.predictedOvulationIndex)

        val kept = CycleEngine(e.data.copy(settings = own.keepOwnDay(a)), e.today)
        assertNull(kept.advice())
        assertEquals(13, kept.predictedOvulationIndex)
    }

    @Test
    fun noAdviceWhenTheLearntDayIsClose() {
        val (starts, entries) = history()
        val own = settings.copy(ovulationDay = 16)
        val e = CycleEngine(AppData(settings = own, cycles = starts.map { Cycle(it.toEpochDay()) }, entries = entries), starts[2].plusDays(2))
        assertNull(e.advice())
    }

    @Test
    fun aCycleWithOnlyAMucusPeakIsLearntToo() {
        val starts = listOf(start, start.plusDays(30))
        val entries = (0 until 30).map { i ->
            DayEntry(start.plusDays(i.toLong()).toEpochDay(), mucus = when (i + 1) { 15 -> 3; 16 -> 4; 17 -> 2; else -> 0 })
        }
        val e = CycleEngine(AppData(settings = settings, cycles = starts.map { Cycle(it.toEpochDay()) }, entries = entries), starts[1].plusDays(1))
        assertEquals(listOf(15), e.pastOvulations)
        // Learnt day 16, typical day 14: Ondea suggests it, the forecast waits for the user.
        assertEquals(PredictionBasis.TYPICAL, e.predictionBasis)
        val a = assertNotNull(e.advice())
        assertEquals(16, a.learnedDay)
        assertEquals(PredictionBasis.LEARNED, CycleEngine(e.data.copy(settings = settings.acceptAdvice(a)), e.today).predictionBasis)
    }
}
