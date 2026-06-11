import UIKit
import DGCharts
import shared

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
        tableView.accessibilityIdentifier = AccessibilityIdentifier.widgetsTable
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
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }

            self.dataSource.apply(snapshot, animatingDifferences: animatingDifferences) {
                UIView.performWithoutAnimation {
                    self.updateTableLayoutWithoutAnimation()
                }
            }
        }
    }

    private func updateTableLayoutWithoutAnimation() {
        self.tableView.beginUpdates()
        self.tableView.endUpdates()
        self.tableView.layoutIfNeeded()
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
            SliderViewCellV3.self,
            forCellReuseIdentifier: SliderViewCellV3.reuseIdentifier
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
        tableView.register(
            GestureViewCellV3.self,
            forCellReuseIdentifier: GestureViewCellV3.reuseIdentifier
        )
        tableView.register(
            GestureUsageChartViewCell.self,
            forCellReuseIdentifier: GestureUsageChartViewCell.reuseIdentifier
        )
        tableView.register(
            BleLogButtonViewCell.self,
            forCellReuseIdentifier: BleLogButtonViewCell.reuseIdentifier
        )
        
        dataSource = UITableViewDiffableDataSource<Section, ListItemType>(
            tableView: tableView
        ) { [weak self] tableView, indexPath, item in
            guard self != nil else {return nil}
            print("[DEBUG] Dequeueing cell for \(indexPath): \(item)")
            switch item {
                case .bleLogButton(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: BleLogButtonViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! BleLogButtonViewCell
                    cell.configure(with: vm)
                    return cell
                case .gestureUsage(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: GestureUsageChartViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! GestureUsageChartViewCell
                    cell.configure(with: vm)
                    return cell
                case .command(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: CommandViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! CommandViewCell
                
                    cell.configure(with: vm)
                    return cell
                case .commandV3(let vm):
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
                    ) as! PlotViewCellV3
                    cell.configure(with: vm)
                    return cell
                case .plotV3(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: PlotViewCell.reuseIdentifier,
                        for: indexPath
                    ) as! PlotViewCellV3
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
                case .sliderV3(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: SliderViewCellV3.reuseIdentifier,
                        for: indexPath
                    ) as! SliderViewCellV3
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
                case .gestureOpticV3(let vm):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: GestureViewCellV3.reuseIdentifier,
                        for: indexPath
                    ) as! GestureViewCellV3
                    print("requestGestureV3 title = \(vm.title)")
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

final class BleLogButtonViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: BleLogButtonViewCell.self)

    private let button = UIControl()
    private let iconView = UIImageView(
        image: (UIImage(named: "ic_note") ?? UIImage(systemName: "doc.text"))?.withRenderingMode(.alwaysTemplate)
    )
    private let titleLabel = UILabel()
    private let chevronView = UIImageView(
        image: (UIImage(named: "ic_navigate_next") ?? UIImage(systemName: "chevron.right"))?.withRenderingMode(.alwaysTemplate)
    )
    private var onTap: (() -> Void)?

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupViews()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupViews()
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        onTap = nil
        titleLabel.text = nil
    }

    func configure(with viewModel: BleLogButtonListItemViewModel) {
        titleLabel.text = viewModel.title
        onTap = viewModel.onTap
    }

    private func setupViews() {
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        contentView.backgroundColor = UIColor(named: "ubi4_back")
        contentView.directionalLayoutMargins = .zero

        button.translatesAutoresizingMaskIntoConstraints = false
        button.backgroundColor = UIColor(named: "ubi4_gray") ?? UIColor(red: 0.216, green: 0.216, blue: 0.216, alpha: 1)
        button.layer.cornerRadius = 12
        button.layer.borderWidth = 1
        button.layer.borderColor = (UIColor(named: "ubi4_gray_border") ?? UIColor(red: 0.267, green: 0.267, blue: 0.267, alpha: 1)).cgColor
        button.addTarget(self, action: #selector(handleTap), for: .touchUpInside)
        contentView.addSubview(button)

        iconView.translatesAutoresizingMaskIntoConstraints = false
        iconView.contentMode = .scaleAspectFit
        iconView.tintColor = UIColor(named: "ubi4_white") ?? .white
        button.addSubview(iconView)

        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.font = UIFont(name: "OpenSans-Regular", size: 14) ?? .systemFont(ofSize: 14, weight: .regular)
        titleLabel.textColor = UIColor(named: "ubi4_white") ?? .white
        titleLabel.numberOfLines = 1
        button.addSubview(titleLabel)

        chevronView.translatesAutoresizingMaskIntoConstraints = false
        chevronView.contentMode = .scaleAspectFit
        chevronView.tintColor = UIColor(named: "ubi4_white") ?? .white
        button.addSubview(chevronView)

        NSLayoutConstraint.activate([
            button.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            button.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            button.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            button.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -4),
            button.heightAnchor.constraint(equalToConstant: 48),

            iconView.leadingAnchor.constraint(equalTo: button.leadingAnchor, constant: 16),
            iconView.centerYAnchor.constraint(equalTo: button.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 24),
            iconView.heightAnchor.constraint(equalToConstant: 24),

            chevronView.trailingAnchor.constraint(equalTo: button.trailingAnchor, constant: -16),
            chevronView.centerYAnchor.constraint(equalTo: button.centerYAnchor),
            chevronView.widthAnchor.constraint(equalToConstant: 24),
            chevronView.heightAnchor.constraint(equalToConstant: 24),

            titleLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 12),
            titleLabel.trailingAnchor.constraint(equalTo: chevronView.leadingAnchor, constant: -12),
            titleLabel.centerYAnchor.constraint(equalTo: button.centerYAnchor)
        ])
    }

    @objc private func handleTap() {
        onTap?()
    }
}

