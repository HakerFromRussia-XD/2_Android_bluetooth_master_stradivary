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
    private static var globalSynchronizationCompleted = false {
        didSet { notifyGlobalSynchronizationStateDidChange() }
    }
    private static var globalSynchronizationInProgress = false {
        didSet { notifyGlobalSynchronizationStateDidChange() }
    }
    private var needsReloadAfterSynchronization = false
    private let defaultLoadingState = LoadingView.State(
        message: SharedLocalizedText.text(SharedRes.strings().synchronization_data),
        progress: 0
    )
    private var widgetsLoadingMax: Float = 0
    private var currentLoadingMessage: String?
    private var lastKnownLoadingState: LoadingView.State?
    private var isViewVisible = false
    private var hasReceivedWidgetsLoadingProgress = false
    private var hasRetriedSynchronizationWithoutProgress = false
    private var open3DGestureId: Int?
    private var open3DGestureIsV3 = false
    private var latestGestureUsageItems: [GestureUsageChartItem] = []
    private var lastWidgetsSignature: String?
    private var specialSettingsSource: SpecialSettingsSource = .prosthetic
    var display: Int32 = 1
    var screenTitleOverride: String?
    let storage = CoreDataWidgetsResponseStorage()
    private let tabsBackgroundColor = UIColor(named: "ubi4_back") ?? .black

    private static func notifyGlobalSynchronizationStateDidChange() {
        NotificationCenter.default.post(
            name: .widgetsSynchronizationStateDidChange,
            object: nil,
            userInfo: [
                "completed": globalSynchronizationCompleted,
                "inProgress": globalSynchronizationInProgress
            ]
        )
    }
    
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
                self?.open3DGestureIsV3 = isV3
                self?.performSegue(withIdentifier: "go3DGripperSettings", sender: nil)
            }
            DispatchQueue.main.async {
                guard isV3 else {
                    openScreen()
                    return
                }
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
        if isUiTestSkipSynchronization {
            isSynchronizationCompleted = true
            isSynchronizationInProgress = false
            showWidgetsContent()
            LoadingView.hide()
        }
        if !isSynchronizationCompleted {
            hideWidgetsContentForSynchronization()
        } else {
            showWidgetsContent()
        }
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
        isViewVisible = true
        PlotListItemViewModel.resetRequestCache()
        PlotListItemViewModelV3.resetRequestCache()
        SliderListItemViewModel.resetRequestCache()
        setPlotPointRenderingPaused(false)
        startObservingWidgetUpdates()
        reloadWidgetsFromShared()
        requestTelemetryDataIfNeeded()
        if isSpecialSettingsMobileSource {
            LoadingView.hide()
            showWidgetsContent()
            return
        }
        if isUiTestSkipSynchronization {
            isSynchronizationCompleted = true
            isSynchronizationInProgress = false
            needsReloadAfterSynchronization = false
            hasReceivedWidgetsLoadingProgress = true
            hasRetriedSynchronizationWithoutProgress = false
            widgetsLoadingMax = 0
            currentLoadingMessage = nil
            lastKnownLoadingState = nil
            LoadingView.hide()
            showWidgetsContent()
            return
        }
        if isSynchronizationCompleted {
            showWidgetsContent()
        } else if isSynchronizationInProgress {
            hideWidgetsContentForSynchronization()
            if let loadingState = lastKnownLoadingState {
                currentLoadingMessage = loadingState.message
                presentLoading(with: loadingState)
            } else {
                beginSynchronization(state: defaultLoadingState)
            }
        } else {
            UiStateBridge.shared.resetWidgetsState()
            beginSynchronization(state: defaultLoadingState)
            viewModel.requestInicializeInformation()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        isViewVisible = false
        setPlotPointRenderingPaused(true)
        print("[WIDGET_COORDINATOR] viewWillDisappear")
        if lastKnownLoadingState != nil {
            LoadingView.hide()
        }
        if isSynchronizationCompleted {
            stopObservingWidgetUpdates()
        }
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
        widgetsLoadingCompletionJob = UiStateBridge.shared.observeWidgetsLoadCompletion { [weak self] in
            DispatchQueue.main.async {
                self?.handleWidgetsLoadingCompletion()
            }
        }
        
        widgetsInitializationInfoJob?.cancel(cause: nil)
        widgetsInitializationInfoJob = UiStateBridge.shared.observeInitializationInfo { [weak self] info in
            DispatchQueue.main.async {
                self?.handleInitializationInfo(info)
            }
        }

        widgetsLoadingProgressJob?.cancel(cause: nil)
        widgetsLoadingProgressJob = UiStateBridge.shared.observeWidgetsLoadingProgress { [weak self] progress in
            DispatchQueue.main.async {
                self?.handleWidgetsLoadingProgress(progress)
            }
        }

        startObservingTelemetryCountersIfNeeded()
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
            applyGestureUsageWidgetIfNeeded()
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
        applyGestureUsageWidgetIfNeeded()
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
            destinationVC.useV3Mode = open3DGestureIsV3
            NSLog(
                "[V3OpenTrace] event=prepare3DDestination thread=main gestureId=%ld useV3Mode=%d",
                destinationVC.gestureNumber,
                destinationVC.useV3Mode
            )
        }
    }

    // MARK: - Private
    @objc private func bottomButtonTapped() {
//        showToast("Тост для проверки Тост для проверки Тост для проверки Тост для проверки")
        resetWidgetsStateForResynchronization()
        beginSynchronization(resetState: true, state: defaultLoadingState)
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
        if isSynchronizationCompleted || isSpecialSettingsMobileSource {
            widgetsTableViewController?.reload()
            showWidgetsContent()
        } else {
            needsReloadAfterSynchronization = true
        }
    }

    private func updateLoading(_ loading: WidgetsListViewModelLoading?) {
        if isSpecialSettingsMobileSource {
            showWidgetsContent()
            widgetsTableViewController?.updateLoading(loading)
            return
        }

        switch loading {
        case .some(.fullScreen(let state)): beginSynchronization(state: state)
        case .some(.nextPage):
            if isSynchronizationCompleted {
                widgetsListContainer.isHidden = false
            }
        case .none:
            if isSynchronizationCompleted {
                showWidgetsContent()
            }
        case .some:
            if !isSynchronizationCompleted {
                beginSynchronization(state: defaultLoadingState)
            }
        }
        widgetsTableViewController?.updateLoading(loading)
    }

    private func showError(_ error: String) {
        guard !error.isEmpty else { return }
        showAlert(title: viewModel.errorTitle, message: error)
    }
    
    private func handleWidgetsLoadingCompletion() {
        guard isSynchronizationInProgress else { return }

        if UiInterfaceModeBridgeV3.shared.isEnabled(),
           isSynchronizationInProgress,
           !hasReceivedWidgetsLoadingProgress {
            beginSynchronization(state: lastKnownLoadingState ?? defaultLoadingState)
            if !hasRetriedSynchronizationWithoutProgress {
                hasRetriedSynchronizationWithoutProgress = true
                viewModel.requestInicializeInformation()
            }
            return
        }

        isSynchronizationCompleted = true
        isSynchronizationInProgress = false
        widgetsLoadingMax = 0
        hasReceivedWidgetsLoadingProgress = false
        hasRetriedSynchronizationWithoutProgress = false
        currentLoadingMessage = nil
        lastKnownLoadingState = nil
        LoadingView.hide()
        if needsReloadAfterSynchronization {
            widgetsTableViewController?.reload()
            needsReloadAfterSynchronization = false
        }
        showWidgetsContent()
        print("[handleWidgetsLoadingCompletion] COMPLETED!!!!")
    }
    
    private func handleInitializationInfo(_ info: FullInicializeConnectionStruct) {
        let totalSteps = info.parametersNum * info.subDeviceNum
        widgetsLoadingMax = totalSteps > 0 ? Float(totalSteps) : 0
    }

    private func handleWidgetsLoadingProgress(_ progress: WidgetsLoadingProgress) {
        guard !isSynchronizationCompleted else { return }
        guard isSynchronizationInProgress else { return }
        hasReceivedWidgetsLoadingProgress = true
        print("[BLE-PROGRESS] total = \(Int(progress.total)) current = \(Int(progress.current))")
        let totalValue = Int(progress.total)
        if totalValue > 0 {
            widgetsLoadingMax = Float(totalValue)
        }
        guard widgetsLoadingMax > 0 else { return }

        let currentValue = Int(progress.current)
        let normalized = min(max(Float(currentValue) / widgetsLoadingMax, 0), 1)
        let message = currentLoadingMessage ?? defaultLoadingState.message
        currentLoadingMessage = message
        presentLoading(with: LoadingView.State(message: message, progress: normalized))
    }
    
    private func beginSynchronization(resetState: Bool = false, state: LoadingView.State? = nil) {
        guard !isSpecialSettingsMobileSource else {
            LoadingView.hide()
            showWidgetsContent()
            return
        }

        if resetState {
            isSynchronizationCompleted = false
            isSynchronizationInProgress = false
            needsReloadAfterSynchronization = false
            widgetsLoadingMax = 0
            hasReceivedWidgetsLoadingProgress = false
            hasRetriedSynchronizationWithoutProgress = false
            lastKnownLoadingState = nil
        }
        guard !isSynchronizationCompleted else {
            if needsReloadAfterSynchronization {
                widgetsTableViewController?.reload()
                needsReloadAfterSynchronization = false
            }
            showWidgetsContent()
            return
        }
        
        hideWidgetsContentForSynchronization()
        let loadingState = state ?? lastKnownLoadingState ?? defaultLoadingState
        if !isSynchronizationInProgress {
            isSynchronizationInProgress = true
            widgetsLoadingMax = 0
            hasReceivedWidgetsLoadingProgress = false
            hasRetriedSynchronizationWithoutProgress = false
        }
        currentLoadingMessage = loadingState.message
        presentLoading(with: loadingState)
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

        if isSpecialSettingsMobileSource {
            LoadingView.hide()
            needsReloadAfterSynchronization = false
            showWidgetsContent()
        } else if isViewVisible {
            if isSynchronizationCompleted {
                showWidgetsContent()
            } else if isSynchronizationInProgress {
                beginSynchronization(state: lastKnownLoadingState ?? defaultLoadingState)
            } else {
                UiStateBridge.shared.resetWidgetsState()
                beginSynchronization(state: defaultLoadingState)
                viewModel.requestInicializeInformation()
            }
        }
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
        telemetryCountersJob = WidgetStateBridge.shared.observeTelemetryGestureCounters { [weak self] counters in
            DispatchQueue.main.async {
                self?.updateGestureUsageWidget(with: counters)
            }
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

    func updateGestureUsageWidget(with counters: TelemetryGestureCounters) {
        latestGestureUsageItems = makeGestureUsageItems(from: counters)
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

    func makeGestureUsageItems(from counters: TelemetryGestureCounters) -> [GestureUsageChartItem] {
        let baseItems: [GestureUsageChartItem] = counters.baseGestureMovementCount.enumerated().compactMap { index, rawCount in
            guard Self.baseGestureIds.indices.contains(index) else { return nil }
            let gestureId = Self.baseGestureIds[index]
            let count = longValue(from: rawCount)
            guard gestureId != 0, count > 0 else { return nil }

            return GestureUsageChartItem(
                gestureId: gestureId,
                title: baseGestureName(for: gestureId),
                count: count,
                colorIndex: gestureId
            )
        }

        let customNames = customGestureNames()
        let customItems: [GestureUsageChartItem] = counters.customGestureMovementCount.enumerated().compactMap { index, rawCount in
            let count = longValue(from: rawCount)
            guard count > 0 else { return nil }
            let gestureId = Self.customGestureBaseId + index

            return GestureUsageChartItem(
                gestureId: gestureId,
                title: customNames.indices.contains(index) ? customNames[index] : "\(SharedLocalizedText.text(SharedRes.strings().custom_gesture)) \(index + 1)",
                count: count,
                colorIndex: gestureId
            )
        }

        return (baseItems + customItems)
            .sorted {
                if $0.count == $1.count {
                    return $0.gestureId < $1.gestureId
                }
                return $0.count > $1.count
            }
    }

    func longValue(from value: Any) -> Int64 {
        switch value {
        case let kotlinLong as KotlinLong:
            return kotlinLong.int64Value
        case let number as NSNumber:
            return number.int64Value
        default:
            return 0
        }
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
            return SharedLocalizedText.text(SharedRes.strings().gesture_double_point)
        case 14:
            return SharedLocalizedText.text(SharedRes.strings().gesture_call_me)
        case 15:
            return SharedLocalizedText.text(SharedRes.strings().gesture_natural_position)
        default:
            return "Gesture \(gestureId)"
        }
    }

    func customGestureNames() -> [String] {
        let stored = GestureService.shared.loadNames()
        let defaults = [
            SharedRes.strings().gesture_1_btn,
            SharedRes.strings().gesture_2_btn,
            SharedRes.strings().gesture_3_btn,
            SharedRes.strings().gesture_4_btn,
            SharedRes.strings().gesture_5_btn,
            SharedRes.strings().gesture_6_btn,
            SharedRes.strings().gesture_7_btn,
            SharedRes.strings().gesture_8_btn,
            SharedRes.strings().gesture_9_btn,
            SharedRes.strings().gesture_10_btn,
            SharedRes.strings().gesture_11_btn,
            SharedRes.strings().gesture_12_btn,
            SharedRes.strings().gesture_13_btn,
            SharedRes.strings().gesture_14_btn,
            SharedRes.strings().gesture_15_btn
        ].map { SharedLocalizedText.text($0) }

        guard stored.count < defaults.count else { return Array(stored.prefix(defaults.count)) }
        return stored + Array(defaults[stored.count...])
    }

    var gestureUsageTotalTitle: String {
        Locale.current.languageCode == "ru" ? "Всего:" : "Total:"
    }

    static var baseGestureIds: [Int] {
        Array(0...15)
    }

    static var customGestureBaseId: Int {
        64
    }

    func presentLoading(with state: LoadingView.State) {
        lastKnownLoadingState = state
        guard isViewVisible else { return }
        //TODO: тут можно отключать лоадер (3)
        LoadingView.show(state: state, in: view)
    }
    
    var isSynchronizationCompleted: Bool {
        get { Self.globalSynchronizationCompleted }
        set { Self.globalSynchronizationCompleted = newValue }
    }

    var isSynchronizationInProgress: Bool {
        get { Self.globalSynchronizationInProgress }
        set { Self.globalSynchronizationInProgress = newValue }
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
