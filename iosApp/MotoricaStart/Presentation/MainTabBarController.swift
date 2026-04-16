//
//  MainTabBarController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.08.2025.
//
import UIKit
import shared

final class MainTabBarController: UITabBarController {
    private let appDIContainer: AppDIContainer
    private var didUpdateTabBarFonts = false
    
    private let tabItemTopPadding: CGFloat = 4
    private let tabTransitionDuration: TimeInterval = 0.2
    private let synchronizationRestrictedTabTags: Set<Int> = [0, 3]
    private var keyboardWillShowObserver: NSObjectProtocol?
    private var keyboardWillHideObserver: NSObjectProtocol?
    private var synchronizationStateObserver: NSObjectProtocol?
    
    init(appDIContainer: AppDIContainer) {
        self.appDIContainer = appDIContainer
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    static func instantiateViewController(_ bundle: Bundle? = nil) -> WidgetsListViewController {
        let preferredBundle = bundle ?? Bundle(for: Self.self)
        let storyboardNames = ["WidgetsListViewController", "SensorWidgetsListViewController"]

        for name in storyboardNames {
            if preferredBundle.path(forResource: name, ofType: "storyboardc") != nil {
                let storyboard = UIStoryboard(name: name, bundle: preferredBundle)
                if let controller = storyboard.instantiateInitialViewController() as? WidgetsListViewController {
                    return controller
                }
            }
        }

        fatalError("Cannot instantiate WidgetsListViewController from expected storyboards: \(storyboardNames)")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.accessibilityIdentifier = AccessibilityIdentifier.mainTabBarRoot
        setupTabs()
        tabBar.backgroundColor = UIColor(named: "ubi4_dark_back")
        tabBar.tintColor = UIColor(named: "ubi4_white")
        tabBar.unselectedItemTintColor = UIColor(named: "ubi4_deactivate_text")
        delegate = self
        
        applyTabBarContentInsets(topPadding: tabItemTopPadding)
        
        selectedIndex = 1 // Sensors tab by default
        registerKeyboardObservers()
        registerSynchronizationObservers()
        updateSynchronizationRestrictedTabAvailability()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        if !didUpdateTabBarFonts {
            unifyTabBarItemFonts()
            didUpdateTabBarFonts = true
        }
    }

    private func setupTabs() {
        let widgetsDI = appDIContainer.makeWidgetsSceneDIContainer()
        let actions = WidgetsListViewModelActions(
            showWidgetDetails: { _ in },
            showWidgetQueriesSuggestions: { _ in },
            closeWidgetQueriesSuggestions: {}
        )
        
        let gesturesVC = widgetsDI.makeGesturesTabViewController(actions: actions)
        gesturesVC.tabBarItem = UITabBarItem(title: NSLocalizedString("Gestures", comment: ""), image: UIImage(named: "ic_gestures"), tag: 0)
        gesturesVC.tabBarItem.accessibilityIdentifier = AccessibilityIdentifier.mainTabGesturesItem

        let sensorsVC = widgetsDI.makeSensorsTabViewController(actions: actions)
        sensorsVC.tabBarItem = UITabBarItem(title: NSLocalizedString("Sensors", comment: ""), image: UIImage(named: "ic_sensors"), tag: 1)

        let specialVC = widgetsDI.makeSpecialSettingsTabViewController(actions: actions)
        specialVC.tabBarItem = UITabBarItem(title: NSLocalizedString("Special settings", comment: ""), image: UIImage(named: "ic_mechanics"), tag: 3)
        specialVC.tabBarItem.accessibilityIdentifier = AccessibilityIdentifier.mainTabSpecialSettingsItem

        var controllers: [UIViewController] = [gesturesVC, sensorsVC]

        let trainingWidgets = DataFactory().prepareData(display: 3)
        if !trainingWidgets.isEmpty {
            let trainingVC = widgetsDI.makeTrainingTabViewController(actions: actions)
            trainingVC.tabBarItem = UITabBarItem(
                title: NSLocalizedString("Training", comment: ""),
                image: UIImage(named: "ic_trophy"),
                tag: 2
            )
            controllers.append(trainingVC)
        }

        controllers.append(specialVC)
        viewControllers = controllers
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

    private func registerKeyboardObservers() {
        keyboardWillShowObserver = NotificationCenter.default.addObserver(
            forName: UIResponder.keyboardWillShowNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.setTabBar(hidden: true, notification: notification)
        }

        keyboardWillHideObserver = NotificationCenter.default.addObserver(
            forName: UIResponder.keyboardWillHideNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.setTabBar(hidden: false, notification: notification)
        }
    }

    private func registerSynchronizationObservers() {
        synchronizationStateObserver = NotificationCenter.default.addObserver(
            forName: .widgetsSynchronizationStateDidChange,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.updateSynchronizationRestrictedTabAvailability()
        }
    }

    private func updateSynchronizationRestrictedTabAvailability() {
        guard let controllers = viewControllers else { return }
        let canOpenRestrictedTabs = WidgetsListViewController.isGlobalSynchronizationCompleted
        let selectedTag = selectedViewController?.tabBarItem.tag
        for controller in controllers where isSynchronizationRestrictedTab(controller) {
            let isSelectedTab = controller.tabBarItem.tag == selectedTag
            controller.tabBarItem.isEnabled = canOpenRestrictedTabs || isSelectedTab
        }
    }

    private func isSynchronizationRestrictedTab(_ viewController: UIViewController) -> Bool {
        synchronizationRestrictedTabTags.contains(viewController.tabBarItem.tag)
    }

    private func setTabBar(hidden: Bool, notification: Notification) {
        let duration = (notification.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? NSNumber)?.doubleValue ?? 0.25
        let rawCurve = (notification.userInfo?[UIResponder.keyboardAnimationCurveUserInfoKey] as? NSNumber)?.intValue ?? UIView.AnimationCurve.easeInOut.rawValue
        let options = UIView.AnimationOptions(rawValue: UInt(rawCurve << 16))

        UIView.animate(withDuration: duration, delay: 0, options: options) {
            self.tabBar.alpha = hidden ? 0 : 1
            self.tabBar.transform = hidden
                ? CGAffineTransform(translationX: 0, y: self.tabBar.bounds.height)
                : .identity
        }
    }

    deinit {
        if let keyboardWillShowObserver {
            NotificationCenter.default.removeObserver(keyboardWillShowObserver)
        }
        if let keyboardWillHideObserver {
            NotificationCenter.default.removeObserver(keyboardWillHideObserver)
        }
        if let synchronizationStateObserver {
            NotificationCenter.default.removeObserver(synchronizationStateObserver)
        }
    }
}

extension MainTabBarController: UITabBarControllerDelegate {
    func tabBarController(_ tabBarController: UITabBarController, shouldSelect viewController: UIViewController) -> Bool {
        if tabBarController.selectedViewController === viewController {
            return true
        }

        guard isSynchronizationRestrictedTab(viewController) else { return true }
        return WidgetsListViewController.isGlobalSynchronizationCompleted
    }

    func tabBarController(_ tabBarController: UITabBarController,
                          animationControllerForTransitionFrom fromVC: UIViewController,
                          to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        TabBarFadeAnimator(duration: tabTransitionDuration)
    }
}

private final class TabBarFadeAnimator: NSObject, UIViewControllerAnimatedTransitioning {
    private let duration: TimeInterval

    init(duration: TimeInterval) {
        self.duration = duration
        super.init()
    }

    func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        duration
    }

    func animateTransition(using transitionContext: UIViewControllerContextTransitioning) {
        guard let fromView = transitionContext.view(forKey: .from),
              let toView = transitionContext.view(forKey: .to),
              let toViewController = transitionContext.viewController(forKey: .to) else {
            transitionContext.completeTransition(false)
            return
        }

        let container = transitionContext.containerView
        let dimmingView = UIView(frame: container.bounds)
        dimmingView.backgroundColor = UIColor.black
        dimmingView.alpha = 0

        toView.frame = transitionContext.finalFrame(for: toViewController)
        toView.alpha = 0

        container.insertSubview(toView, aboveSubview: fromView)
        container.addSubview(dimmingView)

        UIView.animateKeyframes(withDuration: duration, delay: 0, options: [.calculationModeCubic], animations: {
            UIView.addKeyframe(withRelativeStartTime: 0, relativeDuration: 0.5) {
                dimmingView.alpha = 0.18
            }
            UIView.addKeyframe(withRelativeStartTime: 0, relativeDuration: 1) {
                toView.alpha = 1
                fromView.alpha = 0.6
            }
            UIView.addKeyframe(withRelativeStartTime: 0.5, relativeDuration: 0.5) {
                dimmingView.alpha = 0
            }
        }, completion: { finished in
            fromView.alpha = 1
            dimmingView.removeFromSuperview()
            let completed = finished && !transitionContext.transitionWasCancelled
            if completed {
                fromView.removeFromSuperview()
            } else {
                toView.removeFromSuperview()
            }
            transitionContext.completeTransition(completed)
        })
    }
}
