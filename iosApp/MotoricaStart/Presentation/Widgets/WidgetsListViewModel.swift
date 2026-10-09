import Foundation
import Combine
import shared

enum WidgetsSynchronizationAction {
    case visibilityChanged(Bool)
    case viewLoaded(skip: Bool)
    case viewAppeared(isMobile: Bool, skip: Bool)
    case viewDisappeared
    case itemsChanged(isMobile: Bool)
    case loadingChanged(WidgetsListViewModelLoading?, isMobile: Bool)
    case initializationInfo(parameters: Int32, subDevices: Int32)
    case progress(current: Int32, total: Int32)
    case completed(isV3: Bool, isMobile: Bool)
    case begin(reset: Bool, state: LoadingView.State?, isMobile: Bool)
    case sourceChanged(isMobile: Bool)
}

enum WidgetsSynchronizationEffect {
    case renderContent, renderWidgetsContainer, renderLoading, reloadWidgets, reloadTable, resetWidgets, requestInitialization, stopObserving
}

enum WidgetsSynchronizationPresentation {
    case content(isVisible: Bool)
    case widgetsContainer(isVisible: Bool)
    case loading(LoadingView.State?)
}

struct WidgetsSynchronizationUiState {
    let isCompleted: Bool
    let isInProgress: Bool
    let isViewVisible: Bool
    let isContentVisible: Bool
    let isLoadingVisible: Bool
    let loading: LoadingView.State?
}

// The two session flags are shared by the tabs; progress and retry belong to each screen.
final class WidgetsSynchronizationState {
    static var isCompleted = false { didSet { notifySessionChanged() } }
    static var isInProgress = false { didSet { notifySessionChanged() } }
    private var needsReload = false
    private var maximum: Float = 0
    private var message: String?
    private var lastLoading: LoadingView.State?
    private var isViewVisible = false
    private var receivedProgress = false
    private var retriedWithoutProgress = false
    private var isContentVisible = false
    private var isLoadingVisible = false
    private let defaultLoading: LoadingView.State

    init(defaultLoading: LoadingView.State) { self.defaultLoading = defaultLoading }

    var uiState: WidgetsSynchronizationUiState {
        WidgetsSynchronizationUiState(isCompleted: Self.isCompleted, isInProgress: Self.isInProgress,
                                     isViewVisible: isViewVisible, isContentVisible: isContentVisible,
                                     isLoadingVisible: isLoadingVisible, loading: lastLoading)
    }

    private static func notifySessionChanged() {
        NotificationCenter.default.post(name: .widgetsSynchronizationStateDidChange, object: nil,
                                        userInfo: ["completed": isCompleted, "inProgress": isInProgress])
    }

