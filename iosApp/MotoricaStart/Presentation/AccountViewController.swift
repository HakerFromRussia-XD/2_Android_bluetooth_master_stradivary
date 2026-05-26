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
    private let keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    private var loadJob: Kotlinx_coroutines_coreJob?
    private var boardModeJob: Kotlinx_coroutines_coreJob?
    private var profile: AccountBridgeProfile?
    private var boards: [AccountBridgeBoard] = []
    private var firmwareFileNames: [String] = []
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
        firmwareFileNames = firmwareUpdateController.availableFirmwareFileNames()
        setupView()
        observeBoardMode()
        reloadAccount()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    deinit {
        loadJob?.cancel(cause: nil)
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

    private func observeBoardMode() {
        boardModeJob?.cancel(cause: nil)
        boardModeJob = AccountBridge.shared.observeBoardMode { [weak self] mode in
            DispatchQueue.main.async {
                self?.applyBoardMode(mode)
            }
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
        firmwareFileNames = firmwareUpdateController.availableFirmwareFileNames()
        reloadAccount()
    }

    private func reloadAccount() {
        if profile == nil {
            activityIndicator.startAnimating()
        }

        boards = AccountBridge.shared.currentBoards()
        renderContent()

        loadJob?.cancel(cause: nil)
        loadJob = AccountBridge.shared.loadAccount(
            serialNumber: currentSerialNumber(),
            lang: currentLanguageCode()
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.activityIndicator.stopAnimating()
                self?.refreshControl.endRefreshing()
                if let loadedProfile = result.profile {
                    self?.profile = loadedProfile
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

        addSectionTitle(NSLocalizedString("General", comment: ""))
        addCard(makeGeneralSection())
        addSectionTitle(NSLocalizedString("Software information", comment: ""), topInset: AccountMetrics.sectionSpacing)
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
                title: NSLocalizedString("Customer Service", comment: ""),
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
                title: NSLocalizedString("Prosthesis Information", comment: ""),
                textColor: textColor
            ) { [weak self] in
                guard let self, let profile = self.profile else { return }
                self.navigationController?.pushViewController(
                    AccountProsthesisInfoViewController(profile: profile, topTitle: self.currentSerialNumber()),
                    animated: true
                )
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

private final class AccountCustomerServiceViewController: AccountDetailsViewController {
    init(profile: AccountBridgeProfile, topTitle: String) {
        super.init(topTitle: topTitle)
        addRows([
            .init(title: NSLocalizedString("Date of receipt of prosthesis", comment: ""), value: profile.dateOfReceipt),
            .init(title: NSLocalizedString("Warranty expiration date", comment: ""), value: profile.warrantyExpirationDate),
            .init(title: NSLocalizedString("Your manager", comment: ""), value: profile.managerName, phone: profile.managerPhone),
            .init(title: NSLocalizedString("Prosthesis status", comment: ""), value: profile.prosthesisStatus)
        ])
    }
}

private final class AccountProsthesisInfoViewController: AccountDetailsViewController {
    init(profile: AccountBridgeProfile, topTitle: String) {
        super.init(topTitle: topTitle)
        addRows([
            .init(title: NSLocalizedString("Prosthesis model", comment: ""), value: profile.prosthesisModel),
            .init(title: NSLocalizedString("Prosthesis size", comment: ""), value: profile.prosthesisSize),
            .init(title: NSLocalizedString("Hand side", comment: ""), value: profile.handSide),
            .init(title: NSLocalizedString("Rotator type", comment: ""), value: profile.rotatorType),
            .init(title: NSLocalizedString("Touchscreen finger pads", comment: ""), value: profile.touchscreenFingerPads),
            .init(title: NSLocalizedString("Battery type", comment: ""), value: profile.batteryType)
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

    init(iconName: String, title: String, textColor: UIColor, action: @escaping () -> Void) {
        self.action = action
        super.init(frame: .zero)
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
        titleLabel.text = NSLocalizedString("Version App", comment: "")
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
        updateButton.setTitle(NSLocalizedString("Update", comment: ""), for: .normal)
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
    func showToast(_ message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        present(alert, animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            alert.dismiss(animated: true)
        }
    }
}
