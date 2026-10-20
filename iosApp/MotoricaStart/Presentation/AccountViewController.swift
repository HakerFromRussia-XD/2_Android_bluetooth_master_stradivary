import SwiftUI
import UIKit
import shared

private enum AccountMetrics {
    static let sideInset: CGFloat = 16
    static let rowHeight: CGFloat = 56
    static let cardRadius: CGFloat = 12
    static let dividerInset: CGFloat = 8
    static let sectionSpacing: CGFloat = 22
    static let cardTop: CGFloat = 16
}

final class AccountViewController: UIViewController {
    private var loadJob: Kotlinx_coroutines_coreJob?
    private var serialNumberJob: Kotlinx_coroutines_coreJob?
    private var serialObservationID = UUID()
    private var accountLoadID = UUID()
    private var accountSerialNumber = ""
    private var boardModeJob: Kotlinx_coroutines_coreJob?
    private var boardsJob: Kotlinx_coroutines_coreJob?
    private var profile: AccountBridgeProfile?
    private var boards: [AccountBridgeBoard] = []
    private var roleObserver: NSObjectProtocol?
    private var selectedRole = UserFirmwareRoleAccess.selectedRole
    private var isAccountVisible = false
    private lazy var firmwareUpdateController = AccountFirmwareUpdateController(presentingViewController: self)

