//
//  MainTabBarController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.08.2025.
//
import UIKit
import shared

final class MainTabBarController: UITabBarController {
    private struct TabBarContentDescriptor {
        let title: String
        let imageName: String
        let tag: Int
    }

    private struct NativeTabBarContentLayout {
        let iconFrame: CGRect?
        let titleFrame: CGRect?
        let titleFont: UIFont?
    }

    private enum TabTag {
        static let gestures = 0
        static let sensors = 1
        static let training = 2
        static let specialSettings = 3
        static let serviceSettings = 4
    }

    private let appDIContainer: AppDIContainer
    private var didUpdateTabBarFonts = false
    private var didDisableTabBarContinuousInteractionGestures = false
    private var didInstallTabButtonHighlightSuppressor = false
#if DEBUG
    private var tabBarColorProbeDisplayLink: CADisplayLink?
    private let tabBarColorProbeView = UILabel()
#endif
    
    private let tabItemTopPadding: CGFloat = 4
    private let tabTransitionDuration: TimeInterval = 0.2
    private let synchronizationRestrictedTabTags: Set<Int> = [TabTag.gestures, TabTag.specialSettings, TabTag.serviceSettings]
    private let secretSettingsVisibilityKey = "secret_item_visible"
    private let selectedTabItemColor = UIColor(named: "ubi4_white") ?? .white
    private let unselectedTabItemColor = UIColor(named: "ubi4_deactivate_text") ?? UIColor(white: 0.514, alpha: 1)
    private let tabsBackgroundColor = UIColor(named: "ubi4_back") ?? .black
    private let iOS26TabIconVerticalOffset: CGFloat = -6
    private let tabBarContentOverlayView = UIView()
    private var tabBarContentDescriptors: [TabBarContentDescriptor] = []
    private var allTabBarContentDescriptors: [TabBarContentDescriptor] = []
    private var allTabViewControllers: [UIViewController] = []
    private var tabBarContentViews: [MainTabBarContentItemView] = []
    private var widgetsUpdateJob: Kotlinx_coroutines_coreJob?
    private var isKeyboardVisible = false
    private var keyboardWillShowObserver: NSObjectProtocol?
    private var keyboardWillHideObserver: NSObjectProtocol?
    private var synchronizationStateObserver: NSObjectProtocol?
    private var pendingTabColorRefreshWorkItems: [DispatchWorkItem] = []
    private let tabDisplayByTag: [Int: Int32] = [
        TabTag.gestures: 0,
        TabTag.sensors: 1,
        TabTag.training: 3,
        TabTag.specialSettings: 2,
        TabTag.serviceSettings: 4
    ]
    private var isSecretSettingsTabVisible: Bool {
        UserDefaults.standard.bool(forKey: secretSettingsVisibilityKey)
    }
    
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
        view.backgroundColor = tabsBackgroundColor
        view.isOpaque = true
        view.accessibilityIdentifier = AccessibilityIdentifier.mainTabBarRoot
        setupTabs()
        tabBar.backgroundColor = UIColor(named: "ubi4_dark_back")
        tabBar.tintColor = selectedTabItemColor
        tabBar.unselectedItemTintColor = unselectedTabItemColor
        configureTabBarPlatformBehavior()
        delegate = self
        
        applyTabBarContentInsets(topPadding: tabItemTopPadding)
        
