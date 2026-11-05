import Foundation

struct AccountGame: Equatable {
    let id: String
    let title: String
    let bundleId: String
    let urlScheme: String
    let artwork: String
    let accessibilityPrefix: String
    let requiresLaunchToken: Bool
    var storeURL: URL?
    var versionCode: Int64?

    var installedVersionKey: String { "installedGame.\(id)" }

    var launchURL: URL? {
        guard urlScheme.range(of: "^[A-Za-z][A-Za-z0-9+.-]*$", options: .regularExpression) != nil else {
            return nil
        }
        var components = URLComponents()
        components.scheme = urlScheme
        components.host = "launch"
        return components.url
    }

    static let defaults: [AccountGame] = [
        AccountGame(id: "fluxara_drift", title: "Fluxara Drift", bundleId: "io.fluxara.drift",
                    urlScheme: "fluxara-drive", artwork: "motorica_stk_card_background",
                    accessibilityPrefix: "AccessibilityIdentifierAccountGames", requiresLaunchToken: false),
        AccountGame(id: "sushidash", title: "Sushi Dash", bundleId: "com.hakerfromrussia.trashdash",
                    urlScheme: "sushi-dash", artwork: "motorica_sushi_card_background_v2",
                    accessibilityPrefix: "account.games.sushi.", requiresLaunchToken: true)
    ]
}

enum AccountGameAction: Equatable {
    case install(URL?)
    case update(URL?)
    case play(URL)
    case open(URL)

    var titleKey: String {
        switch self {
        case .install: return "download_game"
        case .update: return "update_game"
        case .play: return "play_game"
        case .open: return "open_game"
        }
    }

    var isEnabled: Bool {
        switch self {
        case .install(let url), .update(let url): return url != nil
        case .play, .open: return true
        }
    }
}

struct AccountGameState: Equatable {
    let action: AccountGameAction
    let statusKey: String

    // Installation is verified by the OS. App-group markers can survive uninstall;
    // they supply version metadata only after the OS confirms the game is present.
    static func resolve(game: AccountGame, canOpenURL: Bool, installedVersion: Int64?,
                        catalogUnavailable: Bool, canQueryInstallation: Bool = true) -> AccountGameState {
        if !canQueryInstallation, let launchURL = game.launchURL {
            // iOS limits declared query schemes. An unqueryable game may still be
            // installed; attempting to open it is valid without declaring its scheme.
            return AccountGameState(action: .open(launchURL), statusKey: "game_status_unknown")
        }
        if canOpenURL, let launchURL = game.launchURL {
            if let installedVersion, installedVersion > 0,
               let remoteVersion = game.versionCode, remoteVersion > installedVersion {
                let action = game.storeURL.map { AccountGameAction.update($0) } ?? .play(launchURL)
                return AccountGameState(action: action, statusKey: "game_status_update_available")
            }
            return AccountGameState(action: .play(launchURL), statusKey: "game_status_installed")
        }
        return AccountGameState(action: .install(game.storeURL), statusKey:
            game.storeURL != nil ? "game_status_available" :
                (catalogUnavailable ? "game_status_catalog_unavailable" : "game_status_not_installed"))
    }
}

struct AccountGamesManifest: Decodable {
    let games: [Entry]

    struct Entry: Decodable {
        let id: String
        let title: String?
        let ios: Platform?

        private enum CodingKeys: String, CodingKey { case id, title, ios }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            id = try container.decode(String.self, forKey: .id)
            title = try? container.decode(String.self, forKey: .title)
            // A broken platform entry must not hide every other game in the catalog.
            ios = try? container.decode(Platform.self, forKey: .ios)
        }
    }

    struct Platform: Decodable {
        let bundleId: String
        let urlScheme: String
        let appStoreUrl: String?
        let versionCode: Int64?
    }

    func catalog(defaults: [AccountGame] = AccountGame.defaults) -> [AccountGame] {
        var result = defaults
        var indices = Dictionary(uniqueKeysWithValues: result.enumerated().map { ($0.element.bundleId, $0.offset) })
        for entry in games {
            guard let ios = entry.ios, !ios.bundleId.isEmpty, !ios.urlScheme.isEmpty else { continue }
            let storeURL = ios.appStoreUrl.flatMap(URL.init(string:)).flatMap { url -> URL? in
                guard ["https", "itms-apps"].contains(url.scheme?.lowercased() ?? ""), url.host != nil else { return nil }
                return url
            }
            if let index = indices[ios.bundleId] {
                // Known identities keep their title, artwork and launch protocol even
                // if an older catalog still calls Fluxara "STK".
                result[index].storeURL = storeURL
                result[index].versionCode = ios.versionCode
            } else {
                let game = AccountGame(id: entry.id, title: entry.title ?? entry.id,
                                       bundleId: ios.bundleId, urlScheme: ios.urlScheme,
                                       artwork: "motorica_stk_card_background",
                                       accessibilityPrefix: "account.games.\(entry.id).", requiresLaunchToken: false,
                                       storeURL: storeURL, versionCode: ios.versionCode)
                guard game.launchURL != nil else { continue }
                indices[ios.bundleId] = result.count
                result.append(game)
            }
        }
        return result
    }
}
