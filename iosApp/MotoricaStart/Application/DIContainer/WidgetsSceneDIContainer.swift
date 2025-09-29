import UIKit
import SwiftUI
import shared

final class WidgetsSceneDIContainer {
    struct Dependencies {
        let apiDataTransferService: DataTransferService
        let imageDataTransferService: DataTransferService
        let bleManager: BleManagerKmm
    }
    
    private let dependencies: Dependencies

    // MARK: - Persistent Storage
    lazy var widgetsQueriesStorage: WidgetsQueriesStorage = CoreDataWidgetsQueriesStorage(maxStorageLimit: 10)
    lazy var widgetsResponseCache: WidgetsResponseStorage = CoreDataWidgetsResponseStorage()


    init(dependencies: Dependencies) {
        self.dependencies = dependencies        
    }
    
    // MARK: - Use Cases
    func makeSearchWidgetsUseCase() -> SearchWidgetsUseCase {
        DefaultSearchWidgetsUseCase(
            widgetsRepository: makeWidgetsRepository(),
            widgetsQueriesRepository: makeWidgetsQueriesRepository()
        )
    }
    
    func makeFetchRecentWidgetQueriesUseCase(
        requestValue: FetchRecentWidgetQueriesUseCase.RequestValue,
        completion: @escaping (FetchRecentWidgetQueriesUseCase.ResultValue) -> Void
    ) -> UseCase {
        FetchRecentWidgetQueriesUseCase(
            requestValue: requestValue,
            completion: completion,
            widgetsQueriesRepository: makeWidgetsQueriesRepository()
        )
    }
    
    // MARK: - Repositories
    func makeWidgetsRepository() -> WidgetsRepository {
        DefaultWidgetsRepository(
            dataTransferService: dependencies.apiDataTransferService,
            cache: widgetsResponseCache
        )
    }
    
    func makeWidgetsQueriesRepository() -> WidgetsQueriesRepository {
        DefaultWidgetsQueriesRepository(
            widgetsQueriesPersistentStorage: widgetsQueriesStorage
        )
    }
    
    // MARK: - Widgets List
    func makeWidgetsListViewController(actions: WidgetsListViewModelActions) -> WidgetsListViewController {
        WidgetsListViewController.create(
            with: makeWidgetsListViewModel(actions: actions)
        )
    }
    
    func makeWidgetsListViewModel(actions: WidgetsListViewModelActions) -> WidgetsListViewModel {
        DefaultWidgetsListViewModel(
            searchMWidgetsUseCase: makeSearchWidgetsUseCase(),
            bleManager: dependencies.bleManager,
            actions: actions
        )
    }
}
