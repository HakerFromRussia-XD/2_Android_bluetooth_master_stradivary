import UIKit
import ObjectiveC
import QuartzCore

@objc final class WidgetsListTableViewController: UITableViewController {
    @objc public var savingDeviceName: String = "...."
    private let gestureNamesStorage = CustomGestureNamesStorage.shared
    
    // Assistant: Добавляем enum Section и свойство dataSource для Diffable Data Source
    private enum Section {
        case main
    }
    private var dataSource: UITableViewDiffableDataSource<Section, ListItemType>!
    
    var viewModel: WidgetsListViewModel!

    // MARK: - Lifecycle
    override func viewDidAppear(_ animated: Bool) {
        print("[Lifecycle]  viewDidAppear")
        // отключаем переход на предыдущий экран свайпом влево
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        print("[Lifecycle]  viewDidLoad")
        // Ensure our table view uses WidgetsListTableView without losing storyboard prototype cells
        if !(tableView is WidgetsListTableView) {
            object_setClass(tableView, WidgetsListTableView.self)
            (tableView as? WidgetsListTableView)?.configure()
        }
        setupViews()
        // Assistant: Применяем начальный снапшот данных
        applySnapshot(animatingDifferences: false)
    }
    
    // Assistant: Заменяем reload() на применение снапшота, чтобы сохранять состояния ячеек
    func reload() {
        applySnapshot(animatingDifferences: false)
//        applySnapshot(animatingDifferences: hasAppliedInitialSnapshot)
    }
    
    // Assistant: Общая функция для обновления таблицы через DiffableDataSource
    private func applySnapshot(animatingDifferences: Bool) {
        print("[DEBUG] applySnapshot called")
        print("[DEBUG] items count = \(viewModel.items.value.count)")
        viewModel.items.value.forEach { print(" -> \($0)") }
        
        var snapshot = NSDiffableDataSourceSnapshot<Section, ListItemType>()
        snapshot.appendSections([.main])
        snapshot.appendItems(viewModel.items.value)
        DispatchQueue.main.async {
//            self.dataSource.apply(snapshot, animatingDifferences: animatingDifferences) {
//                CATransaction.begin()
//                CATransaction.setDisableActions(true)
//                UIView.performWithoutAnimation { [weak self] in
//                    self?.updateTableLayoutWithoutAnimation()
//                }
//
//                CATransaction.commit()
//            }

            let animationsWereEnabled = UIView.areAnimationsEnabled
            UIView.setAnimationsEnabled(false)

            UIView.performWithoutAnimation {
                self.dataSource.apply(snapshot, animatingDifferences: animatingDifferences) {
                    self.updateTableLayoutWithoutAnimation()
                    CATransaction.commit()
                    UIView.setAnimationsEnabled(animationsWereEnabled)
                }
            }
            CATransaction.commit()
            
        }
    }

    private func updateTableLayoutWithoutAnimation() {
        self.tableView.beginUpdates()
        self.tableView.endUpdates()
        self.tableView.layoutIfNeeded()
//        CATransaction.commit()
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
            guard let self = self else {return nil}
            print("[DEBUG] Dequeueing cell for \(indexPath): \(item)")
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
                    print("requestSlider  внешний configure title = \(vm.title)")
                    cell.configure(with: vm)
                    return cell

                case .switch(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: SwitchViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! SwitchViewCell
                    print("requestSwitch title = \(vm.title)")
                    cell.configure(with: vm)
                    return cell
                
                case .gestureOptic(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: GestureViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! GestureViewCell
                    print("requestGesture title = \(vm.title)")
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
    
    @objc public func setNameGesture(numberGesture: Int, name: String) {
        let index = numberGesture - 1
        gestureNamesStorage.updateName(name, at: index)
        print("Вызвана функция setNameGesture numberGesture = \(numberGesture)  name = \(name)")
    }
//    @objc public func getGestureName(numberGesture: Int) -> String { return "Жест №1"; }
    @objc public func getGestureName(numberGesture: Int) -> String {
        let index = numberGesture - 1
        let names = gestureNamesStorage.loadNames()
        guard names.indices.contains(index) else { return names.first ?? "" }
        return names[index]
    }
    @objc public func getDeviceName() -> String { var textName: String = ""; return textName }
    @objc public func getStatusConnection() -> Int { return 0; }
    @objc public func getGestureNum() -> Int { return 0; }
    @objc public func getUseFestX() -> Int { return 0; }
    @objc public func getHandSide() -> Int { return 0; }
    @objc public func getGestureTable() -> String { return "" }
    @objc public func getGestureTableBig() -> String {return "" }
    @objc public func getFingersDelay() -> String {
        var data: String = ""
        return data
    }
    @objc public func getFingersDelaySwitch() -> Int { return 0; }
    @objc public func getVersionDriverGreaterThan237() -> Bool { return true; }
    @objc public func sendDataToFest (dataForWrite: Data, characteristic: String, typeFestX: Bool) {
        print("Вызвана функция sendDataToFest  typeFestX = \(typeFestX)")
    }
    @objc func saveDataString(key: String, value: String) {
        print("save   key: \(key) value: \(value)")
    }
}
