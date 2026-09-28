@testable import MotoricaStart
import XCTest
import UIKit
import shared

final class UserDefaultsKeyValueStorageTests: XCTestCase {
    private var userDefaults: UserDefaults!
    private var storage: UserDefaultsKeyValueStorage!

    override func setUp() {
        super.setUp()
        userDefaults = UserDefaults(suiteName: "UserDefaultsKeyValueStorageTests")
        userDefaults.removePersistentDomain(forName: "UserDefaultsKeyValueStorageTests")
        storage = UserDefaultsKeyValueStorage(userDefaults: userDefaults)
    }

    override func tearDown() {
        userDefaults.removePersistentDomain(forName: "UserDefaultsKeyValueStorageTests")
        storage = nil
        userDefaults = nil
        super.tearDown()
    }

    func testSaveAndLoad_roundTripsCodableValue() throws {
        let key = TypedStorageKey<[String]>(rawValue: "test.key.strings")
        let expected = ["a", "b", "c"]

        try storage.save(expected, for: key)
        let loaded = try storage.load(for: key)

        XCTAssertEqual(loaded, expected)
    }

    func testRemoveValue_clearsStoredData() throws {
        let key = TypedStorageKey<Int>(rawValue: "test.key.int")

        try storage.save(42, for: key)
        storage.removeValue(for: key)
        let loaded = try storage.load(for: key)

        XCTAssertNil(loaded)
    }

    func testLoad_withDifferentType_throwsTypeMismatch() throws {
        let intKey = TypedStorageKey<Int>(rawValue: "test.key.shared")
        let stringKey = TypedStorageKey<String>(rawValue: "test.key.shared")

        try storage.save(100, for: intKey)

        XCTAssertThrowsError(try storage.load(for: stringKey)) { error in
            guard case KeyValueStorageError.typeMismatch = error else {
                XCTFail("Expected KeyValueStorageError.typeMismatch, got: \(error)")
                return
            }
        }
    }
}

final class UserFirmwareProgressViewTests: XCTestCase {
    @MainActor
    func testProgressModalInterceptsOutsideTouchesAndHidesAllActions() {
        let controller = UserFirmwareProgressViewController()
        controller.loadViewIfNeeded()
        controller.view.frame = CGRect(x: 0, y: 0, width: 390, height: 844)
        controller.render(UserFirmwareUiState(phase: "updating", boardNumber: 1, boardCount: 3, progress: 40, detail: ""))
        controller.view.layoutIfNeeded()
        XCTAssertTrue(controller.isModalInPresentation)
        XCTAssertTrue(controller.view.accessibilityViewIsModal)
        XCTAssertTrue(controller.view.hitTest(CGPoint(x: 2, y: 2), with: nil) === controller.view)
        let stack = controller.view.subviews.compactMap { $0 as? UIStackView }.first!
        XCTAssertTrue(stack.arrangedSubviews.compactMap { $0 as? UIButton }.allSatisfy { $0.isHidden })
        XCTAssertTrue(controller.view.gestureRecognizers?.isEmpty ?? true)
    }

    @MainActor
    func testWaitingRemainsBlockingAndCompletionUsesOnlyOK() {
        let controller = UserFirmwareProgressViewController()
        controller.render(UserFirmwareUiState(phase: "waiting", boardNumber: 1, boardCount: 3, progress: 0, detail: ""))
        let stack = controller.view.subviews.compactMap { $0 as? UIStackView }.first!
        let button = stack.arrangedSubviews.compactMap { $0 as? UIButton }.first!
        XCTAssertTrue(button.isHidden)
        controller.render(UserFirmwareUiState(phase: "complete", boardNumber: 3, boardCount: 3, progress: 100, detail: ""))
        XCTAssertFalse(button.isHidden)
        XCTAssertEqual(button.title(for: .normal), SharedLocalizedText.text(SharedRes.strings().ok))
    }
}

final class UserFirmwareRoleAccessTests: XCTestCase {
    func testPersistedTechnicalRolesDoNotSelectUserMode() {
        let defaults = UserDefaults.standard
        let previous = defaults.volatileDomain(forName: UserDefaults.argumentDomain)
        defer { defaults.setVolatileDomain(previous, forName: UserDefaults.argumentDomain) }
        for role in [0, 1, 2] {
            defaults.setVolatileDomain(["UBI4_ROLE_SELECTED_V3": role], forName: UserDefaults.argumentDomain)
            XCTAssertEqual(UserFirmwareRoleAccess.selectedRole == 2, role == 2)
        }
    }

    func testRolePersistenceTargetsTheRoleWidgetOnly() {
        XCTAssertTrue(UserFirmwareRoleAccess.isRoleSelector(parameterID: 1, dataCode: 15))
        XCTAssertFalse(UserFirmwareRoleAccess.isRoleSelector(parameterID: 15, dataCode: 0))
        XCTAssertFalse(UserFirmwareRoleAccess.isRoleSelector(parameterID: 1, dataCode: 13))
    }
}
