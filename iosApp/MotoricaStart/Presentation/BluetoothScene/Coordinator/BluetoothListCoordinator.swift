//
//  BluetoothListCoordinator.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import UIKit

final class BluetoothListCoordinator {
    private let navigationController: UINavigationController
    private let diContainer: BluetoothSceneDIContainer

    init(
        navigationController: UINavigationController,
        diContainer: BluetoothSceneDIContainer
    ) {
        self.navigationController = navigationController
        self.diContainer = diContainer
    }

    func start() {
        let vc = diContainer.makeBluetoothListViewController()
        // Показываем сразу без анимации
        navigationController.setViewControllers([vc], animated: false)
    }
}
