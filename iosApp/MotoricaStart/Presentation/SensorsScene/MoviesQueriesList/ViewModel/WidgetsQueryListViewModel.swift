import Foundation

typealias WidgetsQueryListViewModelDidSelectAction = (WidgetQuery) -> Void

protocol WidgetsQueryListViewModelInput {
    func viewWillAppear()
    func didSelect(item: WidgetsQueryListItemViewModel)
}

protocol WidgetsQueryListViewModelOutput {
    var items: Observable<[WidgetsQueryListItemViewModel]> { get }
}

protocol WidgetsQueryListViewModel: WidgetsQueryListViewModelInput, WidgetsQueryListViewModelOutput { }

typealias FetchRecentWidgetQueriesUseCaseFactory = (
    FetchRecentWidgetQueriesUseCase.RequestValue,
    @escaping (FetchRecentWidgetQueriesUseCase.ResultValue) -> Void
) -> UseCase

final class DefaultWidgetsQueryListViewModel: WidgetsQueryListViewModel {

    private let numberOfQueriesToShow: Int
    private let fetchRecentWidgetQueriesUseCaseFactory: FetchRecentWidgetQueriesUseCaseFactory
    private let didSelect: WidgetsQueryListViewModelDidSelectAction?
    private let mainQueue: DispatchQueueType
    
    // MARK: - OUTPUT
    let items: Observable<[WidgetsQueryListItemViewModel]> = Observable([])
    
    init(
        numberOfQueriesToShow: Int,
        fetchRecentWidgetQueriesUseCaseFactory: @escaping FetchRecentWidgetQueriesUseCaseFactory,
        didSelect: WidgetsQueryListViewModelDidSelectAction? = nil,
        mainQueue: DispatchQueueType = DispatchQueue.main
    ) {
        self.numberOfQueriesToShow = numberOfQueriesToShow
        self.fetchRecentWidgetQueriesUseCaseFactory = fetchRecentWidgetQueriesUseCaseFactory
        self.didSelect = didSelect
        self.mainQueue = mainQueue
    }
    
    private func updateWidgetsQueries() {
        let request = FetchRecentWidgetQueriesUseCase.RequestValue(maxCount: numberOfQueriesToShow)
        let completion: (FetchRecentWidgetQueriesUseCase.ResultValue) -> Void = { [weak self] result in
            self?.mainQueue.async {
                switch result {
                case .success(let items):
                    self?.items.value = items
                        .map { $0.query }
                        .map(WidgetsQueryListItemViewModel.init)
                case .failure:
                    break
                }
            }
        }
        let useCase = fetchRecentWidgetQueriesUseCaseFactory(request, completion)
        useCase.start()
    }
}

// MARK: - INPUT. View event methods
extension DefaultWidgetsQueryListViewModel {
        
    func viewWillAppear() {
        updateWidgetsQueries()
    }
    
    func didSelect(item: WidgetsQueryListItemViewModel) {
        didSelect?(WidgetQuery(query: item.query))
    }
}
