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
    
        
        
        
        
        let navigationController = StatusBarNavigationController()
        let ubi4BackgroundColor = UIColor(named: "ubi4_back") ?? UIColor.black
        navigationController.view.backgroundColor = ubi4BackgroundColor
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.backgroundColor = ubi4BackgroundColor
        window?.rootViewController = navigationController
        window?.makeKeyAndVisible()
        installStatusBarOverlay(backgroundColor: ubi4BackgroundColor)
        DispatchQueue.main.async { [weak self] in
            self?.installStatusBarOverlay(backgroundColor: ubi4BackgroundColor)
        }
        appFlowCoordinator = AppFlowCoordinator(
            navigationController: navigationController,
            appDIContainer: appDIContainer
        )
        appFlowCoordinator?.start()
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        CoreDataStorage.shared.saveContext()
    }

    private func installStatusBarOverlay(backgroundColor: UIColor) {
        guard let window else { return }

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
