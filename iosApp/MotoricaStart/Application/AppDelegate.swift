import UIKit
import shared

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    let appDIContainer = AppDIContainer()
    private var appFlowCoordinator: AppFlowCoordinator?
    var window: UIWindow?
    
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        
        SharedBootstrapper.shared.initialize()
        AppAppearance.setupAppearance()
    
        
        
        
        
        let navigationController = UINavigationController()
        let ubi4BackgroundColor = UIColor(named: "ubi4_back") ?? UIColor.black
        navigationController.view.backgroundColor = ubi4BackgroundColor
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.backgroundColor = ubi4BackgroundColor
        window?.rootViewController = navigationController
        window?.makeKeyAndVisible()
        if #available(iOS 13.0, *) {
            if let statusBarFrame = window?.windowScene?.statusBarManager?.statusBarFrame {
                let statusBarView = UIView(frame: statusBarFrame)
                statusBarView.backgroundColor = ubi4BackgroundColor
                statusBarView.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
                window?.addSubview(statusBarView)
            }
        } else {
            let statusBarView = UIView(frame: UIApplication.shared.statusBarFrame)
            statusBarView.backgroundColor = ubi4BackgroundColor
            statusBarView.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
            window?.addSubview(statusBarView)
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
}
