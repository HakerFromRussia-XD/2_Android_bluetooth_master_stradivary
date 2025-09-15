import UIKit
import ObjectiveC

final class WidgetsListTableViewController: UITableViewController {
    @IBOutlet weak var tableViewMy: UITableView!
    
    // Assistant: Добавляем enum Section и свойство dataSource для Diffable Data Source
    private enum Section {
        case main
    }
    private var dataSource: UITableViewDiffableDataSource<Section, ListItemType>!
    
    var viewModel: WidgetsListViewModel!

    var posterImagesRepository: PosterImagesRepository?

    // MARK: - Lifecycle
    override func viewDidAppear(_ animated: Bool) {
        print("[Lifecycle]  viewDidAppear")
        // отключаем переход на предыдущий экран свайпом влево
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
    }
    override func viewDidLoad() {
        print("[Lifecycle]  viewDidLoad")
        // Ensure our table view uses WidgetsListTableView without losing storyboard prototype cells
        if !(tableView is WidgetsListTableView) {
            object_setClass(tableView, WidgetsListTableView.self)
            (tableView as? WidgetsListTableView)?.configure()
        }
        super.viewDidLoad()
        setupViews()
        // Assistant: Применяем начальный снапшот данных
        applySnapshot(animatingDifferences: false)
    }
//    override func viewWillDisappear(_ animated: Bool) {
//        print("[Lifecycle]  viewWillDisappear")
//        super.viewWillDisappear(animated)
//        tableViewMy.visibleCells
//            .compactMap { $0 as? PlotViewCell }
//            .forEach { $0.stopTimer() }
//    }
    
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
        tableView.tableFooterView = nil
    }

    // MARK: - Private
    private func setupViews() {
        tableView.estimatedRowHeight = 56
        tableView.rowHeight = UITableView.automaticDimension
        
        // Register a class for SliderViewCell because it is created from code
        tableView.register(
            SliderViewCell.self,
            forCellReuseIdentifier: SliderViewCell.reuseIdentifier
        )
        
        dataSource = UITableViewDiffableDataSource<Section, ListItemType>(
            tableView: tableView
        ) { [weak self] tableView, indexPath, item in
//            guard let self = self else {return nil}
            switch item {
                case .command(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: CommandViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! CommandViewCell
                    cell.configure(with: vm)
                    return cell
                case .plot(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: PlotViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! PlotViewCell
                    cell.configure(with: vm)
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
            case .plot(let widgetVM):
                guard let cell = tableView.dequeueReusableCell(
                    withIdentifier: PlotViewCell.reuseIdentifier,
                    for: indexPath
                ) as? PlotViewCell else {
                    assertionFailure("Cannot dequeue ad cell")
                    return UITableViewCell()
                }
                return cell
            case .command(let commandVM):
                    guard let cell = tableView.dequeueReusableCell(
                        withIdentifier: CommandViewCell.reuseIdentifier,
                        for: indexPath
                    ) as? CommandViewCell else {
                        assertionFailure("Cannot dequeue ad cell")
                        return UITableViewCell()
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
