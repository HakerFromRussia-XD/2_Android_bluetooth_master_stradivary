import XCTest

class WidgetsSceneUITests: XCTestCase {
    private enum SegmentAnimationDirection {
        case increasing
        case decreasing
    }

    private struct SelectorState {
        let segment: String
        let offset: Double
        let maxStep: Double
        let steps: Int
        let rollback: Bool
    }

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

    func testBluetoothDisconnectAndReconnect_whenDisconnectByDeviceName_thenStatusAnimationAndReconnectWork() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20))

        let romanCandidates = [
            "Роман",
            "Roman",
            "FTHS3-Роман",
            "FTHS3-Roman"
        ]

        guard let romanDeviceElement = waitForDeviceElement(
            namedAnyOf: romanCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device with name Roman/Роман in scan list")
            return
        }

        romanDeviceElement.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12))

        let loadingProgress = app.descendants(matching: .any)["loading.progress"]
        XCTAssertTrue(
            loadingProgress.waitForExistence(timeout: 12),
            "Synchronization progress loader wasn't shown after connecting Roman device"
        )
        XCTAssertTrue(
            waitForProgressAdvance(loadingProgress, timeout: 20),
            "Synchronization progress didn't advance after connecting Roman device"
        )

        let deviceNameButton = app.buttons[AccessibilityIdentifier.statusBarDeviceNameButton]
        XCTAssertTrue(deviceNameButton.waitForExistence(timeout: 5))
        deviceNameButton.tap()

        let confirmDisconnectButton = app.buttons[AccessibilityIdentifier.statusBarDisconnectConfirmButton]
        XCTAssertTrue(confirmDisconnectButton.waitForExistence(timeout: 5))
        let disconnectStartedAt = Date()
        confirmDisconnectButton.tap()

        XCTAssertTrue(devicesTable.waitForExistence(timeout: 3))
        XCTAssertLessThanOrEqual(
            Date().timeIntervalSince(disconnectStartedAt),
            1.5,
            "Scan screen should open immediately after disconnect confirmation"
        )
        XCTAssertFalse(mainTabsRoot.exists, "Main tabs should be closed right after disconnect")

        guard let romanDeviceElementAfterReconnect = waitForDeviceElement(
            namedAnyOf: romanCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device with name Roman/Роман in scan list after disconnect")
            return
        }
        romanDeviceElementAfterReconnect.tap()

        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12))
        let reconnectLoadingProgress = app.descendants(matching: .any)["loading.progress"]
        XCTAssertTrue(
            reconnectLoadingProgress.waitForExistence(timeout: 12),
            "Synchronization progress loader wasn't shown after reconnect"
        )
        XCTAssertTrue(
            staysVisible(reconnectLoadingProgress, for: 0.6),
            "Synchronization loader disappeared too quickly after reconnect"
        )
        XCTAssertTrue(
            waitForProgressAdvance(reconnectLoadingProgress, timeout: 20),
            "Synchronization progress didn't advance after reconnect"
        )

        let statusIndicator = app.otherElements[AccessibilityIdentifier.statusBarConnectionIndicator]
        XCTAssertTrue(statusIndicator.waitForExistence(timeout: 5))
        XCTAssertTrue(
            waitForElementValue(
                statusIndicator,
                expectedValues: ["animating:disconnect_to_connect", "connected"],
                timeout: 3
            ),
            "Reconnect animation state wasn't observed"
        )
    }

    func testMainTabs_whenSynchronizationInProgress_thenGesturesAndSpecialTabsAreBlockedUntilCompletion() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        let romanCandidates = [
            "Роман",
            "Roman",
            "FTHS3-Роман",
            "FTHS3-Roman"
        ]

        guard let romanDeviceElement = waitForDeviceElement(
            namedAnyOf: romanCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device with name Roman/Роман in scan list")
            return
        }

        romanDeviceElement.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12))
        XCTAssertTrue(
            waitForSynchronizationCompletion(in: app, timeout: 60),
            "Initial synchronization did not complete within expected timeout"
        )

        let resyncButton = app.buttons[AccessibilityIdentifier.widgetsResyncButton]
        XCTAssertTrue(resyncButton.waitForExistence(timeout: 5), "Resync button was not found on sensors screen")
        let gesturesTabButton = app.tabBars.buttons[AccessibilityIdentifier.mainTabGesturesItem]
        let specialTabButton = app.tabBars.buttons[AccessibilityIdentifier.mainTabSpecialSettingsItem]
        XCTAssertTrue(gesturesTabButton.waitForExistence(timeout: 5), "Gestures tab button was not found")
        XCTAssertTrue(specialTabButton.waitForExistence(timeout: 5), "Special settings tab button was not found")

        resyncButton.tap()

        gesturesTabButton.tap()
        XCTAssertFalse(
            gesturesTabButton.isSelected,
            "Gestures tab must not become selected before synchronization completion"
        )

        resyncButton.tap()

        specialTabButton.tap()
        XCTAssertFalse(
            specialTabButton.isSelected,
            "Special settings tab must not become selected before synchronization completion"
        )

        XCTAssertTrue(
            waitForSynchronizationCompletion(in: app, timeout: 60),
            "Synchronization did not complete within expected timeout"
        )

        gesturesTabButton.tap()
        XCTAssertTrue(
            gesturesTabButton.isSelected,
            "Gestures tab should open after synchronization completion"
        )

        specialTabButton.tap()
        XCTAssertTrue(
            specialTabButton.isSelected,
            "Special settings tab should open after synchronization completion"
        )
    }

    func testGesturesSegmentSwitch_whenToggleCollectionAndRotation_thenSelectorAnimationIsSmoothWithoutJump() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        let romanCandidates = [
            "Роман",
            "Roman",
            "FTHS3-Роман",
            "FTHS3-Roman"
        ]

        guard let romanDeviceElement = waitForDeviceElement(
            namedAnyOf: romanCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device with name Roman/Роман in scan list")
            return
        }

        romanDeviceElement.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12))
        XCTAssertTrue(
            waitForSynchronizationCompletion(in: app, timeout: 60),
            "Synchronization did not complete before switching to Gestures tab"
        )

        let gesturesTabButton = app.tabBars.buttons[AccessibilityIdentifier.mainTabGesturesItem]
        if gesturesTabButton.waitForExistence(timeout: 5) {
            gesturesTabButton.tap()
        } else {
            let firstTabButton = app.tabBars.buttons.element(boundBy: 0)
            XCTAssertTrue(firstTabButton.waitForExistence(timeout: 5), "Could not find gestures tab button")
            firstTabButton.tap()
        }

        let widgetsTable = app.tables[AccessibilityIdentifier.widgetsTable]
        XCTAssertTrue(widgetsTable.waitForExistence(timeout: 45), "Widgets table did not appear")

        let selector = elementByIdentifierOrLabels(
            in: app,
            identifier: AccessibilityIdentifier.gesturesSegmentSelector,
            fallbackLabels: ["gestures.segment.selector"]
        )
        XCTAssertTrue(
            scrollToElement(selector, in: widgetsTable, maxSwipes: 10),
            "Could not find gestures segment selector"
        )
        XCTAssertTrue(selector.waitForExistence(timeout: 5), "Could not find gestures segment selector")

        tapSelectorSegment(selector, normalizedX: 0.25)
        XCTAssertTrue(
            waitForSelectorSegment(selector, expected: "collection", timeout: 2),
            "Failed to switch selector to collection before animation check"
        )

        assertSelectorMovesSmoothly(
            selector,
            expectedTargetSegment: "rotation",
            direction: .increasing
        ) {
            tapSelectorSegment(selector, normalizedX: 0.75)
        }

        assertSelectorMovesSmoothly(
            selector,
            expectedTargetSegment: "collection",
            direction: .decreasing
        ) {
            tapSelectorSegment(selector, normalizedX: 0.25)
        }
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

    private func scrollToElement(_ element: XCUIElement, in table: XCUIElement, maxSwipes: Int) -> Bool {
        if element.exists {
            return true
        }

        for _ in 0..<maxSwipes {
            table.swipeUp()
            if element.waitForExistence(timeout: 0.8) {
                return true
            }
        }

        for _ in 0..<maxSwipes {
            table.swipeDown()
            if element.waitForExistence(timeout: 0.8) {
                return true
            }
        }

        return element.exists
    }

    private func tapSelectorSegment(_ selector: XCUIElement, normalizedX: CGFloat) {
        let coordinate = selector.coordinate(withNormalizedOffset: CGVector(dx: normalizedX, dy: 0.5))
        coordinate.tap()
    }

    private func elementByIdentifierOrLabels(
        in app: XCUIApplication,
        identifier: String,
        fallbackLabels: [String]
    ) -> XCUIElement {
        var predicateFormat = "identifier == %@"
        var arguments: [Any] = [identifier]
        for _ in fallbackLabels {
            predicateFormat += " OR label CONTAINS[c] %@"
        }
        arguments.append(contentsOf: fallbackLabels)

        let predicate = NSPredicate(format: predicateFormat, argumentArray: arguments)
        return app.descendants(matching: .any).matching(predicate).firstMatch
    }

    private func assertSelectorMovesSmoothly(
        _ selector: XCUIElement,
        expectedTargetSegment: String,
        direction: SegmentAnimationDirection,
        trigger: () -> Void,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let startState = waitForSelectorState(selector, timeout: 1.5) else {
            XCTFail("Could not read initial selector state", file: file, line: line)
            return
        }

        trigger()

        let deadline = Date().addingTimeInterval(2.0)
        var previousOffset = startState.offset
        var maxStepDelta = 0.0
        var meaningfulStepCount = 0
        var rollbackDetected = false
        var sampledOffsets: [Double] = [startState.offset]
        let startMinY = selector.frame.minY
        var maxSelectorYDrift = 0.0

        while Date() < deadline {
            if let state = selectorState(from: selector) {
                let step = state.offset - previousOffset
                if abs(step) > 0.001 {
                    sampledOffsets.append(state.offset)
                    meaningfulStepCount += 1
                    maxStepDelta = max(maxStepDelta, abs(step))

                    switch direction {
                    case .increasing:
                        if step < -0.8 {
                            rollbackDetected = true
                        }
                    case .decreasing:
                        if step > 0.8 {
                            rollbackDetected = true
                        }
                    }
                    previousOffset = state.offset
                }
            }
            if selector.exists {
                maxSelectorYDrift = max(maxSelectorYDrift, abs(selector.frame.minY - startMinY))
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.02))
        }

        guard let finalState = waitForSelectorState(selector, timeout: 0.8) else {
            XCTFail("Could not read final selector state", file: file, line: line)
            return
        }

        XCTAssertEqual(
            finalState.segment,
            expectedTargetSegment,
            "Selector ended in unexpected segment",
            file: file,
            line: line
        )

        XCTAssertGreaterThan(
            abs(finalState.offset - startState.offset),
            20,
            "Selector offset barely changed, expected animated transition",
            file: file,
            line: line
        )

        let measuredMaxStep = finalState.maxStep > 0 ? finalState.maxStep : maxStepDelta
        let measuredRollback = finalState.rollback || rollbackDetected
        let measuredStepCount = max(finalState.steps, meaningfulStepCount)

        if measuredMaxStep >= 80 || measuredRollback {
            let trace = sampledOffsets.map { String(format: "%.2f", $0) }.joined(separator: " -> ")
            print(
                "[UI-DEBUG][selector] segmentTarget=\(expectedTargetSegment) direction=\(direction) " +
                "maxStepDelta=\(measuredMaxStep) rollback=\(measuredRollback) steps=\(measuredStepCount) " +
                "yDrift=\(maxSelectorYDrift) samples=\(trace)"
            )
        }

        XCTAssertLessThan(
            measuredMaxStep,
            80,
            "Selector had a large jump step (\(measuredMaxStep))",
            file: file,
            line: line
        )

        XCTAssertFalse(
            measuredRollback,
            "Selector animation rolled back during transition",
            file: file,
            line: line
        )

        XCTAssertGreaterThan(
            measuredStepCount,
            2,
            "Selector moved too abruptly, expected multiple smooth animation steps",
            file: file,
            line: line
        )

        XCTAssertLessThan(
            maxSelectorYDrift,
            1,
            "Selector container jumped vertically by \(maxSelectorYDrift)pt; only X-axis highlight movement is expected",
            file: file,
            line: line
        )
    }

    private func waitForSelectorSegment(_ selector: XCUIElement, expected: String, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let state = selectorState(from: selector), state.segment == expected {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }
        return false
    }

    private func waitForSelectorState(_ selector: XCUIElement, timeout: TimeInterval) -> SelectorState? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let state = selectorState(from: selector) {
                return state
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }
        return nil
    }

    private func selectorState(from selector: XCUIElement) -> SelectorState? {
        guard let value = selector.value as? String else { return nil }

        let parts = value.split(separator: ";")
        var segment: String?
        var offset: Double?
        var maxStep = 0.0
        var steps = 0
        var rollback = false

        for rawPart in parts {
            let part = rawPart.trimmingCharacters(in: .whitespacesAndNewlines)
            if part.hasPrefix("segment=") {
                segment = String(part.dropFirst("segment=".count))
            } else if part.hasPrefix("offset=") {
                offset = Double(part.dropFirst("offset=".count))
            } else if part.hasPrefix("maxStep=") {
                maxStep = Double(part.dropFirst("maxStep=".count)) ?? 0
            } else if part.hasPrefix("steps=") {
                steps = Int(part.dropFirst("steps=".count)) ?? 0
            } else if part.hasPrefix("rollback=") {
                rollback = String(part.dropFirst("rollback=".count)).lowercased() == "true"
            }
        }

        guard let segment, let offset else { return nil }
        return SelectorState(
            segment: segment,
            offset: offset,
            maxStep: maxStep,
            steps: steps,
            rollback: rollback
        )
    }

    private func waitForElementValue(
        _ element: XCUIElement,
        expectedValues: [String],
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let value = element.value as? String, expectedValues.contains(value) {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }
        return false
    }

    private func staysVisible(_ element: XCUIElement, for duration: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(duration)
        while Date() < deadline {
            if !element.exists {
                return false
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }
        return true
    }

    private func waitForProgressAdvance(_ element: XCUIElement, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        var minObserved: Double?
        var maxObserved: Double?

        while Date() < deadline {
            if let value = progressValue(from: element) {
                if let currentMin = minObserved {
                    minObserved = min(currentMin, value)
                } else {
                    minObserved = value
                }
                if let currentMax = maxObserved {
                    maxObserved = max(currentMax, value)
                } else {
                    maxObserved = value
                }

                if let minObserved, let maxObserved, maxObserved - minObserved >= 0.05 {
                    return true
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }

        return false
    }

    private func waitForSynchronizationCompletion(in app: XCUIApplication, timeout: TimeInterval) -> Bool {
        let progressElement = app.descendants(matching: .any)["loading.progress"]
        let deadline = Date().addingTimeInterval(timeout)
        var hasSeenProgress = progressElement.exists

        while Date() < deadline {
            let isVisible = progressElement.exists
            if isVisible {
                hasSeenProgress = true
            } else if hasSeenProgress {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }

        return !progressElement.exists
    }

    private func progressValue(from element: XCUIElement) -> Double? {
        if let value = element.value as? Double {
            return value
        }

        if let value = element.value as? String {
            let digits = value.filter { "0123456789., ".contains($0) }
                .replacingOccurrences(of: " ", with: "")
                .replacingOccurrences(of: ",", with: ".")

            if let parsedPercent = Double(digits), value.contains("%") {
                return parsedPercent / 100.0
            }

            if let parsed = Double(digits) {
                return parsed > 1 ? min(parsed / 100.0, 1.0) : parsed
            }
        }

        return nil
    }
}
