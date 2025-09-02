import Foundation
import Combine
import shared


enum WidgetsListViewModelLoading {
    case fullScreen
    case nextPage
}

protocol WidgetsListViewModelInput {
    func viewDidLoad()
    func didLoadNextPage()
    func didSearch(query: String)
    func didCancelSearch()
    func showQueriesSuggestions()
    func closeQueriesSuggestions()
    func didSelectItem(at index: Int)
    func sendBytes()
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
    
    var currentPage: Int = 0
    var totalPageCount: Int = 1
    var hasMorePages: Bool { currentPage < totalPageCount }
    var nextPage: Int { hasMorePages ? currentPage + 1 : currentPage }

    private var pages: [WidgetsPage] = []
    private var widgetsLoadTask: Cancellable? { willSet { widgetsLoadTask?.cancel() } }
    private let mainQueue: DispatchQueueType

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
    }

    // MARK: - Private

    private func appendPage(_ widgetsPage: WidgetsPage) {
        print("WidgetsPage widgets: \(widgetsPage.widgets)")
        currentPage = widgetsPage.page
        totalPageCount = widgetsPage.totalPages
        
        pages = pages
            .filter { $0.page != widgetsPage.page }
        + [widgetsPage]
        
        items.value = widgetsPage.widgets.map{ widget in
            switch widget.widgetType {
                case .commandWidget:
                    return ListItemType.slider(SliderListItemViewModel(widget: widget))
                case .sliderWidget:
                    return ListItemType.slider(SliderListItemViewModel(widget: widget))
                case .plotWidget:
                    return ListItemType.widget(WidgetsListItemViewModel(widget: widget))
                @unknown default:
                fatalError("Unknown widgetType: \(String(describing: widget.widgetType))")
            }
        }
        print("Updated items.value: \(items.value)")
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

        widgetsLoadTask = searchWidgetsUseCase.execute(
            requestValue: .init(query: widgetQuery, page: nextPage),
            cached: { [weak self] page in
                self?.mainQueue.async {
                    self?.appendPage(page)
                }
            },
            completion: { [weak self] result in
                self?.mainQueue.async {
                    switch result {
                    case .success(let page):
                        self?.appendPage(page)
                    case .failure(let error):
                        self?.handle(error: error)
                    }
                    self?.loading.value = .none
                }
            }
        )
    }

    private func handle(error: Error) {
        self.error.value = error.isInternetConnectionError ?
            NSLocalizedString("No internet connection", comment: "") :
            NSLocalizedString("Failed loading widgets", comment: "")
    }

    private func update(widgetQuery: WidgetQuery) {
        resetPages()
        load(widgetQuery: widgetQuery, loading: .fullScreen)
    }
    
    internal func sendBytes() {
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
    case widget(WidgetsListItemViewModel)
    case slider(SliderListItemViewModel)
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

// MARK: - Private

private extension Array where Element == WidgetsPage {
    var widgets: [Widget] { flatMap { $0.widgets } }
}