    private let scrollView = UIScrollView()
    private let contentStack = UIStackView()
    private let refreshControl = UIRefreshControl()
    private let activityIndicator = UIActivityIndicatorView(style: .large)
    private var statusBarHostingController: UIHostingController<StatusBarView>?

    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
    private let cardColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
    private let borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444)
    private let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
    private let inactiveTextColor = UIColor.accountColor("ubi4_deactivate_text", fallback: 0x838383)
    private let activeColor = UIColor.accountColor("ubi4_active", fallback: 0xC6F158)

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
        setupView()
        observeBoardMode()
        observeBoards()
        observeFirmwareRole()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
        isAccountVisible = true
        selectedRole = UserFirmwareRoleAccess.selectedRole
        renderContent()
        refreshFirmwareCatalog()
        reloadAccount()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        let isNavigatingAway = navigationController.map { $0.topViewController !== self } ?? false
        // Firmware dialogs cover this screen; keep their selected download alive.
        guard presentedViewController == nil || isMovingFromParent || isBeingDismissed ||
                navigationController?.isBeingDismissed == true || isNavigatingAway else { return }
        isAccountVisible = false
        stopAccountLoading()
        firmwareUpdateController.cancelPendingDownloads()
    }

    deinit {
        loadJob?.cancel(cause: nil)
        serialNumberJob?.cancel(cause: nil)
        boardModeJob?.cancel(cause: nil)
        boardsJob?.cancel(cause: nil)
        if let roleObserver {
            NotificationCenter.default.removeObserver(roleObserver)
        }
    }

    private func setupView() {
        view.backgroundColor = backgroundColor
        view.accessibilityIdentifier = AccessibilityIdentifier.accountRoot
        setupTopBar()
        setupScrollView()
        setupActivityIndicator()
    }

    private func setupTopBar() {
        let hostingController = UIHostingController(
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
        statusBarHostingController = hostingController
        addChild(hostingController)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        hostingController.view.backgroundColor = .clear
        view.addSubview(hostingController.view)

        NSLayoutConstraint.activate([
            hostingController.view.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            hostingController.view.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            hostingController.view.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            hostingController.view.heightAnchor.constraint(equalToConstant: StatusBarView.Constants.height)
        ])
        hostingController.didMove(toParent: self)
    }

    private func setupScrollView() {
        guard let statusBarView = statusBarHostingController?.view else { return }
        scrollView.backgroundColor = backgroundColor
        scrollView.alwaysBounceVertical = true
        scrollView.refreshControl = refreshControl
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scrollView)

        refreshControl.tintColor = textColor
        refreshControl.addTarget(self, action: #selector(handleRefresh), for: .valueChanged)

        contentStack.axis = .vertical
        contentStack.spacing = 0
        contentStack.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(contentStack)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: statusBarView.bottomAnchor, constant: 8),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            contentStack.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            contentStack.leadingAnchor.constraint(equalTo: scrollView.frameLayoutGuide.leadingAnchor),
            contentStack.trailingAnchor.constraint(equalTo: scrollView.frameLayoutGuide.trailingAnchor),
            contentStack.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor, constant: -16)
        ])
    }

    private func setupActivityIndicator() {
        activityIndicator.color = textColor
        activityIndicator.hidesWhenStopped = true
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(activityIndicator)
        NSLayoutConstraint.activate([
            activityIndicator.widthAnchor.constraint(equalToConstant: 100),
            activityIndicator.heightAnchor.constraint(equalToConstant: 100),
            activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
    }

    private func observeBoardMode() {
        boardModeJob?.cancel(cause: nil)
        boardModeJob = AccountBridge.shared.observeBoardMode { [weak self] mode in
            DispatchQueue.main.async {
                self?.applyBoardMode(mode)
            }
        }
    }

    private func observeBoards() {
        boardsJob?.cancel(cause: nil)
        boardsJob = AccountBridge.shared.observeBoards { [weak self] boards in
            DispatchQueue.main.async {
                self?.applyBoards(boards)
            }
        }
    }

    private func applyBoards(_ incomingBoards: [AccountBridgeBoard]) {
        guard !incomingBoards.isEmpty || boards.isEmpty else { return }
        let previousBoards = boards
        boards = incomingBoards.map { board in
            guard let previous = previousBoards.first(where: { $0.deviceAddress == board.deviceAddress }) else {
                return board
            }
            return AccountBridgeBoard(
                boardName: board.boardName,
                deviceCode: board.deviceCode,
                deviceAddress: board.deviceAddress,
                version: board.version,
                canUpdate: board.canUpdate,
                isInBootloader: previous.isInBootloader
            )
        }
        renderContent()
    }

    private func observeFirmwareRole() {
        roleObserver = UserFirmwareRoleAccess.observeChanges { [weak self] in
            guard let self else { return }
            let role = UserFirmwareRoleAccess.selectedRole
            guard self.selectedRole != role else { return }
            self.selectedRole = role
            if !UserFirmwareRoleAccess.canUpdateFirmware {
                self.firmwareUpdateController.cancelPendingDownloads()
            }
            self.renderContent()
            if self.isAccountVisible && UserFirmwareRoleAccess.canUpdateFirmware {
                self.refreshFirmwareCatalog()
            }
        }
    }

    private func refreshFirmwareCatalog() {
        guard UserFirmwareRoleAccess.canUpdateFirmware else {
            firmwareUpdateController.cancelPendingDownloads()
            return
        }
        firmwareUpdateController.refreshCatalog { [weak self] in
            self?.renderContent()
        }
    }

    private func applyBoardMode(_ mode: AccountBridgeBoardMode) {
        boards = boards.map { board in
            guard board.deviceAddress == mode.deviceAddress else { return board }
            return AccountBridgeBoard(
                boardName: board.boardName,
                deviceCode: board.deviceCode,
                deviceAddress: board.deviceAddress,
                version: board.version,
                canUpdate: board.canUpdate,
                isInBootloader: mode.isInBootloader
            )
        }
        renderContent()
    }

    @objc private func handleRefresh() {
        refreshFirmwareCatalog()
        reloadAccount()
    }

    private func reloadAccount() {
        stopAccountLoading()
        accountSerialNumber = ""
        view.accessibilityValue = nil
        profile = nil
        activityIndicator.startAnimating()
        applyBoards(AccountBridge.shared.currentBoards())

        let observationID = serialObservationID
        serialNumberJob = AccountSerialNumberBridge.shared.observe(
            lang: currentLanguageCode(),
            request: { data in
                let gatt = SampleGattAttributes()
                BLEComponents.shared.bleManager.sendBytesKmm(
                    data: data,
                    command: gatt.SERIALPORTCHAR_UUID,
                    typeCommand: gatt.WRITE,
                    onChunkSent: {}
                )
            }
        ) { [weak self] state in
            DispatchQueue.main.async {
                guard let self, self.serialObservationID == observationID else { return }
                self.applySerialNumber(state)
            }
        }
    }

    private func stopAccountLoading() {
        // Invalidate callbacks already queued on the main queue before cancelling their jobs.
        serialObservationID = UUID()
        accountLoadID = UUID()
        serialNumberJob?.cancel(cause: nil)
        serialNumberJob = nil
        loadJob?.cancel(cause: nil)
        loadJob = nil
        activityIndicator.stopAnimating()
        refreshControl.endRefreshing()
    }

    private func applySerialNumber(_ state: AccountSerialNumberState) {
        guard !state.serialNumber.isEmpty else {
            accountLoadID = UUID()
            loadJob?.cancel(cause: nil)
            loadJob = nil
            accountSerialNumber = ""
            view.accessibilityValue = nil
            profile = nil
            renderContent()
            if state.isLoading {
                activityIndicator.startAnimating()
            } else {
                activityIndicator.stopAnimating()
                refreshControl.endRefreshing()
            }
            if isAccountVisible, !state.errorMessage.isEmpty {
                showToast(state.errorMessage)
            }
            return
        }

        guard state.serialNumber != accountSerialNumber else { return }
        accountLoadID = UUID()
        loadJob?.cancel(cause: nil)
        accountSerialNumber = state.serialNumber
        view.accessibilityValue = accountSerialNumber
        profile = nil
        renderContent()
        activityIndicator.startAnimating()

        let serialNumber = accountSerialNumber
        let loadID = accountLoadID
        loadJob = AccountBridge.shared.loadAccount(
            serialNumber: serialNumber,
            lang: currentLanguageCode()
        ) { [weak self] result in
            DispatchQueue.main.async {
                guard let self, self.accountLoadID == loadID,
                      self.accountSerialNumber == serialNumber else { return }
                self.loadJob = nil
                self.activityIndicator.stopAnimating()
                self.refreshControl.endRefreshing()
                self.profile = result.profile
                self.renderContent()
                if self.isAccountVisible, !result.isSuccess, !result.errorMessage.isEmpty {
                    self.showToast(result.errorMessage)
                }
            }
        }
    }

    private func renderContent() {
        contentStack.arrangedSubviews.forEach {
            contentStack.removeArrangedSubview($0)
            $0.removeFromSuperview()
        }

        addSectionTitle(SharedLocalizedText.text(SharedRes.strings().general))
        addCard(makeGeneralSection())
        addSectionTitle(SharedLocalizedText.text(SharedRes.strings().software_information), topInset: AccountMetrics.sectionSpacing)
        if !boards.isEmpty {
            addCard(makeSoftwareSection())
        }
    }

    private func addSectionTitle(_ text: String, topInset: CGFloat = 0) {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false

        let label = UILabel()
        label.text = text
        label.font = .accountInterSemibold(size: 14)
        label.textColor = textColor
        label.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(label)

        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: container.topAnchor, constant: topInset),
            label.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: AccountMetrics.sideInset),
            label.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -AccountMetrics.sideInset),
            label.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        contentStack.addArrangedSubview(container)
    }

    private func addCard(_ card: UIView) {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(card)
        NSLayoutConstraint.activate([
            card.topAnchor.constraint(equalTo: container.topAnchor, constant: AccountMetrics.cardTop),
            card.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: AccountMetrics.sideInset),
            card.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -AccountMetrics.sideInset),
            card.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        contentStack.addArrangedSubview(container)
    }

    private func makeGeneralSection() -> UIView {
        let section = AccountCardView(backgroundColor: cardColor, borderColor: borderColor)
        section.addRow(
            AccountMenuRow(
                iconName: "customer_service",
                title: SharedLocalizedText.text(SharedRes.strings().customer_service),
                textColor: textColor
            ) { [weak self] in
                guard let self, let profile = self.profile else { return }
                self.navigationController?.pushViewController(
                    AccountCustomerServiceViewController(profile: profile, topTitle: self.currentSerialNumber()),
                    animated: true
                )
            }
        )
        section.addDivider(color: borderColor)
        section.addRow(
            AccountMenuRow(
                iconName: "prosthesis_information",
                title: SharedLocalizedText.text(SharedRes.strings().prosthesis_information),
                textColor: textColor
            ) { [weak self] in
                guard let self, let profile = self.profile else { return }
                self.navigationController?.pushViewController(
                    AccountProsthesisInfoViewController(profile: profile, topTitle: self.currentSerialNumber()),
                    animated: true
                )
            }
        )
        section.addDivider(color: borderColor)
        section.addRow(
            AccountMenuRow(
                iconName: "prosthesis_information",
                title: SharedLocalizedText.text(SharedRes.strings().statistics),
                textColor: textColor,
                accessibilityIdentifier: AccessibilityIdentifier.accountStatisticsButton
            ) { [weak self] in
                self?.navigationController?.pushViewController(
                    AccountStatisticsViewController(),
                    animated: true
                )
            }
        )
        section.addDivider(color: borderColor)
        section.addRow(
            AccountMenuRow(
                iconName: "ic_trophy",
                title: NSLocalizedString("motorica_games", comment: ""),
                textColor: textColor
            ) { [weak self] in
                self?.navigationController?.pushViewController(AccountGamesViewController(), animated: true)
            }
        )
        return section
    }

    private func makeSoftwareSection() -> UIView {
        let section = AccountCardView(backgroundColor: cardColor, borderColor: borderColor)
        for board in boards {
            section.addRow(
                AccountBoardRow(
                    board: board,
                    highlight: firmwareUpdateController.isUpdateAvailable(for: board),
                    showUpdate: UserFirmwareRoleAccess.canUpdateFirmware,
                    textColor: textColor,
                    inactiveTextColor: inactiveTextColor,
                    activeColor: activeColor
                ) { [weak self] in
                    guard let self, UserFirmwareRoleAccess.canUpdateFirmware else { return }
                    self.firmwareUpdateController.showFirmwarePicker(for: board) { [weak self] in
                        self?.handleRefresh()
                    }
                }
            )
            section.addDivider(color: borderColor)
        }
        section.addRow(
            AccountAppVersionRow(
                version: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
                textColor: textColor
            )
        )
        section.addDivider(color: borderColor)
        return section
    }

    private func currentSerialNumber() -> String {
        accountSerialNumber
    }

    private func currentLanguageCode() -> String {
        let languageCode: String?
        if #available(iOS 16.0, *) {
            languageCode = Locale.current.language.languageCode?.identifier
        } else {
            languageCode = Locale.current.languageCode
        }
        return languageCode == "ru" ? "ru" : "en"
    }

}

