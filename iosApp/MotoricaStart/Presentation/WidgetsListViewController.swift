import UIKit
import DGCharts
import shared

final class WidgetsListViewController: UIViewController, StoryboardInstantiable, Alertable {
    static var defaultFileName: String { "WidgetsListViewController" }
    @IBOutlet private var contentView: UIView!
    @IBOutlet private var widgetsListContainer: UIView!
    @IBOutlet private(set) var suggestionsListContainer: UIView!
    @IBOutlet private var emptyDataLabel: UILabel!
    private lazy var bottomButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Нажми меня", for: .normal)
        button.addTarget(self, action: #selector(bottomButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private var viewModel: WidgetsListViewModel!

    private var widgetsTableViewController: WidgetsListTableViewController?
    private var widgetsUpdateJob: Kotlinx_coroutines_coreJob?
    private var widgetsLoadingCompletionJob: Kotlinx_coroutines_coreJob?
    private var widgetsInitializationInfoJob: Kotlinx_coroutines_coreJob?
    private var widgetsLoadingProgressJob: Kotlinx_coroutines_coreJob?
    private static var globalSynchronizationCompleted = false
    private static var globalSynchronizationInProgress = false
    private var needsReloadAfterSynchronization = false
    private let defaultLoadingState = LoadingView.State(
        message: NSLocalizedString("Синхронизация данных...", comment: ""),
        progress: 0
    )
    private var widgetsLoadingMax: Float = 0
    private var currentLoadingMessage: String?
    var display: Int32 = 1
    var screenTitleOverride: String?
    let storage = CoreDataWidgetsResponseStorage()

    // MARK: - Lifecycle
    static func create(with viewModel: WidgetsListViewModel) -> WidgetsListViewController {
        let view = WidgetsListViewController.instantiateViewController()
        view.viewModel = viewModel
        return view
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        bind(to: viewModel)
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
        beginSynchronization(state: defaultLoadingState)
        startObservingWidgetUpdates()
        reloadWidgetsFromShared()
        if isSynchronizationCompleted {
            LoadingView.hide()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        print("[WIDGET_COORDINATOR] viewWillDisappear")
        if isSynchronizationCompleted {
            stopObservingWidgetUpdates()
        }
    }
    
    deinit {
        stopObservingWidgetUpdates()
    }

    private func startObservingWidgetUpdates() {
        print("[WIDGET_COORDINATOR] startObservingWidgetUpdates")
//        widgetsUpdateJob?.cancel(cause: nil)
//        widgetsUpdateJob = UiStateBridge.shared.observeUpdates { [weak self] updatedDisplay in
//            guard let self = self, self.display == Int32(updatedDisplay) else { return }
//            self.reloadWidgetsFromShared()
        if widgetsUpdateJob == nil {
            widgetsUpdateJob = UiStateBridge.shared.observeUpdates { [weak self] updatedDisplay in
                guard let self = self, self.display == Int32(updatedDisplay) else { return }
                self.reloadWidgetsFromShared()
            }
        }
        
//        widgetsLoadingCompletionJob?.cancel(cause: nil)
//        widgetsLoadingCompletionJob = UiStateBridge.shared.observeWidgetsLoadCompletion { [weak self] in
//            DispatchQueue.main.async {
//                self?.handleWidgetsLoadingCompletion()
        if widgetsLoadingCompletionJob == nil {
            widgetsLoadingCompletionJob = UiStateBridge.shared.observeWidgetsLoadCompletion { [weak self] in
                DispatchQueue.main.async {
                    self?.handleWidgetsLoadingCompletion()
                }
            }
        }
        
//        widgetsInitializationInfoJob?.cancel(cause: nil)
//        widgetsInitializationInfoJob = UiStateBridge.shared.observeInitializationInfo { [weak self] info in
//            DispatchQueue.main.async {
//                self?.handleInitializationInfo(info)
        if widgetsInitializationInfoJob == nil {
            widgetsInitializationInfoJob = UiStateBridge.shared.observeInitializationInfo { [weak self] info in
                DispatchQueue.main.async {
                    self?.handleInitializationInfo(info)
                }
            }
        }

//        widgetsLoadingProgressJob?.cancel(cause: nil)
//        widgetsLoadingProgressJob = UiStateBridge.shared.observeWidgetsLoadingProgress { [weak self] progress in
//            DispatchQueue.main.async {
//                self?.handleWidgetsLoadingProgress(progress)
        if widgetsLoadingProgressJob == nil {
            widgetsLoadingProgressJob = UiStateBridge.shared.observeWidgetsLoadingProgress { [weak self] progress in
                DispatchQueue.main.async {
                    self?.handleWidgetsLoadingProgress(progress)
                }
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
        let kotlinWidgets = dataFactory.prepareData(display: display)
//        let kotlinWidgets = dataFactory.fakeData()
        print("[WIDGET_COORDINATOR] kotlinWidgets: \(kotlinWidgets)")
        
        // Преобразуем Kotlin-виджеты в DTO, помечая SliderItem как рекламу
        let widgetsDTO: [WidgetsResponseDTO.WidgetDTO] = kotlinWidgets
            .enumerated()
            .map { index, widget in
                print("[WIDGET_COORDINATOR] kotlinWidgets  index = \(index)   widget = \(widget)")
                var widgetType: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO?
                
                var title: String?
                var widgetObject: Any? = widget
                
                switch widget {
                    case let plotItem as PlotItem:
                        widgetType = .plotWidget
                        title = plotItem.title
                        widgetObject = plotItem.widget
                    case let sliderItem as SliderItem:
                        widgetType = .sliderWidget
                        title = sliderItem.title
                        widgetObject = sliderItem.widget
                    case let switchItem as SwitchItem:
                        widgetType = .switchWidget
                        title = switchItem.title
                        widgetObject = switchItem.widget
                    case let oneButtonItem as OneButtonItem:
                        widgetType = .commandWidget
                        title = oneButtonItem.title
                        widgetObject = oneButtonItem.widget
                    case is BaseParameterWidgetEStruct, is BaseParameterWidgetSStruct:
                        widgetType = .commandWidget
                    case is GestureOpticParameterWidgetEStruct:
                        widgetType = .commandWidget
                    case is GestureParameterWidgetEStruct:
                        widgetType = .commandWidget
                    case is OpticStartLearningWidgetEStruct, is OpticStartLearningWidgetSStruct:
                        widgetType = .commandWidget
                    case is PlotParameterWidgetEStruct, is PlotParameterWidgetSStruct:
                        widgetType = .plotWidget
                        widgetObject = widget
                    case is SliderParameterWidgetEStruct, is SliderParameterWidgetSStruct:
                        widgetType = .sliderWidget
                        widgetObject = widget
                    case is SpinnerParameterWidgetEStruct, is SpinnerParameterWidgetSStruct:
                        widgetType = .commandWidget
                    case is SwitchParameterWidgetEStruct, is SwitchParameterWidgetSStruct:
                        widgetType = .switchWidget
                        widgetObject = widget
                    case is ThresholdParameterWidgetEStruct, is ThresholdParameterWidgetSStruct:
                        widgetType = .commandWidget
                    default:
                        widgetType = .commandWidget
                    }

                    if title == nil {
                        title = extractTitle(from: widget) ?? "Widget \(index)"
                }
                
                return WidgetsResponseDTO.WidgetDTO(
                    id: index,
                    title: title,
                    widgetType: widgetType,
                    widget: AnyCodable(widgetObject)
                )
            }
        print("[WIDGET_COORDINATOR] widgetsDTO: \(widgetsDTO)")

        let mockResponseDTO = WidgetsResponseDTO(
            page: 1,
            totalPages: 1,
            widgets: widgetsDTO
        )
        
        
        let requestDTO = WidgetsRequestDTO(query: WidgetQuery(query: "My request").query, page: 1)
        storage.save(response: mockResponseDTO, for: requestDTO) { [weak self] responseDTO in
            self?.viewModel.update(with: responseDTO.toDomain())
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
        }
    }

    // MARK: - Private
    @objc private func bottomButtonTapped() {
        beginSynchronization(resetState: true, state: defaultLoadingState)
        viewModel.requestInicializeInformation()
        print("[handleWidgetsLoadingCompletion] bottomButtonTapped")
    }
    
    private func setupViews() {
        title = viewModel.screenTitle
        title = screenTitleOverride ?? viewModel.screenTitle
        emptyDataLabel.text = viewModel.emptyDataTitle
    }

    private func updateItems() {
//        widgetsListContainer.isHidden = false
//        emptyDataLabel.isHidden = true
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
        isSynchronizationCompleted = true
        isSynchronizationInProgress = false
        widgetsLoadingMax = 0
        currentLoadingMessage = nil
        LoadingView.hide()
        if needsReloadAfterSynchronization {
            widgetsTableViewController?.reload()
            needsReloadAfterSynchronization = false
        }
        showWidgetsContent()
        print("[handleWidgetsLoadingCompletion] COMPLETED!!!!")
    }
    
    
    private func handleInitializationInfo(_ info: FullInicializeConnectionStruct) {
        let totalSteps = info.parametrsNum * info.subDeviceNum
        widgetsLoadingMax = totalSteps > 0 ? Float(totalSteps) : 0
    }

    private func handleWidgetsLoadingProgress(_ progress: WidgetsLoadingProgress) {
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
        LoadingView.show(state: LoadingView.State(message: message, progress: normalized))
    }
    
    private func beginSynchronization(resetState: Bool = false, state: LoadingView.State? = nil) {
        if resetState {
            isSynchronizationCompleted = false
            isSynchronizationInProgress = false
            needsReloadAfterSynchronization = false
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
        let loadingState = state ?? defaultLoadingState
        if !isSynchronizationInProgress {
            isSynchronizationInProgress = true
        }
        widgetsLoadingMax = 0
        currentLoadingMessage = loadingState.message
        LoadingView.show(state: loadingState)
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


private extension WidgetsListViewController {
    var isSynchronizationCompleted: Bool {
        get { Self.globalSynchronizationCompleted }
        set { Self.globalSynchronizationCompleted = newValue }
    }

    var isSynchronizationInProgress: Bool {
        get { Self.globalSynchronizationInProgress }
        set { Self.globalSynchronizationInProgress = newValue }
    }
}
