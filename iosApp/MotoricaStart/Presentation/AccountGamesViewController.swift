import SwiftUI
import UIKit

private enum AccountGamesMetrics {
    static let sideInset: CGFloat = 16
    static let cardHeight: CGFloat = 150
    static let cardRadius: CGFloat = 12
    static let actionHeight: CGFloat = 42
}

final class AccountGamesViewController: UIViewController, UITableViewDataSource {
    private enum Constants {
        static let manifestUrlInfoKey = "MotoricaGamesManifestURL"
    }

    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
    private let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
    private let tableView = UITableView(frame: .zero, style: .plain)
    private let activityIndicator = UIActivityIndicatorView(style: .medium)
    private var statusBarHostingController: UIHostingController<StatusBarView>?
    private var games = AccountGame.defaults
    private var states: [String: AccountGameState] = [:]
    private var catalogUnavailable = false
    private var manifestTask: URLSessionDataTask?
    private var isLoadingManifest = false
    private let querySchemes = Set((Bundle.main.object(forInfoDictionaryKey: "LSApplicationQueriesSchemes") as? [String] ?? [])
        .map { $0.lowercased() })

    init() {
        super.init(nibName: nil, bundle: nil)
        hidesBottomBarWhenPushed = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        manifestTask?.cancel()
        NotificationCenter.default.removeObserver(self)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupView()
        NotificationCenter.default.addObserver(self, selector: #selector(applicationBecameActive),
                                               name: UIApplication.didBecomeActiveNotification, object: nil)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
        refreshStates()
        loadManifest()
    }

    @objc private func applicationBecameActive() {
        guard isViewLoaded, view.window != nil else { return }
        refreshStates()
        loadManifest()
    }

    private func setupView() {
        view.backgroundColor = backgroundColor
        view.accessibilityIdentifier = AccessibilityIdentifier.accountGamesRoot
        let hostingController = UIHostingController(
            rootView: StatusBarView(
                viewModel: WidgetsTabContainerViewController.sharedStatusBarViewModel,
                leadingButton: .back,
                onBackTap: { [weak self] in self?.navigationController?.popViewController(animated: true) },
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
        hostingController.didMove(toParent: self)

        let sectionTitle = UILabel()
        sectionTitle.text = localized("motorica_games")
        sectionTitle.font = .accountGamesInterSemibold(size: 14)
        sectionTitle.textColor = textColor
        sectionTitle.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(sectionTitle)
        activityIndicator.color = textColor
        activityIndicator.hidesWhenStopped = true
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(activityIndicator)

        tableView.backgroundColor = backgroundColor
        tableView.separatorStyle = .none
        tableView.rowHeight = AccountGamesMetrics.cardHeight + 16
        tableView.contentInset.bottom = 16
        tableView.dataSource = self
        tableView.register(AccountGameCardCell.self, forCellReuseIdentifier: AccountGameCardCell.reuseIdentifier)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
        NSLayoutConstraint.activate([
            hostingController.view.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            hostingController.view.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            hostingController.view.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            hostingController.view.heightAnchor.constraint(equalToConstant: StatusBarView.Constants.height),
            sectionTitle.topAnchor.constraint(equalTo: hostingController.view.bottomAnchor, constant: 24),
            sectionTitle.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: AccountGamesMetrics.sideInset),
            activityIndicator.centerYAnchor.constraint(equalTo: sectionTitle.centerYAnchor),
            activityIndicator.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -AccountGamesMetrics.sideInset),
            tableView.topAnchor.constraint(equalTo: sectionTitle.bottomAnchor, constant: 16),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { games.count }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: AccountGameCardCell.reuseIdentifier,
                                                for: indexPath) as! AccountGameCardCell
        let game = games[indexPath.row]
        let state = states[game.bundleId] ?? state(for: game)
        cell.configure(game: game, state: state) { [weak self] in self?.handleAction(for: game.bundleId) }
        return cell
    }

    private func state(for game: AccountGame) -> AccountGameState {
        let canQuery = querySchemes.contains(game.urlScheme.lowercased())
        let installed = canQuery && (game.launchURL.map(UIApplication.shared.canOpenURL) ?? false)
        return AccountGameState.resolve(game: game, canOpenURL: installed,
                                        installedVersion: installed ? installedVersion(for: game) : nil,
                                        catalogUnavailable: catalogUnavailable, canQueryInstallation: canQuery)
    }

    private func refreshStates() {
        states = Dictionary(uniqueKeysWithValues: games.map { ($0.bundleId, state(for: $0)) })
        tableView.reloadData()
    }

    private func handleAction(for bundleId: String) {
        guard let game = games.first(where: { $0.bundleId == bundleId }) else { return }
        // Recheck on tap so an old visible state cannot launch a removed game.
        let current = state(for: game)
        states[bundleId] = current
        tableView.reloadData()
        switch current.action {
        case .install(let url), .update(let url):
            guard let url else {
                showToast(localized("game_app_store_url_missing"))
                return
            }
            UIApplication.shared.open(url)
        case .play:
            openGame(game)
        case .open:
            openGame(game, fallbackToStore: true)
        }
    }

    private func loadManifest() {
        guard !isLoadingManifest else { return }
        guard let url = manifestUrl() else {
            catalogUnavailable = true
            refreshStates()
            return
        }
        isLoadingManifest = true
        activityIndicator.startAnimating()
        resolveManifestUrl(url) { [weak self] result in
            DispatchQueue.main.async {
                guard let self else { return }
                switch result {
                case .success(let resolvedURL): self.downloadManifest(from: resolvedURL)
                case .failure: self.finishManifestLoad(nil)
                }
            }
        }
    }

    private func downloadManifest(from url: URL) {
        manifestTask = URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            let manifest: AccountGamesManifest?
            if error == nil, let response = response as? HTTPURLResponse,
               (200..<300).contains(response.statusCode), let data {
                manifest = try? JSONDecoder().decode(AccountGamesManifest.self, from: data)
            } else {
                manifest = nil
            }
            DispatchQueue.main.async { self?.finishManifestLoad(manifest) }
        }
        manifestTask?.resume()
    }

