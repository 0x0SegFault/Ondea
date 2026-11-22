import Foundation
import OndeaShared
import SwiftUI

/// Holds the shared core, saves every change encrypted, and exposes what the screens show.
@MainActor
final class AppModel: ObservableObject {
    enum LoadState { case loading, ready, failed }

    @Published private(set) var state: LoadState = .loading
    @Published private(set) var today: TodayView?
    @Published private(set) var todayDay: Int64 = EpochDay.today()
    @Published var unlocked = false
    @Published var message: String?

    private(set) var core = OndeaCore(json: nil)
    private let store: SecureStore
    /// Demo mode (screenshots, previews): sample data in memory, never saved.
    private let demo: Bool

    init(store: SecureStore = SecureStore(), demo: Bool = ProcessInfo.processInfo.arguments.contains("--demo-data")) {
        self.store = store
        self.demo = demo
        load()
    }

    var onboarded: Bool { core.onboarded }
    var fahrenheit: Bool { core.fahrenheit }
    var appLock: Bool { core.appLock }

    func load() {
        if demo {
            core = DemoData.core(today: todayDay)
            unlocked = true
            state = .ready
            refresh()
            return
        }
        do {
            core = OndeaCore(json: try store.read())
            unlocked = !core.appLock
            state = .ready
            refresh()
        } catch {
            state = .failed
        }
    }

    func refreshDay() {
        todayDay = EpochDay.today()
        refresh()
    }

    private func refresh() {
        today = core.onboarded ? core.today(day: todayDay) : nil
    }

    /// Applies a change, saves it encrypted, and refreshes the screens.
    func change(_ edit: (OndeaCore) -> Void) {
        objectWillChange.send()
        edit(core)
        if !demo {
            do { try store.write(core.json()) } catch { message = L10n.t("msg_save_failed") }
        }
        refresh()
    }

    func entry(_ day: Int64) -> DayEntry { core.entry(day: day) }

    func lastTemperature() -> Double? { core.lastTemperature()?.doubleValue }

    func eraseAll() {
        objectWillChange.send()
        store.wipe()
        core = OndeaCore(json: nil)
        refresh()
    }

    func unlockIfNeeded() async {
        guard core.appLock, !unlocked else { return }
        unlocked = await AppLock.authenticate(reason: L10n.t("unlock_title"))
    }

    func lock() {
        if core.appLock && !demo { unlocked = false }
    }
}
