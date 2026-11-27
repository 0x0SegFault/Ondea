import SwiftUI

/// A short tour before the setup: what Ondea is, what to record, the temperature chart, how Ondea helps.
/// `replay`: opened again from Settings; the last button closes instead of starting the setup.
struct IntroView: View {
    var replay = false
    let onDone: () -> Void
    @State private var page = 0
    private let pages = 4

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                Button(L10n.t(replay ? "intro_close" : "intro_skip"), action: onDone)
            }
            .padding(.horizontal, 20).padding(.top, 12)
            TabView(selection: $page) {
                ScrollView { whatPage.padding(20) }.tag(0)
                ScrollView { recordPage.padding(20) }.tag(1)
                ScrollView { chartPage.padding(20) }.tag(2)
                ScrollView { helpPage.padding(20) }.tag(3)
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .indexViewStyle(.page(backgroundDisplayMode: .always))
            Button(action: next) {
                Text(L10n.t(buttonKey)).frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(20)
        }
        .background(Color(.systemGroupedBackground))
    }

    private var buttonKey: String {
        if page < pages - 1 { return "intro_next" }
        return replay ? "intro_close" : "intro_start"
    }

    private func next() {
        if page < pages - 1 { withAnimation { page += 1 } } else { onDone() }
    }

    private var whatPage: some View {
        VStack(spacing: 16) {
            badge("thermometer.medium", Palette.rose)
            Text(L10n.t("intro_what_title")).font(.title2.bold())
            Text(L10n.t("intro_what_text")).multilineTextAlignment(.center).foregroundStyle(.secondary)
        }
    }

    private var recordPage: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack { Spacer(); badge("square.and.pencil", Palette.rose); Spacer() }
            Text(L10n.t("intro_record_title")).font(.title2.bold()).frame(maxWidth: .infinity)
            item("thermometer.medium", Palette.ovulation, "intro_record_temperature")
            item("drop.fill", Palette.period, "intro_record_period")
            item("drop", Palette.fertile, "intro_record_mucus")
            item("list.bullet", Palette.peak, "intro_record_other")
        }
    }

    private var chartPage: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(L10n.t("intro_chart_title")).font(.title2.bold()).frame(maxWidth: .infinity)
            TypicalCurve().frame(height: 150)
            Text(L10n.t("intro_chart_text"))
            PhaseTexts()
        }
    }

    private var helpPage: some View {
        VStack(spacing: 16) {
            badge("chart.line.uptrend.xyaxis", Palette.ovulation)
            Text(L10n.t("intro_help_title")).font(.title2.bold())
            Text(L10n.t("intro_help_text")).multilineTextAlignment(.center).foregroundStyle(.secondary)
            Text(L10n.t("intro_help_warning"))
                .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
    }

    private func badge(_ icon: String, _ tint: Color) -> some View {
        Image(systemName: icon).font(.system(size: 40)).foregroundStyle(tint)
            .frame(width: 88, height: 88).background(tint.opacity(0.14), in: Circle())
    }

    private func item(_ icon: String, _ tint: Color, _ key: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon).foregroundStyle(tint)
                .frame(width: 40, height: 40).background(tint.opacity(0.14), in: Circle())
            Text(L10n.t(key))
        }
    }
}

/// The phase explanations under the typical curve.
struct PhaseTexts: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            let keys = ["phase_period_text", "phase_follicular_text", "phase_ovulation_text", "phase_luteal_text"]
            ForEach(keys, id: \.self) { key in
                Text(L10n.t(key)).font(.callout)
            }
        }
    }
}

/// The textbook basal temperature curve of a 28-day cycle (illustration, not data).
struct TypicalCurve: View {
    private let wiggle: [Double] = [0, 3, -2, 1, -1, 2, -3, 1, 0, -2, 2, -1, 1, -4, 4, 6, 8, 7, 9, 8, 6, 9, 7, 8, 6, 7, 5, 3]

    var body: some View {
        Canvas { context, size in
            draw(in: &context, size: size)
        }
        .accessibilityLabel(L10n.t("typical_curve_description"))
    }

    private func value(_ day: Int) -> Double {
        let rise: Double = day < 14 ? 0 : 0.40 * min(1.0, Double(day - 13) / 2.5)
        return 36.40 + rise + wiggle[day] / 100.0
    }

    private func draw(in context: inout GraphicsContext, size: CGSize) {
        let dayWidth: CGFloat = size.width / 28
        let top: CGFloat = 26
        let bottom: CGFloat = size.height - 6
        func yOf(_ value: Double) -> CGFloat { bottom - CGFloat((value - 36.25) / 0.75) * (bottom - top) }
        for phase in CurvePhase.all {
            let width: CGFloat = CGFloat(phase.last - phase.first + 1) * dayWidth - 2
            let rect = CGRect(x: CGFloat(phase.first) * dayWidth + 1, y: 0, width: width, height: 18)
            context.fill(Path(roundedRect: rect, cornerRadius: 9), with: .color(phase.color.opacity(0.22)))
            if phase.last - phase.first > 3 {
                let label = Text(L10n.t(phase.key)).font(.system(size: 9, weight: .semibold)).foregroundColor(phase.color)
                context.draw(label, at: CGPoint(x: rect.midX, y: rect.midY))
            }
        }
        let cover = yOf(36.43)
        var coverLine = Path()
        coverLine.move(to: CGPoint(x: 0, y: cover))
        coverLine.addLine(to: CGPoint(x: size.width, y: cover))
        context.stroke(coverLine, with: .color(Palette.ovulation.opacity(0.7)), style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
        var curve = Path()
        for day in 0..<28 {
            let point = CGPoint(x: (CGFloat(day) + 0.5) * dayWidth, y: yOf(value(day)))
            if day == 0 { curve.move(to: point) } else { curve.addLine(to: point) }
        }
        context.stroke(curve, with: .color(Palette.rose), lineWidth: 2)
        for day in 0..<28 {
            let point = CGPoint(x: (CGFloat(day) + 0.5) * dayWidth, y: yOf(value(day)))
            let dot = Path(ellipseIn: CGRect(x: point.x - 2.5, y: point.y - 2.5, width: 5, height: 5))
            context.fill(dot, with: .color(day >= 14 ? Palette.ovulation : Palette.rose))
        }
    }
}

/// One phase band of the typical curve, days 0-based and inclusive.
private struct CurvePhase {
    let key: String
    let color: Color
    let first: Int
    let last: Int

    static let all = [
        CurvePhase(key: "phase_period", color: Palette.period, first: 0, last: 4),
        CurvePhase(key: "phase_follicular", color: Palette.fertile, first: 5, last: 12),
        CurvePhase(key: "phase_ovulation", color: Palette.ovulation, first: 13, last: 13),
        CurvePhase(key: "phase_luteal", color: Palette.ovulation, first: 14, last: 27)
    ]
}
