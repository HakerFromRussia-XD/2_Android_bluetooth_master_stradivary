import Foundation
import Combine
import shared


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
    func setCustomGestureSettingsOpener(_ handler: @escaping (Int) -> Void)
}

protocol WidgetsListViewModelOutput {
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
    private var customGestureSettingsOpener: ((Int) -> Void)?
    
    var currentPage: Int = 0
    var totalPageCount: Int = 1
    var hasMorePages: Bool { currentPage < totalPageCount }
    var nextPage: Int { hasMorePages ? currentPage + 1 : currentPage }

    private var pages: [WidgetsPage] = []
    private var widgetsLoadTask: Cancellable? { willSet { widgetsLoadTask?.cancel() } }
    private let mainQueue: DispatchQueueType
    private var latestRequestID: Int = 0

    // MARK: - OUTPUT
    let items: Observable<[ListItemType]> = Observable([])
    let loading: Observable<WidgetsListViewModelLoading?> = Observable(.none)
    let query: Observable<String> = Observable("")
    let error: Observable<String> = Observable("")
    var isEmpty: Bool { return items.value.isEmpty }
    let screenTitle = NSLocalizedString("Sensors", comment: "")
    let emptyDataTitle = NSLocalizedString("Search results", comment: "")
    let errorTitle = NSLocalizedString("Error", comment: "")
    let searchBarPlaceholder = NSLocalizedString("Search Widgets", comment: "")

    // MARK: - Init
    init(
        searchMWidgetsUseCase: SearchWidgetsUseCase,
        bleManager: BleManagerKmm,
        actions: WidgetsListViewModelActions? = nil,
        mainQueue: DispatchQueueType = DispatchQueue.main
    ) {
        self.searchWidgetsUseCase = searchMWidgetsUseCase
        self.bleManager = bleManager
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
        let widgetCode = WidgetV3Support.widgetCode(from: widget)

        switch widgetCode {
        case WidgetV3Support.WidgetCode.buttonV3:
            return .command(CommandListItemViewModel(widget: widget, bleManager: bleManager))
        case WidgetV3Support.WidgetCode.spinboxV3, WidgetV3Support.WidgetCode.comboboxV3:
            return .spinnerV3(SpinnerListItemViewModelV3(widget: widget, bleManager: bleManager))
        case WidgetV3Support.WidgetCode.sliderV3:
            return .slider(SliderListItemViewModel(widget: widget, bleManager: bleManager))
        case WidgetV3Support.WidgetCode.plotV3:
            return .plot(PlotListItemViewModel(widget: widget, bleManager: bleManager))
        case WidgetV3Support.WidgetCode.toggleSliderV3:
            return .toggleSliderV3(ToggleSliderListItemViewModelV3(widget: widget, bleManager: bleManager))
        case WidgetV3Support.WidgetCode.textInputV3:
            return .textInputV3(TextInputListItemViewModelV3(widget: widget, bleManager: bleManager))
        case WidgetV3Support.WidgetCode.switchV3:
            return .switcherV3(SwitcherListItemViewModelV3(widget: widget, bleManager: bleManager))
        case WidgetV3Support.WidgetCode.gesturesV3:
            return .gestureOptic(
                GestureListItemViewModel(
                    widget: widget,
                    bleManager: bleManager,
                    openCustomGestureSettings: customGestureSettingsOpener
                )
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
            return .plot(PlotListItemViewModel(widget: widget, bleManager: bleManager))
        case .switchWidget:
            return .switch(SwitchListItemViewModel(widget: widget, bleManager: bleManager))
        case .gestureOpticWidget, .gestureWidget:
            return .gestureOptic(
                GestureListItemViewModel(
                    widget: widget,
                    bleManager: bleManager,
                    openCustomGestureSettings: customGestureSettingsOpener
                )
            )
        case .toggleSliderWidget:
            return .toggleSliderV3(ToggleSliderListItemViewModelV3(widget: widget, bleManager: bleManager))
        case .spinnerWidget:
            return .spinnerV3(SpinnerListItemViewModelV3(widget: widget, bleManager: bleManager))
        case .textInputWidget:
            return .textInputV3(TextInputListItemViewModelV3(widget: widget, bleManager: bleManager))
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
            NSLocalizedString("No internet connection", comment: "") :
            NSLocalizedString("Failed loading widgets", comment: "")
    }

    private func update(widgetQuery: WidgetQuery) {
        resetPages()
    }
    
    internal func requestInicializeInformation() {
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
    
    func setCustomGestureSettingsOpener(_ handler: @escaping (Int) -> Void) {
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
    case command(CommandListItemViewModel)
    case plot(PlotListItemViewModel)
    case slider(SliderListItemViewModel)
    case `switch`(SwitchListItemViewModel)
    case gestureOptic(GestureListItemViewModel)
    case spinnerV3(SpinnerListItemViewModelV3)
    case toggleSliderV3(ToggleSliderListItemViewModelV3)
    case switcherV3(SwitcherListItemViewModelV3)
    case textInputV3(TextInputListItemViewModelV3)
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
