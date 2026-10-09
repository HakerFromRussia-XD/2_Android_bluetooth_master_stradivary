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

enum V3AccountAction {
    case observationStarted
    case reloadRequested
    case profileRequested(serialNumber: String, language: String)
    case boardModeReceived(deviceAddress: Int32, isInBootloader: Bool)
    case profileReceived(V3AccountProfileSnapshotResult)
}

struct V3AccountUiState {
    let profile: V3AccountProfileSnapshot?
    let boards: [AccountBridgeBoard]
    let isLoading: Bool
}

enum V3AccountPresentation {
    case content(V3AccountUiState)
    case loading(V3AccountUiState)
}

enum V3AccountEffect {
    case readProfileContext
    case showError(String)
}

final class V3AccountViewModel {
    private let getBoards: GetAccountBoardsUseCaseV3
    private let observeBoards: ObserveAccountBoardsUseCaseV3
    private let loadProfile: LoadAccountProfileSnapshotUseCaseV3
    private var loadJob: Kotlinx_coroutines_coreJob?
    private var boardModeJob: Kotlinx_coroutines_coreJob?
    private(set) var uiState = V3AccountUiState(profile: nil, boards: [], isLoading: false)
    let presentation = Observable<V3AccountPresentation?>(nil)
    var onEffect: ((V3AccountEffect) -> Void)?

    init(getBoards: GetAccountBoardsUseCaseV3,
         observeBoards: ObserveAccountBoardsUseCaseV3,
         loadProfile: LoadAccountProfileSnapshotUseCaseV3) {
        self.getBoards = getBoards
        self.observeBoards = observeBoards
        self.loadProfile = loadProfile
    }

    func onAction(_ action: V3AccountAction) {
        switch action {
        case .observationStarted:
            boardModeJob?.cancel(cause: nil)
            boardModeJob = observeBoards.observeBootloaderChanges { [weak self] address, isInBootloader in
                DispatchQueue.main.async {
                    self?.onAction(.boardModeReceived(deviceAddress: address.int32Value,
                                                     isInBootloader: isInBootloader.boolValue))
                }
            }
        case .reloadRequested:
            if uiState.profile == nil {
                uiState = V3AccountUiState(profile: nil, boards: uiState.boards, isLoading: true)
                presentation.value = .loading(uiState)
            }
            let boards = getBoards.current(missingVersion: "-").map { board in
                AccountBridgeBoard(boardName: board.name ?? "",
                                   deviceCode: board.deviceCode,
                                   deviceAddress: board.deviceAddress,
                                   version: board.version ?? "-",
                                   canUpdate: true,
                                   isInBootloader: false)
            }
            uiState = V3AccountUiState(profile: uiState.profile, boards: boards, isLoading: uiState.isLoading)
            presentation.value = .content(uiState)
            loadJob?.cancel(cause: nil)
            onEffect?(.readProfileContext)
        case .profileRequested(let serialNumber, let language):
            loadJob = loadProfile.invoke(serialNumber: serialNumber, language: language) { [weak self] result in
                DispatchQueue.main.async {
                    self?.onAction(.profileReceived(result))
                }
            }
        case .boardModeReceived(let address, let isInBootloader):
            let boards = uiState.boards.map { board in
                guard board.deviceAddress == address else { return board }
                return AccountBridgeBoard(boardName: board.boardName,
                                          deviceCode: board.deviceCode,
                                          deviceAddress: board.deviceAddress,
                                          version: board.version,
                                          canUpdate: board.canUpdate,
                                          isInBootloader: isInBootloader)
            }
            uiState = V3AccountUiState(profile: uiState.profile, boards: boards, isLoading: uiState.isLoading)
            presentation.value = .content(uiState)
        case .profileReceived(let result):
            uiState = V3AccountUiState(profile: uiState.profile, boards: uiState.boards, isLoading: false)
            presentation.value = .loading(uiState)
            if let profile = result.profile {
                uiState = V3AccountUiState(profile: profile, boards: uiState.boards, isLoading: false)
                presentation.value = .content(uiState)
            }
            if !result.isSuccess, !result.errorMessage.isEmpty {
                onEffect?(.showError(result.errorMessage))
            }
        }
    }

