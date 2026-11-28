package app.ondea.bridge

import app.ondea.data.AppData

/**
 * Three sample cycles ending [today] (epoch day): two finished cycles with a temperature rise and mucus,
 * and the current one on day 17, with a mucus peak on day 14 and the temperature rising since day 16.
 * For screenshots and tests only; never saved.
 */
object DemoData {
    fun appData(today: Long): AppData {
        val core = OndeaCore(null)
        val start = today - 16 - 58
        core.completeOnboarding(start, cycleLength = 28, periodLength = 5, ovulationDay = 0, fahrenheit = false)
        val starts = listOf(start, start + 29, start + 58)
        starts.forEachIndexed { i, cycleStart ->
            if (i > 0) core.startCycle(cycleStart)
            val end = if (i + 1 < starts.size) starts[i + 1] else today + 1
            for (day in cycleStart until end) {
                val cycleDay = (day - cycleStart).toInt() + 1
                val high = cycleDay >= (if (i == 0) 17 else 16)
                val jitter = (((day * 37) % 13).toInt() - 6) / 100.0
                core.setTemperature(day, (if (high) 36.72 else 36.38) + jitter)
                if (cycleDay <= 5) core.setFlow(day, listOf(3, 4, 3, 2, 1)[cycleDay - 1])
                // Creamy, then egg white up to the peak, then sticky: the peak is confirmed the day after.
                val peak = if (i == 2) 14 else 15
                if (cycleDay in 10..peak) core.setMucus(day, if (cycleDay >= peak - 2) 4 else 3)
                if (cycleDay == peak + 1) core.setMucus(day, 2)
            }
        }
        return core.data
    }
}
