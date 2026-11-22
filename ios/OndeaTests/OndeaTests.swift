import OndeaShared
import XCTest
@testable import Ondea

final class OndeaTests: XCTestCase {
    func testSecureStoreRoundTripAndWipe() throws {
        let store = SecureStore(fileName: "test.dat", keyAccount: "test_key")
        store.wipe()
        XCTAssertNil(try store.read())
        try store.write("{\"hello\":\"world\"}")
        XCTAssertEqual(try store.read(), "{\"hello\":\"world\"}")
        store.wipe()
        XCTAssertNil(try store.read())
    }

    func testSharedCoreFromSwift() {
        let core = OndeaCore(json: nil)
        let start = EpochDay.from(year: 2026, month: 10, day: 1)
        core.completeOnboarding(lastPeriod: start, cycleLength: 28, periodLength: 5, ovulationDay: 0, fahrenheit: false)
        core.setTemperature(day: start + 4, tempC: 36.42)
        let today = core.today(day: start + 4)
        XCTAssertTrue(today.tracking)
        XCTAssertEqual(today.cycleDay, 5)
        XCTAssertEqual(today.headlineKey, "fertile_in")
        let again = OndeaCore(json: core.json())
        XCTAssertEqual(again.entry(day: start + 4).tempC?.doubleValue, 36.42)
    }

    func testEpochDayMatchesKotlin() {
        // 2026-10-01 is epoch day 20727 in kotlinx-datetime.
        XCTAssertEqual(EpochDay.from(year: 2026, month: 10, day: 1), 20727)
    }

    func testTranslationsAreBundled() {
        XCTAssertNotEqual(L10n.t("tab_today"), "tab_today")
        XCTAssertEqual(L10n.plural("period_late", 3).contains("3"), true)
    }
}
