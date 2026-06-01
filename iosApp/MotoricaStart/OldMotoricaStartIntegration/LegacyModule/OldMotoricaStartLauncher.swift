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
    }

    static func seedSmartConnection(with hint: OldMotoricaStartLauncher.ConnectionHint) {
        let normalizedUUID = hint.deviceUUID.uppercased()

        save(key: normalizedUUID, value: hint.deviceName)
        save(key: Key.deviceName, value: hint.deviceName)
        save(key: Key.deviceMac, value: normalizedUUID)
        save(key: Key.lastConnection, value: normalizedUUID)
        save(key: Key.smartConnection, value: "1")
        save(key: Key.deactivateSmartConnection, value: "0")
    }

    private static func save(key: String, value: String) {
        DataManager.save(SaveObjectString(key: key, value: value), with: key)
    }
}
