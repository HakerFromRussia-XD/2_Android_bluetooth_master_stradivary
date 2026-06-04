import UIKit

public final class OldMotoricaStartLauncher {
    public struct ConnectionHint {
        public let deviceName: String
        public let deviceUUID: String

        public init(deviceName: String, deviceUUID: String) {
            self.deviceName = deviceName
            self.deviceUUID = deviceUUID
        }
    }

    public init() {}

    public func makeRootViewController(connectionHint: ConnectionHint?) -> UIViewController {
        if let connectionHint = connectionHint {
            LegacyConnectionStateStore.seedSmartConnection(with: connectionHint)
        }

        let storyboard = UIStoryboard(name: "Main", bundle: Bundle(for: OldMotoricaStartLauncher.self))
        return storyboard.instantiateInitialViewController() ?? UIViewController()
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
        save(key: Key.smartConnection, value: "1")
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