    func onAction(_ action: WidgetsSynchronizationAction, effect: (WidgetsSynchronizationEffect) -> Void) {
        switch action {
        case .visibilityChanged(let visible): isViewVisible = visible
        case .viewLoaded(let skip):
            if skip {
                Self.isCompleted = true
                Self.isInProgress = false
                showContent(effect)
                hideLoading(effect)
            }
            if Self.isCompleted { showContent(effect) } else { hideContent(effect) }
        case .viewAppeared(let mobile, let skip):
            if mobile { hideLoading(effect); showContent(effect); return }
            if skip {
                Self.isCompleted = true
                Self.isInProgress = false
                needsReload = false
                receivedProgress = true
                retriedWithoutProgress = false
                maximum = 0
                message = nil
                lastLoading = nil
                hideLoading(effect)
                showContent(effect)
                return
            }
            if Self.isCompleted { showContent(effect) }
            else if Self.isInProgress {
                hideContent(effect)
                if let state = lastLoading { message = state.message; presentLoading(state, effect) }
                else { begin(reset: false, state: defaultLoading, isMobile: mobile, effect: effect) }
            } else {
                effect(.resetWidgets)
                begin(reset: false, state: defaultLoading, isMobile: mobile, effect: effect)
                effect(.requestInitialization)
            }
        case .viewDisappeared:
            if lastLoading != nil { hideLoading(effect) }
            if Self.isCompleted { effect(.stopObserving) }
        case .itemsChanged(let mobile):
            if Self.isCompleted || mobile { effect(.reloadTable); showContent(effect) }
            else { needsReload = true }
        case .loadingChanged(let loading, let mobile):
            if mobile { showContent(effect); return }
            switch loading {
            case .some(.fullScreen(let state)): begin(reset: false, state: state, isMobile: mobile, effect: effect)
            case .some(.nextPage):
                if Self.isCompleted { isContentVisible = true; effect(.renderWidgetsContainer) }
            case .none:
                if Self.isCompleted { showContent(effect) }
            case .some:
                if !Self.isCompleted { begin(reset: false, state: defaultLoading, isMobile: mobile, effect: effect) }
            }
        case .initializationInfo(let parameters, let subDevices):
            let totalSteps = parameters * subDevices
            maximum = totalSteps > 0 ? Float(totalSteps) : 0
        case .progress(let current, let total):
            guard !Self.isCompleted, Self.isInProgress else { return }
            receivedProgress = true
            print("[BLE-PROGRESS] total = \(Int(total)) current = \(Int(current))")
            if Int(total) > 0 { maximum = Float(Int(total)) }
            guard maximum > 0 else { return }
            let normalized = min(max(Float(Int(current)) / maximum, 0), 1)
            let loadingMessage = message ?? defaultLoading.message
            message = loadingMessage
            presentLoading(LoadingView.State(message: loadingMessage, progress: normalized), effect)
        case .completed(let isV3, let mobile):
            guard Self.isInProgress else { return }
            if isV3, Self.isInProgress, !receivedProgress {
                begin(reset: false, state: lastLoading ?? defaultLoading, isMobile: mobile, effect: effect)
                if !retriedWithoutProgress { retriedWithoutProgress = true; effect(.requestInitialization) }
                return
            }
            if isV3 { effect(.reloadWidgets) }
            Self.isCompleted = true
            Self.isInProgress = false
            maximum = 0
            receivedProgress = false
            retriedWithoutProgress = false
            message = nil
            lastLoading = nil
            hideLoading(effect)
            if needsReload { effect(.reloadTable); needsReload = false }
            showContent(effect)
            print("[handleWidgetsLoadingCompletion] COMPLETED!!!!")
        case .begin(let reset, let state, let mobile):
            begin(reset: reset, state: state, isMobile: mobile, effect: effect)
        case .sourceChanged(let mobile):
            if mobile { hideLoading(effect); needsReload = false; showContent(effect) }
            else if isViewVisible {
                if Self.isCompleted { showContent(effect) }
                else if Self.isInProgress { begin(reset: false, state: lastLoading ?? defaultLoading, isMobile: mobile, effect: effect) }
                else {
                    effect(.resetWidgets)
                    begin(reset: false, state: defaultLoading, isMobile: mobile, effect: effect)
                    effect(.requestInitialization)
                }
            }
        }
    }

    private func begin(reset: Bool, state: LoadingView.State?, isMobile: Bool, effect: (WidgetsSynchronizationEffect) -> Void) {
        if isMobile { hideLoading(effect); showContent(effect); return }
        if reset {
            Self.isCompleted = false
            Self.isInProgress = false
            needsReload = false
            maximum = 0
            receivedProgress = false
            retriedWithoutProgress = false
            lastLoading = nil
        }
        if Self.isCompleted {
            if needsReload { effect(.reloadTable); needsReload = false }
            showContent(effect)
            return
        }
        hideContent(effect)
        let loadingState = state ?? lastLoading ?? defaultLoading
        if !Self.isInProgress {
            Self.isInProgress = true
            maximum = 0
            receivedProgress = false
            retriedWithoutProgress = false
        }
        message = loadingState.message
        presentLoading(loadingState, effect)
    }

    private func presentLoading(_ state: LoadingView.State, _ effect: (WidgetsSynchronizationEffect) -> Void) {
        lastLoading = state
        guard isViewVisible else { return }
        isLoadingVisible = true
        effect(.renderLoading)
    }
    private func hideLoading(_ effect: (WidgetsSynchronizationEffect) -> Void) {
        isLoadingVisible = false
        effect(.renderLoading)
    }
    private func showContent(_ effect: (WidgetsSynchronizationEffect) -> Void) {
        isContentVisible = true
        effect(.renderContent)
    }
    private func hideContent(_ effect: (WidgetsSynchronizationEffect) -> Void) {
        isContentVisible = false
        effect(.renderContent)
    }
}

