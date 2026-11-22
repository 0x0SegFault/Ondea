import SwiftUI

@main
struct OndeaApp: App {
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .tint(Palette.rose)
        }
        .onChange(of: scenePhase) { _, phase in
            switch phase {
            case .active:
                model.refreshDay()
                Task { await model.unlockIfNeeded() }
            case .background:
                model.lock()
            default:
                break
            }
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        switch model.state {
        case .loading:
            ProgressView()
        case .failed:
            DataErrorView()
        case .ready:
            if model.appLock && !model.unlocked {
                LockView()
            } else if !model.onboarded {
                OnboardingView()
            } else {
                MainTabs()
            }
        }
    }
}

struct MainTabs: View {
    var body: some View {
        TabView {
            TodayScreen()
                .tabItem { Label(L10n.t("tab_today"), systemImage: "thermometer.medium") }
            SettingsScreen()
                .tabItem { Label(L10n.t("tab_settings"), systemImage: "gearshape") }
        }
    }
}

struct LockView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "lock").font(.system(size: 48)).foregroundStyle(Palette.rose)
            Text("Ondea").font(.title2.bold())
            Button(L10n.t("unlock")) { Task { await model.unlockIfNeeded() } }
                .buttonStyle(.borderedProminent)
        }
        .task { await model.unlockIfNeeded() }
    }
}

struct DataErrorView: View {
    @EnvironmentObject private var model: AppModel
    @State private var confirmErase = false

    var body: some View {
        VStack(spacing: 16) {
            Text(L10n.t("error_title")).font(.title3.bold()).multilineTextAlignment(.center)
            Text(L10n.t("error_text")).multilineTextAlignment(.center)
            Button(L10n.t("erase_start_over"), role: .destructive) { confirmErase = true }
        }
        .padding(24)
        .confirmationDialog(L10n.t("erase_confirm"), isPresented: $confirmErase, titleVisibility: .visible) {
            Button(L10n.t("erase"), role: .destructive) { model.eraseAll(); model.load() }
        }
    }
}
