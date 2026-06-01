import Foundation

enum MergedProsthesisFamily: String, Codable {
    case newKmm
    case oldLegacy
    case unknown
}

enum MergedScanAppearanceMode {
    case legacyBlue
    case modernGray
}

enum MergedScanStorageKeys {
    static let lastConnectedFamily = TypedStorageKey<MergedProsthesisFamily>(rawValue: "merged.lastConnectedFamily")
    static let hasConnectedNewProsthesis = TypedStorageKey<Bool>(rawValue: "merged.hasConnectedNewProsthesis")
    static let lastNewDeviceName = TypedStorageKey<String>(rawValue: "merged.lastNewDeviceName")
    static let lastNewDeviceUUID = TypedStorageKey<String>(rawValue: "merged.lastNewDeviceUUID")
    static let lastOldDeviceName = TypedStorageKey<String>(rawValue: "merged.lastOldDeviceName")
    static let lastOldDeviceUUID = TypedStorageKey<String>(rawValue: "merged.lastOldDeviceUUID")
}

final class MergedScanAppearanceStore {
    private let keyValueStorage: KeyValueStorage

    init(keyValueStorage: KeyValueStorage) {
        self.keyValueStorage = keyValueStorage
    }

    var initialAppearanceMode: MergedScanAppearanceMode {
        let hasConnectedNew = (try? keyValueStorage.load(for: MergedScanStorageKeys.hasConnectedNewProsthesis)) ?? false
        return hasConnectedNew ? .modernGray : .legacyBlue
    }

    func markNewConnection(device: BLEDevice) {
        save(true, for: MergedScanStorageKeys.hasConnectedNewProsthesis)
        save(MergedProsthesisFamily.newKmm, for: MergedScanStorageKeys.lastConnectedFamily)
        save(device.name, for: MergedScanStorageKeys.lastNewDeviceName)
        save(device.uuid.uuidString, for: MergedScanStorageKeys.lastNewDeviceUUID)
    }

    func markOldSelection(device: BLEDevice) {
        save(MergedProsthesisFamily.oldLegacy, for: MergedScanStorageKeys.lastConnectedFamily)
        save(device.name, for: MergedScanStorageKeys.lastOldDeviceName)
        save(device.uuid.uuidString, for: MergedScanStorageKeys.lastOldDeviceUUID)
    }

    private func save<Value: Codable>(_ value: Value, for key: TypedStorageKey<Value>) {
        do {
            try keyValueStorage.save(value, for: key)
        } catch {
            print("[MergedScanStorage] failed to save \(key.rawValue): \(error)")
        }
    }
}
