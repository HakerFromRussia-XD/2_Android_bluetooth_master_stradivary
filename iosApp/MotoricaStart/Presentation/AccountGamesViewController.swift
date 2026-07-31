import SwiftUI
import UIKit

private enum AccountGamesMetrics {
    static let sideInset: CGFloat = 16
    static let cardHeight: CGFloat = 150
    static let cardRadius: CGFloat = 12
    static let actionHeight: CGFloat = 42
}

final class AccountGamesViewController: UIViewController {
    private enum Constants {
        static let appGroup = "group.com.motorica.start.gamecontroll"
        static let installedGameKey = "installedGame.stk"
        static let manifestUrlInfoKey = "MotoricaGamesManifestURL"
        static let gameId = "stk"
        static let fallbackTitle = "Super Tux Kart"
        static let fallbackBundleId = "com.motorica.games.stk"
        static let fallbackScheme = "motorica-stk"
    }

    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
    private let cardColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
    private let borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444)
    private let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
    private let inactiveTextColor = UIColor.accountColor("ubi4_deactivate_text", fallback: 0x838383)

    private let scrollView = UIScrollView()
    private let contentView = UIView()
    private let cardView = UIView()
    private let titleLabel = UILabel()
    private let statusLabel = UILabel()
    private let actionButton = UIButton(type: .system)
    private let activityIndicator = UIActivityIndicatorView(style: .medium)
    private var statusBarHostingController: UIHostingController<StatusBarView>?

    private var remoteGame: RemoteIosGame?
    private var manifestLoadFailed = false
    private var manifestFailedBecauseOffline = false
    private var currentAction: GameAction = .unavailable
    private let gameDebugLogPrefix = "[BLE stk-game debug]"

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
        renderState()
        loadManifest()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
        renderState()
    }

    private func setupView() {
        view.backgroundColor = backgroundColor
        view.accessibilityIdentifier = AccessibilityIdentifier.accountGamesRoot
        setupTopBar()
        setupScrollView()
        setupGameCard()
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
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scrollView)

        contentView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(contentView)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: statusBarView.bottomAnchor, constant: 8),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            contentView.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.frameLayoutGuide.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.frameLayoutGuide.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor, constant: -16)
        ])
    }

    private func setupGameCard() {
        let sectionTitle = UILabel()
        sectionTitle.text = localized("motorica_games")
        sectionTitle.font = .accountGamesInterSemibold(size: 14)
        sectionTitle.textColor = textColor
        sectionTitle.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(sectionTitle)

        cardView.backgroundColor = cardColor
        cardView.layer.cornerRadius = AccountGamesMetrics.cardRadius
        cardView.layer.borderWidth = 1
        cardView.layer.borderColor = borderColor.cgColor
        cardView.layer.masksToBounds = true
        cardView.accessibilityIdentifier = AccessibilityIdentifier.accountGamesCard
        cardView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(cardView)

        let backgroundImageView = UIImageView(image: UIImage(named: "motorica_stk_card_background"))
        backgroundImageView.contentMode = .scaleAspectFill
        backgroundImageView.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(backgroundImageView)

        let dimView = UIView()
        dimView.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        dimView.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(dimView)

        titleLabel.text = Constants.fallbackTitle
        titleLabel.font = .accountGamesOpenSansSemibold(size: 16)
        titleLabel.textColor = textColor
        titleLabel.numberOfLines = 1
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(titleLabel)

        statusLabel.font = .accountGamesOpenSansRegular(size: 13)
        statusLabel.textColor = inactiveTextColor
        statusLabel.numberOfLines = 2
        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(statusLabel)

        actionButton.titleLabel?.font = .accountGamesOpenSansSemibold(size: 13)
        actionButton.setTitleColor(textColor, for: .normal)
        actionButton.setTitleColor(inactiveTextColor, for: .disabled)
        actionButton.backgroundColor = cardColor
        actionButton.layer.cornerRadius = 10
        actionButton.layer.borderWidth = 1
        actionButton.layer.borderColor = borderColor.cgColor
        actionButton.accessibilityIdentifier = AccessibilityIdentifier.accountGamesActionButton
        actionButton.addTarget(self, action: #selector(handleActionTap), for: .touchUpInside)
        actionButton.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(actionButton)

        activityIndicator.color = textColor
        activityIndicator.hidesWhenStopped = true
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        cardView.addSubview(activityIndicator)

        NSLayoutConstraint.activate([
            sectionTitle.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 16),
            sectionTitle.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: AccountGamesMetrics.sideInset),
            sectionTitle.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -AccountGamesMetrics.sideInset),

            cardView.topAnchor.constraint(equalTo: sectionTitle.bottomAnchor, constant: 16),
            cardView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: AccountGamesMetrics.sideInset),
            cardView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -AccountGamesMetrics.sideInset),
            cardView.heightAnchor.constraint(equalToConstant: AccountGamesMetrics.cardHeight),
            cardView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),

            backgroundImageView.topAnchor.constraint(equalTo: cardView.topAnchor),
            backgroundImageView.leadingAnchor.constraint(equalTo: cardView.leadingAnchor),
            backgroundImageView.trailingAnchor.constraint(equalTo: cardView.trailingAnchor),
            backgroundImageView.bottomAnchor.constraint(equalTo: cardView.bottomAnchor),

            dimView.topAnchor.constraint(equalTo: cardView.topAnchor),
            dimView.leadingAnchor.constraint(equalTo: cardView.leadingAnchor),
            dimView.trailingAnchor.constraint(equalTo: cardView.trailingAnchor),
            dimView.bottomAnchor.constraint(equalTo: cardView.bottomAnchor),

            titleLabel.leadingAnchor.constraint(equalTo: cardView.leadingAnchor, constant: 20),
            titleLabel.topAnchor.constraint(equalTo: cardView.topAnchor, constant: 28),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: actionButton.leadingAnchor, constant: -16),

            statusLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            statusLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10),
            statusLabel.trailingAnchor.constraint(lessThanOrEqualTo: actionButton.leadingAnchor, constant: -16),

            actionButton.trailingAnchor.constraint(equalTo: cardView.trailingAnchor, constant: -20),
            actionButton.centerYAnchor.constraint(equalTo: cardView.centerYAnchor),
            actionButton.widthAnchor.constraint(equalToConstant: 156),
            actionButton.heightAnchor.constraint(equalToConstant: AccountGamesMetrics.actionHeight),

            activityIndicator.centerXAnchor.constraint(equalTo: actionButton.centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: actionButton.centerYAnchor)
        ])
    }

    private func loadManifest() {
        guard let manifestUrl = manifestUrl() else {
            manifestLoadFailed = true
            renderState()
            return
        }

        activityIndicator.startAnimating()
        resolveManifestUrl(manifestUrl) { [weak self] result in
            switch result {
            case .success(let url):
                self?.downloadManifest(from: url)
            case .failure(let error):
                DispatchQueue.main.async {
                    self?.activityIndicator.stopAnimating()
                    self?.handleManifestFailure(error)
                }
            }
        }
    }

    private func downloadManifest(from url: URL) {
        URLSession.shared.dataTask(with: url) { [weak self] data, _, error in
            if let error {
                DispatchQueue.main.async {
                    self?.activityIndicator.stopAnimating()
                    self?.handleManifestFailure(error)
                }
                return
            }

            guard let data else {
                DispatchQueue.main.async {
                    self?.activityIndicator.stopAnimating()
                    self?.handleManifestFailure(URLError(.badServerResponse))
                }
                return
            }

            do {
                let manifest = try JSONDecoder().decode(GamesManifest.self, from: data)
                let game = manifest.games.first { $0.id == Constants.gameId }
                DispatchQueue.main.async {
                    self?.activityIndicator.stopAnimating()
                    self?.remoteGame = game?.ios
                    self?.titleLabel.text = game?.title ?? Constants.fallbackTitle
                    self?.manifestLoadFailed = false
                    self?.manifestFailedBecauseOffline = false
                    self?.renderState()
                }
            } catch {
                DispatchQueue.main.async {
                    self?.activityIndicator.stopAnimating()
                    self?.handleManifestFailure(error)
                }
            }
        }.resume()
    }

    private func handleManifestFailure(_ error: Error) {
        manifestLoadFailed = true
        manifestFailedBecauseOffline = (error as? URLError)?.code == .notConnectedToInternet
        renderState()
    }

    private func renderState() {
        let installed = isGameInstalled()
        let installedGame = installedGameInfo()

        if installed, let remoteGame, let installedGame, installedGame.versionCode > 0,
           remoteGame.versionCode > installedGame.versionCode {
            statusLabel.text = localized("game_status_update_available")
            if remoteGame.appStoreUrl.isEmpty {
                setAction(.unavailableInstall)
            } else {
                setAction(.update(remoteGame))
            }
            return
        }

        if installed {
            statusLabel.text = localized("game_status_installed")
            setAction(.play(remoteGame?.urlScheme ?? Constants.fallbackScheme))
            return
        }

        if manifestLoadFailed && manifestFailedBecauseOffline {
            statusLabel.text = localized("game_status_catalog_unavailable")
            setAction(.unavailable)
            return
        }

        statusLabel.text = localized("game_status_available")
        if let remoteGame, !remoteGame.appStoreUrl.isEmpty {
            setAction(.install(remoteGame))
        } else {
            setAction(.unavailableInstall)
        }
    }

    private func setAction(_ action: GameAction) {
        currentAction = action
        activityIndicator.isHidden = true
        actionButton.isHidden = false

        switch action {
        case .install:
            actionButton.setTitle(localized("install_game"), for: .normal)
            actionButton.isEnabled = true
        case .update:
            actionButton.setTitle(localized("update_game"), for: .normal)
            actionButton.isEnabled = true
        case .play:
            actionButton.setTitle(localized("play_game"), for: .normal)
            actionButton.isEnabled = true
        case .unavailableInstall:
            actionButton.setTitle(localized("install_game"), for: .normal)
            actionButton.isEnabled = false
        case .unavailable:
            actionButton.setTitle(localized("install_game"), for: .normal)
            actionButton.isEnabled = false
        }
    }

    @objc private func handleActionTap() {
        switch currentAction {
        case .install(let game), .update(let game):
            openAppStore(for: game)
        case .play(let scheme):
            openGame(scheme: scheme)
        case .unavailableInstall:
            showToast(localized("game_app_store_url_missing"))
        case .unavailable:
            break
        }
    }

    private func openAppStore(for game: RemoteIosGame) {
        guard let url = URL(string: game.appStoreUrl), !game.appStoreUrl.isEmpty else {
            showToast(localized("game_app_store_url_missing"))
            return
        }
        UIApplication.shared.open(url)
    }

    private func openGame(scheme: String) {
        GameControlBroadcaster.shared.start()
        guard let url = URL(string: "\(scheme)://launch") else {
            showToast(localized("game_launch_failed"))
            return
        }
        UIApplication.shared.open(url) { [weak self] opened in
            if !opened {
                self?.showToast(self?.localized("game_launch_failed") ?? "")
            }
        }
    }

    private func isGameInstalled() -> Bool {
        let scheme = remoteGame?.urlScheme ?? Constants.fallbackScheme
        guard let url = URL(string: "\(scheme)://launch") else {
            NSLog("\(gameDebugLogPrefix) ios games invalid launch url scheme=\(scheme)")
            return false
        }

        let canOpen = UIApplication.shared.canOpenURL(url)
        let hasMarker = installedGameInfo() != nil
        NSLog("\(gameDebugLogPrefix) ios games installed check scheme=\(scheme) canOpenURL=\(canOpen ? 1 : 0) appGroupMarker=\(hasMarker ? 1 : 0)")
        return canOpen || hasMarker
    }

    private func installedGameInfo() -> InstalledGameInfo? {
        guard let defaults = UserDefaults(suiteName: Constants.appGroup) else {
            NSLog("\(gameDebugLogPrefix) ios games app group unavailable: \(Constants.appGroup)")
            return nil
        }

        guard let dictionary = defaults.dictionary(forKey: Constants.installedGameKey) else {
            NSLog("\(gameDebugLogPrefix) ios games app group marker missing key=\(Constants.installedGameKey)")
            return nil
        }

        let bundleId = dictionary["bundleId"] as? String
        let expectedBundleId = remoteGame?.bundleId ?? Constants.fallbackBundleId
        if let bundleId, bundleId != expectedBundleId {
            NSLog("\(gameDebugLogPrefix) ios games app group marker bundle mismatch actual=\(bundleId) expected=\(expectedBundleId)")
            return nil
        }

        let versionName = dictionary["versionName"] as? String ?? ""
        let versionCode: Int64
        if let number = dictionary["versionCode"] as? NSNumber {
            versionCode = number.int64Value
        } else if let string = dictionary["versionCode"] as? String {
            versionCode = Int64(string) ?? 0
        } else {
            versionCode = 0
        }
        NSLog("\(gameDebugLogPrefix) ios games app group marker found versionName=\(versionName) versionCode=\(versionCode)")
        return InstalledGameInfo(versionName: versionName, versionCode: versionCode)
    }

    private func manifestUrl() -> URL? {
        guard let value = Bundle.main.object(forInfoDictionaryKey: Constants.manifestUrlInfoKey) as? String else {
            return nil
        }
        return URL(string: value)
    }

    private func resolveManifestUrl(_ url: URL, completion: @escaping (Result<URL, Error>) -> Void) {
        guard let host = url.host, host.contains("yandex") || host.contains("yadi.sk") else {
            completion(.success(url))
            return
        }

        var sourceComponents = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let path = sourceComponents?.queryItems?.first { $0.name == "path" }?.value
        sourceComponents?.query = nil
        sourceComponents?.fragment = nil
        let publicKey = sourceComponents?.url?.absoluteString ?? url.absoluteString

        var apiComponents = URLComponents(string: "https://cloud-api.yandex.net/v1/disk/public/resources/download")
        var queryItems = [URLQueryItem(name: "public_key", value: publicKey)]
        if let path, !path.isEmpty {
            queryItems.append(URLQueryItem(name: "path", value: path))
        }
        apiComponents?.queryItems = queryItems

        guard let apiUrl = apiComponents?.url else {
            completion(.failure(URLError(.badURL)))
            return
        }

        URLSession.shared.dataTask(with: apiUrl) { data, _, error in
            if let error {
                completion(.failure(error))
                return
            }
            guard let data else {
                completion(.failure(URLError(.badServerResponse)))
                return
            }
            do {
                let response = try JSONDecoder().decode(YandexDownloadResponse.self, from: data)
                guard let href = URL(string: response.href) else {
                    completion(.failure(URLError(.badURL)))
                    return
                }
                completion(.success(href))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    private func localized(_ key: String) -> String {
        NSLocalizedString(key, comment: "")
    }
}

private struct GamesManifest: Decodable {
    let games: [RemoteGame]
}

private struct RemoteGame: Decodable {
    let id: String
    let title: String?
    let ios: RemoteIosGame?
}

private struct RemoteIosGame: Decodable {
    let bundleId: String
    let urlScheme: String
    let appStoreUrl: String
    let versionName: String
    let versionCode: Int64
}

private struct InstalledGameInfo {
    let versionName: String
    let versionCode: Int64
}

private struct YandexDownloadResponse: Decodable {
    let href: String
}

private enum GameAction {
    case install(RemoteIosGame)
    case update(RemoteIosGame)
    case play(String)
    case unavailableInstall
    case unavailable
}

private extension UIFont {
    static func accountGamesInterSemibold(size: CGFloat) -> UIFont {
        UIFont(name: "Inter-SemiBold", size: size)
            ?? UIFont(name: "Inter-Regular", size: size)
            ?? .systemFont(ofSize: size, weight: .semibold)
    }

    static func accountGamesOpenSansRegular(size: CGFloat) -> UIFont {
        UIFont(name: "OpenSans-Regular", size: size)
            ?? .systemFont(ofSize: size, weight: .regular)
    }

    static func accountGamesOpenSansSemibold(size: CGFloat) -> UIFont {
        UIFont(name: "OpenSansRoman-SemiBold", size: size)
            ?? UIFont(name: "OpenSans-Regular", size: size)
            ?? .systemFont(ofSize: size, weight: .semibold)
    }
}
