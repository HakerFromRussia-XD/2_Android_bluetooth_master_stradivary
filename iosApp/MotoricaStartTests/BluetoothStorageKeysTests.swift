@testable import MotoricaStart
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
