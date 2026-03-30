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
