import UIKit

final class AppFlowCoordinator {

    var navigationController: UINavigationController
    private let appDIContainer: AppDIContainer
    
    init(
        navigationController: UINavigationController,
        appDIContainer: AppDIContainer
    ) {
        self.navigationController = navigationController
        self.appDIContainer = appDIContainer
    }

    func start() {
        let bleSceneDI = appDIContainer.makeBluetoothSceneDIContainer()
        let flow = bleSceneDI.makeBluetoothListCoordinator(
            navigationController: navigationController
        )
        flow.start()
        let tabBarController = MainTabBarController(appDIContainer: appDIContainer)
        navigationController.setViewControllers([tabBarController], animated: false)
    }
}
