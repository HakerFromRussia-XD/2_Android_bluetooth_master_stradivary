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
    func makeWidgetsListViewController(
        actions: WidgetsListViewModelActions,
        screenTitle: String? = nil
    ) -> WidgetsListViewController {
        WidgetsListViewController.create(
            with: makeWidgetsListViewModel(actions: actions)
        )
    }
    
    func makeGesturesTabViewController(actions: WidgetsListViewModelActions) -> GesturesTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 0
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().title_home)
        return GesturesTabViewController(contentViewController: controller)
    }

    func makeSensorsTabViewController(actions: WidgetsListViewModelActions) -> SensorsTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 1
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().title_dashboard)
        return SensorsTabViewController(contentViewController: controller)
    }

    func makeTrainingTabViewController(actions: WidgetsListViewModelActions) -> TrainingTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 3
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().training)
        return TrainingTabViewController(contentViewController: controller)
    }

    func makeSpecialSettingsTabViewController(actions: WidgetsListViewModelActions) -> SpecialSettingsTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 2
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().special_settings)
        return SpecialSettingsTabViewController(contentViewController: controller)
    }

    func makeServiceSettingsTabViewController(actions: WidgetsListViewModelActions) -> ServiceSettingsTabViewController {
        let controller = makeWidgetsListViewController(actions: actions)
        controller.display = 4
        controller.screenTitleOverride = SharedLocalizedText.text(SharedRes.strings().service_settings)
        return ServiceSettingsTabViewController(contentViewController: controller)
    }
    
    func makeWidgetsListViewModel(actions: WidgetsListViewModelActions) -> WidgetsListViewModel {
        DefaultWidgetsListViewModel(
            searchMWidgetsUseCase: makeSearchWidgetsUseCase(),
            bleManager: dependencies.bleManager,
            actions: actions
        )
    }
}
