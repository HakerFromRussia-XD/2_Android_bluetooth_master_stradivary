import UIKit


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    let appDIContainer = AppDIContainer()
    private var appFlowCoordinator: AppFlowCoordinator?
    var window: UIWindow?
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        
        AppAppearance.setupAppearance()
        
        
        let navigationController = UINavigationController()
        let ubi4BackgroundColor = UIColor(named: "ubi4_back") ?? UIColor.black
        navigationController.view.backgroundColor = ubi4BackgroundColor
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.backgroundColor = ubi4BackgroundColor
        window?.rootViewController = navigationController
        window?.makeKeyAndVisible()
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
