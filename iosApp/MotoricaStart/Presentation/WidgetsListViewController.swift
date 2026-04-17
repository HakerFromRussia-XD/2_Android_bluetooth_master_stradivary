import UIKit
import DGCharts
import shared

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
        button.setTitle("Нажми меня", for: .normal)
        button.accessibilityIdentifier = AccessibilityIdentifier.widgetsResyncButton
        button.addTarget(self, action: #selector(bottomButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private var viewModel: WidgetsListViewModel!
    private var isUiTestSkipSynchronization: Bool {
        ProcessInfo.processInfo.arguments.contains("-ui-test-skip-synchronization")
    }

    private var widgetsTableViewController: WidgetsListTableViewController?
    private var widgetsUpdateJob: Kotlinx_coroutines_coreJob?
    private var widgetsLoadingCompletionJob: Kotlinx_coroutines_coreJob?
    private var widgetsInitializationInfoJob: Kotlinx_coroutines_coreJob?
    private var widgetsLoadingProgressJob: Kotlinx_coroutines_coreJob?
    private static var globalSynchronizationCompleted = false {
        didSet { notifyGlobalSynchronizationStateDidChange() }
    }
    private static var globalSynchronizationInProgress = false {
        didSet { notifyGlobalSynchronizationStateDidChange() }
    }
    private var needsReloadAfterSynchronization = false
    private let defaultLoadingState = LoadingView.State(
        message: NSLocalizedString("Синхронизация данных...", comment: ""),
        progress: 0
    )
    private var widgetsLoadingMax: Float = 0
    private var currentLoadingMessage: String?
    private var lastKnownLoadingState: LoadingView.State?
    private var isViewVisible = false
    private var hasReceivedWidgetsLoadingProgress = false
    private var hasRetriedSynchronizationWithoutProgress = false
    private var open3DGestureId: Int?
    private var lastWidgetsSignature: String?
    var display: Int32 = 1
    var screenTitleOverride: String?
    let storage = CoreDataWidgetsResponseStorage()

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
        viewModel.setCustomGestureSettingsOpener { [weak self] gestureId in
            DispatchQueue.main.async {
                let animationsWereEnabled = UIView.areAnimationsEnabled
                if !animationsWereEnabled {
                    UIView.setAnimationsEnabled(true)
                }
                
                self?.open3DGestureId = gestureId
                self?.performSegue(withIdentifier: "go3DGripperSettings", sender: nil)
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
    }

    private func reloadWidgetsFromShared() {
        print("[WIDGET_COORDINATOR] reloadWidgetsFromShared")
        let dataFactory = DataFactory()
        //TODO: тут можно включать фейковые виджеты (2)
        var kotlinWidgets = dataFactory.prepareData(display: display)
        if isUiTestSkipSynchronization && kotlinWidgets.isEmpty {
            kotlinWidgets = dataFactory.fakeData()
        }
        
//        let kotlinWidgets = dataFactory.fakeData2()
//        handleWidgetsLoadingCompletion()
        
        print("[WIDGET_COORDINATOR] kotlinWidgets: \(kotlinWidgets)")
        
        let widgetsDTO = WidgetDescriptorFactoryV3.makeWidgetsDTO(from: kotlinWidgets)
        print("[WIDGET_COORDINATOR] widgetsDTO: \(widgetsDTO)")
        let widgetsSignature = makeWidgetsSignature(from: widgetsDTO)
        guard widgetsSignature != lastWidgetsSignature else { return }
        lastWidgetsSignature = widgetsSignature

        let mockResponseDTO = WidgetsResponseDTO(
            page: 1,
            totalPages: 1,
            widgets: widgetsDTO
        )
        
        
        let requestDTO = WidgetsRequestDTO(query: WidgetQuery(query: "My request").query, page: 1)
        storage.save(response: mockResponseDTO, for: requestDTO) { [weak self] responseDTO in
            guard let self = self else { return }
            self.viewModel.update(with: responseDTO.toDomain())
        }
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
            let destinationVC = segue.destination as? AAPLOpenGLViewController {
            destinationVC.gestureNumber = open3DGestureId ?? 0
        }
    }

    // MARK: - Private
    @objc private func bottomButtonTapped() {
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
        if isSynchronizationCompleted {
            widgetsTableViewController?.reload()
            showWidgetsContent()
        } else {
            needsReloadAfterSynchronization = true
        }
    }

    private func updateLoading(_ loading: WidgetsListViewModelLoading?) {
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
