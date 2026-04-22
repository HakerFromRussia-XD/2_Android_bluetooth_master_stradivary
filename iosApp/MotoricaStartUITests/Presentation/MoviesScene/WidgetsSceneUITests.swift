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

    private let preferredDeviceCandidates = [
        "FTHS3-Рома1",
        "Рома1",
        "Роман",
        "FTHS3-Роман",
    ]

    override func setUp() {
        continueAfterFailure = false
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

    func testBluetoothFilterSegment_whenTapAreaOutsideText_thenSelectionChanges() {
        let app = XCUIApplication()
        app.launchArguments += ["-ui-test-fake-ble-device", "-ui-test-ble-noise"]
        app.launch()

        let selector = app.otherElements[AccessibilityIdentifier.bleFilterSegmentSelector]
        XCTAssertTrue(selector.waitForExistence(timeout: 8), "Bluetooth filter selector did not appear")

        guard let initialIndex = waitForBluetoothFilterSelectedIndex(selector, timeout: 2) else {
            XCTFail("Could not read initial bluetooth filter selector index")
            return
        }

        let forwardTapX: CGFloat = initialIndex == 0 ? 0.88 : 0.12
        let backwardTapX: CGFloat = initialIndex == 0 ? 0.12 : 0.88
        let toggledIndex = initialIndex == 0 ? 1 : 0

        tapSelectorSegment(selector, normalizedX: forwardTapX, normalizedY: 0.18)
        XCTAssertTrue(
            waitForBluetoothFilterSelectedIndex(selector, expected: toggledIndex, timeout: 2),
            "Bluetooth filter did not switch after tap outside text area"
        )

        tapSelectorSegment(selector, normalizedX: backwardTapX, normalizedY: 0.82)
        XCTAssertTrue(
            waitForBluetoothFilterSelectedIndex(selector, expected: initialIndex, timeout: 2),
            "Bluetooth filter did not switch back after tap outside text area"
        )
    }
    
    func testBluetoothScan_whenTapRomanDevice_thenMainTabsOpen() {
        let app = XCUIApplication()
        app.launch()
        
        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 12), "BLE table did not appear")
        
        guard let romanDeviceElement = waitForDeviceElement(
            namedAnyOf: preferredDeviceCandidates,
            in: devicesTable,
            timeout: 45
        ) else {
            XCTFail("Could not find preferred BLE device (Рома1/Роман) in scan list")
            return
        }
        
        romanDeviceElement.tap()
        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.mainTabBarRoot].waitForExistence(timeout: 8),
            "Main tabs did not open after tapping Roman device"
        )
    }

    func testMainTabs_whenSynchronizationInProgress_thenGesturesAndSpecialTabsAreBlockedUntilCompletion() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let romanDeviceElement = waitForDeviceElement(
            namedAnyOf: preferredDeviceCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find preferred BLE device (Рома1/Роман) in scan list")
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

        guard let romanDeviceElement = waitForDeviceElement(
            namedAnyOf: preferredDeviceCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find preferred BLE device (Рома1/Роман) in scan list")
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

    func testGesturesRotationFirstLoad_whenDataArrives_thenContentRevealUsesHeightAnimation() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui-test-fake-ble-device",
            "-ui-test-ble-noise",
            "-ui-test-skip-synchronization",
            "-ui-test-force-gestures-widget",
            "-ui-test-gestures-default-rotation",
            "-ui-test-simulate-rotation-group-first-load"
        ]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 12), "BLE table did not appear")

        let firstDeviceCell = devicesTable.cells["ble.deviceCell.0"]
        XCTAssertTrue(firstDeviceCell.waitForExistence(timeout: 12), "Fake BLE device cell did not appear")
        firstDeviceCell.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12), "Main tabs did not open after connecting fake device")

        let gesturesTabButton = app.tabBars.buttons[AccessibilityIdentifier.mainTabGesturesItem]
        XCTAssertTrue(gesturesTabButton.waitForExistence(timeout: 5), "Gestures tab button was not found")
        gesturesTabButton.tap()

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
        XCTAssertTrue(
            waitForSelectorSegment(selector, expected: "rotation", timeout: 2),
            "Rotation segment must stay selected while loading rotation-group data"
        )

        let progressSamples = collectRotationRevealProgressSamples(
            from: selector,
            duration: 3.0,
            interval: 0.03
        )
        XCTAssertGreaterThan(progressSamples.count, 8, "Not enough reveal progress samples to validate animation")

        guard let minProgress = progressSamples.min(), let maxProgress = progressSamples.max() else {
            XCTFail("Could not read reveal progress samples")
            return
        }

        let increasingSteps = zip(progressSamples, progressSamples.dropFirst())
            .filter { ($1 - $0) > 0.01 }
            .count

        XCTAssertLessThan(
            minProgress,
            0.25,
            "Rotation reveal did not start from collapsed height (min progress=\(minProgress))"
        )
        XCTAssertGreaterThan(
            maxProgress,
            0.95,
            "Rotation reveal did not reach full height (max progress=\(maxProgress))"
        )
        XCTAssertGreaterThanOrEqual(
            increasingSteps,
            2,
            "Rotation reveal changed too abruptly; expected multiple height animation steps"
        )
    }

    func testGesturesSegmentSwitch_whenTapSegmentAreaOutsideText_thenSegmentChangesOnSimulator() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui-test-fake-ble-device",
            "-ui-test-ble-noise",
            "-ui-test-skip-synchronization",
            "-ui-test-force-gestures-widget"
        ]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 12), "BLE table did not appear")

        let firstDeviceCell = devicesTable.cells["ble.deviceCell.0"]
        XCTAssertTrue(firstDeviceCell.waitForExistence(timeout: 12), "Fake BLE device cell did not appear")
        firstDeviceCell.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12), "Main tabs did not open after connecting fake device")

        let gesturesTabButton = app.tabBars.buttons[AccessibilityIdentifier.mainTabGesturesItem]
        XCTAssertTrue(gesturesTabButton.waitForExistence(timeout: 5), "Gestures tab button was not found")
        gesturesTabButton.tap()

        let widgetsTable = app.tables[AccessibilityIdentifier.widgetsTable]
        XCTAssertTrue(widgetsTable.waitForExistence(timeout: 30), "Widgets table did not appear")

        let selector = elementByIdentifierOrLabels(
            in: app,
            identifier: AccessibilityIdentifier.gesturesSegmentSelector,
            fallbackLabels: ["gestures.segment.selector"]
        )
        XCTAssertTrue(
            scrollToElement(selector, in: widgetsTable, maxSwipes: 10),
            "Could not find gestures segment selector"
        )
        XCTAssertTrue(selector.waitForExistence(timeout: 5), "Gestures segment selector did not appear")

        // Tap near top-right corner of segment control (outside text baseline) -> rotation segment.
        tapSelectorSegment(selector, normalizedX: 0.88, normalizedY: 0.18)
        XCTAssertTrue(
            waitForSelectorSegment(selector, expected: "rotation", timeout: 2),
            "Rotation segment was not selected by tapping segment area outside text"
        )

        // Tap near bottom-left corner of segment control (outside text baseline) -> collection segment.
        tapSelectorSegment(selector, normalizedX: 0.12, normalizedY: 0.82)
        XCTAssertTrue(
            waitForSelectorSegment(selector, expected: "collection", timeout: 2),
            "Collection segment was not selected by tapping segment area outside text"
        )
    }

    func testGesturesRotationGroup_whenTapRowAreaOutsideText_thenGestureBecomesActive() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui-test-fake-ble-device",
            "-ui-test-ble-noise",
            "-ui-test-skip-synchronization",
            "-ui-test-force-gestures-widget",
            "-ui-test-gestures-default-rotation",
            "-ui-test-simulate-rotation-group-first-load"
        ]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 12), "BLE table did not appear")

        let firstDeviceCell = devicesTable.cells["ble.deviceCell.0"]
        XCTAssertTrue(firstDeviceCell.waitForExistence(timeout: 12), "Fake BLE device cell did not appear")
        firstDeviceCell.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12), "Main tabs did not open after connecting fake device")

        let gesturesTabButton = app.tabBars.buttons[AccessibilityIdentifier.mainTabGesturesItem]
        XCTAssertTrue(gesturesTabButton.waitForExistence(timeout: 5), "Gestures tab button was not found")
        gesturesTabButton.tap()

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
        XCTAssertTrue(
            waitForSelectorSegment(selector, expected: "rotation", timeout: 2),
            "Rotation segment must stay selected while loading rotation-group data"
        )

        let targetTitle = app.staticTexts.matching(identifier: "\(AccessibilityIdentifier.gesturesRotationRowTitlePrefix).2").firstMatch
        XCTAssertTrue(
            scrollToElement(targetTitle, in: widgetsTable, maxSwipes: 10),
            "Could not find rotation-group row title"
        )
        XCTAssertTrue(targetTitle.waitForExistence(timeout: 8), "Rotation-group row title did not appear")

        let rowTapCoordinate = targetTitle.coordinate(withNormalizedOffset: CGVector(dx: 1.8, dy: 0.5))
        rowTapCoordinate.tap()

        XCTAssertTrue(
            waitForElementValue(targetTitle, expectedValues: ["active"], timeout: 2),
            "Rotation-group row did not become active after tapping row area outside text"
        )
    }

    func testBottomBarStyle_whenConnectedToRoma1_thenCaptureRealDeviceScreenshot() {
        let app = XCUIApplication()
        app.launchArguments += ["-ui-test-debug-tabbar"]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: preferredDeviceCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find preferred BLE device (Рома1/Роман) in scan list")
            return
        }

        targetDeviceElement.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 15), "Main tabs did not open after tapping preferred BLE device")

        let sensorsTab = app.tabBars.buttons["Датчики"]
        if sensorsTab.waitForExistence(timeout: 3) {
            sensorsTab.tap()
        }

        RunLoop.current.run(until: Date().addingTimeInterval(1.0))

        let screenshot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = "real-device-bottom-bar-after-connect"
        attachment.lifetime = .keepAlways
        add(attachment)

        let gesturesTab = app.tabBars.buttons[AccessibilityIdentifier.mainTabGesturesItem]
        if gesturesTab.waitForExistence(timeout: 3) {
            gesturesTab.tap()
            RunLoop.current.run(until: Date().addingTimeInterval(0.6))
            let gesturesAttachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            gesturesAttachment.name = "real-device-bottom-bar-gestures-selected"
            gesturesAttachment.lifetime = .keepAlways
            add(gesturesAttachment)
        }

        let specialTab = app.tabBars.buttons[AccessibilityIdentifier.mainTabSpecialSettingsItem]
        if specialTab.waitForExistence(timeout: 3) {
            specialTab.tap()
            RunLoop.current.run(until: Date().addingTimeInterval(0.6))
            let specialAttachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            specialAttachment.name = "real-device-bottom-bar-special-selected"
            specialAttachment.lifetime = .keepAlways
            add(specialAttachment)
        }
    }

    func testSpecialSettingsSpinner_whenTapOption_thenDropdownClosesAndSelectionIsApplied() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: preferredDeviceCandidates,
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find preferred BLE device (Рома1/Роман) in scan list")
            return
        }

        targetDeviceElement.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12), "Main tabs did not open after tapping preferred BLE device")
        XCTAssertTrue(
            waitForSynchronizationCompletion(in: app, timeout: 60),
            "Synchronization did not complete before opening Special settings"
        )

        let specialTabButton = app.tabBars.buttons[AccessibilityIdentifier.mainTabSpecialSettingsItem]
        XCTAssertTrue(specialTabButton.waitForExistence(timeout: 5), "Special settings tab button was not found")
        specialTabButton.tap()
        XCTAssertTrue(specialTabButton.isSelected, "Special settings tab did not become selected")

        let widgetsTable = app.tables[AccessibilityIdentifier.widgetsTable]
        XCTAssertTrue(widgetsTable.waitForExistence(timeout: 45), "Widgets table did not appear")

        let spinnerTitle = elementByIdentifierOrLabels(
            in: app,
            identifier: "unused.spinner.title.identifier",
            fallbackLabels: [
                "Режим работы протеза",
                "Режим работы EMG",
                "Режим работы ЕМГ"
            ]
        )
        XCTAssertTrue(
            scrollToElement(spinnerTitle, in: widgetsTable, maxSwipes: 12),
            "Could not find spinner widget title on Special settings screen"
        )
        XCTAssertTrue(spinnerTitle.waitForExistence(timeout: 5), "Spinner widget title was not found")

        let triggerButton: XCUIElement
        if let dropdownButton = buttonInSameRow(as: spinnerTitle, in: widgetsTable) {
            triggerButton = dropdownButton
            triggerButton.tap()
        } else {
            let openDropdownCoordinate = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0)).withOffset(
                CGVector(
                    dx: min(widgetsTable.frame.maxX - 90, spinnerTitle.frame.maxX + 150),
                    dy: spinnerTitle.frame.midY
                )
            )
            openDropdownCoordinate.tap()
            guard let fallbackButton = buttonInSameRow(as: spinnerTitle, in: widgetsTable) else {
                XCTFail("Could not resolve spinner trigger button after fallback tap")
                return
            }
            triggerButton = fallbackButton
        }

        XCTAssertTrue(
            waitForDropdownOptionButtons(in: app, triggerButton: triggerButton, minimum: 2, timeout: 5),
            "Spinner dropdown did not open (options below trigger button were not detected)"
        )

        let dropdownCluster = dropdownPanelButtons(in: app, triggerButton: triggerButton)
        let preferredLastLabel = "Плавное управление силой и скоростью"
        let initialTriggerLabel = triggerButton.label
        guard let optionToTap = dropdownCluster.first(where: { $0.label == preferredLastLabel })
            ?? dropdownCluster.last(where: { $0.label != initialTriggerLabel })
            ?? dropdownCluster.last
        else {
            XCTFail("Could not resolve selectable spinner option in dropdown")
            return
        }
        let expectedSelectedLabel = optionToTap.label
        optionToTap.tap()

        XCTAssertTrue(
            waitForButtonLabel(triggerButton, expectedValues: [expectedSelectedLabel], timeout: 4),
            "Spinner selection was not applied after tapping dropdown option"
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

    private func tapSelectorSegment(_ selector: XCUIElement, normalizedX: CGFloat, normalizedY: CGFloat = 0.5) {
        let coordinate = selector.coordinate(withNormalizedOffset: CGVector(dx: normalizedX, dy: normalizedY))
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

    private func collectRotationRevealProgressSamples(
        from element: XCUIElement,
        duration: TimeInterval,
        interval: TimeInterval
    ) -> [Double] {
        let deadline = Date().addingTimeInterval(duration)
        var samples: [Double] = []
        while Date() < deadline {
            if let progress = rotationRevealProgress(from: element) {
                samples.append(progress)
            }
            RunLoop.current.run(until: Date().addingTimeInterval(interval))
        }
        return samples
    }

    private func rotationRevealProgress(from element: XCUIElement) -> Double? {
        guard let value = element.value as? String else { return nil }
        let parts = value.split(separator: ";")
        for rawPart in parts {
            let part = rawPart.trimmingCharacters(in: .whitespacesAndNewlines)
            if part.hasPrefix("progress=") {
                return Double(part.dropFirst("progress=".count))
            }
        }
        return nil
    }

    private func waitForBluetoothFilterSelectedIndex(
        _ selector: XCUIElement,
        expected: Int,
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let index = bluetoothFilterSelectedIndex(from: selector), index == expected {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }
        return false
    }

    private func waitForBluetoothFilterSelectedIndex(
        _ selector: XCUIElement,
        timeout: TimeInterval
    ) -> Int? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let index = bluetoothFilterSelectedIndex(from: selector) {
                return index
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }
        return nil
    }

    private func bluetoothFilterSelectedIndex(from selector: XCUIElement) -> Int? {
        guard let value = selector.value as? String else { return nil }
        guard let range = value.range(of: "selectedIndex=") else { return nil }
        let raw = value[range.upperBound...]
        return Int(raw.trimmingCharacters(in: .whitespacesAndNewlines))
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

    private func waitForDropdownOptionButtons(
        in app: XCUIApplication,
        triggerButton: XCUIElement,
        minimum: Int,
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if dropdownOptionButtons(in: app, triggerButton: triggerButton).count >= minimum {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }
        return dropdownOptionButtons(in: app, triggerButton: triggerButton).count >= minimum
    }

    private func waitForDropdownOptionButtons(
        in app: XCUIApplication,
        triggerButton: XCUIElement,
        maximum: Int,
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if dropdownOptionButtons(in: app, triggerButton: triggerButton).count <= maximum {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }
        return dropdownOptionButtons(in: app, triggerButton: triggerButton).count <= maximum
    }

    private func waitForButtonLabel(
        _ button: XCUIElement,
        expectedValues: [String],
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if expectedValues.contains(button.label) {
                return true
            }
            if let value = button.value as? String, expectedValues.contains(value) {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }
        return expectedValues.contains(button.label)
    }

    private func dropdownOptionButtons(in app: XCUIApplication, triggerButton: XCUIElement) -> [XCUIElement] {
        let triggerFrame = triggerButton.frame
        guard !triggerFrame.isEmpty else { return [] }

        let maxHorizontalDelta = max(44.0, triggerFrame.width * 0.4)
        let minY = triggerFrame.maxY + 4
        let maxY = triggerFrame.maxY + 420

        return app.descendants(matching: .button)
            .allElementsBoundByIndex
            .filter { button in
                button.exists &&
                button.frame.minY >= minY &&
                button.frame.maxY <= maxY &&
                abs(button.frame.midX - triggerFrame.midX) <= maxHorizontalDelta &&
                button.frame.width >= triggerFrame.width * 0.6
            }
    }

    private func dropdownPanelButtons(in app: XCUIApplication, triggerButton: XCUIElement) -> [XCUIElement] {
        let sorted = dropdownOptionButtons(in: app, triggerButton: triggerButton)
            .sorted { $0.frame.minY < $1.frame.minY }
        guard let first = sorted.first else { return [] }

        var cluster: [XCUIElement] = [first]
        var previousMaxY = first.frame.maxY
        for button in sorted.dropFirst() {
            let gap = button.frame.minY - previousMaxY
            if gap > 12 {
                break
            }
            cluster.append(button)
            previousMaxY = button.frame.maxY
        }
        return cluster
    }

    private func buttonInSameRow(as titleElement: XCUIElement, in table: XCUIElement) -> XCUIElement? {
        let titleMidY = titleElement.frame.midY
        let titleMaxX = titleElement.frame.maxX

        return table.descendants(matching: .button)
            .allElementsBoundByIndex
            .first(where: { button in
                button.exists &&
                abs(button.frame.midY - titleMidY) < 30 &&
                button.frame.minX > titleMaxX - 20
            })
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