enum WidgetsListViewModelLoading: Equatable {
    case fullScreen(state: LoadingView.State)
    case nextPage
    
    static func == (lhs: WidgetsListViewModelLoading, rhs: WidgetsListViewModelLoading) -> Bool {
        switch (lhs, rhs) {
        case (.nextPage, .nextPage):
            return true
        case (.fullScreen, .fullScreen):
            return true // сравниваем только по типу, без state
        default:
            return false
        }
    }
}

protocol WidgetsListViewModelInput {
    func viewDidLoad()
    func didLoadNextPage()
    func didSearch(query: String)
    func update(with page: WidgetsPage)
    func didCancelSearch()
    func showQueriesSuggestions()
    func closeQueriesSuggestions()
    func didSelectItem(at index: Int)
    func requestInicializeInformation()
    func requestTelemetryData()
    func observeTelemetryCounters(_ callback: @escaping ([V3GestureUsage]) -> Void) -> Kotlinx_coroutines_coreJob
    func customGestureNames() -> [String]
    func observeWidgetsLoadCompletion(_ callback: @escaping () -> Void) -> Kotlinx_coroutines_coreJob
    func observeInitializationInfo(_ callback: @escaping (V3SyncInitializationInfo) -> Void) -> Kotlinx_coroutines_coreJob
    func observeWidgetsLoadingProgress(_ callback: @escaping (V3SyncLoadingProgress) -> Void) -> Kotlinx_coroutines_coreJob
    func setCustomGestureSettingsOpener(_ handler: @escaping (Int, Bool) -> Void)
    func onSynchronizationAction(_ action: WidgetsSynchronizationAction)
    func setSynchronizationEffectHandler(_ handler: @escaping (WidgetsSynchronizationEffect) -> Void)
}

protocol WidgetsListViewModelOutput {
    var synchronizationPresentation: Observable<WidgetsSynchronizationPresentation?> { get }
    var items: Observable<[ListItemType]> { get } 
    var loading: Observable<WidgetsListViewModelLoading?> { get }
    var query: Observable<String> { get }
    var error: Observable<String> { get }
    var isEmpty: Bool { get }
    var screenTitle: String { get }
    var emptyDataTitle: String { get }
    var errorTitle: String { get }
    var searchBarPlaceholder: String { get }
}

typealias WidgetsListViewModel = WidgetsListViewModelInput & WidgetsListViewModelOutput

final class DefaultWidgetsListViewModel: WidgetsListViewModel {
    
//    @Published private(set) var widgets: [WidgetsResponseDTO.WidgetDTO] = []
    @Published private(set) var widgets: [Widget] = []
    private let searchWidgetsUseCase: SearchWidgetsUseCase
    private let actions: WidgetsListViewModelActions?
    private let bleManager: BleManagerKmm
    private let makeSliderViewModel: (Widget) -> SliderListItemViewModelV3
    private let makeToggleSliderViewModel: (Widget) -> ToggleSliderListItemViewModelV3
    private let makeSpinnerViewModel: (Widget) -> SpinnerListItemViewModelV3
    private let makeCommandViewModel: (Widget) -> CommandListItemViewModelV3
    private let makePlotViewModel: (Widget) -> PlotListItemViewModelV3
    private let makeTextInputViewModel: (Widget) -> TextInputListItemViewModelV3
    private let makeGestureViewModel: (Widget, ((Int, Bool) -> Void)?) -> GestureListItemViewModel
    private let requestAccountStatisticsUseCaseV3: RequestAccountStatisticsUseCaseV3
    private let observeAccountStatisticsUseCaseV3: ObserveAccountStatisticsUseCaseV3
    private let getCustomGestureNamesUseCaseV3: GetCustomGestureNamesUseCaseV3
    private let observeSyncUseCaseV3: ObserveSyncUseCaseV3
    private let requestSyncInitializationUseCaseV3: RequestSyncInitializationUseCaseV3
    private let makeSwitcherViewModel: (Widget) -> SwitcherListItemViewModelV3
    private var customGestureSettingsOpener: ((Int, Bool) -> Void)?
    private var synchronizationEffectHandler: ((WidgetsSynchronizationEffect) -> Void)?
    private let synchronization = WidgetsSynchronizationState(defaultLoading: LoadingView.State(
        message: SharedLocalizedText.text(SharedRes.strings().synchronization_data), progress: 0
    ))
    
