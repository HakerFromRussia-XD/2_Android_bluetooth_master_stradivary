import XCTest

final class AccountGameStateTests: XCTestCase {
    func testUninstalledGamesShowDownloadEvenWithStaleVersionMarker() {
        for game in AccountGame.defaults {
            let state = AccountGameState.resolve(game: game, canOpenURL: false, installedVersion: 100,
                                                 catalogUnavailable: false)
            XCTAssertEqual(state.action, .install(nil))
            XCTAssertEqual(state.action.titleKey, "download_game")
            XCTAssertFalse(state.action.isEnabled)
        }
    }

    func testInstalledGamesCanPlayWithoutCatalogOrVersionMarker() {
        for game in AccountGame.defaults {
            let state = AccountGameState.resolve(game: game, canOpenURL: true, installedVersion: nil,
                                                 catalogUnavailable: true)
            XCTAssertEqual(state.action, .play(game.launchURL!))
            XCTAssertTrue(state.action.isEnabled)
        }
    }

    func testStoreURLAndVersionBelongToEachGame() throws {
        let games = try catalog([
            entry(id: "stk", bundle: "io.fluxara.drift", version: 30, store: "https://apps.apple.com/app/id111"),
            entry(id: "sushi_dash", bundle: "com.hakerfromrussia.trashdash", version: 8, store: "https://apps.apple.com/app/id222")
        ])
        XCTAssertEqual(games.count, 2)
        XCTAssertEqual(games[0].title, "Fluxara Drift")
        XCTAssertEqual(games[1].title, "Sushi Dash")
        let update = AccountGameState.resolve(game: games[0], canOpenURL: true, installedVersion: 29,
                                              catalogUnavailable: false)
        let install = AccountGameState.resolve(game: games[1], canOpenURL: false, installedVersion: nil,
                                               catalogUnavailable: false)
        XCTAssertEqual(update.action, .update(URL(string: "https://apps.apple.com/app/id111")))
        XCTAssertEqual(install.action, .install(URL(string: "https://apps.apple.com/app/id222")))
        XCTAssertTrue(install.action.isEnabled)
    }

    func testCurrentAndUnknownInstalledVersionsKeepPlayAction() {
        var game = AccountGame.defaults[1]
        game.versionCode = 8
        for version: Int64? in [nil, 0, 8, 9] {
            let state = AccountGameState.resolve(game: game, canOpenURL: true, installedVersion: version,
                                                 catalogUnavailable: false)
            XCTAssertEqual(state.action, .play(game.launchURL!))
        }
    }

    func testUninstallAfterPlayingSwitchesBackToDownload() {
        let game = AccountGame.defaults[1]
        let before = AccountGameState.resolve(game: game, canOpenURL: true, installedVersion: 8,
                                              catalogUnavailable: false)
        let after = AccountGameState.resolve(game: game, canOpenURL: false, installedVersion: 8,
                                             catalogUnavailable: false)
        XCTAssertEqual(before.action.titleKey, "play_game")
        XCTAssertEqual(after.action.titleKey, "download_game")
    }

    func testMissingUpdateLinkDoesNotPreventPlayingInstalledGame() {
        var game = AccountGame.defaults[1]
        game.versionCode = 8
        let state = AccountGameState.resolve(game: game, canOpenURL: true, installedVersion: 7,
                                             catalogUnavailable: false)
        XCTAssertEqual(state.action, .play(game.launchURL!))
        XCTAssertTrue(state.action.isEnabled)
    }

    func testUnqueryableGameDoesNotClaimItIsUninstalled() {
        let game = AccountGame.defaults[1]
        let state = AccountGameState.resolve(game: game, canOpenURL: false, installedVersion: nil,
                                             catalogUnavailable: false, canQueryInstallation: false)
        XCTAssertEqual(state.action, .open(game.launchURL!))
        XCTAssertEqual(state.statusKey, "game_status_unknown")
        XCTAssertTrue(state.action.isEnabled)
    }

    func testCatalogSupportsTwoHundredIndependentGamesAndDeduplicatesByBundle() throws {
        var entries = (0..<200).map { entry(id: "game\($0)", bundle: "org.motorica.game\($0)", version: Int64($0)) }
        entries.append(entry(id: "renamed", bundle: "org.motorica.game0", version: 500))
        let games = try catalog(entries, defaults: [])
        XCTAssertEqual(games.count, 200)
        XCTAssertEqual(games[0].versionCode, 500)
        XCTAssertEqual(Set(games.map(\.bundleId)).count, 200)
        for (index, game) in games.enumerated() {
            let state = AccountGameState.resolve(game: game, canOpenURL: index.isMultiple(of: 2),
                                                 installedVersion: nil, catalogUnavailable: false)
            XCTAssertEqual(state.action.titleKey, index.isMultiple(of: 2) ? "play_game" : "download_game")
        }
    }

    func testInvalidStoreLinkCannotEnableDownload() throws {
        let game = try XCTUnwrap(catalog([
            entry(id: "sushi", bundle: "com.hakerfromrussia.trashdash", version: 8, store: "file:///tmp/game")
        ]).last)
        XCTAssertNil(game.storeURL)
        XCTAssertFalse(AccountGameState.resolve(game: game, canOpenURL: false, installedVersion: nil,
                                                catalogUnavailable: false).action.isEnabled)
    }

    func testForeignBundleCannotSupplyFluxaraUpdateMetadata() throws {
        let games = try catalog([entry(id: "stk", bundle: "org.other.game", version: 999)])
        XCTAssertNil(games[0].versionCode)
        XCTAssertNil(games[0].storeURL)
    }

    func testBundleIdentifierCaseChangesDoNotDuplicateGamesOrLoseVersionIdentity() throws {
        let games = try catalog([
            entry(id: "sushidash", bundle: "COM.HAKERFROMRUSSIA.TRASHDASH", version: 42),
            entry(id: "renamed", bundle: "com.hakerfromrussia.trashdash", version: 43)
        ])
        XCTAssertEqual(games.count, 2)
        XCTAssertEqual(games[1].id, "sushidash")
        XCTAssertEqual(games[1].versionCode, 43)
        XCTAssertTrue(games[1].hasBundleId("COM.HAKERFROMRUSSIA.TRASHDASH"))
        XCTAssertFalse(games[1].hasBundleId("org.other.game"))
    }

    func testInvalidPlatformMetadataDoesNotHideOtherGames() throws {
        var invalidScheme = entry(id: "invalid", bundle: "org.invalid", version: 1)
        invalidScheme["ios"] = ["bundleId": "org.invalid", "urlScheme": "not a scheme"]
        let games = try catalog([
            ["id": "broken", "ios": ["bundleId": "org.broken"]],
            invalidScheme,
            entry(id: "valid", bundle: "org.valid", version: 1)
        ], defaults: [])
        XCTAssertEqual(games.map(\.bundleId), ["org.valid"])
    }

    private func entry(id: String, bundle: String, version: Int64, store: String = "") -> [String: Any] {
        ["id": id, "title": "Remote title", "ios": ["bundleId": bundle, "urlScheme": "game-\(id)",
                                                    "versionCode": version, "appStoreUrl": store]]
    }

    private func catalog(_ entries: [[String: Any]], defaults: [AccountGame] = AccountGame.defaults) throws -> [AccountGame] {
        let data = try JSONSerialization.data(withJSONObject: ["games": entries])
        return try JSONDecoder().decode(AccountGamesManifest.self, from: data).catalog(defaults: defaults)
    }
}
