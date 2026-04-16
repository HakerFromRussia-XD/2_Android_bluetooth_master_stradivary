import XCTest

class WidgetsSceneUITests: XCTestCase {

    override func setUp() {
        continueAfterFailure = false
    }

    // NOTE: for UI tests to work the keyboard of simulator must be on.
    // Keyboard shortcut COMMAND + SHIFT + K while simulator has focus
    func testOpenWidgetDetails_whenSearchBatmanAndTapOnFirstResultRow_thenWidgetDetailsViewOpensWithTitleBatman() {
        
        let app = XCUIApplication()
        app.launch()
        
        // Search for Batman
        let searchText = "Batman Begins"
        app.searchFields[AccessibilityIdentifier.searchField].tap()
        if !app.keys["A"].waitForExistence(timeout: 5) {
            XCTFail("The keyboard could not be found. Use keyboard shortcut COMMAND + SHIFT + K while simulator has focus on text input")
        }
        _ = app.searchFields[AccessibilityIdentifier.searchField].waitForExistence(timeout: 10)
        app.searchFields[AccessibilityIdentifier.searchField].typeText(searchText)
        app.buttons["search"].tap()
        
        // Tap on first result row
        app.tables.cells.staticTexts[searchText].tap()
        
        // Make sure widget details view
        XCTAssertTrue(app.otherElements[AccessibilityIdentifier.widgetDetailsView].waitForExistence(timeout: 5))
        XCTAssertTrue(app.navigationBars[searchText].waitForExistence(timeout: 5))
    }

    func testBluetoothScan_whenTapFirstCellOnce_thenMainTabsOpen() {
        let app = XCUIApplication()
        app.launchArguments += ["-ui-test-fake-ble-device", "-ui-test-ble-noise"]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 8))

        let firstDeviceCell = devicesTable.cells["ble.deviceCell.0"]
        XCTAssertTrue(firstDeviceCell.waitForExistence(timeout: 8))

        let tapStartedAt = Date()
        firstDeviceCell.tap()

        let didOpenMainTabs = app.otherElements[AccessibilityIdentifier.mainTabBarRoot].waitForExistence(timeout: 5)
        XCTAssertTrue(didOpenMainTabs)
        if didOpenMainTabs {
            XCTAssertLessThanOrEqual(Date().timeIntervalSince(tapStartedAt), 5.0)
        }
    }
    
    func testBluetoothScan_whenTapRomanDevice_thenMainTabsOpen() {
        let app = XCUIApplication()
        app.launch()
        
        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 12), "BLE table did not appear")
        
        let romanCandidates = [
            "Роман",
            "Roman",
            "FTHS3-Роман",
            "FTHS3-Roman"
        ]
        
        guard let romanDeviceElement = waitForDeviceElement(
            namedAnyOf: romanCandidates,
            in: devicesTable,
            timeout: 45
        ) else {
            XCTFail("Could not find BLE device with name Roman/Роман in scan list")
            return
        }
        
        romanDeviceElement.tap()
        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.mainTabBarRoot].waitForExistence(timeout: 8),
            "Main tabs did not open after tapping Roman device"
        )
    }
    
    private func waitForDeviceElement(namedAnyOf candidates: [String], in table: XCUIElement, timeout: TimeInterval) -> XCUIElement? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            for name in candidates {
                let cellByLabel = table.cells.matching(NSPredicate(format: "label CONTAINS[c] %@", name)).firstMatch
                if cellByLabel.exists {
                    return cellByLabel
                }
                
                let staticTextByLabel = table.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] %@", name)).firstMatch
                if staticTextByLabel.exists {
                    return staticTextByLabel
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.4))
        }
        return nil
    }
}
