import UIKit
import SwiftUI

final class WidgetsSceneDIContainer: WidgetsSearchFlowCoordinatorDependencies {
    
    struct Dependencies {
        let apiDataTransferService: DataTransferService
        let imageDataTransferService: DataTransferService
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
    func makePosterImagesRepository() -> PosterImagesRepository {
        DefaultPosterImagesRepository(
            dataTransferService: dependencies.imageDataTransferService
        )
    }
    
    // MARK: - Widgets List
    func makeWidgetsListViewController(actions: WidgetsListViewModelActions) -> WidgetsListViewController {
        WidgetsListViewController.create(
            with: makeWidgetsListViewModel(actions: actions),
            posterImagesRepository: makePosterImagesRepository()
        )
    }
    
    func makeWidgetsListViewModel(actions: WidgetsListViewModelActions) -> WidgetsListViewModel {
        DefaultWidgetsListViewModel(
            searchMWidgetsUseCase: makeSearchWidgetsUseCase(),
            actions: actions
        )
    }
    
    // MARK: - Widget Details
    func makeWidgetsDetailsViewController(widget: Widget) -> UIViewController {
            WidgetDetailsViewController.create(
            with: makeWidgetsDetailsViewModel(widget: widget)
        )
    }
    
    func makeWidgetsDetailsViewModel(widget: Widget) -> WidgetDetailsViewModel {
        DefaultWidgetDetailsViewModel(
            widget: widget,
            posterImagesRepository: makePosterImagesRepository()
        )
    }
    
    // MARK: - Widgets Queries Suggestions List
    func makeWidgetsQueriesSuggestionsListViewController(didSelect: @escaping WidgetsQueryListViewModelDidSelectAction) -> UIViewController {
        if #available(iOS 13.0, *) { // SwiftUI
            let view = WidgetsQueryListView(
                viewModelWrapper: makeWidgetsQueryListViewModelWrapper(didSelect: didSelect)
            )
            return UIHostingController(rootView: view)
        } else { // UIKit
            return WidgetsQueriesTableViewController.create(
                with: makeWidgetsQueryListViewModel(didSelect: didSelect)
            )
        }
    }
    
    func makeWidgetsQueryListViewModel(didSelect: @escaping WidgetsQueryListViewModelDidSelectAction) -> WidgetsQueryListViewModel {
        DefaultWidgetsQueryListViewModel(
            numberOfQueriesToShow: 10,
            fetchRecentWidgetQueriesUseCaseFactory: makeFetchRecentWidgetQueriesUseCase,
            didSelect: didSelect
        )
    }

    @available(iOS 13.0, *)
    func makeWidgetsQueryListViewModelWrapper(
        didSelect: @escaping WidgetsQueryListViewModelDidSelectAction
    ) -> WidgetsQueryListViewModelWrapper {
        WidgetsQueryListViewModelWrapper(
            viewModel: makeWidgetsQueryListViewModel(didSelect: didSelect)
        )
    }

    // MARK: - Flow Coordinators
    func makeWidgetsSearchFlowCoordinator(navigationController: UINavigationController) -> WidgetsSearchFlowCoordinator {
        WidgetsSearchFlowCoordinator(
            navigationController: navigationController,
            dependencies: self
        )
    }
}