    var currentPage: Int = 0
    var totalPageCount: Int = 1
    var hasMorePages: Bool { currentPage < totalPageCount }
    var nextPage: Int { hasMorePages ? currentPage + 1 : currentPage }

    private var pages: [WidgetsPage] = []
    private var widgetsLoadTask: Cancellable? { willSet { widgetsLoadTask?.cancel() } }
    private let mainQueue: DispatchQueueType
    private var latestRequestID: Int = 0

    // MARK: - OUTPUT
    let synchronizationPresentation = Observable<WidgetsSynchronizationPresentation?>(nil)
    let items: Observable<[ListItemType]> = Observable([])
    let loading: Observable<WidgetsListViewModelLoading?> = Observable(.none)
    let query: Observable<String> = Observable("")
    let error: Observable<String> = Observable("")
    var isEmpty: Bool { return items.value.isEmpty }
    let screenTitle = SharedLocalizedText.text(SharedRes.strings().title_dashboard)
    let emptyDataTitle = SharedLocalizedText.text(SharedRes.strings().search_results)
    let errorTitle = SharedLocalizedText.text(SharedRes.strings().error)
    let searchBarPlaceholder = SharedLocalizedText.text(SharedRes.strings().search_widgets)

    // MARK: - Init
    init(
        searchMWidgetsUseCase: SearchWidgetsUseCase,
        bleManager: BleManagerKmm,
        makeSliderViewModel: @escaping (Widget) -> SliderListItemViewModelV3,
        makeToggleSliderViewModel: @escaping (Widget) -> ToggleSliderListItemViewModelV3,
        makeSpinnerViewModel: @escaping (Widget) -> SpinnerListItemViewModelV3,
        makeCommandViewModel: @escaping (Widget) -> CommandListItemViewModelV3,
        makePlotViewModel: @escaping (Widget) -> PlotListItemViewModelV3,
        makeTextInputViewModel: @escaping (Widget) -> TextInputListItemViewModelV3,
        makeGestureViewModel: @escaping (Widget, ((Int, Bool) -> Void)?) -> GestureListItemViewModel,
        requestAccountStatisticsUseCaseV3: RequestAccountStatisticsUseCaseV3,
        observeAccountStatisticsUseCaseV3: ObserveAccountStatisticsUseCaseV3,
        getCustomGestureNamesUseCaseV3: GetCustomGestureNamesUseCaseV3,
        observeSyncUseCaseV3: ObserveSyncUseCaseV3,
        requestSyncInitializationUseCaseV3: RequestSyncInitializationUseCaseV3,
        makeSwitcherViewModel: @escaping (Widget) -> SwitcherListItemViewModelV3,
        actions: WidgetsListViewModelActions? = nil,
        mainQueue: DispatchQueueType = DispatchQueue.main
    ) {
        self.searchWidgetsUseCase = searchMWidgetsUseCase
        self.bleManager = bleManager
        self.makeSliderViewModel = makeSliderViewModel
        self.makeToggleSliderViewModel = makeToggleSliderViewModel
        self.makeSpinnerViewModel = makeSpinnerViewModel
        self.makeCommandViewModel = makeCommandViewModel
        self.makePlotViewModel = makePlotViewModel
        self.makeTextInputViewModel = makeTextInputViewModel
        self.makeGestureViewModel = makeGestureViewModel
        self.requestAccountStatisticsUseCaseV3 = requestAccountStatisticsUseCaseV3
        self.observeAccountStatisticsUseCaseV3 = observeAccountStatisticsUseCaseV3
        self.getCustomGestureNamesUseCaseV3 = getCustomGestureNamesUseCaseV3
        self.observeSyncUseCaseV3 = observeSyncUseCaseV3
        self.requestSyncInitializationUseCaseV3 = requestSyncInitializationUseCaseV3
        self.makeSwitcherViewModel = makeSwitcherViewModel
        self.actions = actions
        self.mainQueue = mainQueue
        
        bleManager.setOnCharacteristicsReadyListener { [weak self] in
            print("[WIDGET_COORDINATOR] setOnCharacteristicsReadyListener")
            if !UiInterfaceModeBridgeV3.shared.isEnabled() {
                self?.requestInicializeInformation()
            }
        }
    }