    deinit {
        loadJob?.cancel(cause: nil)
        boardModeJob?.cancel(cause: nil)
    }
}

final class AccountViewController: UIViewController {
    private let keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    private let makeStatisticsScreen: () -> AccountStatisticsViewController
    private let makeCustomerServiceScreen: (AccountBridgeProfile, String) -> UIViewController
    private let makeProsthesisInfoScreen: (AccountBridgeProfile, String) -> UIViewController
    private var legacyLoadJob: Kotlinx_coroutines_coreJob?
    private var boardModeJob: Kotlinx_coroutines_coreJob?
    private let viewModelV3: V3AccountViewModel?
    private let firmwareViewModelV3: V3ServiceFirmwareViewModel?
    private var legacyProfile: AccountBridgeProfile?
    private var legacyBoards: [AccountBridgeBoard] = []
    private var profile: AccountBridgeProfile? {
        if let viewModelV3 {
            return viewModelV3.uiState.profile.map { profile in
                AccountBridgeProfile(firstName: profile.firstName,
                                     lastName: profile.lastName,
                                     fullName: profile.fullName,
                                     managerName: profile.managerName,
                                     managerPhone: profile.managerPhone,
                                     prosthesisModel: profile.prosthesisModel,
                                     prosthesisSize: profile.prosthesisSize,
                                     handSide: profile.handSide,
                                     rotatorType: profile.rotatorType,
                                     touchscreenFingerPads: profile.touchscreenFingerPads,
                                     batteryType: profile.batteryType,
                                     prosthesisStatus: profile.prosthesisStatus,
                                     dateOfReceipt: profile.dateOfReceipt,
                                     warrantyExpirationDate: profile.warrantyExpirationDate)
            }
        }
        return legacyProfile
    }
    private var boards: [AccountBridgeBoard] {
        viewModelV3?.uiState.boards ?? legacyBoards
    }
    private var firmwareFileNames: [String] = []
    private lazy var firmwareUpdateController = AccountFirmwareUpdateController(
        presentingViewController: self,
        viewModelV3: firmwareViewModelV3
    )

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

    init(viewModelV3: V3AccountViewModel? = nil,
         firmwareViewModelV3: V3ServiceFirmwareViewModel? = nil,
         makeStatisticsScreen: @escaping () -> AccountStatisticsViewController,
         makeCustomerServiceScreen: @escaping (AccountBridgeProfile, String) -> UIViewController,
         makeProsthesisInfoScreen: @escaping (AccountBridgeProfile, String) -> UIViewController) {
        self.viewModelV3 = viewModelV3
        self.firmwareViewModelV3 = firmwareViewModelV3
        self.makeStatisticsScreen = makeStatisticsScreen
        self.makeCustomerServiceScreen = makeCustomerServiceScreen
        self.makeProsthesisInfoScreen = makeProsthesisInfoScreen
        super.init(nibName: nil, bundle: nil)
        hidesBottomBarWhenPushed = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        firmwareFileNames = firmwareUpdateController.availableFirmwareFileNames()
        setupView()
        bindViewModelV3()
        observeBoardMode()
        reloadAccount()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        guard view.window == nil else { return }
        firmwareUpdateController.cancelCatalogRequest()
    }