final class GestureUsageChartViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: GestureUsageChartViewCell.self)

    private let containerView = UIView()
    private let titleLabel = UILabel()
    private let chartContainerView = UIView()
    private let chartView = GestureUsageDonutChartView()
    private let legendStackView = UIStackView()
    private let emptyLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupViews()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupViews()
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        legendStackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
        chartView.configure(items: [], totalTitle: "")
        emptyLabel.isHidden = true
    }

    func configure(with viewModel: GestureUsageListItemViewModel) {
        titleLabel.text = viewModel.title
        chartView.configure(items: viewModel.items, totalTitle: viewModel.totalTitle)
        chartView.isHidden = viewModel.items.isEmpty
        emptyLabel.text = viewModel.emptyTitle
        emptyLabel.isHidden = !viewModel.items.isEmpty
        legendStackView.isHidden = viewModel.items.isEmpty

        legendStackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
        viewModel.items.forEach {
            legendStackView.addArrangedSubview(GestureUsageLegendRowView(item: $0))
        }
    }

    private func setupViews() {
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        contentView.backgroundColor = UIColor(named: "ubi4_back")
        contentView.directionalLayoutMargins = .zero

        containerView.translatesAutoresizingMaskIntoConstraints = false
        containerView.accessibilityIdentifier = AccessibilityIdentifier.widgetsGestureUsageChart
        containerView.backgroundColor = .gestureUsageCardBackground
        containerView.layer.cornerRadius = 12
        containerView.layer.borderWidth = 1
        containerView.layer.borderColor = UIColor.gestureUsageDivider.cgColor
        containerView.layer.masksToBounds = true
        contentView.addSubview(containerView)

        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.font = .systemFont(ofSize: 12, weight: .light)
        titleLabel.textColor = .gestureUsagePrimaryText
        titleLabel.numberOfLines = 0

        chartContainerView.translatesAutoresizingMaskIntoConstraints = false

        chartView.translatesAutoresizingMaskIntoConstraints = false

        legendStackView.axis = .vertical
        legendStackView.alignment = .fill
        legendStackView.distribution = .fill
        legendStackView.spacing = 0
        legendStackView.backgroundColor = .gestureUsageCardBackground
        legendStackView.translatesAutoresizingMaskIntoConstraints = false

        emptyLabel.translatesAutoresizingMaskIntoConstraints = false
        emptyLabel.font = .systemFont(ofSize: 12, weight: .light)
        emptyLabel.textColor = .gestureUsageSecondaryText
        emptyLabel.textAlignment = .center
        emptyLabel.numberOfLines = 0

        containerView.addSubview(titleLabel)
        containerView.addSubview(chartContainerView)
        containerView.addSubview(legendStackView)
        chartContainerView.addSubview(chartView)
        chartContainerView.addSubview(emptyLabel)

        NSLayoutConstraint.activate([
            containerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            containerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            containerView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            containerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -4),

            titleLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 10),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: containerView.trailingAnchor, constant: -10),
            titleLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 8),

            chartContainerView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 10),
            chartContainerView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -10),
            chartContainerView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 6),
            chartContainerView.heightAnchor.constraint(equalToConstant: 220),

            chartView.leadingAnchor.constraint(equalTo: chartContainerView.leadingAnchor),
            chartView.trailingAnchor.constraint(equalTo: chartContainerView.trailingAnchor),
            chartView.topAnchor.constraint(equalTo: chartContainerView.topAnchor),
            chartView.bottomAnchor.constraint(equalTo: chartContainerView.bottomAnchor),

            emptyLabel.centerXAnchor.constraint(equalTo: chartContainerView.centerXAnchor),
            emptyLabel.centerYAnchor.constraint(equalTo: chartContainerView.centerYAnchor),
            emptyLabel.leadingAnchor.constraint(greaterThanOrEqualTo: chartContainerView.leadingAnchor, constant: 8),
            emptyLabel.trailingAnchor.constraint(lessThanOrEqualTo: chartContainerView.trailingAnchor, constant: -8),

            legendStackView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 10),
            legendStackView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -10),
            legendStackView.topAnchor.constraint(equalTo: chartContainerView.bottomAnchor, constant: 4),
            legendStackView.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -10)
        ])
    }
}

