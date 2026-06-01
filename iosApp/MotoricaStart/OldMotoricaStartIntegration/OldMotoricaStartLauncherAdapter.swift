import UIKit
import OldMotoricaStart

final class OldMotoricaStartLauncherAdapter {
    private let launcher = OldMotoricaStartLauncher()

    func makeRootViewController(for device: BLEDevice) -> UIViewController {
        launcher.makeRootViewController(
            connectionHint: .init(
                deviceName: device.name,
                deviceUUID: device.uuid.uuidString
            )
        )
    }
}
