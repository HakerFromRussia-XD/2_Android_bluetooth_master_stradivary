import UIKit
import DGCharts
import shared

enum SpecialSettingsSource: Hashable {
    case prosthetic
    case mobile
}

extension Notification.Name {
    static let v3PausePlotPointRendering = Notification.Name("V3PausePlotPointRendering")
    static let v3ResumePlotPointRendering = Notification.Name("V3ResumePlotPointRendering")
    static let widgetsSynchronizationStateDidChange = Notification.Name("WidgetsSynchronizationStateDidChange")
}

final class WidgetsListViewController: UIViewController, StoryboardInstantiable, Alertable {
    static var defaultFileName: String { "WidgetsListViewController" }
    @IBOutlet private var contentView: UIView!
    @IBOutlet private var widgetsListContainer: UIView!
    @IBOutlet private(set) var suggestionsListContainer: UIView!
    @IBOutlet private var emptyDataLabel: UILabel!
    private let tableView = UITableView()
    @IBOutlet private weak var tableViewWidgets: UITableView!
    @IBAction func unwindToThisGestureViewController (sender: UIStoryboardSegue){
//        loadDataString()
//        initUI()
        print("sGRG initUI() unwindToThisGestureViewController()")
    }
    private lazy var bottomButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle(SharedLocalizedText.text(SharedRes.strings().push_me), for: .normal)
        button.accessibilityIdentifier = AccessibilityIdentifier.widgetsResyncButton
        button.addTarget(self, action: #selector(bottomButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.isHidden = true
        return button
    }()
    
    private var viewModel: WidgetsListViewModel!
    private var isUiTestSkipSynchronization: Bool {
        ProcessInfo.processInfo.arguments.contains("-ui-test-skip-synchronization")
    }
    private var isUiTestForceGesturesWidget: Bool {
        ProcessInfo.processInfo.arguments.contains("-ui-test-force-gestures-widget")
    }
    private var isUiTestGestureUsageSample: Bool {
        ProcessInfo.processInfo.arguments.contains("-ui-test-gesture-usage-sample")
    }

