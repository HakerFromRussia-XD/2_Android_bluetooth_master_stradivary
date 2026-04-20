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
    private var didDisableTabBarContinuousInteractionGestures = false
    private var didInstallTabButtonHighlightSuppressor = false
    
    private let tabItemTopPadding: CGFloat = 4
    private let tabTransitionDuration: TimeInterval = 0.2
    private let synchronizationRestrictedTabTags: Set<Int> = [0, 3]
    private let tabBarBackgroundColor = UIColor(named: "ubi4_back")
        ?? UIColor(named: "ubi4_dark_back")
        ?? UIColor(red: 42 / 255.0, green: 42 / 255.0, blue: 42 / 255.0, alpha: 1.0)
    private let selectedTabItemColor = UIColor(named: "ubi4_white") ?? .white
    private let unselectedTabItemColor = UIColor(named: "ubi4_deactivate_text") ?? UIColor(white: 0.514, alpha: 1)
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

    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
    }

    override var childForStatusBarStyle: UIViewController? {
        nil
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.accessibilityIdentifier = AccessibilityIdentifier.mainTabBarRoot
        setupTabs()
        tabBar.backgroundColor = tabBarBackgroundColor
        tabBar.barTintColor = tabBarBackgroundColor
        tabBar.tintColor = selectedTabItemColor
        tabBar.unselectedItemTintColor = unselectedTabItemColor
        configureTabBarPlatformBehavior()
        delegate = self
        
        applyTabBarContentInsets(topPadding: tabItemTopPadding)
        
        selectedIndex = 1 // Sensors tab by default
        registerKeyboardObservers()
        registerSynchronizationObservers()
        updateSynchronizationRestrictedTabAvailability()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        sanitizeTabBarSelectionOverlaysIfNeeded()
        installTabButtonHighlightSuppressorIfNeeded()
        if !didUpdateTabBarFonts {
            unifyTabBarItemFonts()
            didUpdateTabBarFonts = true
        }
        applyTabBarControlTintFallback()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        setNeedsStatusBarAppearanceUpdate()
        dumpTabBarIfNeeded()
    }

    private func setupTabs() {
        let widgetsDI = appDIContainer.makeWidgetsSceneDIContainer()
        let actions = WidgetsListViewModelActions(
            showWidgetDetails: { _ in },
            showWidgetQueriesSuggestions: { _ in },
            closeWidgetQueriesSuggestions: {}
        )
        
        let gesturesVC = widgetsDI.makeGesturesTabViewController(actions: actions)
        gesturesVC.tabBarItem = UITabBarItem(
            title: NSLocalizedString("Gestures", comment: ""),
            image: tabIcon(named: "ic_gestures"),
            tag: 0
        )
        gesturesVC.tabBarItem.accessibilityIdentifier = AccessibilityIdentifier.mainTabGesturesItem

        let sensorsVC = widgetsDI.makeSensorsTabViewController(actions: actions)
        sensorsVC.tabBarItem = UITabBarItem(
            title: NSLocalizedString("Sensors", comment: ""),
            image: tabIcon(named: "ic_sensors"),
            tag: 1
        )

        let specialVC = widgetsDI.makeSpecialSettingsTabViewController(actions: actions)
        specialVC.tabBarItem = UITabBarItem(
            title: NSLocalizedString("Special settings", comment: ""),
            image: tabIcon(named: "ic_mechanics"),
            tag: 3
        )
        specialVC.tabBarItem.accessibilityIdentifier = AccessibilityIdentifier.mainTabSpecialSettingsItem

        var controllers: [UIViewController] = [gesturesVC, sensorsVC]

        let trainingWidgets = DataFactory().prepareData(display: 3)
        if !trainingWidgets.isEmpty {
            let trainingVC = widgetsDI.makeTrainingTabViewController(actions: actions)
            trainingVC.tabBarItem = UITabBarItem(
                title: NSLocalizedString("Training", comment: ""),
                image: tabIcon(named: "ic_trophy"),
                tag: 2
            )
            controllers.append(trainingVC)
        }

        controllers.append(specialVC)
        viewControllers = controllers
    }

    private func configureTabBarPlatformBehavior() {
        if #available(iOS 18.0, *) {
            mode = .tabBar
        }

        tabBar.isTranslucent = false
        tabBar.clipsToBounds = false
        tabBar.layer.masksToBounds = false
        tabBar.barStyle = .black
        tabBar.itemPositioning = .fill
        tabBar.itemWidth = 0
        tabBar.itemSpacing = 0

        if #available(iOS 26.0, *) {
            // Keep a stable, non-floating tab bar behavior on iOS 26.
            tabBarMinimizeBehavior = .never
            disableTabBarContinuousInteractionGesturesIfNeeded()
        }
    }

    private func dumpTabBarIfNeeded() {
#if DEBUG
        guard ProcessInfo.processInfo.arguments.contains("-ui-test-debug-tabbar") else { return }

        func dumpView(_ view: UIView, indent: String) {
            let className = String(describing: type(of: view))
            print("[TABBAR-DUMP] \(indent)\(className) frame=\(view.frame) alpha=\(view.alpha) hidden=\(view.isHidden)")
            for subview in view.subviews {
                dumpView(subview, indent: indent + "  ")
            }
        }

        print("[TABBAR-DUMP] ===== START =====")
        if #available(iOS 18.0, *) {
            print("[TABBAR-DUMP] mode=\(mode.rawValue)")
        } else {
            print("[TABBAR-DUMP] mode=unavailable(< iOS 18)")
        }
        if #available(iOS 26.0, *) {
            print("[TABBAR-DUMP] minimize=\(tabBarMinimizeBehavior.rawValue)")
        }
        print("[TABBAR-DUMP] tabBar.selectionIndicatorImage.isNil=\(tabBar.selectionIndicatorImage == nil)")
        print("[TABBAR-DUMP] appearance.selectionIndicatorImage.isNil=\(tabBar.standardAppearance.selectionIndicatorImage == nil)")
        print("[TABBAR-DUMP] appearance.selectionIndicatorTintColor=\(String(describing: tabBar.standardAppearance.selectionIndicatorTintColor))")
        dumpView(tabBar, indent: "")
        print("[TABBAR-DUMP] ===== END =====")
