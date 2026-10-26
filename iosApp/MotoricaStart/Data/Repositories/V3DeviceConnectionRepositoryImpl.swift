import Foundation
import shared

final class V3DeviceConnectionRepositoryImpl: V3DeviceConnectionRepository, V3DeviceDisconnectionRepository {
    private let keyValueStorage: KeyValueStorage
    private let bleManager: () -> BleManagerKmm
    private let appearanceStore: MergedScanAppearanceStore

    init(keyValueStorage: KeyValueStorage, bleManager: @escaping () -> BleManagerKmm) {
        self.keyValueStorage = keyValueStorage
        self.bleManager = bleManager
        self.appearanceStore = MergedScanAppearanceStore(keyValueStorage: keyValueStorage)
    }

    func prepareConnection(deviceName: String, deviceAddress: String) -> Bool {
        do {
            try keyValueStorage.save(deviceName, for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
        } catch {
            print("[Storage] failed to persist selected device name: \(error)")
        }
        appearanceStore.markNewConnection(deviceName: deviceName, deviceUUID: deviceAddress)
        return UiInterfaceModeBridgeV3.shared.updateFromDeviceName(deviceName: deviceName)
    }

    func connectDevice(deviceAddress: String) {
        let manager = bleManager()
        logConnect("[BLE-CONNECT] phase=stopScan")
        manager.stopScanKmm()
        logConnect("[BLE-CONNECT] phase=connect")
        manager.connectToDevice(uuid: deviceAddress)
    }

    func disconnectDevice() {
        SmartConnectionSettingsStore().deactivateScanAutoConnectionUntilNextLaunch()
        bleManager().disconnectFromDevice()
        keyValueStorage.removeValue(for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
    }

    private func logConnect(_ message: String) {
        NSLog("%@", message)
        print(message)
    }
}
