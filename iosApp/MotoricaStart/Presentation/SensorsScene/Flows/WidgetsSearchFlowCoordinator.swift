import UIKit

protocol WidgetsSearchFlowCoordinatorDependencies  {
    func makeWidgetsListViewController(
        actions: WidgetsListViewModelActions
    ) -> WidgetsListViewController
}

final class WidgetsSearchFlowCoordinator {
    
    private weak var navigationController: UINavigationController?
    private let dependencies: WidgetsSearchFlowCoordinatorDependencies

    private weak var widgetsListVC: WidgetsListViewController?
    private weak var widgetsQueriesSuggestionsVC: UIViewController?

    init(navigationController: UINavigationController,
         dependencies: WidgetsSearchFlowCoordinatorDependencies) {
        self.navigationController = navigationController
        self.dependencies = dependencies
    }
    
    func start() {}

    private func closeWidgetQueriesSuggestions() {
        widgetsQueriesSuggestionsVC?.remove()
        widgetsQueriesSuggestionsVC = nil
        widgetsListVC?.suggestionsListContainer.isHidden = true
    }
}
