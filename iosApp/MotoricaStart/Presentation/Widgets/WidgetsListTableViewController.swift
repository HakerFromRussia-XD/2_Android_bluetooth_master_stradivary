import UIKit
import QuartzCore

@objc final class WidgetsListTableViewController: UITableViewController {
    @objc public var savingDeviceName: String = "...."
    
    
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
        configureTableTouchBehavior()
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
        tableView.register(
            SpinnerViewCellV3.self,
            forCellReuseIdentifier: SpinnerViewCellV3.reuseIdentifier
        )
        tableView.register(
            ToggleSliderViewCellV3.self,
            forCellReuseIdentifier: ToggleSliderViewCellV3.reuseIdentifier
        )
        tableView.register(
            SwitcherViewCellV3.self,
            forCellReuseIdentifier: SwitcherViewCellV3.reuseIdentifier
        )
        tableView.register(
            TextInputViewCellV3.self,
            forCellReuseIdentifier: TextInputViewCellV3.reuseIdentifier
        )
        
        dataSource = UITableViewDiffableDataSource<Section, ListItemType>(
            tableView: tableView
        ) { [weak self] tableView, indexPath, item in
            guard self != nil else {return nil}
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
                case .spinnerV3(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: SpinnerViewCellV3.reuseIdentifier,
                        for: indexPath
                    ) as! SpinnerViewCellV3
                    cell.configure(with: vm)
                    return cell
                case .toggleSliderV3(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: ToggleSliderViewCellV3.reuseIdentifier,
                        for: indexPath
                    ) as! ToggleSliderViewCellV3
                    cell.configure(with: vm)
                    return cell
                case .switcherV3(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: SwitcherViewCellV3.reuseIdentifier,
                        for: indexPath
                    ) as! SwitcherViewCellV3
                    cell.configure(with: vm)
                    return cell
                case .textInputV3(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: TextInputViewCellV3.reuseIdentifier,
                        for: indexPath
                    ) as! TextInputViewCellV3
                    cell.configure(with: vm)
                    return cell
            }
        }
    }

    private func configureTableTouchBehavior() {
        // Keep storyboard prototype-cell lifecycle untouched.
        // We only need immediate scroll/touch behaviour adjustments.
        tableView.delaysContentTouches = false
        tableView.canCancelContentTouches = true
        if let innerScrollView = tableView.subviews.first as? UIScrollView {
            innerScrollView.delaysContentTouches = false
        }
        tableView.panGestureRecognizer.delaysTouchesBegan = false
    }
    
    // Assistant: Обрабатываем появление последней ячейки для подгрузки следующей страницы
    override func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let itemsCount = viewModel.items.value.count
        if indexPath.row == itemsCount - 1 {
            viewModel.didLoadNextPage()
        }
    }
}