/// V3 gesture statistics opened from Account, matching the Android account flow.
final class AccountStatisticsViewController: UIViewController {
    private let tableView = UITableView(frame: .zero, style: .plain)
    private var statusBarHostingController: UIHostingController<StatusBarView>?
    private var telemetryCountersJob: Kotlinx_coroutines_coreJob?
    private var viewModel = GestureUsageListItemViewModel(
        id: "account-gesture-usage",
        title: SharedLocalizedText.text(SharedRes.strings().gesture_usage_chart_title),
        emptyTitle: SharedLocalizedText.text(SharedRes.strings().gesture_usage_empty),
        totalTitle: AccountStatisticsViewController.totalTitle,
        items: []
    )

    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)

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
        view.backgroundColor = backgroundColor
        view.accessibilityIdentifier = AccessibilityIdentifier.accountStatisticsRoot
        setupTopBar()
        setupTableView()
        observeTelemetry()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
        requestTelemetryData()
    }

    deinit {
        telemetryCountersJob?.cancel(cause: nil)
    }

    private func setupTopBar() {
        let hostingController = UIHostingController(
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
        statusBarHostingController = hostingController
        addChild(hostingController)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        hostingController.view.backgroundColor = .clear
        view.addSubview(hostingController.view)
        NSLayoutConstraint.activate([
            hostingController.view.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            hostingController.view.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            hostingController.view.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            hostingController.view.heightAnchor.constraint(equalToConstant: StatusBarView.Constants.height)
        ])
        hostingController.didMove(toParent: self)
    }

    private func setupTableView() {
        guard let topBar = statusBarHostingController?.view else { return }
        tableView.backgroundColor = backgroundColor
        tableView.separatorStyle = .none
        tableView.showsVerticalScrollIndicator = false
        tableView.estimatedRowHeight = 320
        tableView.rowHeight = UITableView.automaticDimension
        tableView.dataSource = self
        tableView.register(
            GestureUsageChartViewCell.self,
            forCellReuseIdentifier: GestureUsageChartViewCell.reuseIdentifier
        )
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: topBar.bottomAnchor, constant: 8),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func observeTelemetry() {
        telemetryCountersJob?.cancel(cause: nil)
        telemetryCountersJob = WidgetStateBridge.shared.observeTelemetryGestureCounters { [weak self] counters in
            DispatchQueue.main.async {
                guard let self else { return }
                self.viewModel = GestureUsageListItemViewModel(
                    id: "account-gesture-usage",
                    title: SharedLocalizedText.text(SharedRes.strings().gesture_usage_chart_title),
                    emptyTitle: SharedLocalizedText.text(SharedRes.strings().gesture_usage_empty),
                    totalTitle: Self.totalTitle,
                    items: self.makeItems(from: counters)
                )
                self.tableView.reloadData()
            }
        }
    }

    private func requestTelemetryData() {
        guard UiInterfaceModeBridgeV3.shared.isEnabled() else { return }
        let gatt = SampleGattAttributes()
        BLEComponents.shared.bleManager.sendBytesKmm(
            data: BLECommandsV3.shared.requestTelemetryData(),
            command: gatt.SERIALPORTCHAR_UUID,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }

    private func makeItems(from counters: TelemetryGestureCounters) -> [GestureUsageChartItem] {
        let baseItems: [GestureUsageChartItem] = counters.baseGestureMovementCount.enumerated().compactMap { index, rawCount in
            guard Self.baseGestureIds.indices.contains(index) else { return nil }
            let gestureId = Self.baseGestureIds[index]
            let count = longValue(from: rawCount)
            guard gestureId != 0, count > 0 else { return nil }
            return GestureUsageChartItem(
                gestureId: gestureId,
                title: baseGestureName(for: gestureId),
                count: count,
                colorIndex: gestureId
            )
        }
        let customNames = customGestureNames()
        let customItems: [GestureUsageChartItem] = counters.customGestureMovementCount.enumerated().compactMap { index, rawCount in
            let count = longValue(from: rawCount)
            guard count > 0 else { return nil }
            let gestureId = 64 + index
            return GestureUsageChartItem(
                gestureId: gestureId,
                title: customNames.indices.contains(index)
                    ? customNames[index]
                    : "\(SharedLocalizedText.text(SharedRes.strings().custom_gesture)) \(index + 1)",
                count: count,
                colorIndex: gestureId
            )
        }
        return (baseItems + customItems).sorted {
            $0.count == $1.count ? $0.gestureId < $1.gestureId : $0.count > $1.count
        }
    }

    private func longValue(from value: Any) -> Int64 {
        if let value = value as? KotlinLong { return value.int64Value }
        if let value = value as? NSNumber { return value.int64Value }
        return 0
    }

    private func baseGestureName(for gestureId: Int) -> String {
        let resources: [StringResource] = [
            SharedRes.strings().fist, // Gesture 0 is filtered before this lookup.
            SharedRes.strings().fist,
            SharedRes.strings().gesture_point,
            SharedRes.strings().gesture_pinch,
            SharedRes.strings().gesture_fist_thumb_over,
            SharedRes.strings().gesture_key,
            SharedRes.strings().gesture_rock,
            SharedRes.strings().gesture_twizzers,
            SharedRes.strings().gesture_cupholder,
            SharedRes.strings().gesture_half_grab,
            SharedRes.strings().gesture_ok,
            SharedRes.strings().gesture_thumb_up,
            SharedRes.strings().gesture_middle_finger,
            SharedRes.strings().gesture_double_point,
            SharedRes.strings().gesture_call_me,
            SharedRes.strings().gesture_natural_position
        ]
        guard resources.indices.contains(gestureId) else { return "Gesture \(gestureId)" }
        return SharedLocalizedText.text(resources[gestureId])
    }

    private func customGestureNames() -> [String] {
        let stored = GestureService.shared.loadNames()
        let defaults = [
            SharedRes.strings().gesture_1_btn, SharedRes.strings().gesture_2_btn,
            SharedRes.strings().gesture_3_btn, SharedRes.strings().gesture_4_btn,
            SharedRes.strings().gesture_5_btn, SharedRes.strings().gesture_6_btn,
            SharedRes.strings().gesture_7_btn, SharedRes.strings().gesture_8_btn,
            SharedRes.strings().gesture_9_btn, SharedRes.strings().gesture_10_btn,
            SharedRes.strings().gesture_11_btn, SharedRes.strings().gesture_12_btn,
            SharedRes.strings().gesture_13_btn, SharedRes.strings().gesture_14_btn,
            SharedRes.strings().gesture_15_btn
        ].map(SharedLocalizedText.text)
        guard stored.count < defaults.count else { return Array(stored.prefix(defaults.count)) }
        return stored + Array(defaults[stored.count...])
    }

    private static var totalTitle: String {
        Locale.current.languageCode == "ru" ? "Всего:" : "Total:"
    }

    private static let baseGestureIds = Array(0...15)
}

extension AccountStatisticsViewController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { 1 }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(
            withIdentifier: GestureUsageChartViewCell.reuseIdentifier,
            for: indexPath
        ) as! GestureUsageChartViewCell
        cell.configure(with: viewModel)
        return cell
    }
}