    private var widgetsTableViewController: WidgetsListTableViewController?
    private var widgetsUpdateJob: Kotlinx_coroutines_coreJob?
    private var widgetsLoadingCompletionJob: Kotlinx_coroutines_coreJob?
    private var widgetsInitializationInfoJob: Kotlinx_coroutines_coreJob?
    private var widgetsLoadingProgressJob: Kotlinx_coroutines_coreJob?
    private var telemetryCountersJob: Kotlinx_coroutines_coreJob?
    private static var globalSynchronizationCompleted: Bool {
        get { WidgetsSynchronizationState.isCompleted }
        set { WidgetsSynchronizationState.isCompleted = newValue }
    }
    private static var globalSynchronizationInProgress: Bool {
        get { WidgetsSynchronizationState.isInProgress }
        set { WidgetsSynchronizationState.isInProgress = newValue }
    }
    private var open3DGestureId: Int?
    private var open3DGestureUsesV3Protocol = false
    private var latestGestureUsageItems: [GestureUsageChartItem] = []
    private var lastWidgetsSignature: String?
    private var specialSettingsSource: SpecialSettingsSource = .prosthetic
    var display: Int32 = 1
    var screenTitleOverride: String?
    let storage = CoreDataWidgetsResponseStorage()
    private let tabsBackgroundColor = UIColor(named: "ubi4_back") ?? .black

    
    // MARK: - Lifecycle
    static func create(with viewModel: WidgetsListViewModel) -> WidgetsListViewController {
//        let view = WidgetsListViewController.instantiateViewController()
        let storyboard = UIStoryboard(name: defaultFileName, bundle: nil)

        // ищем КОНКРЕТНО твой VC по ID
        let view = storyboard.instantiateViewController(
            withIdentifier: String(describing: WidgetsListViewController.self)
        ) as! WidgetsListViewController
        
        view.viewModel = viewModel
        return view
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        viewModel.setCustomGestureSettingsOpener { [weak self] gestureId, isV3 in
            let tapStartedAt = CACurrentMediaTime()
            NSLog("[V3OpenTrace] event=3dOpenTap thread=main gestureId=%d useV3Mode=%d", gestureId, isV3)
            let openScreen = {
                NSLog(
                    "[V3OpenTrace] event=performSegue thread=main gestureId=%d useV3Mode=%d sinceTapMs=%.3f",
                    gestureId,
                    isV3,
                    (CACurrentMediaTime() - tapStartedAt) * 1000.0
                )
                let animationsWereEnabled = UIView.areAnimationsEnabled
                if !animationsWereEnabled {
                    UIView.setAnimationsEnabled(true)
                }
                
                self?.open3DGestureId = gestureId
                self?.open3DGestureUsesV3Protocol = isV3
                self?.performSegue(withIdentifier: "go3DGripperSettings", sender: nil)
            }
            DispatchQueue.main.async {
                let cache = V3ModelResourceCache.shared()
                NSLog(
                    "[V3OpenTrace] event=mark3DOpenRequested thread=main gestureId=%d useV3Mode=%d cacheState=%ld isReady=%d",
                    gestureId,
                    isV3,
                    cache.state.rawValue,
                    cache.isReady
                )
                cache.mark3DOpenRequested()
                guard !cache.isReady else {
                    NSLog(
                        "[V3OpenTrace] event=preloadReadyImmediate thread=main gestureId=%d useV3Mode=%d sinceTapMs=%.3f",
                        gestureId,
                        isV3,
                        (CACurrentMediaTime() - tapStartedAt) * 1000.0
                    )
                    openScreen()
                    return
                }
                let preloadWaitStartedAt = CACurrentMediaTime()
                cache.preload { ready, error in
                    NSLog(
                        "[V3OpenTrace] event=preloadCallbackBeforeSegue thread=main gestureId=%d useV3Mode=%d ready=%d waitMs=%.3f sinceTapMs=%.3f error=%@",
                        gestureId,
                        isV3,
                        ready,
                        (CACurrentMediaTime() - preloadWaitStartedAt) * 1000.0,
                        (CACurrentMediaTime() - tapStartedAt) * 1000.0,
                        error?.localizedDescription ?? "none"
                    )
                    guard ready else {
                        NSLog("[V3Model] preload before segue failed: %@", error?.localizedDescription ?? "unknown error")
                        return
                    }
                    openScreen()
                }
            }
        }
        
        bind(to: viewModel)
        viewModel.onSynchronizationAction(.viewLoaded(skip: isUiTestSkipSynchronization))
        if display == 4 {
            V3HandSideProvider.shared.startObserving()
        }
        
        view.addSubview(bottomButton)
        NSLayoutConstraint.activate([
            bottomButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            bottomButton.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        print("[WIDGET_COORDINATOR] viewWillAppear")
        viewModel.onSynchronizationAction(.visibilityChanged(true))
        PlotListItemViewModel.resetRequestCache()
        PlotListItemViewModelV3.resetRequestCache()
        SliderListItemViewModel.resetRequestCache()
        setPlotPointRenderingPaused(false)
        startObservingWidgetUpdates()
        reloadWidgetsFromShared()
        viewModel.onSynchronizationAction(.viewAppeared(isMobile: isSpecialSettingsMobileSource,
                                                      skip: isUiTestSkipSynchronization))
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        viewModel.onSynchronizationAction(.visibilityChanged(false))
        setPlotPointRenderingPaused(true)
        print("[WIDGET_COORDINATOR] viewWillDisappear")
        viewModel.onSynchronizationAction(.viewDisappeared)
    }
    
    deinit {
        stopObservingWidgetUpdates()
    }

    private func startObservingWidgetUpdates() {
        print("[WIDGET_COORDINATOR] startObservingWidgetUpdates")
        widgetsUpdateJob?.cancel(cause: nil)
        widgetsUpdateJob = UiStateBridge.shared.observeUpdates { [weak self] updatedDisplay in
            guard let self = self, self.display == Int32(truncating: updatedDisplay) else { return }
            self.reloadWidgetsFromShared()
        }
        
        widgetsLoadingCompletionJob?.cancel(cause: nil)
        let onCompletion: () -> Void = { [weak self] in
            DispatchQueue.main.async {
                self?.handleWidgetsLoadingCompletion()
            }
        }
        widgetsLoadingCompletionJob = UiInterfaceModeBridgeV3.shared.isEnabled()
            ? viewModel.observeWidgetsLoadCompletion(onCompletion)
            : UiStateBridge.shared.observeWidgetsLoadCompletion(callback: onCompletion)
        
        widgetsInitializationInfoJob?.cancel(cause: nil)
        let onInitializationInfo: (Int32, Int32) -> Void = { [weak self] parametersNum, subDeviceNum in
            DispatchQueue.main.async {
                self?.handleInitializationInfo(parametersNum: parametersNum, subDeviceNum: subDeviceNum)
            }
        }
        if UiInterfaceModeBridgeV3.shared.isEnabled() {
            widgetsInitializationInfoJob = viewModel.observeInitializationInfo {
                onInitializationInfo($0.parametersNum, $0.subDeviceNum)
            }
        } else {
            widgetsInitializationInfoJob = UiStateBridge.shared.observeInitializationInfo {
                onInitializationInfo($0.parametersNum, $0.subDeviceNum)
            }
        }

        widgetsLoadingProgressJob?.cancel(cause: nil)
        let onProgress: (Int32, Int32) -> Void = { [weak self] current, total in
            DispatchQueue.main.async {
                self?.handleWidgetsLoadingProgress(current: current, total: total)
            }
        }
        if UiInterfaceModeBridgeV3.shared.isEnabled() {
            widgetsLoadingProgressJob = viewModel.observeWidgetsLoadingProgress {
                onProgress($0.current, $0.total)
            }
        } else {
            widgetsLoadingProgressJob = UiStateBridge.shared.observeWidgetsLoadingProgress {
                onProgress($0.current, $0.total)
            }
        }

    }

    private func stopObservingWidgetUpdates() {
        print("[WIDGET_COORDINATOR] stopObservingWidgetUpdates")
        widgetsUpdateJob?.cancel(cause: nil)
        widgetsUpdateJob = nil
        widgetsLoadingCompletionJob?.cancel(cause: nil)
        widgetsLoadingCompletionJob = nil
        widgetsInitializationInfoJob?.cancel(cause: nil)
        widgetsInitializationInfoJob = nil
        widgetsLoadingProgressJob?.cancel(cause: nil)
        widgetsLoadingProgressJob = nil
        stopObservingTelemetryCounters()
    }

    private func reloadWidgetsFromShared() {
        print("[WIDGET_COORDINATOR] reloadWidgetsFromShared")
        if isUiTestForceGesturesWidget,
           display == 0 {
            let uiTestPage = makeUiTestGesturesOnlyPage()
            lastWidgetsSignature = "ui-test-gestures-only"
            viewModel.update(with: uiTestPage)
            return
        }

        let dataFactory = DataFactory()
        let kotlinWidgets: [Any]
        if isSpecialSettingsMobileSource {
            kotlinWidgets = dataFactory.mobileWidgets()
        } else {
            //TODO: тут можно включать фейковые виджеты (2)
            var prostheticWidgets = dataFactory.prepareData(display: display)
            if isUiTestSkipSynchronization && prostheticWidgets.isEmpty {
                prostheticWidgets = dataFactory.fakeData()
            }
            kotlinWidgets = prostheticWidgets
        }
        
//        let kotlinWidgets = dataFactory.fakeData2()
//        handleWidgetsLoadingCompletion()
        
        print("[WIDGET_COORDINATOR] kotlinWidgets: \(kotlinWidgets)")
        
        let widgetsDTO = WidgetDescriptorFactoryV3.makeWidgetsDTO(from: kotlinWidgets)
        print("[WIDGET_COORDINATOR] widgetsDTO: \(widgetsDTO)")
        let widgetsSignature = makeWidgetsSignature(from: widgetsDTO)
        guard widgetsSignature != lastWidgetsSignature else {
            return
        }
        lastWidgetsSignature = widgetsSignature

        let mockResponseDTO = WidgetsResponseDTO(
            page: 1,
            totalPages: 1,
            widgets: widgetsDTO
        )
        
        
        let requestDTO = WidgetsRequestDTO(query: WidgetQuery(query: "My request").query, page: 1)
        viewModel.update(with: mockResponseDTO.toDomain())
        storage.save(response: mockResponseDTO, for: requestDTO)
    }

    private func makeUiTestGesturesOnlyPage() -> WidgetsPage {
        let widget = Widget(
            id: "ui-test-gestures-widget",
            title: SharedLocalizedText.text(SharedRes.strings().title_home),
            title_2: nil,
            widgetType: .gestureWidget,
            deviceAddress: 0,
            parameterID: 0,
            widget: nil
        )
        return WidgetsPage(page: 1, totalPages: 1, widgets: [widget])
    }

    private func bind(to viewModel: WidgetsListViewModel) {
        viewModel.setSynchronizationEffectHandler { [weak self] effect in
            switch effect {
            case .reloadWidgets: self?.reloadWidgetsFromShared()
            case .reloadTable: self?.widgetsTableViewController?.reload()
            case .resetWidgets: UiStateBridge.shared.resetWidgetsState()
            case .stopObserving: self?.stopObservingWidgetUpdates()
            case .renderContent, .renderWidgetsContainer, .renderLoading, .requestInitialization: break
            }
        }
        viewModel.synchronizationPresentation.observe(on: self) { [weak self] presentation in
            guard let self, let presentation else { return }
            switch presentation {
            case .content(let visible):
                if visible { self.showWidgetsContent() } else { self.hideWidgetsContentForSynchronization() }
            case .widgetsContainer(let visible): self.widgetsListContainer.isHidden = !visible
            case .loading(let state):
                if let state { LoadingView.show(state: state, in: self.view) } else { LoadingView.hide() }
            }
        }
        viewModel.items.observe(on: self) { [weak self] _ in self?.updateItems() }
        viewModel.loading.observe(on: self) { [weak self] in self?.updateLoading($0) }
        viewModel.error.observe(on: self) { [weak self] in self?.showError($0) }
    }
    
    private func extractTitle(from widget: Any?) -> String? {
            switch widget {
            case let plotItem as PlotItem:
                return plotItem.title
            case let sliderItem as SliderItem:
                return sliderItem.title
            case let buttonItem as OneButtonItem:
                return buttonItem.title
            case let gesturesItem as GesturesItem:
                return gesturesItem.title
            case let switchItem as SwitchItem:
                return switchItem.title
            case let trainingItem as TrainingGestureItem:
                return trainingItem.title
            case let spinnerItem as SpinnerItem:
                return spinnerItem.title
            case let textInputItem as TextInputItemV3:
                return "\(textInputItem.title)%\(textInputItem.buttonTitle)"
            default:
                return nil
            }
        }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == String(describing: WidgetsListTableViewController.self),
            let destinationVC = segue.destination as? WidgetsListTableViewController {
            widgetsTableViewController = destinationVC
            widgetsTableViewController?.viewModel = viewModel
            viewModel.viewDidLoad()
        } else if segue.identifier == "go3DGripperSettings",
            let destinationVC = segue.destination as? AAPLOpenGLViewControllerV3 {
            destinationVC.gestureNumber = open3DGestureId ?? 0
            destinationVC.useV3Mode = true
            destinationVC.useV3GestureProtocol = open3DGestureUsesV3Protocol
            NSLog(
                "[V3OpenTrace] event=prepare3DDestination thread=main gestureId=%ld useV3Mode=%d useV3Protocol=%d",
                destinationVC.gestureNumber,
                destinationVC.useV3Mode,
                destinationVC.useV3GestureProtocol
            )
        }
    }

    // MARK: - Private
    @objc private func bottomButtonTapped() {
//        showToast("Тост для проверки Тост для проверки Тост для проверки Тост для проверки")
        resetWidgetsStateForResynchronization()
        viewModel.onSynchronizationAction(.begin(reset: true, state: nil, isMobile: isSpecialSettingsMobileSource))
        viewModel.requestInicializeInformation()
        print("[handleWidgetsLoadingCompletion] bottomButtonTapped")
    }
    
    
    private func resetWidgetsStateForResynchronization() {
        UiStateBridge.shared.resetWidgetsState()
        PlotListItemViewModel.resetRequestCache()
        PlotListItemViewModelV3.resetRequestCache()
        SliderListItemViewModel.resetRequestCache()
        SwitchListItemViewModel.resetRequestCache()
        lastWidgetsSignature = nil
        viewModel.items.value = []
        widgetsTableViewController?.reload()
    }

    private func setPlotPointRenderingPaused(_ paused: Bool) {
        guard display == 1 else { return }
        NotificationCenter.default.post(
            name: paused ? .v3PausePlotPointRendering : .v3ResumePlotPointRendering,
            object: nil
        )
    }
    
    private func setupViews() {
        view.backgroundColor = tabsBackgroundColor
        view.isOpaque = true
        contentView?.backgroundColor = tabsBackgroundColor
        widgetsListContainer?.backgroundColor = tabsBackgroundColor
        suggestionsListContainer?.backgroundColor = .clear
        tableViewWidgets?.backgroundColor = tabsBackgroundColor
        tableViewWidgets?.isOpaque = true
        title = viewModel.screenTitle
        title = screenTitleOverride ?? viewModel.screenTitle
        emptyDataLabel.text = viewModel.emptyDataTitle
    }

    private func makeWidgetsSignature(from widgets: [WidgetsResponseDTO.WidgetDTO]) -> String {
        widgets.map {
            "\($0.id)|\($0.widgetType?.rawValue ?? "unknown")|\($0.title)"
        }.joined(separator: "||")
    }

    private func updateItems() {
        viewModel.onSynchronizationAction(.itemsChanged(isMobile: isSpecialSettingsMobileSource))
    }

    private func updateLoading(_ loading: WidgetsListViewModelLoading?) {
        viewModel.onSynchronizationAction(.loadingChanged(loading, isMobile: isSpecialSettingsMobileSource))
        widgetsTableViewController?.updateLoading(loading)
    }

    private func showError(_ error: String) {
        guard !error.isEmpty else { return }
        showAlert(title: viewModel.errorTitle, message: error)
    }
    
    private func handleWidgetsLoadingCompletion() {
        viewModel.onSynchronizationAction(.completed(isV3: UiInterfaceModeBridgeV3.shared.isEnabled(),
                                                    isMobile: isSpecialSettingsMobileSource))
    }
    
    private func handleInitializationInfo(parametersNum: Int32, subDeviceNum: Int32) {
        viewModel.onSynchronizationAction(.initializationInfo(parameters: parametersNum, subDevices: subDeviceNum))
    }

    private func handleWidgetsLoadingProgress(current: Int32, total: Int32) {
        viewModel.onSynchronizationAction(.progress(current: current, total: total))
    }

    private func hideWidgetsContentForSynchronization() {
        emptyDataLabel.isHidden = true
        widgetsListContainer.isHidden = true
        suggestionsListContainer.isHidden = true
    }

    private func showWidgetsContent() {
        widgetsListContainer.isHidden = false
        emptyDataLabel.isHidden = !viewModel.isEmpty
        suggestionsListContainer.isHidden = true
    }
}

extension WidgetsListViewController {
    func setSpecialSettingsSource(_ source: SpecialSettingsSource) {
        guard display == 2 else { return }
        guard specialSettingsSource != source else { return }
        specialSettingsSource = source
        lastWidgetsSignature = nil
        reloadWidgetsFromShared()

        viewModel.onSynchronizationAction(.sourceChanged(isMobile: isSpecialSettingsMobileSource))
    }
}

extension WidgetsListViewController {
    static var isGlobalSynchronizationCompleted: Bool {
        globalSynchronizationCompleted
    }

