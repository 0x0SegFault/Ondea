import SwiftUI

struct SettingsScreen: View {
    @EnvironmentObject private var model: AppModel
    @State private var lockUnavailable = false
    @State private var confirmErase = false

    var body: some View {
        NavigationStack {
            Form {
                Section(L10n.t("section_cycle")) {
                    Picker(L10n.t("unit"), selection: Binding(get: { model.fahrenheit }, set: { on in
                        model.change { $0.setFahrenheit(on: on) }
                    })) {
                        Text("°C").tag(false)
                        Text("°F").tag(true)
                    }
                    .pickerStyle(.segmented)
                }
                Section(footer: Text(L10n.t("data_stored"))) {
                    Toggle(L10n.t("app_lock"), isOn: Binding(get: { model.appLock }, set: { on in
                        if on && !AppLock.isAvailable { lockUnavailable = true; return }
                        model.change { $0.setAppLock(on: on) }
                        model.unlocked = true
                    }))
                }
                Section {
                    Button(L10n.t("erase_all"), role: .destructive) { confirmErase = true }
                }
                Section(L10n.t("section_about")) {
                    Text("Ondea " + (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""))
                        .font(.headline)
                    Text(L10n.t("about_text"))
                    Text(L10n.t("disclaimer_full")).font(.footnote).foregroundStyle(.secondary)
                }
            }
            .navigationTitle(L10n.t("tab_settings"))
            .alert(L10n.t("app_lock_unavailable"), isPresented: $lockUnavailable) {
                Button(L10n.t("ok")) {}
            }
            .confirmationDialog(L10n.t("erase_confirm"), isPresented: $confirmErase, titleVisibility: .visible) {
                Button(L10n.t("erase"), role: .destructive) { model.eraseAll() }
            }
        }
    }
}