    // MARK: - Private
    private func appendPage(_ widgetsPage: WidgetsPage) {
        print("WidgetsPage widgets: \(widgetsPage.widgets)")
        currentPage = widgetsPage.page
        totalPageCount = widgetsPage.totalPages
        
        pages = pages
            .filter { $0.page != widgetsPage.page }
        + [widgetsPage]
        
        items.value = widgetsPage.widgets.map { makeListItem(for: $0) }
        print("Updated items.value: \(items.value)")
    }

    private func makeListItem(for widget: Widget) -> ListItemType {
        if widget.isBleLogButton {
            let showBleLog = actions?.showBleLog
            return .bleLogButton(
                BleLogButtonListItemViewModel(
                    title: widget.title ?? Self.bleLogTitle,
                    onTap: {
                        showBleLog?()
                    }
                )
            )
        }

        if ProcessInfo.processInfo.arguments.contains("-ui-test-force-gestures-widget"),
           widget.id == "ui-test-gestures-widget" {
            return .gestureOpticV3(
                makeGestureViewModel(widget, customGestureSettingsOpener)
            )
        }

        let widgetCode = WidgetV3Support.widgetCode(from: widget)

        switch widgetCode {
        case WidgetV3Support.WidgetCode.buttonV3:
            return .commandV3(makeCommandViewModel(widget))
        case WidgetV3Support.WidgetCode.spinboxV3, WidgetV3Support.WidgetCode.comboboxV3:
            return .spinnerV3(makeSpinnerViewModel(widget))
        case WidgetV3Support.WidgetCode.sliderV3:
            return .sliderV3(makeSliderViewModel(widget))
        case WidgetV3Support.WidgetCode.plotV3:
            return .plotV3(makePlotViewModel(widget))
        case WidgetV3Support.WidgetCode.toggleSliderV3:
            return .toggleSliderV3(makeToggleSliderViewModel(widget))
        case WidgetV3Support.WidgetCode.textInputV3:
            return .textInputV3(makeTextInputViewModel(widget))
        case WidgetV3Support.WidgetCode.switchV3:
            return .switcherV3(makeSwitcherViewModel(widget))
        case WidgetV3Support.WidgetCode.gesturesV3:
            return .gestureOpticV3(
                makeGestureViewModel(widget, customGestureSettingsOpener)
            )
        default:
            break
        }

        switch widget.widgetType {
        case .commandWidget:
            return .command(CommandListItemViewModel(widget: widget, bleManager: bleManager))
        case .sliderWidget:
            return .slider(SliderListItemViewModel(widget: widget, bleManager: bleManager))
        case .plotWidget:
            if UiInterfaceModeBridgeV3.shared.isEnabled() {
                return .plotV3(makePlotViewModel(widget))
            }
            return .plot(PlotListItemViewModel(widget: widget, bleManager: bleManager))
        case .switchWidget:
            return .switch(SwitchListItemViewModel(widget: widget, bleManager: bleManager))
        case .gestureOpticWidget, .gestureWidget:
            return .gestureOptic(
                makeGestureViewModel(widget, customGestureSettingsOpener)
            )
        case .toggleSliderWidget:
            return .toggleSliderV3(makeToggleSliderViewModel(widget))
        case .spinnerWidget:
            return .spinnerV3(makeSpinnerViewModel(widget))
        case .textInputWidget:
            return .textInputV3(makeTextInputViewModel(widget))
        case .opticStartLearningWidget, .thresholdWidget, .none:
            return .command(CommandListItemViewModel(widget: widget, bleManager: bleManager))
        }
    }

