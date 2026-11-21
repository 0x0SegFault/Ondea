package app.ondea.domain

import app.ondea.data.AppData
import app.ondea.data.Cycle
import app.ondea.data.DayEntry
import app.ondea.data.Settings
import app.ondea.data.withCycleStart
import app.ondea.data.withTrackingStopped
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleEngineTest {
    private val start = LocalDate.of(2026, 10, 1)
    private val settings = Settings(onboarded = true, cycleLength = 28, periodLength = 5)

    private fun data(vararg starts: LocalDate, entries: List<DayEntry> = emptyList()) =
        AppData(settings = settings, cycles = starts.map { Cycle(it.toEpochDay()) }, entries = entries)

    /** Low phase then a rise starting on cycle day [riseDay] (1-based). */
    private fun temps(cycleStart: LocalDate, days: Int, riseDay: Int) = (0 until days).map { i ->
        DayEntry(cycleStart.plusDays(i.toLong()).toEpochDay(), tempC = if (i + 1 < riseDay) 36.40 else 36.75)
    }

    @Test
    fun `without history, prediction uses declared length and 14 day luteal phase`() {
        val e = CycleEngine(data(start), start.plusDays(4))
        val f = e.forecast!!
        assertEquals(5, f.cycleDay)
        assertEquals(start.plusDays(13), f.ovulation) // cycle day 14
        assertEquals(start.plusDays(8), f.fertileStart)
        assertEquals(start.plusDays(14), f.fertileEnd)
        assertEquals(start.plusDays(28), f.nextPeriod)
        assertEquals(OvulationSource.PREDICTED, f.ovulationSource)
        assertEquals(0, f.daysLate)
    }

    @Test
    fun `confirmed shift moves ovulation and ends the fertile window`() {
        val e = CycleEngine(data(start, entries = temps(start, 20, riseDay = 17)), start.plusDays(19))
        val f = e.forecast!!
        assertTrue(f.closed)
        assertEquals(OvulationSource.TEMPERATURE, f.ovulationSource)
        assertEquals(start.plusDays(15), f.ovulation) // day before first high (day 17)
        assertEquals(start.plusDays(18), f.fertileEnd) // third high reading
        assertEquals(start.plusDays(30), f.nextPeriod)
        assertEquals(4, f.highDays)
    }

    @Test
    fun `history drives average length and luteal phase`() {
        val c1 = start
        val c2 = c1.plusDays(30)
        val c3 = c2.plusDays(32)
        val entries = temps(c1, 30, riseDay = 19) + temps(c2, 32, riseDay = 21)
        val e = CycleEngine(data(c1, c2, c3, entries = entries), c3.plusDays(2))
        assertEquals(31, e.avgCycleLength)
        assertEquals(12, e.avgLutealLength) // 30 - 18 and 32 - 20
        assertEquals(c3.plusDays(31L - 12 - 1), e.forecast!!.ovulation)
    }

    @Test
    fun `late period is reported`() {
        val e = CycleEngine(data(start), start.plusDays(31))
        assertEquals(3, e.forecast!!.daysLate)
    }

    @Test
    fun `paused tracking has no forecast and starting again resumes`() {
        val paused = data(start).withTrackingStopped(start.plusDays(10).toEpochDay())
        assertNull(CycleEngine(paused, start.plusDays(12)).forecast)
        val resumed = paused.withCycleStart(start.plusDays(40).toEpochDay())
        val e = CycleEngine(resumed, start.plusDays(41))
        assertNotNull(e.forecast)
        assertEquals(2, e.forecast!!.cycleDay)
        assertNull(e.cycles.first().naturalLength) // paused cycle doesn't count toward averages
    }

    @Test
    fun `calendar marks fertile and predicted days`() {
        val e = CycleEngine(data(start), start.plusDays(4))
        assertTrue(e.dayInfo(start.plusDays(10)).fertilePredicted)
        assertTrue(e.dayInfo(start.plusDays(13)).ovulationPredicted)
        assertTrue(e.dayInfo(start.plusDays(28)).periodPredicted)
        assertFalse(e.dayInfo(start.plusDays(20)).fertilePredicted)
    }

    @Test
    fun `chart covers at least the average cycle`() {
        val e = CycleEngine(data(start, entries = temps(start, 5, riseDay = 99)), start.plusDays(4))
        val chart = e.chart(0)
        assertEquals(28, chart.days)
        assertEquals(4, chart.todayIndex)
        assertEquals(36.40, chart.temps[0]!!, 1e-9)
    }

    private fun withMucus(entries: List<DayEntry>, vararg mucus: Pair<Int, Int>): List<DayEntry> {
        val byDay = mucus.toMap()
        return entries.map { e ->
            val i = (e.date - start.toEpochDay()).toInt()
            byDay[i]?.let { e.copy(mucus = it) } ?: e
        }
    }

    @Test
    fun `usual ovulation day setting drives the prediction`() {
        val d = data(start).copy(settings = settings.copy(ovulationDay = 17))
        val f = CycleEngine(d, start.plusDays(4)).forecast!!
        assertEquals(start.plusDays(16), f.ovulation)
        assertEquals(start.plusDays(11), f.fertileStart)
    }

    @Test
    fun `first mucus opens the fertile window early`() {
        val entries = withMucus(temps(start, 8, riseDay = 99), 7 to 2)
        val f = CycleEngine(data(start, entries = entries), start.plusDays(7)).forecast!!
        assertEquals(start.plusDays(7), f.fertileStart) // predicted start would be day index 8
    }

    @Test
    fun `mucus peak estimates ovulation before the temperature rises`() {
        val entries = withMucus(temps(start, 15, riseDay = 99), 10 to 2, 11 to 3, 12 to 4, 13 to 4, 14 to 2)
        val f = CycleEngine(data(start, entries = entries), start.plusDays(14)).forecast!!
        assertEquals(OvulationSource.MUCUS, f.ovulationSource)
        assertEquals(start.plusDays(13), f.ovulation)
        assertEquals(start.plusDays(16), f.fertileEnd) // 3rd day after peak
        assertEquals(Waiting.TEMPERATURE, f.window.waitingFor)
    }

    @Test
    fun `double check - fertile phase ends at the later of temperature and mucus`() {
        // Temperature confirms on index 18; mucus peak on index 17 -> closes on index 20.
        val base = temps(start, 21, riseDay = 17)
        val waiting = withMucus(base.take(19), 12 to 3, 13 to 4, 14 to 4, 15 to 4, 16 to 4, 17 to 4)
        val w1 = CycleEngine(data(start, entries = waiting), start.plusDays(18)).forecast!!
        assertFalse(w1.closed)
        assertEquals(Waiting.MUCUS, w1.window.waitingFor)

        val done = withMucus(base, 12 to 3, 13 to 4, 14 to 4, 15 to 4, 16 to 4, 17 to 4, 18 to 2, 19 to 1)
        val f = CycleEngine(data(start, entries = done), start.plusDays(20)).forecast!!
        assertTrue(f.closed)
        assertEquals(start.plusDays(20), f.fertileEnd)
        assertEquals(start.plusDays(15), f.ovulation) // temperature wins for the estimate
    }
}
