import OndeaShared
import SwiftUI

/// Temperature input: one wheel per digit (degrees, tenths, hundredths), so each digit is copied from the
/// thermometer. The wheels start at the last reading. The keyboard button switches to typing. Values are stored in °C.
struct TemperatureInput: View {
    @EnvironmentObject private var model: AppModel
    let day: Int64
    @State private var hundredths = 3650
    @State private var typing = false
    @State private var text = ""

    var body: some View {
        let fahrenheit = model.fahrenheit
        let saved = model.entry(day).tempC?.doubleValue
        let value = currentValue(fahrenheit)
        let invalid = typing && !text.isEmpty && value == nil
        VStack(spacing: 12) {
            HStack(spacing: 4) {
                if typing {
                    TextField(format(fromHundredths(hundredths, fahrenheit), fahrenheit), text: $text)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 44, weight: .heavy, design: .rounded))
                        .multilineTextAlignment(.center)
                        .frame(width: 160)
                        .foregroundStyle(invalid ? .red : .primary)
                } else {
                    wheels(fahrenheit)
                }
                Text(fahrenheit ? "°F" : "°C").foregroundStyle(.secondary)
                Button {
                    switchMode(fahrenheit)
                } label: {
                    Image(systemName: typing ? "slider.vertical.3" : "keyboard")
                }
                .accessibilityLabel(L10n.t(typing ? "temperature_use_wheels" : "temperature_type"))
            }
            Text(invalid ? L10n.t("temperature_invalid") : L10n.t("temperature_hint"))
                .font(.footnote).foregroundStyle(invalid ? .red : .secondary)
            Button {
                if let value { model.change { $0.setTemperature(day: day, tempC: value) } }
            } label: {
                Label(saved != nil && value == saved ? L10n.t("temperature_saved") : L10n.t("save_temperature"),
                      systemImage: saved != nil && value == saved ? "checkmark" : "square.and.arrow.down")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(invalid || value == nil || value == saved)
            if saved != nil && value == saved {
                Button(L10n.t("temperature_remove"), role: .destructive) { model.change { $0.clearTemperature(day: day) } }
                    .font(.footnote)
            }
        }
        .onAppear { reset(fahrenheit) }
        .onChange(of: day) { _, _ in reset(fahrenheit) }
    }

    private func wheels(_ fahrenheit: Bool) -> some View {
        let range = fahrenheit ? 95...101 : 35...38
        let whole = Binding(get: { hundredths / 100 }, set: { hundredths = $0 * 100 + hundredths % 100 })
        let tenths = Binding(get: { hundredths / 10 % 10 }, set: { hundredths = hundredths / 100 * 100 + $0 * 10 + hundredths % 10 })
        let last = Binding(get: { hundredths % 10 }, set: { hundredths = hundredths / 10 * 10 + $0 })
        return HStack(spacing: 0) {
            wheel(Array(range), whole, "temperature_degrees", width: 76)
            Text(Locale.current.decimalSeparator ?? ".").font(.title.bold())
            wheel(Array(0...9), tenths, "temperature_tenths", width: 52)
            wheel(Array(0...9), last, "temperature_hundredths", width: 52)
        }
    }

    private func wheel(_ values: [Int], _ selection: Binding<Int>, _ key: String, width: CGFloat) -> some View {
        Picker(L10n.t(key), selection: selection) {
            ForEach(values, id: \.self) { number in
                Text("\(number)").font(.system(size: 34, weight: .heavy, design: .rounded)).tag(number)
            }
        }
        .pickerStyle(.wheel)
        .frame(width: width, height: 130)
        .clipped()
        .accessibilityLabel(L10n.t(key))
    }

    private func currentValue(_ fahrenheit: Bool) -> Double? {
        if typing { return Temperature.shared.parse(text: text, fahrenheit: fahrenheit)?.doubleValue }
        return fromHundredths(hundredths, fahrenheit)
    }

    private func switchMode(_ fahrenheit: Bool) {
        if typing {
            if let typed = Temperature.shared.parse(text: text, fahrenheit: fahrenheit)?.doubleValue {
                hundredths = clamp(Int(Temperature.shared.toHundredths(c: typed, fahrenheit: fahrenheit)), fahrenheit)
            }
        } else {
            text = format(fromHundredths(hundredths, fahrenheit), fahrenheit)
        }
        typing.toggle()
    }

    private func reset(_ fahrenheit: Bool) {
        let start = model.entry(day).tempC?.doubleValue ?? model.lastTemperature() ?? 36.5
        hundredths = clamp(Int(Temperature.shared.toHundredths(c: start, fahrenheit: fahrenheit)), fahrenheit)
        text = model.entry(day).tempC.map { format($0.doubleValue, fahrenheit) } ?? ""
        typing = false
    }

    /// Keeps the whole degrees inside the wheel range.
    private func clamp(_ value: Int, _ fahrenheit: Bool) -> Int {
        let range = fahrenheit ? 95...101 : 35...38
        return min(max(value, range.lowerBound * 100), range.upperBound * 100 + 99)
    }

    private func fromHundredths(_ value: Int, _ fahrenheit: Bool) -> Double {
        Temperature.shared.fromHundredths(h: Int32(value), fahrenheit: fahrenheit)
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

    private static let mucusKeys = ["mucus_none", "mucus_dry", "mucus_sticky", "mucus_creamy", "mucus_eggwhite"]
    private static let mucusHints = ["mucus_hint", "mucus_dry_hint", "mucus_sticky_hint", "mucus_creamy_hint", "mucus_eggwhite_hint"]

    var body: some View {
        let entry = model.entry(day)
        VStack(alignment: .leading, spacing: 12) {
            choice(L10n.t("period"), ["flow_none", "flow_spotting", "flow_light", "flow_medium", "flow_heavy"], Int(entry.flow)) { value in
                model.change { $0.setFlow(day: day, flow: Int32(value)) }
            }
            choice(L10n.t("mucus"), Self.mucusKeys, Int(entry.mucus)) { value in
                model.change { $0.setMucus(day: day, mucus: Int32(value)) }
            }
            Text(L10n.t(Self.mucusHints[Int(entry.mucus)]))
                .font(.footnote).foregroundStyle(.secondary)
            choice(L10n.t("lh_test"), ["lh_none", "lh_negative", "lh_positive"], Int(entry.lh)) { value in
                model.change { $0.setLh(day: day, lh: Int32(value)) }
            }
            Text(L10n.t("lh_hint")).font(.footnote).foregroundStyle(.secondary)
            Toggle(L10n.t("intercourse"), isOn: Binding(get: { entry.intercourse }, set: { isOn in
                model.change { $0.setIntercourse(day: day, on: isOn) }
            }))
            Toggle(L10n.t("disturbed_short"), isOn: Binding(get: { entry.disturbed }, set: { isOn in
                model.change { $0.setDisturbed(day: day, on: isOn) }
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