    private func resetPages() {
        currentPage = 0
        totalPageCount = 1
        pages.removeAll()
        items.value.removeAll()
    }

    private func load(widgetQuery: WidgetQuery, loading: WidgetsListViewModelLoading) {
        self.loading.value = loading
        query.value = widgetQuery.query
        publishFullScreenProgress(0.1)
        
        latestRequestID += 1
        let requestID = latestRequestID
        print("[Lifecycle]  latestRequestID = \(latestRequestID)")
        
        widgetsLoadTask = searchWidgetsUseCase.execute(
            requestValue: .init(query: widgetQuery, page: nextPage),
            requestID: requestID,
            cached: { [weak self] id,page in
                self?.mainQueue.async {
                    guard id == self?.latestRequestID else { return }
                    self?.appendPage(page)
                    self?.publishFullScreenProgress(0.5)
                }
            },
            completion: { [weak self] id, result in
                self?.mainQueue.async {
                    guard id == self?.latestRequestID else { return }
                    switch result {
                    case .success(let page):
                        self?.appendPage(page)
                        self?.publishFullScreenProgress(1)
                    case .failure(let error):
                        self?.handle(error: error)
                    }
                    self?.loading.value = .none
                }
            }
        )
    }
    
    private func publishFullScreenProgress(_ progress: Float) {
        guard case .some(.fullScreen(state: _)) = loading.value else { return }
    }

    private func handle(error: Error) {
        self.error.value = error.isInternetConnectionError ?
            SharedLocalizedText.text(SharedRes.strings().no_internet_connection) :
            SharedLocalizedText.text(SharedRes.strings().failed_loading_widgets)
    }

    private func update(widgetQuery: WidgetQuery) {
        resetPages()
    }
    
    internal func requestInicializeInformation() {
        if UiInterfaceModeBridgeV3.shared.isEnabled() {
            print("[BLE-COMMUNICATION] restart V3 synchronization pipeline")
            requestSyncInitializationUseCaseV3.invoke()
            return
        }

        let command = BLECommands.shared.requestInicializeInformation()
        command.debugPrint()
        print("[BLE-COMMUNICATION] send:  Constants.MAIN_CHANNEL_CHARACTERISTIC = \(Constants.MAIN_CHANNEL_CHARACTERISTIC) Constants.WRITE = \(Constants.WRITE)")

        bleManager.sendBytesKmm(
            data: command,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
    }

    func requestTelemetryData() {
        print("[BLE-COMMUNICATION] request telemetry data")
        requestAccountStatisticsUseCaseV3.invoke()
    }

    func setSynchronizationEffectHandler(_ handler: @escaping (WidgetsSynchronizationEffect) -> Void) {
        synchronizationEffectHandler = handler
    }

    func onSynchronizationAction(_ action: WidgetsSynchronizationAction) {
        synchronization.onAction(action) { [self] effect in
            switch effect {
            case .renderContent:
                synchronizationPresentation.value = .content(isVisible: synchronization.uiState.isContentVisible)
            case .renderWidgetsContainer:
                synchronizationPresentation.value = .widgetsContainer(isVisible: synchronization.uiState.isContentVisible)
            case .renderLoading:
                let state = synchronization.uiState
                synchronizationPresentation.value = .loading(state.isLoadingVisible ? state.loading : nil)
            case .requestInitialization: requestInicializeInformation()
            default: synchronizationEffectHandler?(effect)
            }
        }
    }

    func observeTelemetryCounters(_ callback: @escaping ([V3GestureUsage]) -> Void) -> Kotlinx_coroutines_coreJob {
        observeAccountStatisticsUseCaseV3.observeCounters(callback: callback)
    }

    func customGestureNames() -> [String] {
        getCustomGestureNamesUseCaseV3.invoke().names
    }

    func observeWidgetsLoadCompletion(_ callback: @escaping () -> Void) -> Kotlinx_coroutines_coreJob {
        observeSyncUseCaseV3.observeWidgetsLoadCompletion(callback: callback)
    }

    func observeInitializationInfo(_ callback: @escaping (V3SyncInitializationInfo) -> Void) -> Kotlinx_coroutines_coreJob {
        observeSyncUseCaseV3.observeInitializationInfo(callback: callback)
    }

    func observeWidgetsLoadingProgress(_ callback: @escaping (V3SyncLoadingProgress) -> Void) -> Kotlinx_coroutines_coreJob {
        observeSyncUseCaseV3.observeWidgetsLoadingProgress(callback: callback)
    }
    
    func setCustomGestureSettingsOpener(_ handler: @escaping (Int, Bool) -> Void) {
        customGestureSettingsOpener = handler
    }
}

extension KotlinByteArray {
    func debugPrint() {
        var result = Data(capacity: Int(self.size))
        
        for i in 0..<self.size {
            let int8Value = self.get(index: i)
            let uint8Value = UInt8(bitPattern: int8Value)
            result.append(uint8Value)
        }
        
        let uint8Array = [UInt8](result)
        let hexString = uint8Array.map { String(format: "%02X", $0) }.joined(separator: " ")
        
        print("[BLE-COMMUNICATION] send: \(hexString)")
    }
}

enum ListItemType: Hashable { // Assistant: добавил Hashable
    case gestureUsage(GestureUsageListItemViewModel)
    case bleLogButton(BleLogButtonListItemViewModel)
    case command(CommandListItemViewModel)
    case commandV3(CommandListItemViewModelV3)
    case plot(PlotListItemViewModel)
    case plotV3(PlotListItemViewModelV3)
    case slider(SliderListItemViewModel)
    case sliderV3(SliderListItemViewModelV3)
    case `switch`(SwitchListItemViewModel)
    case gestureOptic(GestureListItemViewModel)
    case gestureOpticV3(GestureListItemViewModel)
    case spinnerV3(SpinnerListItemViewModelV3)
    case toggleSliderV3(ToggleSliderListItemViewModelV3)
    case switcherV3(SwitcherListItemViewModelV3)
    case textInputV3(TextInputListItemViewModelV3)
}

struct BleLogButtonListItemViewModel: Hashable {
    let id = "ble-log-button"
    let title: String
    let onTap: () -> Void

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(title)
    }