    static var isGlobalSynchronizationInProgress: Bool {
        globalSynchronizationInProgress
    }

    static func resetGlobalSynchronizationState() {
        globalSynchronizationCompleted = false
        globalSynchronizationInProgress = false
    }
}


private extension WidgetsListViewController {
    var isSpecialSettingsMobileSource: Bool {
        display == 2 && specialSettingsSource == .mobile
    }

    var isServiceSettingsDisplay: Bool {
        display == 4
    }

    func startObservingTelemetryCountersIfNeeded() {
        guard isServiceSettingsDisplay else { return }

        telemetryCountersJob?.cancel(cause: nil)
        telemetryCountersJob = viewModel.observeTelemetryCounters { [weak self] usage in
            self?.updateGestureUsageWidget(with: usage)
        }
        applyGestureUsageWidgetIfNeeded()
    }

    func stopObservingTelemetryCounters() {
        telemetryCountersJob?.cancel(cause: nil)
        telemetryCountersJob = nil
    }

    func requestTelemetryDataIfNeeded() {
        guard isServiceSettingsDisplay else { return }
        guard UiInterfaceModeBridgeV3.shared.isEnabled() else { return }
        viewModel.requestTelemetryData()
    }

    func updateGestureUsageWidget(with usage: [V3GestureUsage]) {
        latestGestureUsageItems = makeGestureUsageItems(from: usage)
        applyGestureUsageWidgetIfNeeded()
    }