    deinit {
        legacyLoadJob?.cancel(cause: nil)
        boardModeJob?.cancel(cause: nil)
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

    private func bindViewModelV3() {
        guard let viewModelV3 else { return }
        viewModelV3.onEffect = { [weak self] effect in
            switch effect {
            case .readProfileContext:
                guard let self else { return }
                self.viewModelV3?.onAction(.profileRequested(serialNumber: self.currentSerialNumber(),
                                                            language: self.currentLanguageCode()))
            case .showError(let message): self?.showToast(message)
            }
        }
        viewModelV3.presentation.observe(on: self) { [weak self] presentation in
            guard let self, let presentation else { return }
            switch presentation {
            case .content:
                self.renderContent()
            case .loading(let state):
                if state.isLoading {
                    self.activityIndicator.startAnimating()
                } else {
                    self.activityIndicator.stopAnimating()
                    self.refreshControl.endRefreshing()
                }
            }
        }
    }

    private func observeBoardMode() {
        if let viewModelV3 {
            viewModelV3.onAction(.observationStarted)
            return
        }
        boardModeJob?.cancel(cause: nil)
        boardModeJob = AccountBridge.shared.observeBoardMode { [weak self] mode in
            DispatchQueue.main.async {
                self?.applyBoardMode(mode)
            }
        }
    }

    private func applyBoardMode(_ mode: AccountBridgeBoardMode) {
        legacyBoards = boards.map { board in
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
        firmwareFileNames = firmwareUpdateController.availableFirmwareFileNames()
        reloadAccount()
    }

    private func reloadAccount() {
        if let viewModelV3 {
            viewModelV3.onAction(.reloadRequested)
            return
        }
        if profile == nil {
            activityIndicator.startAnimating()
        }

        legacyBoards = AccountBridge.shared.currentBoards()
        renderContent()

        requestProfile()
    }

    private func requestProfile() {
        legacyLoadJob?.cancel(cause: nil)
        legacyLoadJob = AccountBridge.shared.loadAccount(
            serialNumber: currentSerialNumber(),
            lang: currentLanguageCode()
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.activityIndicator.stopAnimating()
                self?.refreshControl.endRefreshing()
                if let loadedProfile = result.profile {
                    self?.legacyProfile = loadedProfile
                    self?.renderContent()
                }
                if !result.isSuccess, !result.errorMessage.isEmpty {
                    self?.showToast(result.errorMessage)
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
                    self.makeCustomerServiceScreen(profile, self.currentSerialNumber()),
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
                    self.makeProsthesisInfoScreen(profile, self.currentSerialNumber()),
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
                guard let self else { return }
                self.navigationController?.pushViewController(
                    self.makeStatisticsScreen(),
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
                    firmwareFileNames: firmwareFileNames,
                    textColor: textColor,
                    inactiveTextColor: inactiveTextColor,
                    activeColor: activeColor
                ) { [weak self] in
                    guard let self else { return }
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
//        let storedName = (try? keyValueStorage.load(for: BluetoothStorageKeys.selectedDeviceNameStorageKey)) ?? ""
//        return DeviceNameBridgeV3.shared.displayName(deviceName: storedName)
        return "FEST-F-06879"
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
    private let statisticsViewModel: AccountStatisticsViewModelV3
    private var viewModel: GestureUsageListItemViewModel

    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)

    init(viewModel: AccountStatisticsViewModelV3) {
        self.statisticsViewModel = viewModel
        self.viewModel = viewModel.chart
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
        statisticsViewModel.onAction(.viewAppeared)
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
        statisticsViewModel.observeChart { [weak self] chart in
            guard let self else { return }
            self.viewModel = chart
            self.tableView.reloadData()
        }
    }
}

enum AccountStatisticsActionV3 {
    case viewAppeared
}

/// Owns the account statistics actions and native chart projection.
final class AccountStatisticsViewModelV3 {
    private let requestStatistics: RequestAccountStatisticsUseCaseV3
    private let observeStatistics: ObserveAccountStatisticsUseCaseV3
    private let getCustomGestureNames: GetCustomGestureNamesUseCaseV3
    private var telemetryCountersJob: Kotlinx_coroutines_coreJob?
    private(set) var chart = GestureUsageListItemViewModel(
        id: "account-gesture-usage",
        title: SharedLocalizedText.text(SharedRes.strings().gesture_usage_chart_title),
        emptyTitle: SharedLocalizedText.text(SharedRes.strings().gesture_usage_empty),
        totalTitle: AccountStatisticsViewModelV3.totalTitle,
        items: []
    )

    init(
        requestStatistics: RequestAccountStatisticsUseCaseV3,
        observeStatistics: ObserveAccountStatisticsUseCaseV3,
        getCustomGestureNames: GetCustomGestureNamesUseCaseV3
    ) {
        self.requestStatistics = requestStatistics
        self.observeStatistics = observeStatistics
        self.getCustomGestureNames = getCustomGestureNames
    }

    func onAction(_ action: AccountStatisticsActionV3) {
        switch action {
        case .viewAppeared:
            requestStatistics.invoke()
        }
    }

    func observeChart(_ onChanged: @escaping (GestureUsageListItemViewModel) -> Void) {
        telemetryCountersJob?.cancel(cause: nil)
        // Keep the native telemetry-only subscription and its initial/repeated events.
        telemetryCountersJob = observeStatistics.observeCounters { [weak self] usage in
            guard let self else { return }
            self.chart = GestureUsageListItemViewModel(
                id: "account-gesture-usage",
                title: SharedLocalizedText.text(SharedRes.strings().gesture_usage_chart_title),
                emptyTitle: SharedLocalizedText.text(SharedRes.strings().gesture_usage_empty),
                totalTitle: Self.totalTitle,
                items: self.makeItems(from: usage)
            )
            onChanged(self.chart)
        }
    }

    deinit {
        telemetryCountersJob?.cancel(cause: nil)
    }

    private func makeItems(from usage: [V3GestureUsage]) -> [GestureUsageChartItem] {
        GestureUsageChartItem.makeItems(from: usage, customNames: customGestureNames(), baseName: baseGestureName)
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
        GestureUsageChartItem.localizedCustomNames(getCustomGestureNames.invoke().names)
    }

    private static var totalTitle: String {
        Locale.current.languageCode == "ru" ? "Всего:" : "Total:"
    }

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

enum V3CustomerServiceAction {
    case viewLoaded
    case managerPhoneRequested
}

struct V3CustomerServiceUiState {
    let info: V3CustomerServiceInfo?
    let managerPhone: String?
}

final class V3CustomerServiceViewModel {
    private let getInfo: GetCustomerServiceInfoUseCaseV3
    private let getManagerPhone: GetCustomerServiceManagerPhoneUseCaseV3
    private(set) var uiState = V3CustomerServiceUiState(info: nil, managerPhone: nil)

    init(getInfo: GetCustomerServiceInfoUseCaseV3, getManagerPhone: GetCustomerServiceManagerPhoneUseCaseV3) {
        self.getInfo = getInfo
        self.getManagerPhone = getManagerPhone
    }

    func onAction(_ action: V3CustomerServiceAction) {
        switch action {
        case .viewLoaded:
            uiState = V3CustomerServiceUiState(info: getInfo.invoke(), managerPhone: uiState.managerPhone)
        case .managerPhoneRequested:
            uiState = V3CustomerServiceUiState(info: uiState.info, managerPhone: getManagerPhone.invoke())
        }
    }
}

enum V3ProsthesisInformationAction {
    case viewLoaded
}

struct V3ProsthesisInformationUiState {
    let info: V3ProsthesisInformation?
}

final class V3ProsthesisInformationViewModel {
    private let getInfo: GetProsthesisInformationUseCaseV3
    private(set) var uiState = V3ProsthesisInformationUiState(info: nil)

    init(getInfo: GetProsthesisInformationUseCaseV3) {
        self.getInfo = getInfo
    }

    func onAction(_ action: V3ProsthesisInformationAction) {
        switch action {
        case .viewLoaded:
            uiState = V3ProsthesisInformationUiState(info: getInfo.invoke())
        }
    }
}

final class AccountCustomerServiceViewController: AccountDetailsViewController {
    init(viewModel: V3CustomerServiceViewModel, topTitle: String) {
        super.init(topTitle: topTitle)
        viewModel.onAction(.viewLoaded)
        guard let info = viewModel.uiState.info else { return }
        addRows([
            .init(title: SharedLocalizedText.text(SharedRes.strings().date_of_receipt_of_prosthesis), value: info.transferDate),
            .init(title: SharedLocalizedText.text(SharedRes.strings().warranty_expiration_date), value: info.warrantyExpirationDate ?? ""),
            .init(title: SharedLocalizedText.text(SharedRes.strings().your_manager), value: info.managerName,
                  phone: info.managerPhone, phoneProvider: {
                      viewModel.onAction(.managerPhoneRequested)
                      return viewModel.uiState.managerPhone ?? ""
                  }),
            .init(title: SharedLocalizedText.text(SharedRes.strings().prosthesis_status), value: info.prosthesisStatus)
        ])
    }

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

final class AccountProsthesisInfoViewController: AccountDetailsViewController {
    init(viewModel: V3ProsthesisInformationViewModel, topTitle: String) {
        super.init(topTitle: topTitle)
        viewModel.onAction(.viewLoaded)
        guard let info = viewModel.uiState.info else { return }
        addRows([
            .init(title: SharedLocalizedText.text(SharedRes.strings().prosthesis_model), value: info.prosthesisModel),
            .init(title: SharedLocalizedText.text(SharedRes.strings().prosthesis_size), value: info.prosthesisSize),
            .init(title: SharedLocalizedText.text(SharedRes.strings().hand_side_2), value: info.handSide),
            .init(title: SharedLocalizedText.text(SharedRes.strings().rotator_type), value: info.rotatorType),
            .init(title: SharedLocalizedText.text(SharedRes.strings().touchscreen_finger_pads), value: info.touchscreenFingerPads),
            .init(title: SharedLocalizedText.text(SharedRes.strings().battery_type), value: info.batteryType)
        ])
    }

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

class AccountDetailsViewController: UIViewController {
    struct DetailRow {
        let title: String
        let value: String
        var phone: String?
        var phoneProvider: (() -> String)? = nil
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
                    phoneProvider: row.phoneProvider,
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
    init(title: String, value: String, phone: String? = nil, phoneProvider: (() -> String)? = nil, textColor: UIColor) {
        super.init(frame: .zero)
        setup(title: title, value: value, phone: phone, phoneProvider: phoneProvider, textColor: textColor)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(title: String, value: String, phone: String?, phoneProvider: (() -> String)?, textColor: UIColor) {
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
                guard let url = PhoneDialURLFormatter.dialURL(from: phoneProvider?() ?? phone) else { return }
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
        firmwareFileNames: [String],
        textColor: UIColor,
        inactiveTextColor: UIColor,
        activeColor: UIColor,
        updateAction: @escaping () -> Void
    ) {
        super.init(frame: .zero)
        setup(
            board: board,
            firmwareFileNames: firmwareFileNames,
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
        firmwareFileNames: [String],
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
        addSubview(versionLabel)

        let bootloaderLabel = UILabel()
        bootloaderLabel.text = "▷"
        bootloaderLabel.accessibilityIdentifier = "\(AccessibilityIdentifier.accountBoardBootloaderPrefix).\(boardKey)"
        bootloaderLabel.accessibilityValue = board.isInBootloader ? "bootloader=true" : "bootloader=false"
        bootloaderLabel.font = .accountOpenSansSemibold(size: 12)
        bootloaderLabel.textColor = textColor
        bootloaderLabel.isHidden = !board.isInBootloader
        bootloaderLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(bootloaderLabel)

        let updateButton = UIButton(type: .system)
        updateButton.setTitle(SharedLocalizedText.text(SharedRes.strings().update_firmware), for: .normal)
        updateButton.accessibilityIdentifier = "\(AccessibilityIdentifier.accountBoardUpdateButtonPrefix).\(boardKey)"
        updateButton.titleLabel?.font = .accountOpenSansSemibold(size: 12)
        let highlight = FirmwareVersionCatalog.shared.shouldHighlightUpdate(
            boardName: board.boardName,
            deviceVersion: board.version,
            fileNames: firmwareFileNames
        )
        updateButton.setTitleColor(board.canUpdate ? (highlight ? activeColor : textColor) : inactiveTextColor, for: .normal)
        updateButton.accessibilityValue = "board=\(board.boardName);version=\(board.version);updateAvailable=\(highlight);enabled=\(board.canUpdate)"
        updateButton.isEnabled = board.canUpdate
        updateButton.addAction(UIAction { _ in updateAction() }, for: .touchUpInside)
        updateButton.translatesAutoresizingMaskIntoConstraints = false
        addSubview(updateButton)

        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: versionLabel.leadingAnchor, constant: -12),

            updateButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            updateButton.centerYAnchor.constraint(equalTo: centerYAnchor),

            bootloaderLabel.trailingAnchor.constraint(equalTo: updateButton.leadingAnchor, constant: -12),
            bootloaderLabel.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),

            versionLabel.trailingAnchor.constraint(equalTo: bootloaderLabel.leadingAnchor, constant: -12),
            versionLabel.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor)
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
