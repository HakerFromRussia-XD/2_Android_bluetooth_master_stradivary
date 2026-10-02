import XCTest
final class InstalledAppChecks: XCTestCase {
    override func setUp() { continueAfterFailure = false }
    func testReadScreen() {
        let app = XCUIApplication(bundleIdentifier: "com.motorica.startttt")
        app.activate()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 15))
        print("MS_SCREEN_BEGIN\n\(app.debugDescription)\nMS_SCREEN_END")
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.lifetime = .keepAlways
        add(attachment)
    }
    func testRealConnectAndOpenGames() {
        let app = XCUIApplication(bundleIdentifier: "com.motorica.startttt")
        app.activate()
        let account = app.buttons["AccessibilityIdentifierStatusBarAccountButton"]
        let devices = app.tables["AccessibilityIdentifierBLEDevicesTable"]
        if devices.waitForExistence(timeout: 3) {
            let present = NSPredicate(format: "count > 0")
            let wait = XCTNSPredicateExpectation(predicate: present, object: devices.cells)
            XCTAssertEqual(XCTWaiter.wait(for: [wait], timeout: 20), .completed)
            print("BLE_DEVICES\n\(devices.debugDescription)")
            XCTAssertEqual(devices.cells.count, 1, "Select only the single available prosthesis")
            devices.cells.firstMatch.tap()
        }
        XCTAssertTrue(account.waitForExistence(timeout: 30))
        let loading = app.otherElements["loading.progress"]
        let ready = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: loading)
        XCTAssertEqual(XCTWaiter.wait(for: [ready], timeout: 45), .completed)
        print("REAL_CONNECTED_SCREEN\n\(app.debugDescription)")
        account.tap()
        print("ACCOUNT_SCREEN\n\(app.debugDescription)")
        let games = app.staticTexts.matching(NSPredicate(format: "label == 'Games' OR label == 'Игры' OR label == 'Juegos'")).firstMatch
        XCTAssertTrue(games.waitForExistence(timeout: 15))
        games.tap()
        XCTAssertTrue(app.staticTexts["Fluxara Drift"].waitForExistence(timeout: 10))
        print("REAL_GAMES_SCREEN\n\(app.debugDescription)")
        let capture = XCTAttachment(screenshot: app.screenshot())
        capture.lifetime = .keepAlways
        add(capture)
    }
    func testRealMSLaunch() {
        let ms = XCUIApplication(bundleIdentifier: "com.motorica.startttt")
        ms.activate()
        XCTAssertTrue(ms.staticTexts["Fluxara Drift"].waitForExistence(timeout: 10))
        XCTAssertEqual(ms.otherElements["AccessibilityIdentifierStatusBarConnectionIndicator"].value as? String, "connected")
        let play = ms.buttons["AccessibilityIdentifierAccountGamesActionButton"]
        XCTAssertTrue(play.isEnabled)
        XCTAssertTrue(["Play", "Играть", "Jugar"].contains(play.label))
        play.tap()
        let game = XCUIApplication(bundleIdentifier: "io.fluxara.drift")
        XCTAssertTrue(game.wait(for: .runningForeground, timeout: 30))
        print("FLUXARA_LAUNCHED_FROM_MS\n\(game.debugDescription)")
        let capture = XCTAttachment(screenshot: game.screenshot())
        capture.lifetime = .keepAlways
        add(capture)
    }
    func testReadGame() {
        let game = XCUIApplication(bundleIdentifier: "io.fluxara.drift")
        XCTAssertTrue(game.wait(for: .runningForeground, timeout: 10))
        print("FLUXARA_SCREEN\n\(game.debugDescription)")
        let capture = XCTAttachment(screenshot: game.screenshot())
        capture.lifetime = .keepAlways
        add(capture)
    }

    func testTapHomePlay() {
        let game = XCUIApplication(bundleIdentifier: "io.fluxara.drift")
        XCTAssertTrue(game.wait(for: .runningForeground, timeout: 10))
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.78)).tap()
        Thread.sleep(forTimeInterval: 1)
        let capture = XCTAttachment(screenshot: game.screenshot())
        capture.lifetime = .keepAlways
        add(capture)
    }

    func testTapCanyon() {
        let game = XCUIApplication(bundleIdentifier: "io.fluxara.drift")
        XCTAssertTrue(game.wait(for: .runningForeground, timeout: 10))
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.33)).tap()
        Thread.sleep(forTimeInterval: 2)
        let capture = XCTAttachment(screenshot: game.screenshot())
        capture.lifetime = .keepAlways
        add(capture)
    }

    func testStartRace() {
        let game = XCUIApplication(bundleIdentifier: "io.fluxara.drift")
        XCTAssertTrue(game.wait(for: .runningForeground, timeout: 10))
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.885)).tap()
        for i in 0..<5 {
            Thread.sleep(forTimeInterval: 3)
            let capture = XCTAttachment(screenshot: game.screenshot())
            capture.name = "Real race sample \(i)"
            capture.lifetime = .keepAlways
            add(capture)
        }
    }

    func testSelectAndObserveRace() {
        let game = XCUIApplication(bundleIdentifier: "io.fluxara.drift")
        XCTAssertTrue(game.wait(for: .runningForeground, timeout: 10))
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.92)).tap()
        for i in 0..<8 {
            Thread.sleep(forTimeInterval: 3)
            print("REAL_CAPTURE_MS=\(Int64(Date().timeIntervalSince1970 * 1000)) sample=\(i)")
            let capture = XCTAttachment(screenshot: game.screenshot())
            capture.name = "BLE HUD race \(i)"
            capture.lifetime = .keepAlways
            add(capture)
        }
    }

    func testLaunchAndReturnToRace() {
        let ms = XCUIApplication(bundleIdentifier: "com.motorica.startttt")
        ms.activate()
        XCTAssertTrue(ms.staticTexts["Fluxara Drift"].waitForExistence(timeout: 10))
        ms.buttons["AccessibilityIdentifierAccountGamesActionButton"].tap()
        let game = XCUIApplication(bundleIdentifier: "io.fluxara.drift")
        XCTAssertTrue(game.wait(for: .runningForeground, timeout: 30))
        Thread.sleep(forTimeInterval: 5)
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.78)).tap()
        Thread.sleep(forTimeInterval: 1)
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.33)).tap()
        Thread.sleep(forTimeInterval: 1)
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.885)).tap()
        Thread.sleep(forTimeInterval: 1)
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.92)).tap()
        for i in 0..<4 {
            Thread.sleep(forTimeInterval: 3)
            let capture = XCTAttachment(screenshot: game.screenshot())
            capture.name = "Verified race geometry \(i)"
            capture.lifetime = .keepAlways
            add(capture)
        }
    }

}
