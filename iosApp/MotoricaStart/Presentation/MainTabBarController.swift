//
//  MainTabBarController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.08.2025.
//
import UIKit

final class MainTabBarController: UITabBarController {
    private let appDIContainer: AppDIContainer

    init(appDIContainer: AppDIContainer) {
        self.appDIContainer = appDIContainer
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupTabs()
        selectedIndex = 1 // Sensors tab by default
    }

    private func setupTabs() {
        let gesturesVC = Scene0ViewController()
        gesturesVC.tabBarItem = UITabBarItem(title: "Gestures", image: UIImage(named: "ic_gestures"), tag: 0)

        let widgetsDI = appDIContainer.makeWidgetsSceneDIContainer()
        let sensorsVC = widgetsDI.makeWidgetsListViewController(actions: .init(
            showWidgetDetails: { _ in },
            showWidgetQueriesSuggestions: { _ in },
            closeWidgetQueriesSuggestions: {}
        ))
        sensorsVC.tabBarItem = UITabBarItem(title: "Sensors", image: UIImage(named: "ic_sensors"), tag: 1)

        let trainingVC = Scene1ViewController()
        trainingVC.tabBarItem = UITabBarItem(title: "Training", image: UIImage(named: "trophy"), tag: 2)

        let specialVC = Scene2ViewController()
        specialVC.tabBarItem = UITabBarItem(title: "Special settings", image: UIImage(named: "ic_mechanics"), tag: 3)

        viewControllers = [gesturesVC, sensorsVC, trainingVC, specialVC]
    }
}

final class Scene0ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "Scene0"
    }
}

final class Scene1ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "Scene1"
    }
}

final class Scene2ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "Scene2"
    }
}
