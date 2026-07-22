import SwiftUI
import UIKit
import Combine
import shared

enum BleLogSettings {
    private static let hideGraphStreamKey = "BLE_LOG_HIDE_GRAPH_STREAM"

    static var hidesGraphStream: Bool {
        get {
            guard UserDefaults.standard.object(forKey: hideGraphStreamKey) != nil else { return true }
            return UserDefaults.standard.bool(forKey: hideGraphStreamKey)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: hideGraphStreamKey)
            BleLogBridge.shared.setHideGraphStream(hide: newValue)
        }
    }

    static func syncSharedStore() {
        BleLogBridge.shared.setHideGraphStream(hide: hidesGraphStream)
    }
}

final class BleLogViewController: UIViewController {
    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
    private let cardColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
    private let borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444)
    private let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
    private let inactiveTextColor = UIColor.accountColor("ubi4_deactivate_text", fallback: 0x838383)

    private let headerView = UIView()
    private let titleLabel = UILabel()
    private let graphStreamFilterControl = UIControl()
    private let graphStreamFilterLabel = UILabel()
    private let tableView = UITableView(frame: .zero, style: .plain)
    private let emptyLabel = UILabel()
    private var statusBarHostingController: UIHostingController<StatusBarView>?
    private var graphStreamSwitchHostingController: UIHostingController<BleLogHeaderSwitchView>?
    private var graphStreamSwitchProvider: SwitchProvider?
    private var graphStreamSwitchCancellable: AnyCancellable?
    private var observeJob: Kotlinx_coroutines_coreJob?
    private var entries: [BleLogEntryUi] = []
    private var lastEntryId: Int64 = 0
    private var isAppendScheduled = false
    private var rowHeightCache: [Int64: CGFloat] = [:]

    init() {
        super.init(nibName: nil, bundle: nil)
        hidesBottomBarWhenPushed = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        BleLogSettings.syncSharedStore()
        setupView()
        reloadSnapshot()
        observeLog()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    deinit {
        graphStreamSwitchCancellable?.cancel()
        observeJob?.cancel(cause: nil)
    }

    private func setupView() {
        view.backgroundColor = backgroundColor
        setupTopBar()
        setupHeader()
        setupTable()
        setupEmptyLabel()
    }

    private func setupTopBar() {
        let statusBar = UIHostingController(
            rootView: StatusBarView(
                viewModel: WidgetsTabContainerViewController.sharedStatusBarViewModel,
                leadingButton: .back,
                onBackTap: { [weak self] in
                    self?.navigationController?.popViewController(animated: true)
                },
                onDisconnectConfirmed: { [weak self] in
                    StatusBarDisconnectCoordinator.disconnectAndShowScan(from: self)
                }
            )
        )
        statusBarHostingController = statusBar
        addChild(statusBar)
        statusBar.view.translatesAutoresizingMaskIntoConstraints = false
        statusBar.view.backgroundColor = .clear
        view.addSubview(statusBar.view)

        NSLayoutConstraint.activate([
            statusBar.view.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            statusBar.view.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            statusBar.view.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            statusBar.view.heightAnchor.constraint(equalToConstant: StatusBarView.Constants.height)
        ])
        statusBar.didMove(toParent: self)
    }

    private func setupHeader() {
        guard let statusBarView = statusBarHostingController?.view else { return }
        headerView.translatesAutoresizingMaskIntoConstraints = false
        headerView.backgroundColor = backgroundColor
        view.addSubview(headerView)

        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.text = Self.bleLogTitle
        titleLabel.textColor = textColor
        titleLabel.font = .systemFont(ofSize: 14, weight: .semibold)
        titleLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        headerView.addSubview(titleLabel)

        graphStreamFilterControl.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(graphStreamFilterControl)

        graphStreamFilterLabel.translatesAutoresizingMaskIntoConstraints = false
        graphStreamFilterLabel.text = Self.graphStreamFilterTitle
        graphStreamFilterLabel.textColor = textColor
        graphStreamFilterLabel.font = .systemFont(ofSize: 11, weight: .regular)
        graphStreamFilterLabel.textAlignment = .right
        graphStreamFilterLabel.numberOfLines = 2
        graphStreamFilterLabel.isUserInteractionEnabled = true
        graphStreamFilterLabel.addGestureRecognizer(
            UITapGestureRecognizer(target: self, action: #selector(toggleGraphStreamFilter))
        )
        graphStreamFilterControl.addSubview(graphStreamFilterLabel)

        let switchProvider = SwitchProvider(isOn: BleLogSettings.hidesGraphStream, title: "")
        graphStreamSwitchProvider = switchProvider
        graphStreamSwitchCancellable = switchProvider.$isOn
            .dropFirst()
            .removeDuplicates()
            .sink { [weak self] isOn in
                self?.updateGraphStreamFilter(isOn)
            }

        let switchHost = UIHostingController(rootView: BleLogHeaderSwitchView(provider: switchProvider))
        graphStreamSwitchHostingController = switchHost
        addChild(switchHost)
        switchHost.view.translatesAutoresizingMaskIntoConstraints = false
        switchHost.view.backgroundColor = .clear
        graphStreamFilterControl.addSubview(switchHost.view)

        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: statusBarView.bottomAnchor, constant: 8),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            headerView.heightAnchor.constraint(equalToConstant: 48),

            titleLabel.leadingAnchor.constraint(equalTo: headerView.leadingAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: headerView.centerYAnchor),

            graphStreamFilterControl.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor, constant: 12),
            graphStreamFilterControl.trailingAnchor.constraint(equalTo: headerView.trailingAnchor),
            graphStreamFilterControl.topAnchor.constraint(equalTo: headerView.topAnchor),
            graphStreamFilterControl.bottomAnchor.constraint(equalTo: headerView.bottomAnchor),

            switchHost.view.trailingAnchor.constraint(equalTo: graphStreamFilterControl.trailingAnchor),
            switchHost.view.centerYAnchor.constraint(equalTo: graphStreamFilterControl.centerYAnchor),
            switchHost.view.widthAnchor.constraint(equalToConstant: 52),
            switchHost.view.heightAnchor.constraint(equalToConstant: 32),

            graphStreamFilterLabel.leadingAnchor.constraint(equalTo: graphStreamFilterControl.leadingAnchor),
            graphStreamFilterLabel.trailingAnchor.constraint(equalTo: switchHost.view.leadingAnchor, constant: -8),
            graphStreamFilterLabel.centerYAnchor.constraint(equalTo: graphStreamFilterControl.centerYAnchor)
        ])
        switchHost.didMove(toParent: self)
    }

    private func setupTable() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.backgroundColor = backgroundColor
        tableView.separatorStyle = .none
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 52
        tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 16, right: 0)
        tableView.scrollIndicatorInsets = tableView.contentInset
        tableView.dataSource = self
        tableView.delegate = self
        tableView.allowsSelection = false
        tableView.register(BleLogEntryViewCell.self, forCellReuseIdentifier: BleLogEntryViewCell.reuseIdentifier)
        view.addSubview(tableView)

        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: headerView.bottomAnchor, constant: 8),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func setupEmptyLabel() {
        emptyLabel.translatesAutoresizingMaskIntoConstraints = false
        emptyLabel.text = Self.emptyTitle
        emptyLabel.textColor = inactiveTextColor
        emptyLabel.font = .systemFont(ofSize: 14, weight: .regular)
        emptyLabel.textAlignment = .center
        view.addSubview(emptyLabel)

        NSLayoutConstraint.activate([
            emptyLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            emptyLabel.centerYAnchor.constraint(equalTo: tableView.centerYAnchor),
            emptyLabel.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 24),
            emptyLabel.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -24)
        ])
    }

    private func observeLog() {
        observeJob?.cancel(cause: nil)
        observeJob = BleLogBridge.shared.observeVersion { [weak self] _ in
            DispatchQueue.main.async {
                self?.scheduleAppendNewEntries()
            }
        }
    }

    private func reloadSnapshot() {
        entries = BleLogBridge.shared.snapshot()
        lastEntryId = entries.last?.id ?? 0
        rowHeightCache.removeAll()
        emptyLabel.isHidden = !entries.isEmpty
        tableView.reloadData()
        scrollToBottom(animated: false)
    }

    private func scheduleAppendNewEntries() {
        guard !isAppendScheduled else { return }
        isAppendScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(40)) { [weak self] in
            guard let self else { return }
            self.isAppendScheduled = false
            self.appendNewEntries()
        }
    }

    private func appendNewEntries() {
        let newEntries = BleLogBridge.shared.entriesAfter(id: lastEntryId)
        guard !newEntries.isEmpty else { return }
        let wasAtBottom = isScrolledNearBottom()
        let startIndex = entries.count
        entries.append(contentsOf: newEntries)
        lastEntryId = entries.last?.id ?? lastEntryId
        emptyLabel.isHidden = true

        if newEntries.count > 80 {
            tableView.reloadData()
            if wasAtBottom {
                scrollToBottom(animated: false)
            }
            return
        }

        let indexPaths = newEntries.indices.map { IndexPath(row: startIndex + $0, section: 0) }
        UIView.performWithoutAnimation {
            tableView.performBatchUpdates {
                tableView.insertRows(at: indexPaths, with: .none)
            } completion: { [weak self] _ in
                if wasAtBottom {
                    self?.scrollToBottom(animated: false)
                }
            }
        }
    }

    private func isScrolledNearBottom() -> Bool {
        guard tableView.contentSize.height > tableView.bounds.height else { return true }
        let distance = tableView.contentSize.height - tableView.bounds.height - tableView.contentOffset.y
        return distance < 80
    }

    private func scrollToBottom(animated: Bool) {
        guard !entries.isEmpty else { return }
        tableView.scrollToRow(at: IndexPath(row: entries.count - 1, section: 0), at: .bottom, animated: animated)
    }

    @objc private func toggleGraphStreamFilter() {
        graphStreamSwitchProvider?.isOn.toggle()
    }

    private func updateGraphStreamFilter(_ isOn: Bool) {
        BleLogSettings.hidesGraphStream = isOn
    }

    private func height(for entry: BleLogEntryUi) -> CGFloat {
        if let cachedHeight = rowHeightCache[entry.id] {
            return cachedHeight
        }
        let tableWidth = max(tableView.bounds.width, view.bounds.width)
        let bytesWidth = max(120, tableWidth - 180)
        let rect = entry.bytesHex.boundingRect(
            with: CGSize(width: bytesWidth, height: .greatestFiniteMagnitude),
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            attributes: [.font: BleLogEntryViewCell.bytesFont],
            context: nil
        )
        let height = max(52, ceil(rect.height) + 24)
        rowHeightCache[entry.id] = height
        return height
    }

    private static var bleLogTitle: String {
        isRussianLocale ? "Журнал BLE" : "BLE Log"
    }

    private static var graphStreamFilterTitle: String {
        isRussianLocale ? "Отключение стрима графиков" : "Disable graph stream"
    }

    private static var emptyTitle: String {
        isRussianLocale ? "Нет BLE-команд" : "No BLE commands"
    }

    private static var isRussianLocale: Bool {
        Locale.preferredLanguages.first?.hasPrefix("ru") == true
    }
}