    func applyGestureUsageWidgetIfNeeded() {
        guard isServiceSettingsDisplay else { return }

        if isUiTestGestureUsageSample {
            latestGestureUsageItems = makeGestureUsageSampleItems()
        }
        let viewModelItem = GestureUsageListItemViewModel(
            id: "gesture-usage",
            title: SharedLocalizedText.text(SharedRes.strings().gesture_usage_chart_title),
            emptyTitle: SharedLocalizedText.text(SharedRes.strings().gesture_usage_empty),
            totalTitle: gestureUsageTotalTitle,
            items: latestGestureUsageItems
        )
        let listItem = ListItemType.gestureUsage(viewModelItem)
        var items = viewModel.items.value.filter {
            if case .gestureUsage = $0 { return false }
            return true
        }
        items.insert(listItem, at: 0)

        guard items != viewModel.items.value else { return }
        viewModel.items.value = items
        widgetsTableViewController?.reload()
    }

    func makeGestureUsageSampleItems() -> [GestureUsageChartItem] {
        [
            GestureUsageChartItem(gestureId: 5, title: baseGestureName(for: 5), count: 69, colorIndex: 5),
            GestureUsageChartItem(gestureId: 1, title: baseGestureName(for: 1), count: 53, colorIndex: 1),
            GestureUsageChartItem(gestureId: 4, title: baseGestureName(for: 4), count: 24, colorIndex: 4),
            GestureUsageChartItem(gestureId: 3, title: baseGestureName(for: 3), count: 17, colorIndex: 3),
            GestureUsageChartItem(gestureId: 6, title: baseGestureName(for: 6), count: 6, colorIndex: 6),
            GestureUsageChartItem(gestureId: 64, title: customGestureNames().first ?? SharedLocalizedText.text(SharedRes.strings().gesture_1_btn), count: 5, colorIndex: 64),
            GestureUsageChartItem(gestureId: 2, title: baseGestureName(for: 2), count: 3, colorIndex: 2),
            GestureUsageChartItem(gestureId: 7, title: baseGestureName(for: 7), count: 1, colorIndex: 7),
            GestureUsageChartItem(gestureId: 8, title: baseGestureName(for: 8), count: 1, colorIndex: 8),
            GestureUsageChartItem(gestureId: 14, title: baseGestureName(for: 14), count: 1, colorIndex: 14),
            GestureUsageChartItem(gestureId: 68, title: customGestureNames().indices.contains(4) ? customGestureNames()[4] : SharedLocalizedText.text(SharedRes.strings().gesture_5_btn), count: 1, colorIndex: 68)
        ]
    }