        applyWidgetDrivenTabVisibility(preferredSelectionTag: TabTag.sensors)
        DispatchQueue.main.async { [weak self] in
            self?.forceUpdateTabBarItemColors()
        }
        registerKeyboardObservers()
        registerSynchronizationObservers()
        registerWidgetUpdates()
        updateSynchronizationRestrictedTabAvailability()
#if DEBUG
        configureTabBarColorProbeIfNeeded()
#endif
        configureTabBarContentOverlayIfNeeded()
        updateTabBarContentOverlay(animated: false)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: false)
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        sanitizeTabBarSelectionOverlaysIfNeeded()
        installTabButtonHighlightSuppressorIfNeeded()
        if !didUpdateTabBarFonts {
            unifyTabBarItemFonts()
            didUpdateTabBarFonts = true
        }
        forceUpdateTabBarItemColors()
        configureTabBarContentOverlayIfNeeded()
        layoutTabBarContentOverlayIfNeeded()
        hideNativeTabBarContentIfNeeded()
        updateTabBarContentOverlay(animated: false)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        setNeedsStatusBarAppearanceUpdate()
        scheduleTabBarColorRefreshBurst()
        dumpTabBarIfNeeded()
    }

    private func setupTabs() {
        let widgetsDI = appDIContainer.makeWidgetsSceneDIContainer()
        let actions = makeWidgetsActions()

        let gesturesTitle = SharedLocalizedText.text(SharedRes.strings().title_home)
        let sensorsTitle = SharedLocalizedText.text(SharedRes.strings().title_dashboard)
        let specialTitle = SharedLocalizedText.text(SharedRes.strings().special_settings)
        let trainingTitle = SharedLocalizedText.text(SharedRes.strings().training)

        let gesturesVC = widgetsDI.makeGesturesTabViewController(actions: actions)
        gesturesVC.tabBarItem = makeTabBarItem(
            title: gesturesTitle,
            imageName: "ic_gestures",
            tag: TabTag.gestures
        )
        gesturesVC.tabBarItem.accessibilityIdentifier = AccessibilityIdentifier.mainTabGesturesItem

        let sensorsVC = widgetsDI.makeSensorsTabViewController(actions: actions)
        sensorsVC.tabBarItem = makeTabBarItem(
            title: sensorsTitle,
            imageName: "ic_sensors",
            tag: TabTag.sensors
        )

        let specialVC = widgetsDI.makeSpecialSettingsTabViewController(actions: actions)
        specialVC.tabBarItem = makeTabBarItem(
            title: specialTitle,
            imageName: "ic_mechanics",
            tag: TabTag.specialSettings
        )
        specialVC.tabBarItem.accessibilityIdentifier = AccessibilityIdentifier.mainTabSpecialSettingsItem
        let trainingVC = widgetsDI.makeTrainingTabViewController(actions: actions)
        trainingVC.tabBarItem = makeTabBarItem(
            title: trainingTitle,
            imageName: "ic_trophy",
            tag: TabTag.training
        )
        let serviceVC = makeServiceSettingsTabViewController()

        allTabViewControllers = [gesturesVC, sensorsVC, trainingVC, specialVC, serviceVC]
        allTabBarContentDescriptors = [
            TabBarContentDescriptor(title: gesturesTitle, imageName: "ic_gestures", tag: TabTag.gestures),
            TabBarContentDescriptor(title: sensorsTitle, imageName: "ic_sensors", tag: TabTag.sensors),
            TabBarContentDescriptor(title: trainingTitle, imageName: "ic_trophy", tag: TabTag.training),
            TabBarContentDescriptor(title: specialTitle, imageName: "ic_mechanics", tag: TabTag.specialSettings),
            makeServiceSettingsDescriptor()
        ]
        tabBarContentDescriptors = allTabBarContentDescriptors
        viewControllers = allTabViewControllers
    }

    private func makeWidgetsActions() -> WidgetsListViewModelActions {
        WidgetsListViewModelActions(
            showWidgetDetails: { _ in },
            showWidgetQueriesSuggestions: { _ in },
            closeWidgetQueriesSuggestions: {},
            showBleLog: { [weak self] in
                self?.showBleLogScreen()
            }
        )
    }

    private func showBleLogScreen() {
        if navigationController?.topViewController is BleLogViewController { return }
        navigationController?.pushViewController(BleLogViewController(), animated: true)
    }

    private func makeServiceSettingsTabViewController() -> ServiceSettingsTabViewController {
        let widgetsDI = appDIContainer.makeWidgetsSceneDIContainer()
        let serviceTitle = SharedLocalizedText.text(SharedRes.strings().service_settings)
        let serviceVC = widgetsDI.makeServiceSettingsTabViewController(actions: makeWidgetsActions())
        serviceVC.tabBarItem = makeTabBarItem(
            title: serviceTitle,
            imageName: "ic_navigate_next",
            tag: TabTag.serviceSettings
        )
        serviceVC.tabBarItem.accessibilityIdentifier = AccessibilityIdentifier.mainTabServiceSettingsItem
        return serviceVC
    }

    private func makeServiceSettingsDescriptor() -> TabBarContentDescriptor {
        TabBarContentDescriptor(
            title: SharedLocalizedText.text(SharedRes.strings().service_settings),
            imageName: "ic_navigate_next",
            tag: TabTag.serviceSettings
        )
    }

    func toggleSecretSettingsTabVisibility() {
        setSecretSettingsTabVisible(!isSecretSettingsTabVisible, selectWhenShown: true)
    }

    private func setSecretSettingsTabVisible(_ isVisible: Bool, selectWhenShown: Bool) {
        UserDefaults.standard.set(isVisible, forKey: secretSettingsVisibilityKey)
        let preferredTag = isVisible && selectWhenShown ? TabTag.serviceSettings : nil
        applyWidgetDrivenTabVisibility(preferredSelectionTag: preferredTag)
    }

    private func applyWidgetDrivenTabVisibility(preferredSelectionTag: Int? = nil) {
        let dataFactory = DataFactory()
        let visibleDisplays = Set((0...4).compactMap { display -> Int32? in
            dataFactory.prepareData(display: Int32(display)).isEmpty ? nil : Int32(display)
        })
        let previousSelectedTag = selectedViewController?.tabBarItem.tag

        let permittedControllers = allTabViewControllers.filter { controller in
            guard let display = tabDisplayByTag[controller.tabBarItem.tag] else { return false }
            let hasWidgets = visibleDisplays.contains(display)
            let hasAccess = controller.tabBarItem.tag != TabTag.serviceSettings || isSecretSettingsTabVisible
            return hasWidgets && hasAccess
        }
        let permittedTags = Set(permittedControllers.map { $0.tabBarItem.tag })
        tabBarContentDescriptors = allTabBarContentDescriptors.filter { permittedTags.contains($0.tag) }
        viewControllers = permittedControllers

        guard !permittedControllers.isEmpty else {
            tabBar.isHidden = true
            tabBarContentOverlayView.isHidden = true
            refreshTabBarAfterContentChange()
            return
        }

        tabBar.isHidden = false
        tabBarContentOverlayView.isHidden = false
        let targetTag: Int
        if let preferredSelectionTag, permittedTags.contains(preferredSelectionTag) {
            targetTag = preferredSelectionTag
        } else if let previousSelectedTag, permittedTags.contains(previousSelectedTag) {
            targetTag = previousSelectedTag
        } else if permittedTags.contains(TabTag.sensors) {
            targetTag = TabTag.sensors
        } else {
            targetTag = permittedControllers[0].tabBarItem.tag
        }
        selectTab(withTag: targetTag)
        refreshTabBarAfterContentChange()
        updateTabBarContainerForKeyboardState()
    }

    private func selectTab(withTag tag: Int?) {
        guard let tag, let controllers = viewControllers else { return }
        guard let index = controllers.firstIndex(where: { $0.tabBarItem.tag == tag }) else { return }
        selectedIndex = index
    }

    private func refreshTabBarAfterContentChange() {
        didUpdateTabBarFonts = false
        applyTabBarContentInsets(topPadding: tabItemTopPadding)
        updateSynchronizationRestrictedTabAvailability()
        configureTabBarContentOverlayIfNeeded()
        layoutTabBarContentOverlayIfNeeded()
        hideNativeTabBarContentIfNeeded()
        refreshTabBarItemColors()
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
            .foregroundColor: normalColor
        ]
        
        for item in items {
            item.setTitleTextAttributes(normalAttributes, for: .normal)
            item.setTitleTextAttributes(selectedAttributes, for: .selected)
            item.setTitleTextAttributes(disabledAttributes, for: .disabled)
            item.setTitleTextAttributes(focusedAttributes, for: .focused)
            if #available(iOS 26.0, *) {
                item.setTitleTextAttributes(normalAttributes, for: .highlighted)
            }
        }
    }
    
    private func applyTabBarContentInsets(topPadding: CGFloat) {
        guard let items = tabBar.items else { return }

        // 1) Смещаем иконки вниз (появляется "воздух" сверху)
        for item in items {
            if #available(iOS 26.0, *) {
                item.imageInsets = .zero
                item.titlePositionAdjustment = .zero
            } else {
                item.imageInsets = UIEdgeInsets(top: topPadding, left: 0, bottom: -topPadding, right: 0)
                item.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: topPadding / 2)
            }
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
            if #available(iOS 26.0, *) {
                let normalTitleAttributes: [NSAttributedString.Key: Any] = [
                    .foregroundColor: unselected
                ]
                let selectedTitleAttributes: [NSAttributedString.Key: Any] = [
                    .foregroundColor: selected
                ]
                layout.normal.iconColor = unselected
                layout.selected.iconColor = selected
                layout.disabled.iconColor = unselected
                layout.focused.iconColor = unselected

                layout.normal.titleTextAttributes = normalTitleAttributes
                layout.selected.titleTextAttributes = selectedTitleAttributes
                layout.disabled.titleTextAttributes = normalTitleAttributes
                layout.focused.titleTextAttributes = normalTitleAttributes

                layout.normal.titlePositionAdjustment = .zero
                layout.selected.titlePositionAdjustment = .zero
                layout.disabled.titlePositionAdjustment = .zero
                layout.focused.titlePositionAdjustment = .zero
                return
            }

            layout.normal.iconColor = unselected

            layout.selected.iconColor = selected

            layout.disabled.iconColor = unselected

            layout.focused.iconColor = unselected   // FIX

            layout.normal.titleTextAttributes[.foregroundColor] = unselected

            layout.selected.titleTextAttributes[.foregroundColor] = selected

            layout.disabled.titleTextAttributes[.foregroundColor] = unselected

            layout.focused.titleTextAttributes[.foregroundColor] = unselected // FIX

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
        if #available(iOS 26.0, *) {
            for item in items {
                item.standardAppearance = appearance
                item.scrollEdgeAppearance = appearance
            }
        } else {
            for item in items {
                item.selectedImage = item.image?.withRenderingMode(.alwaysTemplate)
            }
        }
    }

    private func makeTabBarItem(title: String, imageName: String, tag: Int) -> UITabBarItem {
        return UITabBarItem(
            title: title,
            image: tabIcon(named: imageName),
            tag: tag
        )
    }

    private func tabIcon(named name: String) -> UIImage? {
        UIImage(named: name)?.withRenderingMode(.alwaysTemplate)
    }

    private func configureTabBarContentOverlayIfNeeded() {
        guard #available(iOS 26.0, *), !tabBarContentDescriptors.isEmpty else { return }

        if tabBarContentOverlayView.superview == nil {
            tabBarContentOverlayView.isUserInteractionEnabled = false
            tabBarContentOverlayView.accessibilityElementsHidden = true
            tabBarContentOverlayView.backgroundColor = .clear
            tabBarContentOverlayView.translatesAutoresizingMaskIntoConstraints = false

            tabBar.addSubview(tabBarContentOverlayView)
            NSLayoutConstraint.activate([
                tabBarContentOverlayView.leadingAnchor.constraint(equalTo: tabBar.leadingAnchor),
                tabBarContentOverlayView.trailingAnchor.constraint(equalTo: tabBar.trailingAnchor),
                tabBarContentOverlayView.topAnchor.constraint(equalTo: tabBar.topAnchor),
                tabBarContentOverlayView.bottomAnchor.constraint(equalTo: tabBar.bottomAnchor)
            ])
        }

        let currentTags = tabBarContentViews.map(\.tagValue)
        let expectedTags = tabBarContentDescriptors.map(\.tag)
        guard currentTags != expectedTags else { return }

        tabBarContentViews.forEach { $0.removeFromSuperview() }

        tabBarContentViews = tabBarContentDescriptors.map { descriptor in
            MainTabBarContentItemView(
                title: descriptor.title,
                image: tabIcon(named: descriptor.imageName),
                tagValue: descriptor.tag,
                topPadding: 0,
                iconVerticalOffset: iOS26TabIconVerticalOffset
            )
        }

        tabBarContentViews.forEach { view in
            view.translatesAutoresizingMaskIntoConstraints = true
            tabBarContentOverlayView.addSubview(view)
        }
        layoutTabBarContentOverlayIfNeeded()
        hideNativeTabBarContentIfNeeded()
    }

    private func layoutTabBarContentOverlayIfNeeded() {
        guard #available(iOS 26.0, *),
              tabBarContentOverlayView.superview != nil,
              !tabBarContentViews.isEmpty else { return }

        tabBar.layoutIfNeeded()
        tabBarContentOverlayView.layoutIfNeeded()

        let controls = tabBarButtonControls()
        if controls.count == tabBarContentViews.count {
            for (index, pair) in zip(tabBarContentViews, controls).enumerated() {
                let (itemView, control) = pair
                itemView.frame = control.convert(control.bounds, to: tabBarContentOverlayView)
                let descriptor = tabBarContentDescriptors.indices.contains(index) ? tabBarContentDescriptors[index] : nil
                if let nativeLayout = nativeTabBarContentLayout(in: control, matchingTitle: descriptor?.title) {
                    itemView.applyNativeLayout(
                        iconFrame: nativeLayout.iconFrame,
                        titleFrame: nativeLayout.titleFrame,
                        titleFont: nativeLayout.titleFont
                    )
                } else {
                    itemView.resetToFallbackLayout(topPadding: tabItemTopPadding)
                }
            }
            return
        }

        let itemWidth = tabBarContentOverlayView.bounds.width / CGFloat(tabBarContentViews.count)
        for (index, itemView) in tabBarContentViews.enumerated() {
            itemView.frame = CGRect(
                x: CGFloat(index) * itemWidth,
                y: 0,
                width: itemWidth,
                height: tabBarContentOverlayView.bounds.height
            )
            itemView.resetToFallbackLayout(topPadding: tabItemTopPadding)
        }
    }

    private func nativeTabBarContentLayout(in control: UIControl, matchingTitle title: String?) -> NativeTabBarContentLayout? {
        let imageViews = collectSubviews(in: control, as: UIImageView.self)
            .filter { imageView in
                imageView.image != nil &&
                imageView.bounds.width > 0 &&
                imageView.bounds.height > 0 &&
                imageView.bounds.width <= 80 &&
                imageView.bounds.height <= 80
            }
            .sorted { first, second in
                let firstFrame = first.convert(first.bounds, to: control)
                let secondFrame = second.convert(second.bounds, to: control)
                return abs(firstFrame.midX - control.bounds.midX) < abs(secondFrame.midX - control.bounds.midX)
            }

        let labels = collectSubviews(in: control, as: UILabel.self)
            .filter { label in
                guard let text = label.text, !text.isEmpty else { return false }
                if let title, text != title {
                    return false
                }
                return label.bounds.width > 0 && label.bounds.height > 0
            }
            .sorted { first, second in
                let firstFrame = first.convert(first.bounds, to: control)
                let secondFrame = second.convert(second.bounds, to: control)
                return abs(firstFrame.midX - control.bounds.midX) < abs(secondFrame.midX - control.bounds.midX)
            }

        guard let imageView = imageViews.first ?? collectSubviews(in: control, as: UIImageView.self)
            .first(where: { $0.image != nil && $0.bounds.width > 0 && $0.bounds.height > 0 }),
              let label = labels.first ?? collectSubviews(in: control, as: UILabel.self)
            .first(where: { $0.text?.isEmpty == false && $0.bounds.width > 0 && $0.bounds.height > 0 }) else {
            return nil
        }

        return NativeTabBarContentLayout(
            iconFrame: imageView.convert(imageView.bounds, to: tabBarContentOverlayView),
            titleFrame: label.convert(label.bounds, to: tabBarContentOverlayView),
            titleFont: label.font
        )
    }

    private func collectSubviews<T: UIView>(in view: UIView, as type: T.Type) -> [T] {
        var result: [T] = []

        func collect(from current: UIView) {
            if let typed = current as? T {
                result.append(typed)
            }
            for subview in current.subviews {
                collect(from: subview)
            }
        }

        collect(from: view)
        return result
    }

    private func hideNativeTabBarContentIfNeeded() {
        guard #available(iOS 26.0, *), tabBarContentOverlayView.superview != nil else { return }

        func hideNativeContent(in view: UIView) {
            for subview in view.subviews {
                if subview === tabBarContentOverlayView || subview.isDescendant(of: tabBarContentOverlayView) {
                    continue
                }

                if subview is UILabel || subview is UIImageView {
                    subview.isHidden = true
                    subview.alpha = 0
                    subview.tintColor = .clear
                    if let label = subview as? UILabel {
                        label.textColor = .clear
                        label.highlightedTextColor = .clear
                    }
                }

                hideNativeContent(in: subview)
            }
        }

        hideNativeContent(in: tabBar)
        tabBar.bringSubviewToFront(tabBarContentOverlayView)
    }

    private func updateTabBarContentOverlay(animated: Bool) {
        guard #available(iOS 26.0, *) else { return }

        let selectedTag = selectedViewController?.tabBarItem.tag
        for itemView in tabBarContentViews {
            itemView.setSelected(
                itemView.tagValue == selectedTag,
                selectedColor: selectedTabItemColor,
                unselectedColor: unselectedTabItemColor,
                animated: animated
            )
        }
    }

    private func refreshTabBarItemColors() {
        applyTabBarContentInsets(topPadding: tabItemTopPadding)
        unifyTabBarItemFonts()
        sanitizeTabBarSelectionOverlaysIfNeeded()
        forceUpdateTabBarItemColors()
        updateTabBarContentOverlay(animated: true)
        DispatchQueue.main.async { [weak self] in
            self?.forceUpdateTabBarItemColors()
            self?.updateTabBarContentOverlay(animated: false)
        }
    }

    private func applyTabBarControlTintFallback() {
        forceUpdateTabBarItemColors()
    }

    private func tabBarButtonControls() -> [UIControl] {
        var result: [UIControl] = []

        func collectControls(from view: UIView) {
            if let control = view as? UIControl {
                let className = String(describing: type(of: control)).lowercased()
                let looksLikeTabButton = className.contains("tabbarbutton") || className.contains("tabbaritem")

                if looksLikeTabButton,
                   !control.isHidden,
                   control.bounds.width > 0,
                   control.bounds.height > 0 {
                    result.append(control)
                }
            }

            for subview in view.subviews {
                collectControls(from: subview)
            }
        }

        collectControls(from: tabBar)

        if result.isEmpty {
            result = tabBar.subviews
                .compactMap { $0 as? UIControl }
                .filter { !$0.isHidden && $0.bounds.width > 0 && $0.bounds.height > 0 }
        }

        return result
            .removingDuplicatesByObjectIdentity()
            .sorted { first, second in
                let firstFrame = first.convert(first.bounds, to: tabBar)
                let secondFrame = second.convert(second.bounds, to: tabBar)
                return firstFrame.midX < secondFrame.midX
            }
    }

    private func forceUpdateTabBarItemColors() {
        guard #available(iOS 26.0, *) else { return }
        guard let items = tabBar.items else { return }

        let selectedTag = selectedViewController?.tabBarItem.tag

        for item in items {
            let color = item.tag == selectedTag
                ? selectedTabItemColor
                : unselectedTabItemColor

            item.setTitleTextAttributes([.foregroundColor: color], for: .normal)
            item.setTitleTextAttributes([.foregroundColor: color], for: .selected)
            item.setTitleTextAttributes([.foregroundColor: color], for: .focused)
            item.setTitleTextAttributes([.foregroundColor: color], for: .highlighted)
            item.setTitleTextAttributes([.foregroundColor: unselectedTabItemColor], for: .disabled)

            if #available(iOS 15.0, *) {
                item.standardAppearance = tabBar.standardAppearance
                item.scrollEdgeAppearance = tabBar.scrollEdgeAppearance
            }
        }

        tabBar.tintColor = selectedTabItemColor
        tabBar.unselectedItemTintColor = unselectedTabItemColor
        tabBar.setNeedsLayout()
        layoutTabBarContentOverlayIfNeeded()
        hideNativeTabBarContentIfNeeded()
        updateTabBarContentOverlay(animated: false)
