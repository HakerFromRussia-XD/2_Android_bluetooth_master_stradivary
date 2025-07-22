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
        // In App Flow we can check if user needs to login, if yes we would run login flow
//        let moviesSceneDIContainer = appDIContainer.makeMoviesSceneDIContainer()
//        let flow = moviesSceneDIContainer.makeMoviesSearchFlowCoordinator(navigationController: navigationController)
//        flow.start()
        // Теперь основной экран — список BLE-устройств
        let bleSceneDI = appDIContainer.makeBluetoothSceneDIContainer()
        let flow = bleSceneDI.makeBluetoothListCoordinator(
            navigationController: navigationController
        )
        flow.start()
    }
}