    func makeGestureUsageItems(from usage: [V3GestureUsage]) -> [GestureUsageChartItem] {
        GestureUsageChartItem.makeItems(from: usage, customNames: customGestureNames(), baseName: baseGestureName)
    }

    func baseGestureName(for gestureId: Int) -> String {
        switch gestureId {
        case 1:
            return SharedLocalizedText.text(SharedRes.strings().fist)
        case 2:
            return SharedLocalizedText.text(SharedRes.strings().gesture_point)
        case 3:
            return SharedLocalizedText.text(SharedRes.strings().gesture_pinch)
        case 4:
            return SharedLocalizedText.text(SharedRes.strings().gesture_fist_thumb_over)
        case 5:
            return SharedLocalizedText.text(SharedRes.strings().gesture_key)
        case 6:
            return SharedLocalizedText.text(SharedRes.strings().gesture_rock)
        case 7:
            return SharedLocalizedText.text(SharedRes.strings().gesture_twizzers)
        case 8:
            return SharedLocalizedText.text(SharedRes.strings().gesture_cupholder)
        case 9:
            return SharedLocalizedText.text(SharedRes.strings().gesture_half_grab)
        case 10:
            return SharedLocalizedText.text(SharedRes.strings().gesture_ok)
        case 11:
            return SharedLocalizedText.text(SharedRes.strings().gesture_thumb_up)
        case 12:
            return SharedLocalizedText.text(SharedRes.strings().gesture_middle_finger)
        case 13:
            return Locale.current.language.languageCode?.identifier == "ru" ? "Щёпоть 2" : "Pinch 2"
        case 14:
            return SharedLocalizedText.text(SharedRes.strings().gesture_call_me)
        case 15:
            return SharedLocalizedText.text(SharedRes.strings().gesture_natural_position)
        default:
            return "Gesture \(gestureId)"
        }
    }

    func customGestureNames() -> [String] {
        GestureUsageChartItem.localizedCustomNames(viewModel.customGestureNames())
    }

    var gestureUsageTotalTitle: String {
        Locale.current.languageCode == "ru" ? "Всего:" : "Total:"
    }


}


enum WidgetsSynchronizationLoadingConfiguration {
    private static let userDefaultsKey = "widgetsSynchronizationLoadingEnabled"

    /// Controls whether the fullscreen LoadingView should be shown during widgets synchronization.
    /// - Note: The value is persisted in `UserDefaults` so it can be toggled from debug utilities
    ///         or other parts of the application and remembered between launches.
    static var isEnabled: Bool {
        get {
            guard UserDefaults.standard.object(forKey: userDefaultsKey) != nil else { return true }
            return UserDefaults.standard.bool(forKey: userDefaultsKey)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: userDefaultsKey)
        }
    }

    /// Removes the persisted preference forcing the controller to fallback to the default behaviour.
    static func reset() {
        UserDefaults.standard.removeObject(forKey: userDefaultsKey)
    }
}