private final class AccountCustomerServiceViewController: AccountDetailsViewController {
    init(profile: AccountBridgeProfile, topTitle: String) {
        super.init(topTitle: topTitle)
        addRows([
            .init(title: SharedLocalizedText.text(SharedRes.strings().date_of_receipt_of_prosthesis), value: profile.dateOfReceipt),
            .init(title: SharedLocalizedText.text(SharedRes.strings().warranty_expiration_date), value: profile.warrantyExpirationDate),
            .init(title: SharedLocalizedText.text(SharedRes.strings().your_manager), value: profile.managerName, phone: profile.managerPhone),
            .init(title: SharedLocalizedText.text(SharedRes.strings().prosthesis_status), value: profile.prosthesisStatus)
        ])
    }
}

private final class AccountProsthesisInfoViewController: AccountDetailsViewController {
    init(profile: AccountBridgeProfile, topTitle: String) {
        super.init(topTitle: topTitle)
        addRows([
            .init(title: SharedLocalizedText.text(SharedRes.strings().prosthesis_model), value: profile.prosthesisModel),
            .init(title: SharedLocalizedText.text(SharedRes.strings().prosthesis_size), value: profile.prosthesisSize),
            .init(title: SharedLocalizedText.text(SharedRes.strings().hand_side_2), value: profile.handSide),
            .init(title: SharedLocalizedText.text(SharedRes.strings().rotator_type), value: profile.rotatorType),
            .init(title: SharedLocalizedText.text(SharedRes.strings().touchscreen_finger_pads), value: profile.touchscreenFingerPads),
            .init(title: SharedLocalizedText.text(SharedRes.strings().battery_type), value: profile.batteryType)
        ])
    }
}

