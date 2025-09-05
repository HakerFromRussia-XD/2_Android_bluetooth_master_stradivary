//
//  MainTabBarController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.08.2025.
//
import UIKit

final class MainTabBarController: UITabBarController {
    private let appDIContainer: AppDIContainer
    private var didUpdateTabBarFonts = false
    
    private let tabItemTopPadding: CGFloat = 4

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
        tabBar.backgroundColor = UIColor(named: "ubi4_dark_back")
        tabBar.tintColor = UIColor(named: "ubi4_white")
        tabBar.unselectedItemTintColor = UIColor(named: "ubi4_deactivate_text")
        
        applyTabBarContentInsets(topPadding: tabItemTopPadding)
        
        selectedIndex = 1 // Sensors tab by default
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        if !didUpdateTabBarFonts {
            unifyTabBarItemFonts()
            didUpdateTabBarFonts = true
        }
    }

    private func setupTabs() {
        let gesturesVC = Scene0ViewController()
        gesturesVC.tabBarItem = UITabBarItem(title: NSLocalizedString("Gestures", comment: ""), image: UIImage(named: "ic_gestures"), tag: 0)

        let widgetsDI = appDIContainer.makeWidgetsSceneDIContainer()
        let sensorsVC = widgetsDI.makeWidgetsListViewController(actions: .init(
            showWidgetDetails: { _ in },
            showWidgetQueriesSuggestions: { _ in },
            closeWidgetQueriesSuggestions: {}
        ))
        sensorsVC.tabBarItem = UITabBarItem(title: NSLocalizedString("Sensors", comment: ""), image: UIImage(named: "ic_sensors"), tag: 1)

        let trainingVC = Scene1ViewController()
        trainingVC.tabBarItem = UITabBarItem(title: NSLocalizedString("Training", comment: ""), image: UIImage(named: "ic_trophy"), tag: 2)

        let specialVC = Scene2ViewController()
        specialVC.tabBarItem = UITabBarItem(title: NSLocalizedString("Special settings", comment: ""), image: UIImage(named: "ic_mechanics"), tag: 3)

        viewControllers = [gesturesVC, sensorsVC, trainingVC, specialVC]
    }
    
    private func unifyTabBarItemFonts() {
        guard let items = tabBar.items else { return }
        let labels = tabBar.subviews
            .compactMap { $0 as? UIControl }
            .flatMap { $0.subviews.compactMap { $0 as? UILabel } }
        guard let minSize = labels.map({ $0.font.pointSize }).min() else { return }
        let attributes: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: minSize)]
        for item in items {
            item.setTitleTextAttributes(attributes, for: .normal)
            item.setTitleTextAttributes(attributes, for: .selected)
        }
    }
    
    private func applyTabBarContentInsets(topPadding: CGFloat) {
        guard let items = tabBar.items else { return }

        // 1) Смещаем иконки вниз (появляется "воздух" сверху)
        for item in items {
            item.imageInsets = UIEdgeInsets(top: topPadding, left: 0, bottom: -topPadding, right: 0)
            item.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
        }

        // 2) Полностью настраиваем Appearance, чтобы НЕ сломать цвета
        let bg = tabBar.backgroundColor ?? .systemBackground
        let selected = tabBar.tintColor ?? .label
        let unselected = tabBar.unselectedItemTintColor ?? .secondaryLabel

        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()         // без блюра
        appearance.backgroundColor = bg

        func tune(_ layout: inout UITabBarItemAppearance) {
            layout.normal.iconColor = unselected
            layout.selected.iconColor = selected
            layout.normal.titleTextAttributes[.foregroundColor] = unselected
            layout.selected.titleTextAttributes[.foregroundColor] = selected
            layout.normal.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
            layout.selected.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
        }

        tune(&appearance.stackedLayoutAppearance)      // iPhone, подпись под иконкой
        tune(&appearance.inlineLayoutAppearance)       // iPad в сайдбаре/toolbar
        tune(&appearance.compactInlineLayoutAppearance)

        tabBar.standardAppearance = appearance
        if #available(iOS 15.0, *) {
            tabBar.scrollEdgeAppearance = appearance
        }
    }
}

final class Scene0ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = NSLocalizedString("Gestures", comment: "")
    }
}

final class Scene1ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = NSLocalizedString("Training", comment: "")
    }
}

final class Scene2ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = NSLocalizedString("Special settings", comment: "")
    }
}
