//
//  BluetoothSceneDIContainer.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//

import UIKit
import Combine

final class BluetoothSceneDIContainer {

    struct Dependencies {
        let bluetoothRepository: BluetoothRepository
    }

    private let dependencies: Dependencies

    init(dependencies: Dependencies) {
        self.dependencies = dependencies
    }

    // MARK: - View & ViewModel
    func makeBluetoothListViewController() -> BluetoothListViewController {
        let vm = BluetoothListViewModel(repository: dependencies.bluetoothRepository)
        // Будем грузить из сториборда
        let storyboard = UIStoryboard(name: "BluetoothList", bundle: nil)
        let vc = storyboard.instantiateViewController(
            identifier: BluetoothListViewController.storyboardID,
            creator: { coder in
                BluetoothListViewController(coder: coder, viewModel: vm)
            }
        )
        return vc
    }

    // MARK: - Coordinator
    func makeBluetoothListCoordinator(
        navigationController: UINavigationController
    ) -> BluetoothListCoordinator {
        return BluetoothListCoordinator(
            navigationController: navigationController,
            diContainer: self
        )
    }
}