private class AccountDetailsViewController: UIViewController {
    struct DetailRow {
        let title: String
        let value: String
        var phone: String?
    }

    private let stack = UIStackView()
    private var statusBarHostingController: UIHostingController<StatusBarView>?
    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
    private let cardColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
    private let borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444)
    private let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)

    init(topTitle: String) {
        super.init(nibName: nil, bundle: nil)
        hidesBottomBarWhenPushed = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupView()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    private func setupView() {
        view.backgroundColor = backgroundColor

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

        stack.axis = .vertical
        stack.spacing = 0
        stack.translatesAutoresizingMaskIntoConstraints = false
        let card = AccountCardView(backgroundColor: cardColor, borderColor: borderColor)
        card.addRow(stack)
        view.addSubview(card)

        NSLayoutConstraint.activate([
            statusBar.view.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            statusBar.view.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            statusBar.view.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            statusBar.view.heightAnchor.constraint(equalToConstant: StatusBarView.Constants.height),

            card.topAnchor.constraint(equalTo: statusBar.view.bottomAnchor, constant: 32),
            card.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: AccountMetrics.sideInset),
            card.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -AccountMetrics.sideInset)
        ])
        statusBar.didMove(toParent: self)
    }

    func addRows(_ rows: [DetailRow]) {
        for (index, row) in rows.enumerated() {
            stack.addArrangedSubview(
                AccountDetailRow(
                    title: row.title,
                    value: row.value,
                    phone: row.phone,
                    textColor: textColor
                )
            )
            if index != rows.indices.last {
                stack.addArrangedSubview(AccountDivider(color: borderColor))
            }
        }
    }
}

