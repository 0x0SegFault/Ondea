import OndeaShared
import SwiftUI

struct TodayScreen: View {
    @EnvironmentObject private var model: AppModel
    @State private var confirmStop = false
    @State private var confirmNewCycle = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(EpochDay.long(model.todayDay))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if let today = model.today, today.tracking {
                    ring(today)
                    status(today)
                    facts(today)
                    if !today.signsKey.isEmpty { signs(today) }
                    if today.adviceLearnedDay > 0 { advice(today) }
                    ChanceCard(view: today)
                } else {
                    paused
                }

                Card(title: L10n.t("morning_temperature"), systemImage: "thermometer.medium") {
                    TemperatureInput(day: model.todayDay)
                }
                Card(title: L10n.t("todays_observations"), systemImage: "square.and.pencil") {
                    DayDetails(day: model.todayDay)
                }

                HStack {
                    Button(L10n.t("new_cycle")) { confirmNewCycle = true }
                        .buttonStyle(.bordered)
                    if model.today?.tracking == true {
                        Button(L10n.t("pause_tracking")) { confirmStop = true }
                    }
                }
                Text(L10n.t("disclaimer_short"))
                    .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .confirmationDialog(L10n.t("pause_confirm"), isPresented: $confirmStop, titleVisibility: .visible) {
            Button(L10n.t("pause")) { model.change { $0.stopTracking(day: model.todayDay) } }
        }
        .confirmationDialog(L10n.t("new_cycle"), isPresented: $confirmNewCycle) {
            Button(L10n.t("start")) { model.change { $0.startCycle(day: model.todayDay) } }
        }
    }

    private func ring(_ today: TodayView) -> some View {
        CycleRing(days: today.ringDays, cycleDay: Int(today.cycleDay))
            .frame(width: 250, height: 250)
            .overlay {
                VStack(spacing: 0) {
                    Text(L10n.t("cycle_day_label")).font(.subheadline).foregroundStyle(.secondary)
                    Text("\(today.cycleDay)").font(.system(size: 64, weight: .heavy, design: .rounded))
                    Text(L10n.t("of_days", Int(today.cycleLength))).font(.subheadline).foregroundStyle(.secondary)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(L10n.t("cycle_day", Int(today.cycleDay)))
    }

    private func status(_ today: TodayView) -> some View {
        let accent = Palette.tone(today.tone)
        let headline = today.headlinePlural
            ? L10n.plural(today.headlineKey, Int(today.headlineCount))
            : L10n.t(today.headlineKey)
        let dates: [CVarArg] = today.detailDates.map { EpochDay.dayMonth($0.int64Value) }
        let detail = detailText(today, dates)
        return VStack(spacing: 6) {
            Text(headline).font(.title2.bold()).foregroundStyle(accent).multilineTextAlignment(.center)
            Text(detail).font(.callout).foregroundStyle(.secondary).multilineTextAlignment(.center)
            Text(L10n.t(today.sourceKey))
                .font(.caption.weight(.semibold))
                .foregroundStyle(accent)
                .padding(.horizontal, 12).padding(.vertical, 4)
                .background(accent.opacity(0.12), in: Capsule())
        }
    }

    private func detailText(_ today: TodayView, _ dates: [CVarArg]) -> String {
        switch today.detailKey {
        case "maybe_pregnant":
            return L10n.t(today.detailKey, Int(today.detailNumber))
        case "temp_rising":
            let date: CVarArg = dates.first ?? ""
            return L10n.t(today.detailKey, Int(today.detailNumber), date)
        default:
            return String(format: L10n.t(today.detailKey), locale: Locale.current, arguments: dates)
        }
    }

    /// Do temperature and mucus point to the same ovulation this cycle?
    private func signs(_ today: TodayView) -> some View {
        let dates: [CVarArg] = today.signsDates.map { EpochDay.dayMonth($0.int64Value) }
        let icon = today.signsKey == "signs_disagree" ? "exclamationmark.triangle" : "checkmark.circle"
        return Card(title: L10n.t("signs_title"), systemImage: icon) {
            Text(String(format: L10n.t(today.signsKey), locale: Locale.current, arguments: dates)).font(.callout)
        }
    }

    /// After a cycle with signs, Ondea's learnt ovulation day may differ from the day used so far. The user chooses.
    private func advice(_ today: TodayView) -> some View {
        let setting = Int(today.adviceSettingDay)
        let learned = Int(today.adviceLearnedDay)
        let textKey = today.adviceFromUser ? "advice_text_own" : "advice_text_typical"
        return Card(title: L10n.t("advice_title"), systemImage: "chart.line.uptrend.xyaxis") {
            Text(L10n.t(textKey, setting, learned)).font(.callout)
            HStack {
                Button(L10n.t("advice_use", learned)) { model.change { $0.acceptAdvice(today: model.todayDay) } }
                    .buttonStyle(.borderedProminent)
                Button(L10n.t("advice_keep", setting)) { model.change { $0.keepOwnDay(today: model.todayDay) } }
            }
        }
    }

    private func facts(_ today: TodayView) -> some View {
        HStack(spacing: 8) {
            Fact(label: L10n.t("stat_fertile"),
                 value: EpochDay.dayMonth(today.fertileStart) + "–" + EpochDay.dayMonth(today.fertileEnd), color: Palette.fertile)
            Fact(label: L10n.t("stat_ovulation"), value: EpochDay.dayMonth(today.ovulation), color: Palette.ovulation)
            Fact(label: L10n.t("stat_next_period"), value: EpochDay.dayMonth(today.nextPeriod), color: Palette.period)
        }
    }

    private var paused: some View {
        VStack(spacing: 8) {
            Text(L10n.t("not_tracking_title")).font(.title2.bold())
            Text(L10n.t("not_tracking_text")).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
        .padding(.vertical, 24)
    }
}

private struct Fact: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption2.weight(.semibold)).foregroundStyle(.secondary).lineLimit(1)
            Text(value).font(.subheadline.weight(.bold)).lineLimit(1).minimumScaleFactor(0.8)
        }
        .padding(.horizontal, 12).padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.10), in: RoundedRectangle(cornerRadius: 16))
    }
}

/// Chance of conception by day before ovulation (Wilcox et al., NEJM 1995).
private struct ChanceCard: View {
    let view: TodayView
    private let byDay = [0.10, 0.16, 0.14, 0.27, 0.31, 0.33]

    var body: some View {
        let levelKey = ["HIGH": "chance_high", "MEDIUM": "chance_medium"][view.chanceLevel] ?? "chance_low"
        let color = view.chanceLevel == "HIGH" ? Palette.peak : view.chanceLevel == "MEDIUM" ? Palette.fertile : Color.secondary
        Card(title: L10n.t("chance_title"), systemImage: "heart") {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(L10n.t(levelKey)).font(.title2.bold()).foregroundStyle(color)
                    Text(view.chancePercent > 0 ? L10n.t("chance_percent", Int(view.chancePercent)) : L10n.t("chance_outside"))
                        .font(.callout).foregroundStyle(.secondary)
                }
                Spacer()
                HStack(alignment: .bottom, spacing: 4) {
                    ForEach(0..<6, id: \.self) { i in
                        VStack(spacing: 2) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Palette.fertile.opacity(0.3))
                                .frame(width: 16, height: byDay[i] / 0.33 * 52)
                            Text(i == 5 ? L10n.t("ovulation_short") : "\(i - 5)").font(.caption2).foregroundStyle(.secondary)
                        }
                    }
                }
                .accessibilityHidden(true)
            }
            Text(L10n.t("chance_source")).font(.caption2).foregroundStyle(.secondary)
        }
    }
}
