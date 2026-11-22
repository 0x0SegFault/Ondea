import OndeaShared

/// Three sample cycles for screenshots (launch argument --demo-data). Never saved.
enum DemoData {
    static func core(today: Int64) -> OndeaCore {
        let core = OndeaCore(json: nil)
        let start = today - 4 - 58
        core.completeOnboarding(lastPeriod: start, cycleLength: 28, periodLength: 5, ovulationDay: 0, fahrenheit: false)
        let starts: [Int64] = [start, start + 29, start + 58]
        for (i, cycleStart) in starts.enumerated() {
            if i > 0 { core.startCycle(day: cycleStart) }
            let end = i + 1 < starts.count ? starts[i + 1] : today + 1
            for day in cycleStart..<end {
                let cycleDay = Int(day - cycleStart) + 1
                let high = i < 2 && cycleDay >= (i == 0 ? 17 : 16)
                let jitter = Double((Int(day) * 37) % 13 - 6) / 100
                core.setTemperature(day: day, tempC: (high ? 36.72 : 36.38) + jitter)
                if cycleDay <= 5 { core.setFlow(day: day, flow: Int32([3, 4, 3, 2, 1][cycleDay - 1])) }
                if i < 2, (10...15).contains(cycleDay) { core.setMucus(day: day, mucus: Int32(cycleDay >= 13 ? 4 : 3)) }
            }
        }
        return core
    }
}
