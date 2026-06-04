import UIKit

public final class OldMotoricaStartLauncher {
    public enum LaunchMode {
        case showScan
        case connectToHint
    }

    public struct ConnectionHint {
        public let deviceName: String
        public let deviceUUID: String

        public init(deviceName: String, deviceUUID: String) {
            self.deviceName = deviceName
            self.deviceUUID = deviceUUID
        }
    }

    public init() {}

    public func makeRootViewController(
        connectionHint: ConnectionHint?,
        launchMode: LaunchMode = .showScan
    ) -> UIViewController {
        if let connectionHint = connectionHint {
            LegacyConnectionStateStore.seedSmartConnection(with: connectionHint)
        }

        let storyboard = UIStoryboard(name: "Main", bundle: Bundle(for: OldMotoricaStartLauncher.self))
        let rootViewController = storyboard.instantiateInitialViewController() ?? UIViewController()
        if launchMode == .connectToHint, let connectionHint {
            configureDirectConnection(on: rootViewController, connectionHint: connectionHint)
        }
        return rootViewController
    }

    private func configureDirectConnection(on rootViewController: UIViewController, connectionHint: ConnectionHint) {
        if let scanViewController = rootViewController as? ScanViewController {
            scanViewController.directConnectionHint = connectionHint
            return
        }

        guard let navigationController = rootViewController as? UINavigationController else { return }
        navigationController.viewControllers
            .compactMap { $0 as? ScanViewController }
            .first?
            .directConnectionHint = connectionHint
    }
}

private enum LegacyConnectionStateStore {
    private enum Key {
        static let deviceName = "DEVICE_NAME"
        static let deviceMac = "DEVICE_MAC"
        static let lastConnection = "LAST_CONNECTION"
        static let smartConnection = "SMART_CONNECTION"
        static let deactivateSmartConnection = "DEACTIVATE_SMART_CONNECTION"
        static let statusConnection = "STATUS_CONNECTION"
        static let useMultigrab = "USE_MULTIGRAB"
        static let useMultigrabFestH = "USE_MULTIGRAB_FESTH"
        static let useMultigrabFestX = "USE_MULTIGRAB_FESTX"
    }

    static func seedSmartConnection(with hint: OldMotoricaStartLauncher.ConnectionHint) {
        let normalizedUUID = hint.deviceUUID.uppercased()
        let isFestX = hint.deviceName.localizedCaseInsensitiveContains("FEST-X")
        let isFestH = hint.deviceName.localizedCaseInsensitiveContains("FEST-H")
        let isKnownLegacyMultigrab = isFestX || isFestH ||
            hint.deviceName.localizedCaseInsensitiveContains("FEST-F") ||
            hint.deviceName.localizedCaseInsensitiveContains("BT05")

        save(key: normalizedUUID, value: hint.deviceName)
        save(key: Key.deviceName, value: hint.deviceName)
        save(key: Key.deviceMac, value: normalizedUUID)
        save(key: Key.lastConnection, value: normalizedUUID)
        LegacySmartConnectionStateStore.ensureDefaultIfNeeded()
        save(key: Key.deactivateSmartConnection, value: "0")
        save(key: Key.statusConnection, value: "1")
        save(key: Key.useMultigrab, value: isKnownLegacyMultigrab ? "USE" : "0")
        save(key: Key.useMultigrabFestH, value: isFestH ? "1" : "0")
        save(key: Key.useMultigrabFestX, value: isFestX ? "1" : "0")
    }

    private static func save(key: String, value: String) {
        DataManager.save(SaveObjectString(key: key, value: value), with: key)
    }
}

extension Notification.Name {
    static let smartConnectionSettingsDidChange = Notification.Name("SmartConnectionSettingsStore.didChange")
}

enum LegacySmartConnectionStateStore {
    private enum Key {
        static let newAutoConnection = "SET_MODE_SMART_CONNECTION"
        static let legacySmartConnection = "SMART_CONNECTION"
        static let legacyDeactivateSmartConnection = "DEACTIVATE_SMART_CONNECTION"
    }

    static func ensureDefaultIfNeeded(defaultValue: Bool = true) {
        _ = currentValue(defaultValue: defaultValue)
    }

    static func currentValue(defaultValue: Bool = true) -> Bool {
        if let legacyValue = loadLegacyBool(for: Key.legacySmartConnection) {
            mirrorToUserDefaults(legacyValue)
            return legacyValue
        }

        if UserDefaults.standard.object(forKey: Key.newAutoConnection) != nil {
            let value = UserDefaults.standard.bool(forKey: Key.newAutoConnection)
            saveLegacyBool(value, for: Key.legacySmartConnection)
            return value
        }

        saveLegacyBool(defaultValue, for: Key.legacySmartConnection)
        mirrorToUserDefaults(defaultValue)
        return defaultValue
    }

    static func setEnabled(_ enabled: Bool, notify: Bool = true) {
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

    static func isAutoConnectionAllowed() -> Bool {
        currentValue() && !(loadLegacyBool(for: Key.legacyDeactivateSmartConnection) ?? false)
    }

    private static func mirrorToUserDefaults(_ enabled: Bool) {
        UserDefaults.standard.set(enabled, forKey: Key.newAutoConnection)
    }

    private static func loadLegacyBool(for key: String) -> Bool? {
        guard let data = try? Data(contentsOf: legacyURL(for: key)),
              let object = try? JSONDecoder().decode(SaveObjectString.self, from: data),
              object.key == key else {
            return nil
        }
        return boolValue(from: object.value)
    }

    private static func saveLegacyBool(_ value: Bool, for key: String) {
        DataManager.save(SaveObjectString(key: key, value: value ? "1" : "0"), with: key)
    }

    private static func legacyURL(for key: String) -> URL {
        FileManager.default
            .urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(key, isDirectory: false)
    }

    private static func boolValue(from rawValue: String) -> Bool {
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
