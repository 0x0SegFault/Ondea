package app.ondea.domain

import app.ondea.data.AppData
import app.ondea.data.Cycle
import app.ondea.data.DayEntry
import app.ondea.data.Settings
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodayStatusTest {
    private val start = LocalDate(2026, 10, 1)
    private val settings = Settings(onboarded = true, cycleLength = 28, periodLength = 5)

    private fun engine(today: LocalDate, entries: List<DayEntry> = emptyList()) =
        CycleEngine(AppData(settings = settings, cycles = listOf(Cycle(start.toEpochDay())), entries = entries), today)

    private fun temps(days: Int, riseDay: Int) = (0 until days).map { i ->
        DayEntry(start.plusDays(i).toEpochDay(), tempC = if (i + 1 < riseDay) 36.40 else 36.75)
    }

    @Test
    fun beforeTheWindowCountsDaysAndPredicts() {
        val s = engine(start.plusDays(4)).todayStatus()!!
        assertEquals(Headline.FERTILE_IN, s.headline)
        assertEquals(4, s.headlineCount) // window opens on index 8
        assertEquals(Detail.OVULATION_EXPECTED, s.detail)
        assertEquals(listOf(start.plusDays(13)), s.detailDates)
        assertEquals(Source.TYPICAL, s.source)
    }

    @Test
    fun latePeriod() {
        val s = engine(start.plusDays(30)).todayStatus()!!
        assertEquals(Headline.PERIOD_LATE, s.headline)
        assertEquals(2, s.headlineCount)
        assertEquals(Detail.LATE_HINT, s.detail)
    }

    @Test
    fun confirmedOvulationAfterTheWindow() {
        val s = engine(start.plusDays(20), temps(21, riseDay = 17)).todayStatus()!!
        assertEquals(Headline.OVULATION_CONFIRMED, s.headline)
        assertEquals(Detail.OVULATED_ON, s.detail)
        assertEquals(StatusTone.OVULATION, s.tone)
        assertEquals(Source.TEMPERATURE, s.source)
        assertEquals(2, s.detailDates.size)
    }

    @Test
    fun eggWhiteMucusMeansPeak() {
        val today = start.plusDays(10)
        val s = engine(today, listOf(DayEntry(today.toEpochDay(), mucus = 4))).todayStatus()!!
        assertEquals(Headline.PEAK_FERTILITY, s.headline)
        // Egg white on day 11 moves the estimate from the calendar day (14) to today.
        assertEquals(Detail.MUCUS_MOVED, s.detail)
        assertEquals(listOf(today, start.plusDays(13)), s.detailDates)
        assertEquals(Source.MUCUS_SIGN, s.source)
    }

    @Test
    fun pausedTrackingHasNoStatus() {
        val paused = AppData(settings = settings, cycles = listOf(Cycle(start.toEpochDay(), end = start.plusDays(3).toEpochDay())))
        assertNull(CycleEngine(paused, start.plusDays(5)).todayStatus())
    }
}