    static func == (lhs: BleLogButtonListItemViewModel, rhs: BleLogButtonListItemViewModel) -> Bool {
        lhs.id == rhs.id && lhs.title == rhs.title
    }
}

struct GestureUsageChartItem: Hashable {
    static func makeItems(from usage: [V3GestureUsage], customNames: [String], baseName: (Int) -> String) -> [GestureUsageChartItem] {
        usage.map { item in
            let gestureId = Int(item.gestureId)
            let title: String
            if let rawIndex = item.customGestureIndex {
                let index = Int(rawIndex.intValue)
                title = customNames.indices.contains(index)
                    ? customNames[index]
                    : "\(SharedLocalizedText.text(SharedRes.strings().custom_gesture)) \(index + 1)"
            } else {
                title = baseName(gestureId)
            }
            return GestureUsageChartItem(gestureId: gestureId, title: title,
                                         count: item.count, colorIndex: gestureId)
        }
    }

    static func localizedCustomNames(_ stored: [String]) -> [String] {
        let defaults = [
            SharedRes.strings().gesture_1_btn, SharedRes.strings().gesture_2_btn,
            SharedRes.strings().gesture_3_btn, SharedRes.strings().gesture_4_btn,
            SharedRes.strings().gesture_5_btn, SharedRes.strings().gesture_6_btn,
            SharedRes.strings().gesture_7_btn, SharedRes.strings().gesture_8_btn,
            SharedRes.strings().gesture_9_btn, SharedRes.strings().gesture_10_btn,
            SharedRes.strings().gesture_11_btn, SharedRes.strings().gesture_12_btn,
            SharedRes.strings().gesture_13_btn, SharedRes.strings().gesture_14_btn,
            SharedRes.strings().gesture_15_btn
        ].map(SharedLocalizedText.text)
        guard stored.count < defaults.count else { return Array(stored.prefix(defaults.count)) }
        return stored + Array(defaults[stored.count...])
    }

    let gestureId: Int
    let title: String
    let count: Int64
    let colorIndex: Int
}

