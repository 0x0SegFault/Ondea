package app.ondea.bridge

import app.ondea.domain.toEpochDay
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OndeaCoreTest {
    private val start = LocalDate(2026, 10, 1).toEpochDay()

    @Test
    fun onboardingThenTodayAndJsonRoundTrip() {
        val core = OndeaCore(null)
        assertFalse(core.onboarded)
        core.completeOnboarding(start, cycleLength = 28, periodLength = 5, ovulationDay = 0, fahrenheit = false)
        core.setTemperature(start + 4, 36.42)
        core.setMucus(start + 4, 1) // dry: no sign yet

        val t = core.today(start + 4)
        assertTrue(t.tracking)
        assertEquals(5, t.cycleDay)
        assertEquals("fertile_in", t.headlineKey)
        assertTrue(t.headlinePlural)
        assertEquals(28, t.ringDays.size)
        assertEquals("neutral", t.ringDays[4])

        val again = OndeaCore(core.json())
        assertEquals(36.42, again.entry(start + 4).tempC)
        assertEquals(1, again.entry(start + 4).mucus)
    }

    @Test
    fun recordedPeriodShowsOnTheRing() {
        val core = OndeaCore(null)
        core.completeOnboarding(start, 28, 5, 0, false)
        core.setFlow(start, 3)
        assertEquals("period", core.today(start + 1).ringDays[0])
    }

    @Test
    fun pausedTrackingReturnsThePausedView() {
        val core = OndeaCore(null)
        core.completeOnboarding(start, 28, 5, 0, false)
        core.stopTracking(start + 2)
        val t = core.today(start + 5)
        assertFalse(t.tracking)
        assertEquals("not_tracking_title", t.headlineKey)
    }

    @Test
    fun emptyEditsRemoveTheDay() {
        val core = OndeaCore(null)
        core.completeOnboarding(start, 28, 5, 0, false)
        core.setTemperature(start, 36.5)
        core.clearTemperature(start)
        assertTrue(core.data.entries.isEmpty())
    }

    @Test
    fun stickyMucusOpensTheFertileWindowEarly() {
        val core = OndeaCore(null)
        core.completeOnboarding(start, 28, 5, 0, false)
        core.setMucus(start + 4, 2)
        assertEquals("fertile_window", core.today(start + 4).headlineKey)
    }
}
