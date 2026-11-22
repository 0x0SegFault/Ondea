import OndeaShared
import SwiftUI

/// Temperature with +/- steps and a Save button. Values are stored in °C.
struct TemperatureInput: View {
    @EnvironmentObject private var model: AppModel
    let day: Int64
    @State private var text = ""

    var body: some View {
        let fahrenheit = model.fahrenheit
        let saved = model.entry(day).tempC?.doubleValue
        let parsed = Temperature.shared.parse(text: text, fahrenheit: fahrenheit)?.doubleValue
        let invalid = !text.isEmpty && parsed == nil
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                step("minus", -1, fahrenheit)
                TextField(placeholder(fahrenheit), text: $text)
                    .keyboardType(.decimalPad)
                    .font(.system(size: 44, weight: .heavy, design: .rounded))
                    .multilineTextAlignment(.center)
                    .frame(width: 160)
                    .foregroundStyle(invalid ? .red : .primary)
                Text(fahrenheit ? "°F" : "°C").foregroundStyle(.secondary)
                step("plus", 1, fahrenheit)
            }
            Text(invalid ? L10n.t("temperature_invalid") : L10n.t("temperature_hint"))
                .font(.footnote).foregroundStyle(invalid ? .red : .secondary)
            Button {
                if let parsed { model.change { $0.setTemperature(day: day, tempC: parsed) } }
                else if text.isEmpty { model.change { $0.clearTemperature(day: day) } }
            } label: {
                Label(saved != nil && parsed == saved ? L10n.t("temperature_saved") : L10n.t("save_temperature"),
                      systemImage: saved != nil && parsed == saved ? "checkmark" : "square.and.arrow.down")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(invalid || parsed == saved)
        }
        .onAppear { text = format(saved, fahrenheit) }
        .onChange(of: day) { _, _ in text = format(model.entry(day).tempC?.doubleValue, fahrenheit) }
    }

    private func step(_ icon: String, _ direction: Double, _ fahrenheit: Bool) -> some View {
        Button {
            let base = Temperature.shared.parse(text: text, fahrenheit: fahrenheit)?.doubleValue ?? model.lastTemperature() ?? 36.5
            let display = Temperature.shared.toDisplay(c: base, fahrenheit: fahrenheit)
                + direction * Temperature.shared.step(fahrenheit: fahrenheit)
            let celsius = Temperature.shared.fromDisplay(v: display, fahrenheit: fahrenheit)
            text = format(celsius, fahrenheit)
        } label: {
            Image(systemName: icon).font(.title2.bold()).frame(width: 48, height: 48)
        }
        .buttonStyle(.bordered)
        .clipShape(Circle())
        .accessibilityLabel(L10n.t(direction > 0 ? "increase" : "decrease"))
    }

    private func placeholder(_ fahrenheit: Bool) -> String {
        format(model.lastTemperature() ?? 36.5, fahrenheit)
    }

    private func format(_ celsius: Double?, _ fahrenheit: Bool) -> String {
        guard let celsius else { return "" }
        return String(format: "%.2f", locale: Locale.current, Temperature.shared.toDisplay(c: celsius, fahrenheit: fahrenheit))
    }
}

/// Period, mucus, ovulation test and other observations. Each change is saved at once.
struct DayDetails: View {
    @EnvironmentObject private var model: AppModel
    let day: Int64

    var body: some View {
        let entry = model.entry(day)
        VStack(alignment: .leading, spacing: 12) {
            choice(L10n.t("period"), ["flow_none", "flow_spotting", "flow_light", "flow_medium", "flow_heavy"], Int(entry.flow)) { value in
                model.change { $0.setFlow(day: day, flow: Int32(value)) }
            }
            choice(L10n.t("mucus"), ["mucus_none", "mucus_dry", "mucus_sticky", "mucus_creamy", "mucus_eggwhite"], Int(entry.mucus)) { value in
                model.change { $0.setMucus(day: day, mucus: Int32(value)) }
            }
            Text(L10n.t(["mucus_hint", "mucus_dry_hint", "mucus_sticky_hint", "mucus_creamy_hint", "mucus_eggwhite_hint"][Int(entry.mucus)]))
                .font(.footnote).foregroundStyle(.secondary)
            choice(L10n.t("lh_test"), ["lh_none", "lh_negative", "lh_positive"], Int(entry.lh)) { value in
                model.change { $0.setLh(day: day, lh: Int32(value)) }
            }
            Text(L10n.t("lh_hint")).font(.footnote).foregroundStyle(.secondary)
            Toggle(L10n.t("intercourse"), isOn: Binding(get: { entry.intercourse }, set: { on in
                model.change { $0.setIntercourse(day: day, on: on) }
            }))
            Toggle(L10n.t("disturbed_short"), isOn: Binding(get: { entry.disturbed }, set: { on in
                model.change { $0.setDisturbed(day: day, on: on) }
            }))
        }
    }

    private func choice(_ title: String, _ keys: [String], _ selected: Int, _ onSelect: @escaping (Int) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(keys.indices, id: \.self) { i in
                        Button(L10n.t(keys[i])) { onSelect(i) }
                            .buttonStyle(.bordered)
                            .tint(i == selected ? Palette.rose : .secondary)
                            .accessibilityAddTraits(i == selected ? .isSelected : [])
                    }
                }
            }
        }
    }
}