struct GestureUsageListItemViewModel: Hashable {
    let id: String
    let title: String
    let emptyTitle: String
    let totalTitle: String
    let items: [GestureUsageChartItem]

    var totalCount: Int64 {
        items.reduce(0) { $0 + $1.count }
    }
}

private extension DefaultWidgetsListViewModel {
    static var bleLogTitle: String {
        Locale.preferredLanguages.first?.hasPrefix("ru") == true ? "Журнал BLE" : "BLE Log"
    }
}

private extension Widget {
    var isBleLogButton: Bool {
        (widget?.value as? String) == WidgetDescriptorFactoryV3.bleLogPayload
    }
}
// MARK: - INPUT. View event methods

extension DefaultWidgetsListViewModel {

    func viewDidLoad() { }

    func didLoadNextPage() {
        guard hasMorePages, loading.value == .none else { return }
        load(widgetQuery: .init(query: query.value),
             loading: .nextPage)
    }

    func didSearch(query: String) {
        guard !query.isEmpty else { return }
        update(widgetQuery: WidgetQuery(query: query))
    }

    func update(with page: WidgetsPage) {
        resetPages()
        appendPage(page)
    }

    func didCancelSearch() {
        widgetsLoadTask?.cancel()
    }

    func showQueriesSuggestions() {
        actions?.showWidgetQueriesSuggestions(update(widgetQuery:))
    }

    func closeQueriesSuggestions() {
        actions?.closeWidgetQueriesSuggestions()
    }

    func didSelectItem(at index: Int) {
        actions?.showWidgetDetails(pages.widgets[index])
    }
}


// MARK: - ParameterInfoData helpers
extension ParameterInfoData {
    static func makeSet(from parameterInfoSet: Any?) -> Set<ParameterInfoData> {
        guard let parameterInfoSet else { return [] }

        func makeData(from info: ParameterInfo<AnyObject, AnyObject, AnyObject, AnyObject>) -> ParameterInfoData? {
            guard
                let parameterID = intValue(from: info.parameterID),
                let dataCode = intValue(from: info.dataCode),
                let deviceAddress = intValue(from: info.deviceAddress),
                let dataOffset = intValue(from: info.dataOffsets)
            else {
                return nil
            }

            return ParameterInfoData(
                parameterID: parameterID,
                dataCode: dataCode,
                deviceAddress: deviceAddress,
                dataOffset: dataOffset
            )
        }

        if let swiftSet = parameterInfoSet as? Set<ParameterInfo<AnyObject, AnyObject, AnyObject, AnyObject>> {
            return Set(swiftSet.compactMap(makeData))
        }

        if let kotlinSet = parameterInfoSet as? KotlinMutableSet<AnyObject> {
            // KotlinMutableSet автоматически наследует NSSet в Swift
            let nsSet = kotlinSet as NSSet

            let mapped: [ParameterInfoData] = nsSet.compactMap { element in
                guard let info = element as? ParameterInfo<AnyObject, AnyObject, AnyObject, AnyObject> else {
                    return nil
                }
                return makeData(from: info)
            }
            return Set(mapped)
        }

        if let nsSet = parameterInfoSet as? NSSet {
            let mapped: [ParameterInfoData] = nsSet.compactMap { element in
                guard let info = element as? ParameterInfo<AnyObject, AnyObject, AnyObject, AnyObject> else {
                    return nil
                }
                return makeData(from: info)
            }
            return Set(mapped)
        }

        if let array = parameterInfoSet as? [ParameterInfo<AnyObject, AnyObject, AnyObject, AnyObject>] {
            return Set(array.compactMap(makeData))
        }

        return []
    }

    private static func intValue(from value: Any?) -> Int? {
        switch value {
        case let kotlinInt as KotlinInt:
            return Int(kotlinInt.intValue)
        case let kotlinLong as KotlinLong:
            return Int(kotlinLong.intValue)
        case let kotlinUInt as KotlinUInt:
            return Int(kotlinUInt.intValue)
        case let number as NSNumber:
            return number.intValue
        default:
            return nil
        }
    }
}


// MARK: - Private
private extension Array where Element == WidgetsPage {
    var widgets: [Widget] { flatMap { $0.widgets } }
}
