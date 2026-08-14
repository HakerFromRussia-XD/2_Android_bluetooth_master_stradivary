@testable import MotoricaStart
import shared
import XCTest

final class BluetoothStorageKeysTests: XCTestCase {
    func testRawValues_areStable() {
        XCTAssertEqual(BluetoothStorageKeys.devicesStorageKey.rawValue, "BLEDevices")
        XCTAssertEqual(BluetoothStorageKeys.selectedFilterIndexStorageKey.rawValue, "selectedFilterIndex")
        XCTAssertEqual(BluetoothStorageKeys.selectedDeviceNameStorageKey.rawValue, "selectedDeviceName")
        XCTAssertEqual(BluetoothStorageKeys.customGestureNameStorageKey.rawValue, "customGestureNames")
    }

    func testTypeMetadata_matchesExpectedTypes() {
        XCTAssertEqual(
            BluetoothStorageKeys.selectedFilterIndexStorageKey.typeName,
            String(describing: Int.self)
        )
        XCTAssertEqual(
            BluetoothStorageKeys.selectedDeviceNameStorageKey.typeName,
            String(describing: String.self)
        )
    }
}

final class BluetoothScanDeviceNameFormatterTests: XCTestCase {
    func testDisplayName_formatsLegacyFestXNames() {
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName("FEST-XFTHS04921"),
            "FEST-H-04921"
        )
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName("FEST-XFTFS12345"),
            "FEST-F-12345"
        )
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName("FEST-XFTEPABCDE"),
            "FEST-EP-ABCDE"
        )
    }

    func testDisplayName_formatsNewTransportPrefixNames() {
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName("FTHS3-111111"),
            "111111"
        )
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName(" FTHS3-Роман "),
            "Роман"
        )
    }

    func testDisplayName_keepsAlreadyFormattedAndForeignNames() {
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName("FEST-H-04921"),
            "FEST-H-04921"
        )
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName("JBL Tune720BT"),
            "JBL Tune720BT"
        )
        XCTAssertEqual(
            BluetoothScanDeviceNameFormatter.displayName(nil),
            ""
        )
    }
}

final class TextInputDeviceNameTransportV3Tests: XCTestCase {
    private var userDefaults: UserDefaults!
    private var storage: UserDefaultsKeyValueStorage!

    override func setUp() {
        super.setUp()
        userDefaults = UserDefaults(suiteName: "TextInputDeviceNameTransportV3Tests")
        userDefaults.removePersistentDomain(forName: "TextInputDeviceNameTransportV3Tests")
        storage = UserDefaultsKeyValueStorage(userDefaults: userDefaults)
    }

    override func tearDown() {
        UiInterfaceModeBridgeV3.shared.updateFromDeviceName(deviceName: "UNKNOWN")
        userDefaults.removePersistentDomain(forName: "TextInputDeviceNameTransportV3Tests")
        storage = nil
        userDefaults = nil
        super.tearDown()
    }

