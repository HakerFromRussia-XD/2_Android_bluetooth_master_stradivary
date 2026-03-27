@testable import MotoricaStart
import XCTest

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
