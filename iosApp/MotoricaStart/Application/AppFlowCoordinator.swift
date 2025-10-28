import UIKit

final class AppFlowCoordinator {

    var navigationController: UINavigationController
    private let appDIContainer: AppDIContainer
    private var bluetoothCoordinator: BluetoothListCoordinator?
    
    init(
        navigationController: UINavigationController,
        appDIContainer: AppDIContainer
    ) {
        self.navigationController = navigationController
        self.appDIContainer = appDIContainer
    }

    func start() {
        let bluetoothDI = appDIContainer.makeBluetoothSceneDIContainer()
        let coordinator = bluetoothDI.makeBluetoothListCoordinator(
            navigationController: navigationController
        )
        bluetoothCoordinator = coordinator
        coordinator.start()
    }
}