private final class GestureUsageDonutChartView: UIView {
    private let pieChartView = PieChartView()
    private let totalLabel = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupViews()
    }

    func configure(items: [GestureUsageChartItem], totalTitle: String) {
        let visibleItems = items.filter { $0.count > 0 }
        let totalCount = visibleItems.reduce(Int64(0)) { $0 + $1.count }
        let total = Self.numberFormatter.string(from: NSNumber(value: totalCount)) ?? "\(totalCount)"
        totalLabel.text = "\(totalTitle)\n\(total)"

        guard !visibleItems.isEmpty else {
            pieChartView.clear()
            pieChartView.data = nil
            totalLabel.isHidden = true
            return
        }

        totalLabel.isHidden = false
        let entries = visibleItems.map {
            PieChartDataEntry(value: Double($0.count), label: $0.title)
        }
        let dataSet = PieChartDataSet(entries: entries, label: "")
        dataSet.colors = visibleItems.map { UIColor.gestureUsageColor(index: $0.colorIndex) }
        dataSet.drawIconsEnabled = false
        dataSet.drawValuesEnabled = true
        dataSet.sliceSpace = 1
        dataSet.selectionShift = 0
        dataSet.valueLineColor = .clear
        dataSet.valueTextColor = .white
        dataSet.valueFont = .systemFont(ofSize: 11, weight: .regular)
        dataSet.valueFormatter = GestureUsagePercentValueFormatter()

        let data = PieChartData(dataSet: dataSet)
        data.setDrawValues(true)
        pieChartView.data = data
        pieChartView.notifyDataSetChanged()
    }

    private func setupViews() {
        backgroundColor = .clear
        isOpaque = false

        pieChartView.translatesAutoresizingMaskIntoConstraints = false
        pieChartView.backgroundColor = .clear
        pieChartView.usePercentValuesEnabled = true
        pieChartView.drawEntryLabelsEnabled = false
        pieChartView.drawHoleEnabled = true
        pieChartView.drawSlicesUnderHoleEnabled = false
        pieChartView.holeRadiusPercent = 0.54
        pieChartView.transparentCircleRadiusPercent = 0.58
        pieChartView.transparentCircleColor = .clear
        pieChartView.holeColor = .clear
        pieChartView.drawCenterTextEnabled = false
        pieChartView.rotationEnabled = false
        pieChartView.dragDecelerationEnabled = false
        pieChartView.highlightPerTapEnabled = false
        pieChartView.isUserInteractionEnabled = false
        pieChartView.legend.enabled = false
        pieChartView.chartDescription.enabled = false
        pieChartView.noDataText = ""
        pieChartView.setExtraOffsets(left: 0, top: 0, right: 0, bottom: 0)
        addSubview(pieChartView)

        totalLabel.font = .systemFont(ofSize: 12, weight: .light)
        totalLabel.textColor = .gestureUsagePrimaryText
        totalLabel.textAlignment = .center
        totalLabel.numberOfLines = 2
        totalLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(totalLabel)

        NSLayoutConstraint.activate([
            pieChartView.leadingAnchor.constraint(equalTo: leadingAnchor),
            pieChartView.trailingAnchor.constraint(equalTo: trailingAnchor),
            pieChartView.topAnchor.constraint(equalTo: topAnchor),
            pieChartView.bottomAnchor.constraint(equalTo: bottomAnchor),

            totalLabel.centerXAnchor.constraint(equalTo: centerXAnchor),
            totalLabel.centerYAnchor.constraint(equalTo: centerYAnchor),
            totalLabel.widthAnchor.constraint(equalToConstant: 96)
        ])
    }

    private static let numberFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return formatter
    }()
}

