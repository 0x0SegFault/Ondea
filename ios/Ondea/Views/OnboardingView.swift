import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel
    @State private var lastPeriod = Date()
    @State private var cycleLength = 28
    @State private var periodLength = 5
    @State private var knowsOvulation = false
    @State private var ovulationDay = 14
    @State private var fahrenheit = false
    @State private var accepted = false

    var body: some View {
        Form {
            Section {
                VStack(spacing: 8) {
                    Image(systemName: "thermometer.medium").font(.system(size: 44)).foregroundStyle(Palette.rose)
                    Text(L10n.t("welcome_title")).font(.title.bold())
                    Text(L10n.t("welcome_text")).multilineTextAlignment(.center).foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
            }
            Section(L10n.t("last_period_start")) {
                DatePicker(L10n.t("last_period_start"), selection: $lastPeriod, in: ...Date(), displayedComponents: .date)
                    .labelsHidden()
            }
            Section(footer: Text(L10n.t("cycle_length_onboarding_hint"))) {
                Stepper(L10n.t("cycle_length") + ": " + L10n.t("n_days", cycleLength), value: $cycleLength, in: 20...45)
                Stepper(L10n.t("period_length") + ": " + L10n.t("n_days", periodLength), value: $periodLength, in: 2...10)
            }
            Section(footer: Text(L10n.t("ovulation_day_onboarding_hint"))) {
                Toggle(L10n.t("know_ovulation_day"), isOn: $knowsOvulation)
                if knowsOvulation {
                    Stepper(L10n.t("day_n", ovulationDay), value: $ovulationDay, in: 8...35)
                }
            }
            Section(L10n.t("unit")) {
                Picker(L10n.t("unit"), selection: $fahrenheit) {
                    Text("°C").tag(false)
                    Text("°F").tag(true)
                }
                .pickerStyle(.segmented)
            }
            Section(L10n.t("how_to_measure_title")) {
                Text(L10n.t("how_to_measure"))
            }
            Section {
                Toggle(L10n.t("disclaimer_accept"), isOn: $accepted)
                Button(L10n.t("start")) {
                    model.change {
                        $0.completeOnboarding(
                            lastPeriod: EpochDay.from(date: lastPeriod),
                            cycleLength: Int32(cycleLength),
                            periodLength: Int32(periodLength),
                            ovulationDay: Int32(knowsOvulation ? ovulationDay : 0),
                            fahrenheit: fahrenheit
                        )
                    }
                }
                .disabled(!accepted)
                .frame(maxWidth: .infinity)
            }
        }
    }
}
