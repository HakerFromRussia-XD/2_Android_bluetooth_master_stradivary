import UIKit
import shared

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    let appDIContainer = AppDIContainer()
    private var appFlowCoordinator: AppFlowCoordinator?
    var window: UIWindow?
    private weak var statusBarOverlayView: UIView?
    
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        
        SharedBootstrapper.shared.initialize()
        AppAppearance.setupAppearance()
        FirmwareDocumentsDirectory.prepareSharedFolder()
        SmartConnectionSettingsStore().resetScanAutoConnectionDeactivationForLaunch()
        BleLogSettings.syncSharedStore()
    
        
        
        
        
        let navigationController = StatusBarNavigationController()
        let ubi4BackgroundColor = UIColor(named: "ubi4_back") ?? UIColor.black
        navigationController.view.backgroundColor = ubi4BackgroundColor
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.backgroundColor = ubi4BackgroundColor
        window?.rootViewController = navigationController
        window?.makeKeyAndVisible()
        updateStatusBarOverlay(backgroundColor: ubi4BackgroundColor)
        DispatchQueue.main.async { [weak self] in
            guard let self, let backgroundColor = self.window?.backgroundColor else { return }
            self.updateStatusBarOverlay(backgroundColor: backgroundColor)
        }
        appFlowCoordinator = AppFlowCoordinator(
            navigationController: navigationController,
            appDIContainer: appDIContainer
        )
        appFlowCoordinator?.start()
        LegacyBleCommandBridge.startIfNeeded()
        LegacyBleCommandProbe.startIfNeeded { [weak self] in
            self?.window
        }
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        CoreDataStorage.shared.saveContext()
    }

    func updateStatusBarOverlay(backgroundColor: UIColor) {
        guard let window else { return }
        window.backgroundColor = backgroundColor

        // On Dynamic Island devices statusBarFrame can be shorter than safeAreaInsets.top.
        // We fill the whole top safe area to avoid a visible seam.
        let statusBarHeight = window.windowScene?.statusBarManager?.statusBarFrame.height ?? 0
        let overlayHeight = max(window.safeAreaInsets.top, statusBarHeight)
        guard overlayHeight > 0 else { return }

        let overlay: UIView
        if let existingOverlay = statusBarOverlayView {
            overlay = existingOverlay
        } else {
            overlay = UIView()
            overlay.isUserInteractionEnabled = false
            overlay.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
            window.addSubview(overlay)
            statusBarOverlayView = overlay
        }

        overlay.frame = CGRect(x: 0, y: 0, width: window.bounds.width, height: overlayHeight)
        overlay.backgroundColor = backgroundColor
    }
}
