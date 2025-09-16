import UIKit

final class WidgetsQueriesTableViewController: UITableViewController, StoryboardInstantiable {
    
    private var viewModel: WidgetsQueryListViewModel!

    // MARK: - Lifecycle

    static func create(with viewModel: WidgetsQueryListViewModel) -> WidgetsQueriesTableViewController {
        let view = WidgetsQueriesTableViewController.instantiateViewController()
        view.viewModel = viewModel
        return view
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        bind(to: viewModel)
    }
    
    private func bind(to viewModel: WidgetsQueryListViewModel) {
        viewModel.items.observe(on: self) { [weak self] _ in self?.tableView.reloadData() }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        viewModel.viewWillAppear()
    }

    // MARK: - Private

    private func setupViews() {
        tableView.tableFooterView = UIView()
        tableView.backgroundColor = .clear
        tableView.estimatedRowHeight = WidgetsQueriesItemCell.height
        tableView.rowHeight = UITableView.automaticDimension
    }
}