private struct BleLogHeaderSwitchView: View {
    @ObservedObject var provider: SwitchProvider

    var body: some View {
        Toggle(
            "",
            isOn: Binding(
                get: { provider.isOn },
                set: { provider.isOn = $0 }
            )
        )
        .labelsHidden()
        .toggleStyle(UbiSwitchStyle())
        .frame(width: 52, height: 32)
    }
}

extension BleLogViewController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        entries.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(
            withIdentifier: BleLogEntryViewCell.reuseIdentifier,
            for: indexPath
        ) as! BleLogEntryViewCell
        cell.configure(with: entries[indexPath.row])
        return cell
    }
}

extension BleLogViewController: UITableViewDelegate {
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        height(for: entries[indexPath.row])
    }

    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        rowHeightCache[entries[indexPath.row].id] ?? 52
    }
}

private final class BleLogEntryViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: BleLogEntryViewCell.self)
    static let bytesFont = UIFont.monospacedSystemFont(ofSize: 12, weight: .regular)
    private static let timeFont = UIFont.monospacedDigitSystemFont(ofSize: 12, weight: .regular)
    private static let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()

    private let containerView = UIView()
    private let timeLabel = UILabel()
    private let sourceIconView = UIImageView()
    private let arrowIconView = UIImageView()
    private let targetIconView = UIImageView()
    private let bytesLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupViews()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupViews()
    }

    func configure(with entry: BleLogEntryUi) {
        let date = Date(timeIntervalSince1970: TimeInterval(entry.timestampMillis) / 1_000.0)
        timeLabel.text = Self.formatter.string(from: date)
        bytesLabel.text = entry.bytesHex

        if entry.isOutgoing {
            sourceIconView.image = Self.phoneImage
            targetIconView.image = Self.prosthesisImage
        } else {
            sourceIconView.image = Self.prosthesisImage
            targetIconView.image = Self.phoneImage
        }
    }

    private func setupViews() {
        selectionStyle = .none
        backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
        contentView.backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
        contentView.isOpaque = true

        containerView.translatesAutoresizingMaskIntoConstraints = false
        containerView.backgroundColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
        containerView.layer.cornerRadius = 10
        containerView.layer.borderWidth = 1
        containerView.layer.borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444).cgColor
        contentView.addSubview(containerView)

        timeLabel.translatesAutoresizingMaskIntoConstraints = false
        timeLabel.font = Self.timeFont
        timeLabel.textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
        timeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        containerView.addSubview(timeLabel)

        sourceIconView.translatesAutoresizingMaskIntoConstraints = false
        sourceIconView.contentMode = .scaleAspectFit
        containerView.addSubview(sourceIconView)

        arrowIconView.translatesAutoresizingMaskIntoConstraints = false
        arrowIconView.image = Self.arrowImage
        arrowIconView.contentMode = .scaleAspectFit
        arrowIconView.tintColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
        containerView.addSubview(arrowIconView)

        targetIconView.translatesAutoresizingMaskIntoConstraints = false
        targetIconView.contentMode = .scaleAspectFit
        containerView.addSubview(targetIconView)

        bytesLabel.translatesAutoresizingMaskIntoConstraints = false
        bytesLabel.font = Self.bytesFont
        bytesLabel.textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
        bytesLabel.numberOfLines = 0
        containerView.addSubview(bytesLabel)

        NSLayoutConstraint.activate([
            containerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            containerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            containerView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            containerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -4),
            containerView.heightAnchor.constraint(greaterThanOrEqualToConstant: 44),

            timeLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 10),
            timeLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            timeLabel.widthAnchor.constraint(equalToConstant: 64),

            sourceIconView.leadingAnchor.constraint(equalTo: timeLabel.trailingAnchor, constant: 6),
            sourceIconView.centerYAnchor.constraint(equalTo: timeLabel.centerYAnchor),
            sourceIconView.widthAnchor.constraint(equalToConstant: 16),
            sourceIconView.heightAnchor.constraint(equalToConstant: 16),

            arrowIconView.leadingAnchor.constraint(equalTo: sourceIconView.trailingAnchor, constant: 2),
            arrowIconView.centerYAnchor.constraint(equalTo: timeLabel.centerYAnchor),
            arrowIconView.widthAnchor.constraint(equalToConstant: 16),
            arrowIconView.heightAnchor.constraint(equalToConstant: 16),

            targetIconView.leadingAnchor.constraint(equalTo: arrowIconView.trailingAnchor, constant: 2),
            targetIconView.centerYAnchor.constraint(equalTo: timeLabel.centerYAnchor),
            targetIconView.widthAnchor.constraint(equalToConstant: 16),
            targetIconView.heightAnchor.constraint(equalToConstant: 16),

            bytesLabel.leadingAnchor.constraint(equalTo: targetIconView.trailingAnchor, constant: 8),
            bytesLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -10),
            bytesLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 8),
            bytesLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -8)
        ])
    }

    private static var phoneImage: UIImage? {
        UIImage(named: "img_mobile") ?? UIImage(named: "ic_phone") ?? UIImage(systemName: "iphone")
    }

    private static var prosthesisImage: UIImage? {
        UIImage(named: "robotic_hand") ?? UIImage(named: "prosthesis_information") ?? UIImage(systemName: "hand.raised")
    }

    private static var arrowImage: UIImage? {
        (UIImage(named: "arrow_forward_24") ?? UIImage(systemName: "arrow.right"))?.withRenderingMode(.alwaysTemplate)
    }
}
