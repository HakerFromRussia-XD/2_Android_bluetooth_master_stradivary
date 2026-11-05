import XCTest

final class AccountGamesCatalogClientTests: XCTestCase {
    private var session: URLSession!
    private var client: AccountGamesCatalogClient!
    private let url = URL(string: "https://firmware-ms.157.22.203.6.nip.io/games/manifest.json")!

    override func setUp() {
        super.setUp()
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [GamesCatalogURLProtocol.self]
        session = URLSession(configuration: configuration)
        client = AccountGamesCatalogClient(session: session)
    }

    override func tearDown() {
        session.invalidateAndCancel()
        GamesCatalogURLProtocol.handler = nil
        super.tearDown()
    }

    func testServerRefreshReadsDirectHTTPSAndNeverReusesCachedVersion() throws {
        var requests: [URLRequest] = []
        GamesCatalogURLProtocol.handler = { request in
            requests.append(request)
            return (200, Self.catalog(version: requests.count == 1 ? 42 : 43))
        }
        let first = try load().get().catalog()
        let second = try load().get().catalog()
        XCTAssertEqual(requests.count, 2)
        for request in requests {
            XCTAssertEqual(request.url, url)
            XCTAssertEqual(request.httpMethod, "GET")
            XCTAssertEqual(request.cachePolicy, .reloadIgnoringLocalCacheData)
            XCTAssertEqual(request.value(forHTTPHeaderField: "Cache-Control"), "no-cache")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Accept"), "application/json")
            XCTAssertNil(request.value(forHTTPHeaderField: "Authorization"))
        }
        XCTAssertEqual(first.map(\.bundleId), AccountGame.defaults.map(\.bundleId))
        let sushi = try XCTUnwrap(second.first { $0.bundleId == "com.hakerfromrussia.trashdash" })
        XCTAssertEqual(sushi.versionCode, 43)
        XCTAssertEqual(sushi.urlScheme, "sushi-dash")
        XCTAssertNil(sushi.storeURL)
        let current = AccountGameState.resolve(game: first[1], canOpenURL: true, installedVersion: 42,
                                               catalogUnavailable: false)
        let update = AccountGameState.resolve(game: sushi, canOpenURL: true, installedVersion: 42,
                                              catalogUnavailable: false)
        XCTAssertEqual(current.statusKey, "game_status_installed")
        XCTAssertEqual(update.statusKey, "game_status_update_available")
        // Without a published store URL, the installed game remains playable.
        XCTAssertEqual(update.action, .play(sushi.launchURL!))
    }

    func testServerFailureIsNotTreatedAsAnEmptySuccessfulCatalog() {
        GamesCatalogURLProtocol.handler = { _ in (503, Data("{\"games\":[]}".utf8)) }
        guard case .failure(let error) = load() else { return XCTFail("HTTP failure must be reported") }
        XCTAssertEqual(error as? AccountGamesCatalogError, .httpStatus(503))
    }

    func testMalformedServerResponseIsReported() {
        GamesCatalogURLProtocol.handler = { _ in (200, Data("not JSON".utf8)) }
        guard case .failure(let error) = load() else { return XCTFail("Invalid JSON must not replace the last valid catalog") }
        XCTAssertTrue(error is DecodingError)
    }

    func testNetworkFailureIsReported() {
        GamesCatalogURLProtocol.handler = { _ in throw URLError(.notConnectedToInternet) }
        guard case .failure(let error) = load() else { return XCTFail("Network failure must be reported") }
        XCTAssertEqual((error as? URLError)?.code, .notConnectedToInternet)
    }

    private func load() -> Result<AccountGamesManifest, Error> {
        let finished = expectation(description: "Catalog response")
        var result: Result<AccountGamesManifest, Error>?
        client.load(from: url) {
            result = $0
            finished.fulfill()
        }
        wait(for: [finished], timeout: 5)
        return result ?? .failure(URLError(.timedOut))
    }

    private static func catalog(version: Int) -> Data {
        Data("""
        {"games":[
          {"id":"stk","title":"Super Tux Kart","android":{"packageName":"com.motorica.games.stk","versionCode":1016}},
          {"id":"sushidash","title":"Sushi Dash",
           "android":{"packageName":"com.unity.trashdash","versionCode":350},
           "ios":{"bundleId":"com.hakerfromrussia.trashdash","urlScheme":"sushi-dash","versionName":"1.1","versionCode":\(version),"appStoreUrl":""}}
        ]}
        """.utf8)
    }
}

private final class GamesCatalogURLProtocol: URLProtocol {
    static var handler: ((URLRequest) throws -> (Int, Data))?

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        do {
            let (status, body) = try XCTUnwrap(Self.handler)(request)
            let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: "HTTP/1.1",
                                           headerFields: ["Content-Type": "application/json"])!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: body)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }
    override func stopLoading() {}
}
