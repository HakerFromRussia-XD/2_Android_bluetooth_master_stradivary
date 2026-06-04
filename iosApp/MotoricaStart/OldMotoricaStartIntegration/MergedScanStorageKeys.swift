import Foundation

enum MergedProsthesisFamily: String, Codable {
    case newKmm
    case oldLegacy
    case unknown
}

enum MergedScanAppearanceMode: Equatable {
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

extension Notification.Name {
    static let smartConnectionSettingsDidChange = Notification.Name("SmartConnectionSettingsStore.didChange")
}

struct SmartConnectionSettingsStore {
    static let mobileSettingsKeyAutoLogin = "AUTO_LOGIN"

    private enum Key {
        static let newAutoConnection = "SET_MODE_SMART_CONNECTION"
        static let legacySmartConnection = "SMART_CONNECTION"
        static let legacyDeactivateSmartConnection = "DEACTIVATE_SMART_CONNECTION"
    }

    private struct LegacySaveObjectString: Codable {
        let key: String
        let value: String
    }

    private let userDefaults: UserDefaults
    private let fileManager: FileManager
    private let documentsDirectory: URL

    init(
        userDefaults: UserDefaults = .standard,
        fileManager: FileManager = .default,
        documentsDirectory: URL? = nil
    ) {
        self.userDefaults = userDefaults
        self.fileManager = fileManager
        self.documentsDirectory = documentsDirectory
            ?? fileManager.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
    }

    var isEnabled: Bool {
        currentValue()
    }

    var isScanAutoConnectionAllowed: Bool {
        currentValue() && !(loadLegacyBool(for: Key.legacyDeactivateSmartConnection) ?? false)
    }

    @discardableResult
    func currentValue(defaultValue: Bool = true) -> Bool {
        if let legacyValue = loadLegacyBool(for: Key.legacySmartConnection) {
            mirrorToUserDefaults(legacyValue)
            return legacyValue
        }

        if userDefaults.object(forKey: Key.newAutoConnection) != nil {
            let value = userDefaults.bool(forKey: Key.newAutoConnection)
            saveLegacyBool(value, for: Key.legacySmartConnection)
            return value
        }

        saveLegacyBool(defaultValue, for: Key.legacySmartConnection)
        mirrorToUserDefaults(defaultValue)
        return defaultValue
    }

    func setEnabled(_ enabled: Bool, notify: Bool = true) {
        saveLegacyBool(enabled, for: Key.legacySmartConnection)
        if enabled {
            saveLegacyBool(false, for: Key.legacyDeactivateSmartConnection)
        }
        mirrorToUserDefaults(enabled)

        guard notify else { return }
        NotificationCenter.default.post(
            name: .smartConnectionSettingsDidChange,
            object: nil,
            userInfo: ["isEnabled": enabled]
        )
    }

    func deactivateScanAutoConnectionUntilNextLaunch() {
        saveLegacyBool(true, for: Key.legacyDeactivateSmartConnection)
    }

    func resetScanAutoConnectionDeactivationForLaunch() {
        saveLegacyBool(false, for: Key.legacyDeactivateSmartConnection)
    }

    private func mirrorToUserDefaults(_ enabled: Bool) {
        userDefaults.set(enabled, forKey: Key.newAutoConnection)
    }

    private func legacyURL(for key: String) -> URL {
        documentsDirectory.appendingPathComponent(key, isDirectory: false)
    }

    private func loadLegacyBool(for key: String) -> Bool? {
        let url = legacyURL(for: key)
        guard let data = try? Data(contentsOf: url),
              let object = try? JSONDecoder().decode(LegacySaveObjectString.self, from: data),
              object.key == key else {
            return nil
        }
        return boolValue(from: object.value)
    }

    private func saveLegacyBool(_ value: Bool, for key: String) {
        do {
            try fileManager.createDirectory(at: documentsDirectory, withIntermediateDirectories: true)
            let object = LegacySaveObjectString(key: key, value: value ? "1" : "0")
            let data = try JSONEncoder().encode(object)
            try data.write(to: legacyURL(for: key), options: .atomic)
        } catch {
            print("[SmartConnectionSettingsStore] failed to save \(key): \(error)")
        }
    }

    private func boolValue(from rawValue: String) -> Bool {
        switch rawValue.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "1", "true", "yes":
            return true
        case "0", "false", "no":
            return false
        default:
            return (rawValue as NSString).boolValue
        }
    }
}

struct SmartScanAutoConnectionTargetStore {
    private enum LegacyKey {
        static let deviceName = "DEVICE_NAME"
        static let deviceMac = "DEVICE_MAC"
        static let lastConnection = "LAST_CONNECTION"
    }

    private struct LegacySaveObjectString: Codable {
        let key: String
        let value: String
    }

    private let keyValueStorage: KeyValueStorage
    private let fileManager: FileManager
    private let documentsDirectory: URL

    init(
        keyValueStorage: KeyValueStorage,
        fileManager: FileManager = .default,
        documentsDirectory: URL? = nil
    ) {
        self.keyValueStorage = keyValueStorage
        self.fileManager = fileManager
        self.documentsDirectory = documentsDirectory
            ?? fileManager.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
    }

    func targetUUIDs() -> Set<String> {
        var values: [String] = []

        if let lastNewUUID: String = try? keyValueStorage.load(for: MergedScanStorageKeys.lastNewDeviceUUID) {
            values.append(lastNewUUID)
        }
        if let lastOldUUID: String = try? keyValueStorage.load(for: MergedScanStorageKeys.lastOldDeviceUUID) {
            values.append(lastOldUUID)
        }
        if let legacyLastConnection = loadLegacyString(for: LegacyKey.lastConnection) {
            values.append(legacyLastConnection)
        }
        if let legacyDeviceMac = loadLegacyString(for: LegacyKey.deviceMac) {
            values.append(legacyDeviceMac)
        }

        return Set(values.compactMap(Self.normalizedUUID))
    }

    func targetNames() -> Set<String> {
        var values: [String] = []

        if let lastNewName: String = try? keyValueStorage.load(for: MergedScanStorageKeys.lastNewDeviceName) {
            values.append(lastNewName)
        }
        if let lastOldName: String = try? keyValueStorage.load(for: MergedScanStorageKeys.lastOldDeviceName) {
            values.append(lastOldName)
        }
        if let legacyDeviceName = loadLegacyString(for: LegacyKey.deviceName) {
            values.append(legacyDeviceName)
        }

        return Set(values.compactMap(Self.normalizedName))
    }

    static func normalizedUUID(_ rawValue: String) -> String? {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if let uuid = UUID(uuidString: trimmed) {
            return uuid.uuidString.uppercased()
        }
        return trimmed.uppercased()
    }

    static func normalizedName(_ rawValue: String) -> String? {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        return trimmed.uppercased()
    }

    private func legacyURL(for key: String) -> URL {
        documentsDirectory.appendingPathComponent(key, isDirectory: false)
    }

    private func loadLegacyString(for key: String) -> String? {
        let url = legacyURL(for: key)
        guard fileManager.fileExists(atPath: url.path),
              let data = try? Data(contentsOf: url),
              let object = try? JSONDecoder().decode(LegacySaveObjectString.self, from: data),
              object.key == key else {
            return nil
        }
        return object.value
    }
}