private final class AccountCardView: UIView {
    private let stack = UIStackView()

    init(backgroundColor: UIColor, borderColor: UIColor) {
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false
        self.backgroundColor = backgroundColor
        layer.cornerRadius = AccountMetrics.cardRadius
        layer.borderColor = borderColor.cgColor
        layer.borderWidth = 1
        layer.masksToBounds = true

        stack.axis = .vertical
        stack.spacing = 0
        stack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: topAnchor),
            stack.leadingAnchor.constraint(equalTo: leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func addRow(_ row: UIView) {
        stack.addArrangedSubview(row)
    }

    func addDivider(color: UIColor) {
        stack.addArrangedSubview(AccountDivider(color: color))
    }
}

private final class AccountDivider: UIView {
    init(color: UIColor) {
        super.init(frame: .zero)
        backgroundColor = color
        translatesAutoresizingMaskIntoConstraints = false
        heightAnchor.constraint(equalToConstant: 1).isActive = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private final class AccountMenuRow: UIControl {
    private let action: () -> Void

    init(
        iconName: String,
        title: String,
        textColor: UIColor,
        accessibilityIdentifier: String? = nil,
        action: @escaping () -> Void
    ) {
        self.action = action
        super.init(frame: .zero)
        self.accessibilityIdentifier = accessibilityIdentifier
        setup(iconName: iconName, title: title, textColor: textColor)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(iconName: String, title: String, textColor: UIColor) {
        heightAnchor.constraint(equalToConstant: AccountMetrics.rowHeight).isActive = true

        let icon = UIImageView(image: UIImage(named: iconName)?.withRenderingMode(.alwaysTemplate))
        icon.tintColor = textColor
        icon.contentMode = .scaleAspectFit
        icon.translatesAutoresizingMaskIntoConstraints = false
        addSubview(icon)

        let label = UILabel()
        label.text = title
        label.font = .accountOpenSansRegular(size: 14)
        label.textColor = textColor
        label.translatesAutoresizingMaskIntoConstraints = false
        addSubview(label)

        let chevron = UIImageView(image: UIImage(named: "ic_navigate_next")?.withRenderingMode(.alwaysTemplate))
        chevron.tintColor = textColor
        chevron.contentMode = .scaleAspectFit
        chevron.translatesAutoresizingMaskIntoConstraints = false
        addSubview(chevron)

        addTarget(self, action: #selector(handleTap), for: .touchUpInside)

        NSLayoutConstraint.activate([
            icon.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            icon.centerYAnchor.constraint(equalTo: centerYAnchor),
            icon.widthAnchor.constraint(equalToConstant: 24),
            icon.heightAnchor.constraint(equalToConstant: 24),

            chevron.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            chevron.centerYAnchor.constraint(equalTo: centerYAnchor),
            chevron.widthAnchor.constraint(equalToConstant: 24),
            chevron.heightAnchor.constraint(equalToConstant: 24),

            label.leadingAnchor.constraint(equalTo: icon.trailingAnchor, constant: 12),
            label.trailingAnchor.constraint(lessThanOrEqualTo: chevron.leadingAnchor, constant: -12),
            label.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
    }

    @objc private func handleTap() {
        action()
    }
}

private final class AccountAppVersionRow: UIView {
    init(version: String, textColor: UIColor) {
        super.init(frame: .zero)
        setup(version: version, textColor: textColor)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(version: String, textColor: UIColor) {
        heightAnchor.constraint(equalToConstant: AccountMetrics.rowHeight).isActive = true

        let titleLabel = UILabel()
        titleLabel.text = SharedLocalizedText.text(SharedRes.strings().version_app)
        titleLabel.font = .accountOpenSansRegular(size: 14)
        titleLabel.textColor = textColor
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)

        let valueLabel = UILabel()
        valueLabel.text = version
        valueLabel.font = .accountOpenSansSemibold(size: 12)
        valueLabel.textColor = textColor
        valueLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(valueLabel)

        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: valueLabel.leadingAnchor, constant: -12),

            valueLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            valueLabel.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
    }
}

private final class AccountDetailRow: UIView {
    init(title: String, value: String, phone: String? = nil, textColor: UIColor) {
        super.init(frame: .zero)
        setup(title: title, value: value, phone: phone, textColor: textColor)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(title: String, value: String, phone: String?, textColor: UIColor) {
        heightAnchor.constraint(equalToConstant: AccountMetrics.rowHeight).isActive = true

        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.font = .accountOpenSansRegular(size: 14)
        titleLabel.textColor = textColor
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)

        let valueLabel = UILabel()
        valueLabel.text = value.isEmpty ? "-" : value
        valueLabel.font = .accountOpenSansSemibold(size: 12)
        valueLabel.textColor = textColor
        valueLabel.lineBreakMode = .byTruncatingTail
        valueLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(valueLabel)

        var trailingAnchor = self.trailingAnchor
        if let phone, !phone.isEmpty {
            let phoneButton = UIButton(type: .custom)
            phoneButton.setImage(UIImage(named: "ic_phone_call")?.withRenderingMode(.alwaysTemplate), for: .normal)
            phoneButton.tintColor = textColor
            phoneButton.addAction(UIAction { _ in
                guard let url = PhoneDialURLFormatter.dialURL(from: phone) else { return }
                UIApplication.shared.open(url)
            }, for: .touchUpInside)
            phoneButton.translatesAutoresizingMaskIntoConstraints = false
            addSubview(phoneButton)
            trailingAnchor = phoneButton.leadingAnchor
            NSLayoutConstraint.activate([
                phoneButton.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -12),
                phoneButton.centerYAnchor.constraint(equalTo: self.centerYAnchor),
                phoneButton.widthAnchor.constraint(equalToConstant: 32),
                phoneButton.heightAnchor.constraint(equalToConstant: 32)
            ])
        }

        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 25),
            titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            titleLabel.heightAnchor.constraint(equalToConstant: 20),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -12),

            valueLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 25),
            valueLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor),
            valueLabel.heightAnchor.constraint(equalToConstant: 20),
            valueLabel.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -12)
        ])
    }
}