    private func finishManifestLoad(_ manifest: AccountGamesManifest?) {
        isLoadingManifest = false
        manifestTask = nil
        activityIndicator.stopAnimating()
        catalogUnavailable = manifest == nil
        // Keep the last successful metadata during transient network failures.
        if let manifest { games = manifest.catalog() }
        refreshStates()
    }

    private func installedVersion(for game: AccountGame) -> Int64? {
        guard let appGroup = MotoricaGameControlAppGroup.value,
              let defaults = UserDefaults(suiteName: appGroup),
              let marker = defaults.dictionary(forKey: game.installedVersionKey),
              marker["bundleId"] as? String == game.bundleId else { return nil }
        if let number = marker["versionCode"] as? NSNumber { return number.int64Value }
        return (marker["versionCode"] as? String).flatMap(Int64.init)
    }

    private func openGame(_ game: AccountGame, fallbackToStore: Bool = false) {
        guard let url = game.launchURL else { return }
        let launchToken = MotoricaGameLaunchRequest.create(scheme: game.urlScheme)
        if game.requiresLaunchToken && launchToken == nil {
            showToast(localized("game_launch_failed"))
            return
        }
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        if let launchToken { components?.queryItems = [URLQueryItem(name: "token", value: launchToken)] }
        guard let launchURL = components?.url else {
            MotoricaGameLaunchRequest.clear(token: launchToken)
            return
        }
        GameControlBroadcaster.shared.start()
        UIApplication.shared.open(launchURL) { [weak self] opened in
            if !opened {
                MotoricaGameLaunchRequest.clear(token: launchToken)
                GameControlBroadcaster.shared.stop()
                self?.refreshStates()
                if fallbackToStore, let storeURL = game.storeURL {
                    UIApplication.shared.open(storeURL)
                } else {
                    self?.showToast(self?.localized(fallbackToStore ? "game_app_store_url_missing" : "game_launch_failed") ?? "")
                }
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                MotoricaGameLaunchRequest.clear(token: launchToken)
            }
        }
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

    private func localized(_ key: String) -> String { NSLocalizedString(key, comment: "") }
}

private struct YandexDownloadResponse: Decodable { let href: String }

private final class AccountGameCardCell: UITableViewCell {
    static let reuseIdentifier = "AccountGameCardCell"
    private let card = UIView()
    private let backgroundImageView = UIImageView()
    private let titleLabel = UILabel()
    private let statusLabel = UILabel()
    private let actionButton = UIButton(type: .system)
    private var onAction: (() -> Void)?

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        backgroundColor = .clear
        let cardColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
        let borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444)
        let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
        let inactiveTextColor = UIColor.accountColor("ubi4_deactivate_text", fallback: 0x838383)
        card.backgroundColor = cardColor
        card.layer.cornerRadius = AccountGamesMetrics.cardRadius
        card.layer.borderWidth = 1
        card.layer.borderColor = borderColor.cgColor
        card.layer.masksToBounds = true
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)
        backgroundImageView.contentMode = .scaleAspectFill
        let dimView = UIView()
        dimView.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        titleLabel.font = .accountGamesOpenSansSemibold(size: 16)
        titleLabel.textColor = textColor
        statusLabel.font = .accountGamesOpenSansRegular(size: 13)
        statusLabel.textColor = inactiveTextColor
        statusLabel.numberOfLines = 2
        actionButton.titleLabel?.font = .accountGamesOpenSansSemibold(size: 13)
        actionButton.setTitleColor(textColor, for: .normal)
        actionButton.setTitleColor(inactiveTextColor, for: .disabled)
        actionButton.backgroundColor = cardColor
        actionButton.layer.cornerRadius = 10
        actionButton.layer.borderWidth = 1
        actionButton.layer.borderColor = borderColor.cgColor
        actionButton.addTarget(self, action: #selector(tapped), for: .touchUpInside)
        for subview in [backgroundImageView, dimView, titleLabel, statusLabel, actionButton] {
            subview.translatesAutoresizingMaskIntoConstraints = false
            card.addSubview(subview)
        }
        NSLayoutConstraint.activate([
            card.topAnchor.constraint(equalTo: contentView.topAnchor),
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: AccountGamesMetrics.sideInset),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -AccountGamesMetrics.sideInset),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -16),
            backgroundImageView.topAnchor.constraint(equalTo: card.topAnchor),
            backgroundImageView.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            backgroundImageView.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            backgroundImageView.bottomAnchor.constraint(equalTo: card.bottomAnchor),
            dimView.topAnchor.constraint(equalTo: card.topAnchor),
            dimView.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            dimView.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            dimView.bottomAnchor.constraint(equalTo: card.bottomAnchor),
            titleLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 20),
            titleLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 28),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: actionButton.leadingAnchor, constant: -16),
            statusLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            statusLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10),
            statusLabel.trailingAnchor.constraint(lessThanOrEqualTo: actionButton.leadingAnchor, constant: -16),
            actionButton.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -20),
            actionButton.centerYAnchor.constraint(equalTo: card.centerYAnchor),
            actionButton.widthAnchor.constraint(equalToConstant: 156),
            actionButton.heightAnchor.constraint(equalToConstant: AccountGamesMetrics.actionHeight)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func prepareForReuse() {
        super.prepareForReuse()
        onAction = nil
    }

    func configure(game: AccountGame, state: AccountGameState, onAction: @escaping () -> Void) {
        titleLabel.text = game.title
        backgroundImageView.image = UIImage(named: game.artwork)
        statusLabel.text = NSLocalizedString(state.statusKey, comment: "")
        actionButton.setTitle(NSLocalizedString(state.action.titleKey, comment: ""), for: .normal)
        actionButton.isEnabled = state.action.isEnabled
        card.accessibilityIdentifier = game.accessibilityPrefix + (game.accessibilityPrefix.hasSuffix(".") ? "card" : "Card")
        actionButton.accessibilityIdentifier = game.accessibilityPrefix + (game.accessibilityPrefix.hasSuffix(".") ? "action" : "ActionButton")
        self.onAction = onAction
    }

    @objc private func tapped() { onAction?() }
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