private final class GestureUsagePercentValueFormatter: NSObject, ValueFormatter {
    func stringForValue(
        _ value: Double,
        entry: ChartDataEntry,
        dataSetIndex: Int,
        viewPortHandler: ViewPortHandler?
    ) -> String {
        "\(Int(value.rounded()))%"
    }
}

private final class GestureUsageLegendRowView: UIView {
    private let item: GestureUsageChartItem

    override var intrinsicContentSize: CGSize {
        CGSize(width: UIView.noIntrinsicMetric, height: 49)
    }

    init(item: GestureUsageChartItem) {
        self.item = item
        super.init(frame: .zero)
        setupViews()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupViews() {
        backgroundColor = .gestureUsageCardBackground

        let dividerView = UIView()
        dividerView.translatesAutoresizingMaskIntoConstraints = false
        dividerView.backgroundColor = .gestureUsageDivider

        let colorView = UIView()
        colorView.translatesAutoresizingMaskIntoConstraints = false
        colorView.backgroundColor = UIColor.gestureUsageColor(index: item.colorIndex)
        colorView.layer.cornerRadius = 6

        let titleLabel = UILabel()
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.font = .systemFont(ofSize: 12, weight: .light)
        titleLabel.textColor = .gestureUsagePrimaryText
        titleLabel.numberOfLines = 2
        titleLabel.lineBreakMode = .byTruncatingTail
        titleLabel.text = item.title

        let countLabel = UILabel()
        countLabel.translatesAutoresizingMaskIntoConstraints = false
        countLabel.font = .systemFont(ofSize: 12, weight: .light)
        countLabel.textColor = .gestureUsagePrimaryText
        countLabel.textAlignment = .right
        countLabel.text = Self.numberFormatter.string(from: NSNumber(value: item.count)) ?? "\(item.count)"
        countLabel.setContentHuggingPriority(.required, for: .horizontal)
        countLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

        addSubview(dividerView)
        addSubview(colorView)
        addSubview(titleLabel)
        addSubview(countLabel)

        NSLayoutConstraint.activate([
            dividerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            dividerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            dividerView.topAnchor.constraint(equalTo: topAnchor),
            dividerView.heightAnchor.constraint(equalToConstant: 1),

            colorView.widthAnchor.constraint(equalToConstant: 12),
            colorView.heightAnchor.constraint(equalToConstant: 12),
            colorView.leadingAnchor.constraint(equalTo: leadingAnchor),
            colorView.centerYAnchor.constraint(equalTo: dividerView.bottomAnchor, constant: 24),

            titleLabel.leadingAnchor.constraint(equalTo: colorView.trailingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(equalTo: countLabel.leadingAnchor, constant: -12),
            titleLabel.centerYAnchor.constraint(equalTo: colorView.centerYAnchor),

            countLabel.trailingAnchor.constraint(equalTo: trailingAnchor),
            countLabel.centerYAnchor.constraint(equalTo: colorView.centerYAnchor),
            countLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 34),

            bottomAnchor.constraint(equalTo: dividerView.bottomAnchor, constant: 48)
        ])
    }

    private static let numberFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return formatter
    }()
}

private extension UIColor {
    static var gestureUsageCardBackground: UIColor {
        UIColor(named: "ubi4_gray") ?? UIColor(red: 0.216, green: 0.216, blue: 0.216, alpha: 1)
    }

    static var gestureUsageDivider: UIColor {
        UIColor(named: "ubi4_gray_border") ?? UIColor(red: 0.267, green: 0.267, blue: 0.267, alpha: 1)
    }

    static var gestureUsagePrimaryText: UIColor {
        UIColor(named: "ubi4_white") ?? .white
    }

    static var gestureUsageSecondaryText: UIColor {
        UIColor(named: "ubi4_deactivate_text") ?? UIColor(white: 0.514, alpha: 1)
    }

    static func gestureUsageColor(index: Int) -> UIColor {
        let colors: [UIColor] = [
            UIColor(red: 0.776, green: 0.945, blue: 0.345, alpha: 1),
            UIColor(red: 0.039, green: 0.518, blue: 1.000, alpha: 1),
            UIColor(red: 0.976, green: 0.604, blue: 0.263, alpha: 1),
            UIColor(red: 1.000, green: 0.271, blue: 0.227, alpha: 1),
            UIColor(red: 0.294, green: 0.675, blue: 0.784, alpha: 1),
            UIColor(red: 0.573, green: 0.682, blue: 0.059, alpha: 1),
            UIColor(red: 0.514, green: 0.514, blue: 0.514, alpha: 1),
            UIColor(red: 0.341, green: 0.420, blue: 0.455, alpha: 1)
        ]
        return colors[abs(index) % colors.count]
    }
}
