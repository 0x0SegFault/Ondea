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

                if let t = model.today, t.tracking {
                    ring(t)
                    status(t)
                    facts(t)
                    ChanceCard(view: t)
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

    private func ring(_ t: TodayView) -> some View {
        CycleRing(days: t.ringDays, cycleDay: Int(t.cycleDay))
            .frame(width: 250, height: 250)
            .overlay {
                VStack(spacing: 0) {
                    Text(L10n.t("cycle_day_label")).font(.subheadline).foregroundStyle(.secondary)
                    Text("\(t.cycleDay)").font(.system(size: 64, weight: .heavy, design: .rounded))
                    Text(L10n.t("of_days", Int(t.cycleLength))).font(.subheadline).foregroundStyle(.secondary)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(L10n.t("cycle_day", Int(t.cycleDay)))
    }

    private func status(_ t: TodayView) -> some View {
        let accent = Palette.tone(t.tone)
        let headline = t.headlinePlural
            ? L10n.plural(t.headlineKey, Int(t.headlineCount))
            : L10n.t(t.headlineKey)
        let dates: [CVarArg] = t.detailDates.map { EpochDay.dayMonth($0.int64Value) }
        let detail = t.detailKey == "maybe_pregnant"
            ? L10n.t(t.detailKey, Int(t.detailNumber))
            : String(format: L10n.t(t.detailKey), locale: Locale.current, arguments: dates)
        return VStack(spacing: 6) {
            Text(headline).font(.title2.bold()).foregroundStyle(accent).multilineTextAlignment(.center)
            Text(detail).font(.callout).foregroundStyle(.secondary).multilineTextAlignment(.center)
            Text(L10n.t(t.sourceKey))
                .font(.caption.weight(.semibold))
                .foregroundStyle(accent)
                .padding(.horizontal, 12).padding(.vertical, 4)
                .background(accent.opacity(0.12), in: Capsule())
        }
    }

    private func facts(_ t: TodayView) -> some View {
        HStack(spacing: 8) {
            Fact(label: L10n.t("stat_fertile"),
                 value: EpochDay.dayMonth(t.fertileStart) + "–" + EpochDay.dayMonth(t.fertileEnd), color: Palette.fertile)
            Fact(label: L10n.t("stat_ovulation"), value: EpochDay.dayMonth(t.ovulation), color: Palette.ovulation)
            Fact(label: L10n.t("stat_next_period"), value: EpochDay.dayMonth(t.nextPeriod), color: Palette.period)
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