    func testTransportName_preservesINDY3PrefixedMode() throws {
        UiInterfaceModeBridgeV3.shared.updateFromDeviceName(deviceName: "INDY3-0000000000")

        let result = try XCTUnwrap(
            DeviceNameBridgeV3.shared.transportName(
                rawName: "MY-HAND",
                currentFullName: "INDY3-0000000000"
            )
        )

        XCTAssertEqual(result, "INDY3-MY-HAND")
        try storage.save(result, for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
        let stored = try XCTUnwrap(
            try storage.load(for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
        )
        XCTAssertEqual(stored, result)
    }

    func testTransportName_keepsPrefixFreeModeAcrossRepeatedEdits() {
        UiInterfaceModeBridgeV3.shared.updateFromDeviceName(deviceName: "INDY3-0000000000")

        let first = DeviceNameBridgeV3.shared.transportName(
            rawName: "1234567890",
            currentFullName: "0000000000"
        )
        let second = DeviceNameBridgeV3.shared.transportName(
            rawName: "9876543210",
            currentFullName: first
        )

        XCTAssertEqual(first, "1234567890")
        XCTAssertEqual(second, "9876543210")
    }

    func testTransportName_removesManualPrefixAccordingToCurrentMode() {
        UiInterfaceModeBridgeV3.shared.updateFromDeviceName(deviceName: "FTHS3-0000000000")

        XCTAssertEqual(
            DeviceNameBridgeV3.shared.transportName(
                rawName: "indy3-MY-HAND",
                currentFullName: "FTHS3-0000000000"
            ),
            "FTHS3-MY-HAND"
        )
        XCTAssertEqual(
            DeviceNameBridgeV3.shared.transportName(
                rawName: "fths3-MY-HAND",
                currentFullName: "0000000000"
            ),
            "MY-HAND"
        )
        XCTAssertNil(
            DeviceNameBridgeV3.shared.transportName(
                rawName: "FTHS3-",
                currentFullName: "FTHS3-0000000000"
            )
        )
    }

    func testEditableNameLimit_preservesCompleteUtf8Characters() {
        XCTAssertEqual(TextInputNameLimitV3.maxBytes, 13)
        XCTAssertEqual(TextInputNameLimitV3.trimToLimit("ПротезAB"), "ПротезA")
        XCTAssertEqual(TextInputNameLimitV3.trimToLimit("ABC😀DEFGHIJ"), "ABC😀DEFGHI")
    }
}

final class SmartConnectionSettingsStoreTests: XCTestCase {
    private struct LegacySaveObjectString: Codable {
        let key: String
        let value: String
    }

    private var userDefaults: UserDefaults!
    private var documentsDirectory: URL!

    override func setUpWithError() throws {
        try super.setUpWithError()
        userDefaults = UserDefaults(suiteName: "SmartConnectionSettingsStoreTests")
        userDefaults.removePersistentDomain(forName: "SmartConnectionSettingsStoreTests")
        documentsDirectory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: documentsDirectory, withIntermediateDirectories: true)
    }

    override func tearDownWithError() throws {
        if let documentsDirectory {
            try? FileManager.default.removeItem(at: documentsDirectory)
        }
        userDefaults.removePersistentDomain(forName: "SmartConnectionSettingsStoreTests")
        userDefaults = nil
        documentsDirectory = nil
        try super.tearDownWithError()
    }

    func testCurrentValue_defaultsToEnabledAndPersistsBothKeys() throws {
        let store = makeStore()

        XCTAssertTrue(store.currentValue())
        XCTAssertTrue(userDefaults.bool(forKey: "SET_MODE_SMART_CONNECTION"))
        XCTAssertEqual(try loadLegacyValue(for: "SMART_CONNECTION"), "1")
    }

    func testSetEnabled_writesLegacyAndNewAutoConnectionKeys() throws {
        let store = makeStore()

        store.setEnabled(false, notify: false)

        XCTAssertFalse(userDefaults.bool(forKey: "SET_MODE_SMART_CONNECTION"))
        XCTAssertEqual(try loadLegacyValue(for: "SMART_CONNECTION"), "0")
    }

    func testScanAutoConnectionDeactivationKeepsSmartEnabledUntilLaunchReset() throws {
        let store = makeStore()

        store.deactivateScanAutoConnectionUntilNextLaunch()

        XCTAssertTrue(store.currentValue())
        XCTAssertTrue(userDefaults.bool(forKey: "SET_MODE_SMART_CONNECTION"))
        XCTAssertEqual(try loadLegacyValue(for: "SMART_CONNECTION"), "1")
        XCTAssertEqual(try loadLegacyValue(for: "DEACTIVATE_SMART_CONNECTION"), "1")
        XCTAssertFalse(store.isScanAutoConnectionAllowed)

        store.resetScanAutoConnectionDeactivationForLaunch()

        XCTAssertTrue(store.isScanAutoConnectionAllowed)
        XCTAssertEqual(try loadLegacyValue(for: "DEACTIVATE_SMART_CONNECTION"), "0")
    }

    func testCurrentValue_prefersLegacyKeyAndMirrorsToUserDefaults() throws {
        try writeLegacyValue(key: "SMART_CONNECTION", value: "0")
        userDefaults.set(true, forKey: "SET_MODE_SMART_CONNECTION")
        let store = makeStore()

        XCTAssertFalse(store.currentValue())
        XCTAssertFalse(userDefaults.bool(forKey: "SET_MODE_SMART_CONNECTION"))
    }

    func testScanAutoConnectionTargetStore_readsMergedAndLegacyTargets() throws {
        let keyValueStorage = UserDefaultsKeyValueStorage(userDefaults: userDefaults)
        try keyValueStorage.save(
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            for: MergedScanStorageKeys.lastNewDeviceUUID
        )
        try keyValueStorage.save(
            "FEST-H-04922",
            for: MergedScanStorageKeys.lastOldDeviceName
        )
        try writeLegacyValue(
            key: "LAST_CONNECTION",
            value: "11111111-2222-3333-4444-555555555555"
        )
        try writeLegacyValue(key: "DEVICE_NAME", value: "fest-h-04922")

        let targetStore = SmartScanAutoConnectionTargetStore(
            keyValueStorage: keyValueStorage,
            documentsDirectory: documentsDirectory
        )

        XCTAssertEqual(
            targetStore.targetUUIDs(),
            Set([
                "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE",
                "11111111-2222-3333-4444-555555555555"
            ])
        )
        XCTAssertEqual(targetStore.targetNames(), Set(["FEST-H-04922"]))
    }

    private func makeStore() -> SmartConnectionSettingsStore {
        SmartConnectionSettingsStore(
            userDefaults: userDefaults,
            documentsDirectory: documentsDirectory
        )
    }

    private func loadLegacyValue(for key: String) throws -> String {
        let data = try Data(contentsOf: documentsDirectory.appendingPathComponent(key, isDirectory: false))
        return try JSONDecoder().decode(LegacySaveObjectString.self, from: data).value
    }

    private func writeLegacyValue(key: String, value: String) throws {
        let object = LegacySaveObjectString(key: key, value: value)
        let data = try JSONEncoder().encode(object)
        try data.write(to: documentsDirectory.appendingPathComponent(key, isDirectory: false))
    }
}