private final class AccountBoardRow: UIView {
    init(
        board: AccountBridgeBoard,
        highlight: Bool,
        showUpdate: Bool,
        textColor: UIColor,
        inactiveTextColor: UIColor,
        activeColor: UIColor,
        updateAction: @escaping () -> Void
    ) {
        super.init(frame: .zero)
        setup(
            board: board,
            highlight: highlight,
            showUpdate: showUpdate,
            textColor: textColor,
            inactiveTextColor: inactiveTextColor,
            activeColor: activeColor,
            updateAction: updateAction
        )
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(
        board: AccountBridgeBoard,
        highlight: Bool,
        showUpdate: Bool,
        textColor: UIColor,
        inactiveTextColor: UIColor,
        activeColor: UIColor,
        updateAction: @escaping () -> Void
    ) {
        heightAnchor.constraint(equalToConstant: AccountMetrics.rowHeight).isActive = true

        let titleLabel = UILabel()
        titleLabel.text = board.boardName
        let boardKey = accountAccessibilityKey(board.boardName)
        accessibilityIdentifier = "\(AccessibilityIdentifier.accountBoardRowPrefix).\(boardKey)"
        accessibilityValue = "board=\(board.boardName);version=\(board.version);bootloader=\(board.isInBootloader)"
        titleLabel.font = .accountOpenSansRegular(size: 14)
        titleLabel.textColor = textColor
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)

        let versionLabel = UILabel()
        versionLabel.text = board.version
        versionLabel.accessibilityIdentifier = "\(AccessibilityIdentifier.accountBoardVersionPrefix).\(boardKey)"
        versionLabel.accessibilityValue = board.version
        versionLabel.font = .accountOpenSansSemibold(size: 12)
        versionLabel.textColor = textColor
        versionLabel.translatesAutoresizingMaskIntoConstraints = false

        let bootloaderLabel = UILabel()
        bootloaderLabel.text = "▷"
        bootloaderLabel.accessibilityIdentifier = "\(AccessibilityIdentifier.accountBoardBootloaderPrefix).\(boardKey)"
        bootloaderLabel.accessibilityValue = board.isInBootloader ? "bootloader=true" : "bootloader=false"
        bootloaderLabel.font = .accountOpenSansSemibold(size: 12)
        bootloaderLabel.textColor = textColor
        bootloaderLabel.isHidden = !board.isInBootloader
        bootloaderLabel.translatesAutoresizingMaskIntoConstraints = false

        let updateButton = UIButton(type: .system)
        updateButton.setTitle(SharedLocalizedText.text(SharedRes.strings().update_firmware), for: .normal)
        updateButton.accessibilityIdentifier = "\(AccessibilityIdentifier.accountBoardUpdateButtonPrefix).\(boardKey)"
        updateButton.titleLabel?.font = .accountOpenSansSemibold(size: 12)
        let canUpdate = board.canUpdate && showUpdate
        updateButton.setTitleColor(board.canUpdate ? (highlight ? activeColor : textColor) : inactiveTextColor, for: .normal)
        updateButton.accessibilityValue = "board=\(board.boardName);version=\(board.version);updateAvailable=\(highlight);enabled=\(canUpdate)"
        updateButton.isHidden = !showUpdate
        updateButton.isEnabled = canUpdate
        updateButton.addAction(UIAction { _ in updateAction() }, for: .touchUpInside)
        updateButton.translatesAutoresizingMaskIntoConstraints = false

        let trailingStack = UIStackView(arrangedSubviews: [versionLabel, bootloaderLabel, updateButton])
        trailingStack.axis = .horizontal
        trailingStack.alignment = .center
        trailingStack.spacing = 12
        trailingStack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(trailingStack)

        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: trailingStack.leadingAnchor, constant: -12),
            trailingStack.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            trailingStack.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
    }
}