#if DEBUG
        updateTabBarColorProbeValue()
#endif
    }

    private func scheduleTabBarColorRefreshBurst() {
        pendingTabColorRefreshWorkItems.forEach { $0.cancel() }
        pendingTabColorRefreshWorkItems.removeAll()

        let delays: [TimeInterval] = [0, 0.03, 0.08, 0.16, 0.28, 0.42]
        for delay in delays {
            let workItem = DispatchWorkItem { [weak self] in
                self?.refreshTabBarItemColors()
            }
            pendingTabColorRefreshWorkItems.append(workItem)
            DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: workItem)
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
        guard #available(iOS 26.0, *) else { return }

        let controls = tabBarButtonControls()
        guard !controls.isEmpty else { return }

        for control in controls {
            control.removeTarget(self, action: #selector(handleTabButtonTouchDown(_:)), for: [.touchDown, .touchDragEnter])
            control.removeTarget(self, action: #selector(handleTabButtonTouchMove(_:)), for: .touchDragInside)
            control.removeTarget(self, action: #selector(handleTabButtonTouchEnd(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel, .touchDragExit])

            control.addTarget(self, action: #selector(handleTabButtonTouchDown(_:)), for: [.touchDown, .touchDragEnter])
            control.addTarget(self, action: #selector(handleTabButtonTouchMove(_:)), for: .touchDragInside)
            control.addTarget(self, action: #selector(handleTabButtonTouchEnd(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel, .touchDragExit])
        }

        didInstallTabButtonHighlightSuppressor = true
    }
    @objc
    private func handleTabButtonTouchDown(_ sender: UIControl) {
        suppressTabButtonHighlight(sender)
#if DEBUG
        updateTabBarColorProbeValue()
#endif
    }

    @objc
    private func handleTabButtonTouchMove(_ sender: UIControl) {
        suppressTabButtonHighlight(sender)
#if DEBUG
        updateTabBarColorProbeValue()
#endif
    }

    @objc
    private func handleTabButtonTouchEnd(_ sender: UIControl) {
        suppressTabButtonHighlight(sender)
        scheduleTabBarColorRefreshBurst()
#if DEBUG
        updateTabBarColorProbeValue()
#endif
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

    private func registerWidgetUpdates() {
        widgetsUpdateJob?.cancel(cause: nil)
        widgetsUpdateJob = UiStateBridge.shared.observeUpdates { [weak self] _ in
            DispatchQueue.main.async {
                self?.applyWidgetDrivenTabVisibility()
            }
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
        isKeyboardVisible = hidden
        guard viewControllers?.isEmpty == false else {
            tabBar.isHidden = true
            return
        }
        tabBar.isHidden = false
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

    private func updateTabBarContainerForKeyboardState() {
        let hasVisibleTabs = viewControllers?.isEmpty == false
        tabBar.isHidden = !hasVisibleTabs
        guard hasVisibleTabs else { return }
        tabBar.alpha = isKeyboardVisible ? 0 : 1
        tabBar.transform = isKeyboardVisible
            ? CGAffineTransform(translationX: 0, y: tabBar.bounds.height)
            : .identity
    }

    deinit {
        pendingTabColorRefreshWorkItems.forEach { $0.cancel() }
        widgetsUpdateJob?.cancel(cause: nil)
#if DEBUG
        tabBarColorProbeDisplayLink?.invalidate()
#endif
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

private final class MainTabBarContentItemView: UIView {
    let tagValue: Int

    private let iconView = UIImageView()
    private let titleLabel = UILabel()
    private var usesNativeLayout = false
    private var fallbackTopPadding: CGFloat
    private let iconVerticalOffset: CGFloat

    var iconColor: UIColor {
        iconView.tintColor
    }

    var titleColor: UIColor {
        titleLabel.textColor
    }

    init(title: String, image: UIImage?, tagValue: Int, topPadding: CGFloat, iconVerticalOffset: CGFloat) {
        self.tagValue = tagValue
        self.fallbackTopPadding = topPadding
        self.iconVerticalOffset = iconVerticalOffset
        super.init(frame: .zero)
        isUserInteractionEnabled = false
        backgroundColor = .clear

        iconView.image = image
        iconView.contentMode = .scaleAspectFit
        iconView.tintColor = UIColor(named: "ubi4_deactivate_text") ?? UIColor(white: 0.514, alpha: 1)

        titleLabel.text = title
        titleLabel.font = .systemFont(ofSize: 10)
        titleLabel.textAlignment = .center
        titleLabel.adjustsFontSizeToFitWidth = true
        titleLabel.minimumScaleFactor = 0.75
        titleLabel.textColor = UIColor(named: "ubi4_deactivate_text") ?? UIColor(white: 0.514, alpha: 1)

        addSubview(iconView)
        addSubview(titleLabel)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard !usesNativeLayout else { return }

        let iconSize = CGSize(width: 28, height: 28)
        let titleSize = titleLabel.sizeThatFits(CGSize(width: max(bounds.width - 4, 0), height: .greatestFiniteMagnitude))
        let totalHeight = iconSize.height + 2 + titleSize.height
        let startY = ((bounds.height - totalHeight) / 2) + fallbackTopPadding

        iconView.frame = CGRect(
            x: (bounds.width - iconSize.width) / 2,
            y: startY + iconVerticalOffset,
            width: iconSize.width,
            height: iconSize.height
        )
        titleLabel.frame = CGRect(
            x: 2,
            y: iconView.frame.maxY + 2,
            width: max(bounds.width - 4, 0),
            height: titleSize.height
        )
    }

    func applyNativeLayout(iconFrame: CGRect?, titleFrame: CGRect?, titleFont: UIFont?) {
        usesNativeLayout = true

        if let titleFont {
            titleLabel.font = titleFont
        }

        if let iconFrame {
            iconView.frame = convert(iconFrame, from: superview).offsetBy(dx: 0, dy: iconVerticalOffset)
        }

        if let titleFrame {
            titleLabel.frame = convert(titleFrame, from: superview)
        }
    }

    func resetToFallbackLayout(topPadding: CGFloat) {
        usesNativeLayout = false
        fallbackTopPadding = topPadding
        setNeedsLayout()
    }

    func setSelected(_ isSelected: Bool, selectedColor: UIColor, unselectedColor: UIColor, animated: Bool) {
        let color = isSelected ? selectedColor : unselectedColor
        let changes = {
            self.iconView.tintColor = color
            self.titleLabel.textColor = color
        }

        guard animated else {
            changes()
            return
        }

        UIView.animate(
            withDuration: 0.18,
            delay: 0,
            options: [.beginFromCurrentState, .allowUserInteraction, .curveEaseInOut],
            animations: changes
        )
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
        DispatchQueue.main.async { [weak self] in
            self?.refreshTabBarItemColors()
            self?.forceUpdateTabBarItemColors()
            self?.updateTabBarContentOverlay(animated: true)
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak self] in
            self?.forceUpdateTabBarItemColors()
            self?.updateTabBarContentOverlay(animated: false)
        }
    }

    func tabBarController(_ tabBarController: UITabBarController,
                          animationControllerForTransitionFrom fromVC: UIViewController,
                          to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        nil
    }
}

#if DEBUG
private extension MainTabBarController {
    var isTabBarColorProbeEnabled: Bool {
        ProcessInfo.processInfo.arguments.contains("-ui-test-tabbar-color-probe")
    }

    func configureTabBarColorProbeIfNeeded() {
        guard isTabBarColorProbeEnabled, tabBarColorProbeDisplayLink == nil else { return }

        tabBarColorProbeView.isAccessibilityElement = true
        tabBarColorProbeView.accessibilityIdentifier = AccessibilityIdentifier.mainTabBarColorProbe
        tabBarColorProbeView.backgroundColor = .clear
        tabBarColorProbeView.textColor = .clear
        tabBarColorProbeView.font = .systemFont(ofSize: 1)
        tabBarColorProbeView.numberOfLines = 1
        tabBarColorProbeView.isUserInteractionEnabled = false
        tabBarColorProbeView.translatesAutoresizingMaskIntoConstraints = false
        if tabBarColorProbeView.superview == nil {
            view.addSubview(tabBarColorProbeView)
            NSLayoutConstraint.activate([
                tabBarColorProbeView.widthAnchor.constraint(equalToConstant: 1),
                tabBarColorProbeView.heightAnchor.constraint(equalToConstant: 1),
                tabBarColorProbeView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
                tabBarColorProbeView.topAnchor.constraint(equalTo: view.topAnchor)
            ])
        }

        updateTabBarColorProbeValue()

        let displayLink = CADisplayLink(target: self, selector: #selector(handleTabBarColorProbeTick))
        displayLink.preferredFrameRateRange = CAFrameRateRange(minimum: 30, maximum: 120, preferred: 60)
        displayLink.add(to: .main, forMode: .common)
        tabBarColorProbeDisplayLink = displayLink
    }

    @objc
    func handleTabBarColorProbeTick() {
        updateTabBarColorProbeValue()
    }

    func updateTabBarColorProbeValue() {
        guard isTabBarColorProbeEnabled else { return }

        let selectedTag = selectedViewController?.tabBarItem.tag ?? -1
        let items = tabBar.items ?? []
        let controls = tabBarButtonControls()
        let itemStates = items.enumerated().map { index, item -> String in
            let control = controls.indices.contains(index) ? controls[index] : nil
            let tag = item.tag
            let isSelected = tag == selectedTag
            let visibleImageViews = visibleImageViews(in: tabBar, itemIndex: index, itemCount: items.count)
            let visibleLabels = visibleLabels(in: tabBar, itemIndex: index, itemCount: items.count)
            let iconColors = visibleImageViews.map { colorHex($0.tintColor) }.removingDuplicates()
            let textColors = visibleLabels.map { colorHex($0.textColor) }.removingDuplicates()
            let iconValue = iconColors.isEmpty ? "_" : iconColors.joined(separator: "+")
            let textValue = textColors.isEmpty ? "_" : textColors.joined(separator: "+")
            let highlighted = control?.isHighlighted == true ? "1" : "0"
            return "\(index),\(tag),\(isSelected ? "1" : "0"),\(iconValue),\(textValue),\(highlighted),\(visibleImageViews.count),\(visibleLabels.count)"
        }

        let payload = [
            "selectedTag=\(selectedTag)",
            "allowed=\(colorHex(selectedTabItemColor)),\(colorHex(unselectedTabItemColor))",
            "items=\(itemStates.joined(separator: "|"))"
        ].joined(separator: ";")

        tabBarColorProbeView.accessibilityLabel = payload
        tabBarColorProbeView.accessibilityValue = payload
        tabBarColorProbeView.text = payload
    }

    func visibleImageViews(in view: UIView, itemIndex: Int, itemCount: Int) -> [UIImageView] {
        return collectVisibleSubviews(in: view, as: UIImageView.self)
            .filter { $0.image != nil }
            .filter { isView($0, inTabItemIndex: itemIndex, itemCount: itemCount) }
    }

    func visibleLabels(in view: UIView, itemIndex: Int, itemCount: Int) -> [UILabel] {
        return collectVisibleSubviews(in: view, as: UILabel.self)
            .filter { $0 !== tabBarColorProbeView }
            .filter { $0.text?.isEmpty == false }
            .filter { isView($0, inTabItemIndex: itemIndex, itemCount: itemCount) }
    }

    func isView(_ view: UIView, inTabItemIndex itemIndex: Int, itemCount: Int) -> Bool {
        guard itemCount > 0, tabBar.bounds.width > 0 else { return false }

        let frame = view.convert(view.bounds, to: tabBar)
        guard !frame.isEmpty else { return false }

        let itemWidth = tabBar.bounds.width / CGFloat(itemCount)
        let minX = CGFloat(itemIndex) * itemWidth
        let maxX = itemIndex == itemCount - 1 ? tabBar.bounds.maxX : minX + itemWidth
        return frame.midX >= minX && frame.midX < maxX
    }

    func collectVisibleSubviews<T: UIView>(in view: UIView, as type: T.Type) -> [T] {
        var result: [T] = []

        func collect(from current: UIView) {
            guard !current.isHidden, current.alpha > 0.01 else { return }
            if let typed = current as? T, typed.bounds.width > 0, typed.bounds.height > 0 {
                result.append(typed)
            }
            for subview in current.subviews {
                collect(from: subview)
            }
        }

        collect(from: view)
        return result
    }

    func colorHex(_ color: UIColor) -> String {
        let resolved = color.resolvedColor(with: traitCollection)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0

        guard resolved.getRed(&red, green: &green, blue: &blue, alpha: &alpha) else {
            return String(describing: resolved)
        }

        return String(
            format: "#%02X%02X%02X",
            Int(round(red * 255)),
            Int(round(green * 255)),
            Int(round(blue * 255))
        )
    }
}

private extension Array where Element: Hashable {
    func removingDuplicates() -> [Element] {
        var seen = Set<Element>()
        var result: [Element] = []

        for element in self where seen.insert(element).inserted {
            result.append(element)
        }

        return result
    }
}
#endif

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

private extension Array where Element: AnyObject {
    func removingDuplicatesByObjectIdentity() -> [Element] {
        var seen = Set<ObjectIdentifier>()
        var result: [Element] = []

        for element in self {
            let identifier = ObjectIdentifier(element)
            if seen.insert(identifier).inserted {
                result.append(element)
            }
        }

        return result
    }
}
