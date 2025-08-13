import UIKit

final class WidgetsListTableViewController: UITableViewController {

    // Assistant: Добавляем enum Section и свойство dataSource для Diffable Data Source
    private enum Section {
        case main
    }
    private var dataSource: UITableViewDiffableDataSource<Section, ListItemType>!
    
    var viewModel: WidgetsListViewModel!

    var posterImagesRepository: PosterImagesRepository?
    var nextPageLoadingSpinner: UIActivityIndicatorView?

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        // Assistant: Применяем начальный снапшот данных
        applySnapshot(animatingDifferences: false)
        // отключаем переход на предыдущий экран свайпом влево
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
    }
    
    // Assistant: Заменяем reload() на применение снапшота, чтобы сохранять состояния ячеек
    func reload() {
        applySnapshot(animatingDifferences: false)
    }

    
    // Assistant: Общая функция для обновления таблицы через DiffableDataSource
    private func applySnapshot(animatingDifferences: Bool) {
        var snapshot = NSDiffableDataSourceSnapshot<Section, ListItemType>()
        snapshot.appendSections([.main])
        snapshot.appendItems(viewModel.items.value)
        dataSource.apply(snapshot, animatingDifferences: animatingDifferences)
    }


    func updateLoading(_ loading: WidgetsListViewModelLoading?) {
        switch loading {
        case .nextPage:
            nextPageLoadingSpinner?.removeFromSuperview()
            nextPageLoadingSpinner = makeActivityIndicator(size: .init(width: tableView.frame.width, height: 44))
            tableView.tableFooterView = nextPageLoadingSpinner
        case .fullScreen, .none:
            tableView.tableFooterView = nil
        }
    }

    // MARK: - Private
    private func setupViews() {
        tableView.estimatedRowHeight = WidgetsListItemCell.height
        tableView.rowHeight = UITableView.automaticDimension
        
        // Register a class for SliderViewCell because it is created from code
        tableView.register(
            SliderViewCell.self,
            forCellReuseIdentifier: SliderViewCell.reuseIdentifier
        )
        
        dataSource = UITableViewDiffableDataSource<Section, ListItemType>(
            tableView: tableView
        ) { [weak self] tableView, indexPath, item in
            guard let self = self else {return nil}
            switch item {
            case .widget(let vm):
                let cell = tableView.dequeueReusableCell(
                    withIdentifier: WidgetsListItemCell.reuseIdentifier,
                    for: indexPath
                ) as! WidgetsListItemCell
                cell.fill(with: vm, posterImagesRepository: self.posterImagesRepository)
                // подгрузка следующей страницы
                if indexPath.row == (self.viewModel.items.value.count) - 1 {
                    self.viewModel.didLoadNextPage()
                }
                return cell

            case .slider(let vm):
                let cell = tableView.dequeueReusableCell(
                    withIdentifier: SliderViewCell.reuseIdentifier,
                    for: indexPath
                ) as! SliderViewCell
                cell.configure(with: vm)
                return cell
            }
        }
    }
    
    // Assistant: Обрабатываем появление последней ячейки для подгрузки следующей страницы
    override func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let itemsCount = viewModel.items.value.count
        if indexPath.row == itemsCount - 1 {
            viewModel.didLoadNextPage()
        }
    }
}

// MARK: - UITableViewDataSource, UITableViewDelegate
extension WidgetsListTableViewController {

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel.items.value.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let item = viewModel.items.value[indexPath.row]
        print("Item at \(indexPath.row): \(item)")
        
        switch item {
            case .widget(let widgetVM):
                guard let cell = tableView.dequeueReusableCell(
                    withIdentifier: WidgetsListItemCell.reuseIdentifier,
                    for: indexPath
                ) as? WidgetsListItemCell else {
                    assertionFailure("Cannot dequeue reusable cell \(WidgetsListItemCell.self) with reuseIdentifier: \(WidgetsListItemCell.reuseIdentifier)")
                    return UITableViewCell()
                }
                
                cell.fill(with: widgetVM, posterImagesRepository: posterImagesRepository)
                
                if indexPath.row == viewModel.items.value.count - 1 {
                    viewModel.didLoadNextPage()
                }
                
                return cell
                
        case .slider(_):
                guard let cell = tableView.dequeueReusableCell(
                    withIdentifier: SliderViewCell.reuseIdentifier,
                    for: indexPath
                ) as? SliderViewCell else {
                    assertionFailure("Cannot dequeue ad cell")
                    return UITableViewCell()
                }
//                cell.configure(with: adVM)
                return cell
        }
    }

    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return viewModel.isEmpty ? tableView.frame.height : super.tableView(tableView, heightForRowAt: indexPath)
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        viewModel.didSelectItem(at: indexPath.row)
    }
}