extension UIColor {
    static func accountColor(_ name: String, fallback hex: UInt32) -> UIColor {
        UIColor(named: name) ?? UIColor(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

func accountAccessibilityKey(_ value: String) -> String {
    let allowed = CharacterSet.alphanumerics
    let normalized = value.lowercased().unicodeScalars.map { scalar -> String in
        allowed.contains(scalar) ? String(scalar) : "-"
    }.joined()
    return normalized
        .split(separator: "-")
        .joined(separator: "-")
}

private extension UIFont {
    static func accountInterSemibold(size: CGFloat) -> UIFont {
        UIFont(name: "Inter-SemiBold", size: size)
            ?? UIFont(name: "Inter-Regular", size: size)
            ?? .systemFont(ofSize: size, weight: .semibold)
    }

    static func accountOpenSansRegular(size: CGFloat) -> UIFont {
        UIFont(name: "OpenSans-Regular", size: size)
            ?? .systemFont(ofSize: size, weight: .regular)
    }

    static func accountOpenSansSemibold(size: CGFloat) -> UIFont {
        UIFont(name: "OpenSansRoman-SemiBold", size: size)
            ?? UIFont(name: "OpenSans-Regular", size: size)
            ?? .systemFont(ofSize: size, weight: .semibold)
    }
}

extension UIViewController {
    func showToast(_ message: String, iconName: String = "motorica_launch_v2") {
        guard Thread.isMainThread else {
            DispatchQueue.main.async { [weak self] in
                self?.showToast(message, iconName: iconName)
            }
            return
        }

        let toastTag = 707_197
        view.viewWithTag(toastTag)?.removeFromSuperview()

        let container = UIView()
        container.tag = toastTag
        container.backgroundColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
        container.layer.cornerRadius = 12
        container.layer.borderWidth = 1
        container.layer.borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444).cgColor
        container.layer.shadowColor = UIColor.black.cgColor
        container.layer.shadowOpacity = 0.7
        container.layer.shadowRadius = 12
        container.layer.shadowOffset = CGSize(width: 0, height: 8)
        container.alpha = 0
        container.transform = CGAffineTransform(translationX: 0, y: 12)
        container.translatesAutoresizingMaskIntoConstraints = false

        let iconView = UIImageView(image: UIImage(named: iconName))
        iconView.contentMode = .scaleAspectFit
        iconView.translatesAutoresizingMaskIntoConstraints = false

        let label = UILabel()
        label.text = message
        label.textColor = UIColor.accountColor("ubi4_white", fallback: 0xFFFFFF)
        label.font = .systemFont(ofSize: 12, weight: .light)
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false

        container.addSubview(iconView)
        container.addSubview(label)
        view.addSubview(container)

        NSLayoutConstraint.activate([
            container.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            container.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            container.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 40),
            container.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -40),
            container.heightAnchor.constraint(greaterThanOrEqualToConstant: 48),

            iconView.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 22),
            iconView.centerYAnchor.constraint(equalTo: container.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 32),
            iconView.heightAnchor.constraint(equalToConstant: 32),

            label.topAnchor.constraint(greaterThanOrEqualTo: container.topAnchor, constant: 12),
            label.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 18),
            label.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -24),
            label.centerYAnchor.constraint(equalTo: container.centerYAnchor),
            label.bottomAnchor.constraint(lessThanOrEqualTo: container.bottomAnchor, constant: -12)
        ])

        UIView.animate(
            withDuration: 0.2,
            delay: 0,
            options: [.curveEaseOut, .beginFromCurrentState]
        ) {
            container.alpha = 1
            container.transform = .identity
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak container] in
            UIView.animate(
                withDuration: 0.25,
                delay: 0,
                options: [.curveEaseIn, .beginFromCurrentState]
            ) {
                container?.alpha = 0
                container?.transform = CGAffineTransform(translationX: 0, y: 12)
            } completion: { _ in
                container?.removeFromSuperview()
            }
        }
    }
}
