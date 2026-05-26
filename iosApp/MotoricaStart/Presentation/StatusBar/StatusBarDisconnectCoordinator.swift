import UIKit
import shared

enum StatusBarDisconnectCoordinator {
    private static let rootTransitionDuration: TimeInterval = 0.35
    private static var isDisconnectFlowInProgress = false

    static func disconnectAndShowScan(from viewController: UIViewController?) {
        guard !isDisconnectFlowInProgress else { return }
        isDisconnectFlowInProgress = true
        defer { isDisconnectFlowInProgress = false }

        let keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
        BLEComponents.shared.bleManager.disconnectFromDevice()
        keyValueStorage.removeValue(for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
        WidgetsTabContainerViewController.sharedStatusBarViewModel.update(
            serialNumber: "—",
            batteryLevel: 0,
            isConnected: false
        )
        UiStateBridge.shared.resetWidgetsState()
        WidgetsListViewController.resetGlobalSynchronizationState()

        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return }
        let bluetoothVC = appDelegate.appDIContainer
            .makeBluetoothSceneDIContainer()
            .makeBluetoothListViewController()

        guard let window = appDelegate.window else {
            viewController?.navigationController?.setViewControllers([bluetoothVC], animated: false)
            return
        }

        let transition = CATransition()
        transition.type = .push
        transition.subtype = .fromRight
        transition.duration = rootTransitionDuration
        transition.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
        window.layer.add(transition, forKey: kCATransition)

        if let rootNavigationController = window.rootViewController as? UINavigationController {
            rootNavigationController.setViewControllers([bluetoothVC], animated: false)
        } else {
            window.rootViewController = StatusBarNavigationController(rootViewController: bluetoothVC)
        }
        window.makeKeyAndVisible()
    }
}