#endif
    }

    private func sanitizeTabBarSelectionOverlaysIfNeeded() {
        guard #available(iOS 26.0, *) else { return }
        disableTabBarContinuousInteractionGesturesIfNeeded()
        stripTabBarSystemInteractions()

        tabBar.backgroundImage = UIImage()
        tabBar.shadowImage = UIImage()
        tabBar.selectionIndicatorImage = UIImage()
        tabBar.unselectedItemTintColor = unselectedTabItemColor
        tabBar.tintColor = selectedTabItemColor

        let killTokens = ["selection", "indicator", "highlight", "pill", "capsule"]
        clearSystemOverlays(in: tabBar, killTokens: killTokens, rootTabBarBounds: tabBar.bounds)
    }

    private func clearSystemOverlays(in view: UIView, killTokens: [String], rootTabBarBounds: CGRect) {
        for subview in view.subviews {
            let className = String(describing: type(of: subview)).lowercased()
            let isTokenMatch = killTokens.contains { className.contains($0) }
            let isLargeEffectOverlay =
                (subview is UIVisualEffectView) &&
                !className.contains("background") &&
                subview.frame.width >= rootTabBarBounds.width * 0.7 &&
                subview.frame.height >= rootTabBarBounds.height * 0.45

            if isTokenMatch || isLargeEffectOverlay {
                subview.isHidden = true
                subview.alpha = 0
                subview.backgroundColor = .clear
                subview.layer.backgroundColor = UIColor.clear.cgColor
            }

            clearSystemOverlays(in: subview, killTokens: killTokens, rootTabBarBounds: rootTabBarBounds)
        }
    }
    
    private func unifyTabBarItemFonts() {
        guard let items = tabBar.items else { return }
        let labels = tabBar.subviews
            .compactMap { $0 as? UIControl }
            .flatMap { $0.subviews.compactMap { $0 as? UILabel } }
        guard let minSize = labels.map({ $0.font.pointSize }).min() else { return }
        let selectedColor = selectedTabItemColor
        let normalColor = unselectedTabItemColor
        let normalAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: minSize),
            .foregroundColor: normalColor
        ]
        let selectedAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: minSize),
            .foregroundColor: selectedColor
        ]
        let disabledAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: minSize),
            .foregroundColor: normalColor
        ]
        let focusedAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: minSize),
            .foregroundColor: selectedColor
        ]
        for item in items {
            item.setTitleTextAttributes(normalAttributes, for: .normal)
            item.setTitleTextAttributes(selectedAttributes, for: .selected)
            item.setTitleTextAttributes(disabledAttributes, for: .disabled)
            item.setTitleTextAttributes(focusedAttributes, for: .focused)
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
        let selected = selectedTabItemColor
        let unselected = unselectedTabItemColor

        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()         // без блюра
        appearance.backgroundColor = bg
        appearance.backgroundEffect = nil
        appearance.shadowColor = .clear
        appearance.selectionIndicatorTintColor = .clear
        appearance.selectionIndicatorImage = nil
        appearance.stackedItemPositioning = .fill
        appearance.stackedItemWidth = 0
        appearance.stackedItemSpacing = 0

        func tune(_ layout: inout UITabBarItemAppearance) {
            layout.normal.iconColor = unselected
            layout.selected.iconColor = selected
            layout.disabled.iconColor = unselected
            layout.focused.iconColor = selected
            layout.normal.titleTextAttributes[.foregroundColor] = unselected
            layout.selected.titleTextAttributes[.foregroundColor] = selected
            layout.disabled.titleTextAttributes[.foregroundColor] = unselected
            layout.focused.titleTextAttributes[.foregroundColor] = selected
            layout.normal.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
            layout.selected.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
            layout.disabled.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
            layout.focused.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
        }

        tune(&appearance.stackedLayoutAppearance)      // iPhone, подпись под иконкой
        tune(&appearance.inlineLayoutAppearance)       // iPad в сайдбаре/toolbar
        tune(&appearance.compactInlineLayoutAppearance)

        tabBar.standardAppearance = appearance
        tabBar.tintColor = selected
        tabBar.unselectedItemTintColor = unselected
        tabBar.selectionIndicatorImage = nil
        if #available(iOS 15.0, *) {
            tabBar.scrollEdgeAppearance = appearance
        }
    }

    private func tabIcon(named name: String) -> UIImage? {
        UIImage(named: name)?.withRenderingMode(.alwaysTemplate)
    }

    private func refreshTabBarItemColors() {
        applyTabBarContentInsets(topPadding: tabItemTopPadding)
        unifyTabBarItemFonts()
        sanitizeTabBarSelectionOverlaysIfNeeded()
        applyTabBarControlTintFallback()
    }

    private func applyTabBarControlTintFallback() {
        let controls = tabBar.subviews
            .compactMap { $0 as? UIControl }
            .filter { !$0.isHidden && $0.bounds.width > 0 && $0.bounds.height > 0 }
            .sorted { $0.frame.minX < $1.frame.minX }
        guard !controls.isEmpty else { return }

        let clampedSelectedIndex = max(0, min(selectedIndex, controls.count - 1))

        for (index, control) in controls.enumerated() {
            let isSelectedControl = index == clampedSelectedIndex
            let color = (!control.isEnabled || !control.isUserInteractionEnabled)
                ? unselectedTabItemColor
                : (isSelectedControl ? selectedTabItemColor : unselectedTabItemColor)
            control.tintColor = color
            applyTintRecursively(in: control, color: color)
        }
        tabBar.tintColor = selectedTabItemColor
        tabBar.unselectedItemTintColor = unselectedTabItemColor
    }

    private func applyTintRecursively(in view: UIView, color: UIColor) {
        if let label = view as? UILabel {
            label.textColor = color
            label.highlightedTextColor = color
        } else if let button = view as? UIButton {
            button.setTitleColor(color, for: .normal)
            button.setTitleColor(color, for: .selected)
            button.setTitleColor(color, for: .highlighted)
            button.setTitleColor(color, for: .disabled)
        } else if let imageView = view as? UIImageView {
            imageView.tintColor = color
        }
        for subview in view.subviews {
            applyTintRecursively(in: subview, color: color)
        }
    }

    private func disableTabBarContinuousInteractionGesturesIfNeeded() {
        guard #available(iOS 26.0, *), !didDisableTabBarContinuousInteractionGestures else { return }
        let killTokens = ["longpress", "reorder", "scrub", "edit", "hover", "drag", "lift", "contextmenu"]
        var didDisableAnyGesture = false

        func disableGesturesRecursively(in view: UIView) {
            let recognizers = view.gestureRecognizers ?? []
            for recognizer in recognizers {
                if recognizer is UITapGestureRecognizer {
                    continue
                }
                let className = String(describing: type(of: recognizer)).lowercased()
                let shouldDisableByToken = killTokens.contains { className.contains($0) }
                let shouldDisableLongPress = recognizer is UILongPressGestureRecognizer
                if shouldDisableByToken || shouldDisableLongPress {
                    recognizer.isEnabled = false
                    didDisableAnyGesture = true
                }
            }
            for subview in view.subviews {
                disableGesturesRecursively(in: subview)
            }
        }

        disableGesturesRecursively(in: tabBar)

        if didDisableAnyGesture {
            didDisableTabBarContinuousInteractionGestures = true
        }
    }

    private func stripTabBarSystemInteractions() {
        guard #available(iOS 26.0, *) else { return }

        func strip(in view: UIView) {
            view.showsLargeContentViewer = false
            let interactions = view.interactions
            for interaction in interactions {
                view.removeInteraction(interaction)
            }
            for subview in view.subviews {
                strip(in: subview)
            }
        }

        strip(in: tabBar)
    }

    private func installTabButtonHighlightSuppressorIfNeeded() {
        guard #available(iOS 26.0, *), !didInstallTabButtonHighlightSuppressor else { return }
        let controls = tabBar.subviews.compactMap { $0 as? UIControl }
        guard !controls.isEmpty else { return }

        for control in controls {
            control.addTarget(self, action: #selector(handleTabButtonTouchDown(_:)), for: [.touchDown, .touchDragEnter])
            control.addTarget(self, action: #selector(handleTabButtonTouchMove(_:)), for: .touchDragInside)
            control.addTarget(self, action: #selector(handleTabButtonTouchEnd(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel, .touchDragExit])
        }

        didInstallTabButtonHighlightSuppressor = true
    }

    @objc
    private func handleTabButtonTouchDown(_ sender: UIControl) {
        suppressTabButtonHighlight(sender)
    }

    @objc
    private func handleTabButtonTouchMove(_ sender: UIControl) {
        suppressTabButtonHighlight(sender)
    }

    @objc
    private func handleTabButtonTouchEnd(_ sender: UIControl) {
        suppressTabButtonHighlight(sender)
        DispatchQueue.main.async { [weak self] in
            self?.refreshTabBarItemColors()
        }
    }

    private func suppressTabButtonHighlight(_ control: UIControl) {
        stripTabBarSystemInteractions()
        if control.isHighlighted {
            control.isHighlighted = false
        }
        clearTabButtonPressedOverlays(in: control)
        if #available(iOS 26.0, *) {
            let killTokens = ["selection", "indicator", "highlight", "pill", "capsule", "pressed"]
            clearSystemOverlays(in: tabBar, killTokens: killTokens, rootTabBarBounds: tabBar.bounds)
        }
    }

    private func clearTabButtonPressedOverlays(in view: UIView) {
        let className = String(describing: type(of: view)).lowercased()
        let killTokens = ["highlight", "pressed", "selection", "indicator", "pill", "capsule"]
        if killTokens.contains(where: { className.contains($0) }) {
            view.alpha = 0
            view.isHidden = true
            view.backgroundColor = .clear
            view.layer.backgroundColor = UIColor.clear.cgColor
        }

        for subview in view.subviews {
            clearTabButtonPressedOverlays(in: subview)
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

    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        refreshTabBarItemColors()
        DispatchQueue.main.async { [weak self] in
            self?.refreshTabBarItemColors()
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) { [weak self] in
            self?.refreshTabBarItemColors()
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) { [weak self] in
            self?.refreshTabBarItemColors()
        }
    }

    func tabBarController(_ tabBarController: UITabBarController,
                          animationControllerForTransitionFrom fromVC: UIViewController,
                          to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        nil
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

        toView.frame = transitionContext.finalFrame(for: toViewController)
        toView.alpha = 0

        container.insertSubview(toView, aboveSubview: fromView)

        UIView.animate(withDuration: duration, delay: 0, options: [.curveEaseInOut], animations: {
            toView.alpha = 1
            fromView.alpha = 0.98
        }, completion: { finished in
            fromView.alpha = 1
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
