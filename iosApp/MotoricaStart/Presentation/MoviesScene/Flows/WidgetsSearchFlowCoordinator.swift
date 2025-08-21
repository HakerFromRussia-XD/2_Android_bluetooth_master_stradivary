import UIKit

protocol WidgetsSearchFlowCoordinatorDependencies  {
    func makeWidgetsListViewController(
        actions: WidgetsListViewModelActions
    ) -> WidgetsListViewController
    func makeWidgetsDetailsViewController(widget: Widget) -> UIViewController
    func makeWidgetsQueriesSuggestionsListViewController(
        didSelect: @escaping WidgetsQueryListViewModelDidSelectAction
    ) -> UIViewController
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
    
    func start() {
        // Note: here we keep strong reference with actions, this way this flow do not need to be strong referenced
        let actions = WidgetsListViewModelActions(showWidgetDetails: showWidgetDetails,
                                                 showWidgetQueriesSuggestions: showWidgetQueriesSuggestions,
                                                 closeWidgetQueriesSuggestions: closeWidgetQueriesSuggestions)
        let vc = dependencies.makeWidgetsListViewController(actions: actions)

        navigationController?.pushViewController(vc, animated: false)
        widgetsListVC = vc
    }

    private func showWidgetDetails(widget: Widget) {
        let vc = dependencies.makeWidgetsDetailsViewController(widget: widget)
        navigationController?.pushViewController(vc, animated: true)
    }

    private func showWidgetQueriesSuggestions(didSelect: @escaping (WidgetQuery) -> Void) {
        guard let widgetsListViewController = widgetsListVC, widgetsQueriesSuggestionsVC == nil,
            let container = widgetsListViewController.suggestionsListContainer else { return }

        let vc = dependencies.makeWidgetsQueriesSuggestionsListViewController(didSelect: didSelect)

        widgetsListViewController.add(child: vc, container: container)
        widgetsQueriesSuggestionsVC = vc
        container.isHidden = false
    }

    private func closeWidgetQueriesSuggestions() {
        widgetsQueriesSuggestionsVC?.remove()
        widgetsQueriesSuggestionsVC = nil
        widgetsListVC?.suggestionsListContainer.isHidden = true
    }
}
