import XCTest
import UIKit

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

    private struct TabBarColorFrame {
        let selectedTag: Int
        let allowedColors: Set<String>
        let items: [TabBarColorItem]
    }

    private struct TabBarColorItem {
        let index: Int
        let tag: Int
        let isSelected: Bool
        let iconColors: Set<String>
        let textColors: Set<String>
        let isHighlighted: Bool
        let iconCount: Int
        let textCount: Int
    }

    private struct LegacyBleCapturedCommand {
        let sequence: Int
        let type: String
        let characteristic: String
        let bytes: String
        let caseValue: String
    }

    private struct LegacyBleExpectedCommand {
        let actionName: String
        let expectedDescription: String
        let type: String
        let characteristics: [String]
        let minimumCount: Int
        let requiredBytes: String?
        let minimumDistinctBytes: Int?
    }

    private struct LegacyBleForbiddenCommand {
        let actionName: String
        let reason: String
        let type: String
        let characteristics: [String]
    }

    private let preferredDeviceCandidates = [
        "FTHS3-Рома1",
        "Рома1",
        "Роман",
        "FTHS3-Роман",
    ]
    private let legacyBleCommandProbeIdentifier = "legacyBleCommandProbe"

    override func setUp() {
        continueAfterFailure = false
    }

    func testBluetoothScan_whenTapFirstCellOnce_thenMainTabsOpen() {
        let app = XCUIApplication()
        app.launchArguments += ["-ui-test-fake-ble-device", "-ui-test-ble-noise"]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 1))

        let firstDeviceCell = devicesTable.cells["ble.deviceCell.0"]
        XCTAssertTrue(firstDeviceCell.waitForExistence(timeout: 1))

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
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 1), "BLE table did not appear")
        
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

    func testMergedScanRealDevice_whenTapFestH04921_thenLegacyFlowOpens() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["FEST-H-04921"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device FEST-H-04921 in scan list")
            return
        }

        targetDeviceElement.tap()

        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].waitForExistence(timeout: 10),
            "Legacy OldMotoricaStart root did not open after tapping FEST-H-04921"
        )
        XCTAssertFalse(
            app.otherElements[AccessibilityIdentifier.mainTabBarRoot].exists,
            "New MainTabBar opened for FEST-H-04921, expected legacy flow"
        )
        XCTAssertTrue(
            waitForAnyStaticText(
                containingAnyOf: ["Activity Sensors", "opening sensor sensitivity", "Driver", "Датчики"],
                in: app,
                timeout: 45
            ),
            "Legacy sensors UI did not appear after tapping FEST-H-04921"
        )

        let screenshotAttachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshotAttachment.name = "legacy-flow-after-fest"
        screenshotAttachment.lifetime = .keepAlways
        add(screenshotAttachment)
    }

    func testMergedScanRealDevice_whenTapLegacyGestureSettings_thenGestureConfiguratorOpens() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["FEST-H-04921"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device FEST-H-04921 in scan list")
            return
        }

        targetDeviceElement.tap()

        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].waitForExistence(timeout: 10),
            "Legacy OldMotoricaStart root did not open after tapping FEST-H-04921"
        )
        XCTAssertTrue(
            waitForAnyStaticText(
                containingAnyOf: ["Activity Sensors", "opening sensor sensitivity", "Driver", "Датчики"],
                in: app,
                timeout: 45
            ),
            "Legacy sensors UI did not appear after tapping FEST-H-04921"
        )

        guard let legacyGestureSettingsButton = legacyGestureSettingsButton(in: app, deviceName: "FEST-H-04921") else {
            XCTFail("Legacy gesture settings button did not appear")
            return
        }
        XCTAssertTrue(legacyGestureSettingsButton.isHittable, "Legacy gesture settings button is not hittable")
        legacyGestureSettingsButton.tap()

        XCTAssertTrue(
            waitForAnyStaticText(
                containingAnyOf: ["Activity Gestures", "gesture switching by sensors", "GESTURE 1", "ЖЕСТ"],
                in: app,
                timeout: 10
            ),
            "Legacy gesture configurator did not open"
        )

        let screenshotAttachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshotAttachment.name = "legacy-gesture-configurator-after-fest"
        screenshotAttachment.lifetime = .keepAlways
        add(screenshotAttachment)
    }

    func testMergedScanRealDevice_whenTapLegacyGestureGear_thenGestureGripperConfiguratorOpens() {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["FEST-H-04921"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device FEST-H-04921 in scan list")
            return
        }

        targetDeviceElement.tap()

        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].waitForExistence(timeout: 10),
            "Legacy OldMotoricaStart root did not open after tapping FEST-H-04921"
        )
        XCTAssertTrue(
            waitForAnyStaticText(
                containingAnyOf: ["Activity Sensors", "opening sensor sensitivity", "Driver", "Датчики"],
                in: app,
                timeout: 45
            ),
            "Legacy sensors UI did not appear after tapping FEST-H-04921"
        )

        guard let legacyGestureSettingsButton = legacyGestureSettingsButton(in: app, deviceName: "FEST-H-04921") else {
            XCTFail("Legacy gesture settings button did not appear")
            return
        }
        XCTAssertTrue(legacyGestureSettingsButton.isHittable, "Legacy gesture settings button is not hittable")
        legacyGestureSettingsButton.tap()
        XCTAssertTrue(
            waitForAnyStaticText(
                containingAnyOf: ["Activity Gestures", "gesture switching by sensors", "GESTURE 1", "ЖЕСТ"],
                in: app,
                timeout: 10
            ),
            "Legacy gesture screen did not open"
        )

        guard let gesture2SettingsButton = legacyGestureGearButton(in: app, gestureLabel: "gesture 2") else {
            XCTFail("Legacy gesture 2 settings button did not appear")
            return
        }
        XCTAssertTrue(gesture2SettingsButton.isHittable, "Legacy gesture 2 settings button is not hittable")
        gesture2SettingsButton.tap()
        XCTAssertTrue(
            waitForAnyVisibleElement(
                containingAnyOf: ["open state", "Activity Gripper", "little finger"],
                in: app,
                timeout: 10
            ),
            "Legacy gripper configurator did not open after tapping gesture gear"
        )

        let screenshotAttachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshotAttachment.name = "legacy-gesture-gripper-configurator-after-fest"
        screenshotAttachment.lifetime = .keepAlways
        add(screenshotAttachment)
    }

    func testStandaloneOldAppRealDevice_whenTapFestH04921_thenRecordAdvancedSettingsState() {
        let app = XCUIApplication(bundleIdentifier: "com.motorica.startt")
        app.launch()
        dismissBluetoothPermissionIfNeeded()

        if !waitForLegacySensorsScreen(in: app, timeout: 8) {
            guard let targetDeviceElement = waitForLegacyDeviceElement(
                namedAnyOf: ["FEST-H-04921"],
                in: app,
                timeout: 60
            ) else {
                XCTFail("Could not find BLE device FEST-H-04921 in standalone old app scan list")
                return
            }

            targetDeviceElement.tap()
            XCTAssertTrue(
                waitForLegacySensorsScreen(in: app, timeout: 45),
                "Standalone old app legacy sensors UI did not appear after tapping FEST-H-04921"
            )
        }

        recordLegacyAdvancedSettingsState(
            in: app,
            attachmentName: "standalone-old-app-advanced-settings-state-after-fest"
        )
        app.terminate()
    }

    func testMergedOldAppRealDevice_whenTapFestH04921_thenRecordAdvancedSettingsState() {
        let app = XCUIApplication()
        app.launch()
        dismissBluetoothPermissionIfNeeded()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")
        selectAllDevicesFilterIfVisible(in: app)

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["FEST-H-04921", "FEST-XFTHS04921"],
            in: devicesTable,
            timeout: 60
        ) else {
            attachLegacyBleProbeValue(dumpUIStatePage(in: app, pageIndex: 0), name: "merged-old-app-short-ble-probe-scan-state")
            XCTFail("Could not find BLE device FEST-H-04921 in merged scan list")
            return
        }

        targetDeviceElement.tap()
        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].waitForExistence(timeout: 10),
            "Merged legacy OldMotoricaStart root did not open after tapping FEST-H-04921"
        )
        XCTAssertTrue(
            waitForLegacySensorsScreen(in: app, timeout: 45),
            "Merged legacy sensors UI did not appear after tapping FEST-H-04921"
        )

        recordLegacyAdvancedSettingsState(
            in: app,
            attachmentName: "merged-old-app-advanced-settings-state-after-fest"
        )
        app.terminate()
    }

    func testStandaloneOldAppRealDevice_whenExerciseAdvancedSettingsControls_thenEmitBleCommandMarkers() {
        let app = XCUIApplication(bundleIdentifier: "com.motorica.startt")
        app.launch()
        dismissBluetoothPermissionIfNeeded()

        if !waitForLegacySensorsScreen(in: app, timeout: 8) {
            guard let targetDeviceElement = waitForLegacyDeviceElement(
                namedAnyOf: ["FEST-H-04921"],
                in: app,
                timeout: 60
            ) else {
                XCTFail("Could not find BLE device FEST-H-04921 in standalone old app scan list")
                return
            }

            targetDeviceElement.tap()
            XCTAssertTrue(
                waitForLegacySensorsScreen(in: app, timeout: 45),
                "Standalone old app legacy sensors UI did not appear after tapping FEST-H-04921"
            )
        }

        XCTAssertTrue(openLegacyAdvancedSettings(in: app), "Standalone old app advanced settings did not open")
        exerciseLegacyAdvancedSettingsControls(in: app, runName: "standalone-old-app", probeSession: nil)
        app.terminate()
    }

    func testMergedOldAppRealDevice_whenExerciseAdvancedSettingsControls_thenEmitBleCommandMarkers() {
        let app = XCUIApplication()
        let probeSession = configureLegacyBleCommandLogEnvironment(for: app)
        app.launch()
        dismissBluetoothPermissionIfNeeded()

        XCTAssertTrue(
            waitForLegacyBleProbeReady(session: probeSession, in: app, timeout: 5),
            "Legacy BLE command probe did not start in merged app"
        )

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")
        selectAllDevicesFilterIfVisible(in: app)

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["FEST-H-04921", "FEST-XFTHS04921"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device FEST-H-04921 in merged scan list")
            return
        }

        targetDeviceElement.tap()
        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].waitForExistence(timeout: 10),
            "Merged legacy OldMotoricaStart root did not open after tapping FEST-H-04921"
        )
        XCTAssertTrue(
            waitForLegacySensorsScreen(in: app, timeout: 45),
            "Merged legacy sensors UI did not appear after tapping FEST-H-04921"
        )

        XCTAssertTrue(openLegacyAdvancedSettings(in: app), "Merged old app advanced settings did not open")
        exerciseLegacyAdvancedSettingsControls(in: app, runName: "merged-old-app", probeSession: probeSession)
        app.terminate()
    }

    func testMergedOldAppRealDevice_whenAcceptLegacyResetDialogs_thenCapturesResetBleCommands() {
        let app = XCUIApplication()
        let probeSession = configureLegacyBleCommandLogEnvironment(for: app)
        app.launch()
        dismissBluetoothPermissionIfNeeded()

        XCTAssertTrue(
            waitForLegacyBleProbeReady(session: probeSession, in: app, timeout: 5),
            "Legacy BLE command probe did not start in merged app"
        )

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")
        selectAllDevicesFilterIfVisible(in: app)

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["FEST-H-04921", "FEST-XFTHS04921"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device FEST-H-04921 in merged scan list")
            return
        }

        targetDeviceElement.tap()
        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].waitForExistence(timeout: 10),
            "Merged legacy OldMotoricaStart root did not open after tapping FEST-H-04921"
        )
        XCTAssertTrue(
            waitForLegacySensorsScreen(in: app, timeout: 45),
            "Merged legacy sensors UI did not appear after tapping FEST-H-04921"
        )

        guard let legacyGestureSettingsButton = legacyGestureSettingsButton(in: app, deviceName: "FEST-H-04921") else {
            XCTFail("Legacy gesture settings button did not appear")
            return
        }
        XCTAssertTrue(legacyGestureSettingsButton.isHittable, "Legacy gesture settings button is not hittable")
        legacyGestureSettingsButton.tap()
        XCTAssertTrue(
            waitForAnyStaticText(
                containingAnyOf: ["Activity Gestures", "gesture switching by sensors", "GESTURE 1", "ЖЕСТ"],
                in: app,
                timeout: 10
            ),
            "Legacy gesture settings screen did not open"
        )

        var probeValue = waitForLegacyBleProbeDrain(
            session: probeSession,
            in: app,
            runName: "merged-old-app-reset-commands",
            quietPeriod: 0.5,
            timeout: 5
        ) ?? legacyBleProbeValue(session: probeSession, in: app) ?? "count=0 last=none"
        var baselineCount = legacyBleProbeCount(from: probeValue) ?? 0

        XCTAssertTrue(tapLegacyButtonVisible(titled: "RESET GESTURES", in: app), "Could not tap RESET GESTURES")
        XCTAssertTrue(tapDialogButton(titledAnyOf: ["OK", "RESET", "reset", "ОК"], in: app), "Could not accept gestures reset dialog")
        guard let gestureResetProbeValue = waitForLegacyBleCommandCapture(
            after: baselineCount,
            session: probeSession,
            in: app,
            timeout: 5,
            characteristics: resetCommandCharacteristics(),
            requiredBytes: "03"
        ) else {
            let value = legacyBleProbeValue(session: probeSession, in: app) ?? "probe value missing"
            attachLegacyBleProbeValue(value, name: "merged-old-app-reset-commands-gesture-reset-failed")
            XCTFail("Gesture reset command 03 was not captured. Probe: \(value)")
            return
        }
        attachLegacyBleProbeValue(gestureResetProbeValue, name: "merged-old-app-reset-commands-after-gesture-reset")

        XCTAssertTrue(tapLegacyButtonVisible(titled: " Back ", in: app), "Could not return from gesture settings")
        XCTAssertTrue(waitForLegacySensorsScreen(in: app, timeout: 10), "Legacy sensors screen did not reappear after gesture settings")

        XCTAssertTrue(openLegacyAdvancedSettings(in: app), "Merged old app advanced settings did not open")
        scrollLegacyAdvancedSettingsToBottomOnce(in: app)

        probeValue = waitForLegacyBleProbeDrain(
            session: probeSession,
            in: app,
            runName: "merged-old-app-reset-commands",
            quietPeriod: 0.5,
            timeout: 5
        ) ?? legacyBleProbeValue(session: probeSession, in: app) ?? gestureResetProbeValue
        baselineCount = legacyBleProbeCount(from: probeValue) ?? baselineCount

        XCTAssertTrue(tapLegacyButtonVisible(titled: "SOFT RESET TO FACTORY SETTINGS", in: app), "Could not tap SOFT RESET TO FACTORY SETTINGS")
        XCTAssertTrue(tapDialogButton(titledAnyOf: ["RESET", "reset"], in: app), "Could not accept soft reset dialog")
        guard let softResetProbeValue = waitForLegacyBleCommandCapture(
            after: baselineCount,
            session: probeSession,
            in: app,
            timeout: 5,
            characteristics: resetCommandCharacteristics(),
            requiredBytes: "02"
        ) else {
            let value = legacyBleProbeValue(session: probeSession, in: app) ?? "probe value missing"
            attachLegacyBleProbeValue(value, name: "merged-old-app-reset-commands-soft-reset-failed")
            XCTFail("Soft reset command 02 was not captured. Probe: \(value)")
            return
        }
        attachLegacyBleProbeValue(softResetProbeValue, name: "merged-old-app-reset-commands-after-soft-reset")

        scrollLegacyAdvancedSettingsToBottomOnce(in: app)
        baselineCount = legacyBleProbeCount(from: softResetProbeValue) ?? baselineCount

        XCTAssertTrue(tapLegacyButtonVisible(titled: "RESET TO FACTORY SETTINGS", in: app), "Could not tap RESET TO FACTORY SETTINGS")
        XCTAssertTrue(tapDialogButton(titledAnyOf: ["RESET", "reset"], in: app), "Could not accept hard reset dialog")
        guard let hardResetProbeValue = waitForLegacyBleCommandCapture(
            after: baselineCount,
            session: probeSession,
            in: app,
            timeout: 5,
            characteristics: resetCommandCharacteristics(),
            requiredBytes: "01"
        ) else {
            let value = legacyBleProbeValue(session: probeSession, in: app) ?? "probe value missing"
            attachLegacyBleProbeValue(value, name: "merged-old-app-reset-commands-hard-reset-failed")
            XCTFail("Hard reset command 01 was not captured. Probe: \(value)")
            return
        }

        let finalCommands = legacyBleCommands(from: hardResetProbeValue)
        let resetCommands = finalCommands.filter { command in
            resetCommandCharacteristics().contains {
                command.characteristic.caseInsensitiveCompare($0) == .orderedSame
            }
        }
        let report = legacyResetCommandReport(
            session: probeSession,
            commands: resetCommands,
            finalProbeValue: hardResetProbeValue
        )
        attachLegacyBleProbeValue(report, name: "merged-old-app-reset-commands-history")
        app.terminate()
    }

    func testMergedOldAppRealDevice_whenTapOpen_thenCapturesLegacyMotorBleCommandWithinOneSecond() {
        let app = XCUIApplication()
        let probeSession = configureLegacyBleCommandLogEnvironment(for: app)
        app.launch()
        dismissBluetoothPermissionIfNeeded()

        XCTAssertTrue(
            waitForLegacyBleProbeReady(session: probeSession, in: app, timeout: 5),
            "Legacy BLE command probe did not appear"
        )

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["FEST-H-04921"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device FEST-H-04921 in merged scan list")
            return
        }

        targetDeviceElement.tap()
        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].waitForExistence(timeout: 10),
            "Merged legacy OldMotoricaStart root did not open after tapping FEST-H-04921"
        )
        XCTAssertTrue(
            waitForLegacySensorsScreen(in: app, timeout: 45),
            "Merged legacy sensors UI did not appear after tapping FEST-H-04921"
        )

        let initialProbeValue = legacyBleProbeValue(session: probeSession, in: app) ?? "count=0 last=none"
        let initialCount = legacyBleProbeCount(from: initialProbeValue) ?? 0
        print("[BLE_COMMAND_SHORT_TEST] initialProbeValue=\"\(initialProbeValue)\"")

        XCTAssertTrue(
            tapLegacyButtonVisible(titled: "OPEN", in: app),
            "Could not tap visible OPEN button"
        )

        guard let capturedProbeValue = waitForLegacyMotorBleCommandCapture(
            after: initialCount,
            session: probeSession,
            in: app,
            timeout: 1.0
        ) else {
            let finalProbeValue = legacyBleProbeValue(session: probeSession, in: app) ?? "probe value missing"
            attachLegacyBleProbeValue(finalProbeValue, name: "merged-old-app-short-ble-probe-failed")
            XCTFail("No OPEN/CLOSE motor BLE command was captured within 1 second. Probe: \(finalProbeValue)")
            return
        }

        attachLegacyBleProbeValue(capturedProbeValue, name: "merged-old-app-short-ble-probe-captured")
        print("[BLE_COMMAND_SHORT_TEST] capturedProbeValue=\"\(capturedProbeValue)\"")
        app.terminate()
    }

    @discardableResult
    private func configureLegacyBleCommandLogEnvironment(for app: XCUIApplication) -> String {
        let probeSession = UUID().uuidString
        app.launchArguments += ["-legacy-ble-command-probe"]
        app.launchEnvironment["OS_ACTIVITY_DT_MODE"] = "YES"
        app.launchEnvironment["NSUnbufferedIO"] = "YES"
        app.launchEnvironment["MOTORICA_LEGACY_BLE_COMMAND_PROBE"] = "1"
        app.launchEnvironment["MOTORICA_LEGACY_BLE_COMMAND_PROBE_SESSION"] = probeSession
        return probeSession
    }

    private func waitForLegacyBleProbeReady(
        session: String,
        in app: XCUIApplication,
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if legacyBleProbeValue(session: session, in: app) != nil {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }
        return false
    }

    private func legacyBleProbeValue(session: String, in app: XCUIApplication) -> String? {
        if let pasteboardValue = UIPasteboard(name: UIPasteboard.Name("com.motorica.legacyBleCommandProbe"), create: false)?.string,
           pasteboardValue.contains("session=\(session)") {
            return pasteboardValue
        }

        let probe = app.descendants(matching: .any)[legacyBleCommandProbeIdentifier]
        guard probe.exists else {
            return nil
        }

        if let value = probe.value as? String, !value.isEmpty {
            return value.contains("session=\(session)") ? value : nil
        }

        let label = probe.label
        guard !label.isEmpty, label.contains("session=\(session)") else {
            return nil
        }
        return label
    }

    private func legacyBleProbeCount(from value: String) -> Int? {
        let pattern = #"count=(\d+)"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else {
            return nil
        }

        let nsRange = NSRange(value.startIndex..<value.endIndex, in: value)
        guard let match = regex.firstMatch(in: value, range: nsRange),
              let range = Range(match.range(at: 1), in: value) else {
            return nil
        }

        return Int(value[range])
    }

    private func waitForLegacyMotorBleCommandCapture(
        after initialCount: Int,
        session: String,
        in app: XCUIApplication,
        timeout: TimeInterval
    ) -> String? {
        let motorCharacteristics = [
            "43680002-4D74-1001-726B-526F64696F6E",
            "43680003-4D74-1001-726B-526F64696F6E",
            "43686172-4D74-726B-0002-526F64696F6E",
            "43686172-4D74-726B-0003-526F64696F6E"
        ]
        let deadline = Date().addingTimeInterval(timeout)

        while Date() < deadline {
            if let value = legacyBleProbeValue(session: session, in: app),
               let count = legacyBleProbeCount(from: value),
               count > initialCount,
               motorCharacteristics.contains(where: { value.localizedCaseInsensitiveContains($0) }) {
                return value
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }

        return nil
    }

    private func waitForLegacyBleCommandCapture(
        after initialCount: Int,
        session: String,
        in app: XCUIApplication,
        timeout: TimeInterval,
        characteristics: [String],
        requiredBytes: String
    ) -> String? {
        let deadline = Date().addingTimeInterval(timeout)

        while Date() < deadline {
            if let value = legacyBleProbeValue(session: session, in: app) {
                let commands = legacyBleCommands(from: value).filter { $0.sequence > initialCount }
                let didCapture = commands.contains { command in
                    command.type.caseInsensitiveCompare("WRITE") == .orderedSame &&
                        command.bytes.caseInsensitiveCompare(requiredBytes) == .orderedSame &&
                        characteristics.contains {
                            command.characteristic.caseInsensitiveCompare($0) == .orderedSame
                        }
                }
                if didCapture {
                    return value
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }

        return nil
    }

    private func resetCommandCharacteristics() -> [String] {
        [
            "43680100-4D74-1001-726B-526F64696F6E",
            "43686172-4D74-726B-0100-526F64696F6E"
        ]
    }

    private func legacyResetCommandReport(
        session: String,
        commands: [LegacyBleCapturedCommand],
        finalProbeValue: String
    ) -> String {
        var lines = [
            "run=merged-old-app-reset-commands",
            "session=\(session)",
            "Captured reset commands:"
        ]
        if commands.isEmpty {
            lines.append("- none")
        } else {
            for command in commands {
                lines.append(
                    "- seq=\(command.sequence) type=\(command.type) characteristic=\(command.characteristic) bytes=\(command.bytes) case=\(command.caseValue)"
                )
            }
        }
        lines.append("")
        lines.append("Expected reset payloads:")
        lines.append("- gestures reset: bytes=03")
        lines.append("- soft reset: bytes=02")
        lines.append("- hard reset: bytes=01")
        lines.append("")
        lines.append("Raw final probe value:")
        lines.append(finalProbeValue)
        return lines.joined(separator: "\n")
    }

    private func attachLegacyBleProbeValue(_ value: String, name: String) {
        let attachment = XCTAttachment(
            data: Data(value.utf8),
            uniformTypeIdentifier: "public.plain-text"
        )
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func selectAllDevicesFilterIfVisible(in app: XCUIApplication) {
        for title in ["All devices", "Все устройства"] {
            let button = app.buttons.matching(NSPredicate(format: "label ==[c] %@", title)).firstMatch
            if button.waitForExistence(timeout: 0.5) {
                button.tap()
                RunLoop.current.run(until: Date().addingTimeInterval(0.3))
                return
            }

            let staticText = app.staticTexts.matching(NSPredicate(format: "label ==[c] %@", title)).firstMatch
            if staticText.waitForExistence(timeout: 0.5) {
                staticText.tap()
                RunLoop.current.run(until: Date().addingTimeInterval(0.3))
                return
            }
        }
    }

    func testMergedScanRealDevice_whenTap111111_thenNewFlowOpens() {
        let app = XCUIApplication()
        app.launchArguments += ["-ui-test-skip-synchronization"]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["111111"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device 111111 in scan list")
            return
        }

        targetDeviceElement.tap()

        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.mainTabBarRoot].waitForExistence(timeout: 12),
            "Main tabs did not open after tapping BLE device 111111"
        )
        XCTAssertFalse(
            app.otherElements[AccessibilityIdentifier.oldMotoricaStartRoot].exists,
            "Legacy OldMotoricaStart root opened for 111111, expected new flow"
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

    func testGestureSettingsV3_whenOpenCustomGesture7_thenGesturePayloadArrivesAndMapsToFingers() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui-test-skip-synchronization",
            "-ui-test-force-gestures-widget",
            "-ui-test-expose-gesture-settings-state",
            "-ui-test-inject-v3-gesture-70"
        ]
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
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12), "Main tabs did not open after connecting preferred BLE device")

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
        XCTAssertTrue(selector.waitForExistence(timeout: 5), "Gestures segment selector did not appear")

        tapSelectorSegment(selector, normalizedX: 0.25, normalizedY: 0.5)
        XCTAssertTrue(
            waitForSelectorSegment(selector, expected: "collection", timeout: 2),
            "Collection segment was not selected before opening custom gesture settings"
        )

        let gesture7SettingsButtonIdentifier = "\(AccessibilityIdentifier.gesturesCustomSettingsButtonPrefix).70"
        let gesture7SettingsButtons = app.buttons.matching(identifier: gesture7SettingsButtonIdentifier)
        let gesture7SettingsButton = gesture7SettingsButtons.firstMatch
        XCTAssertTrue(
            scrollToElement(gesture7SettingsButton, in: widgetsTable, maxSwipes: 12),
            "Could not find settings button for custom gesture #7 (id=70)"
        )
        XCTAssertTrue(gesture7SettingsButton.waitForExistence(timeout: 4), "Gesture #7 settings button did not appear")
        let buttonToTap = gesture7SettingsButtons.allElementsBoundByIndex.first(where: { $0.exists && $0.isHittable })
            ?? gesture7SettingsButton
        buttonToTap.tap()

        let gestureSettingsScreen = app.staticTexts[AccessibilityIdentifier.gestureSettingsScreen]
        XCTAssertTrue(gestureSettingsScreen.waitForExistence(timeout: 6), "Gesture settings screen did not open")

        let expectedTokens = [
            "gestureId=70",
            "openStage1=0",
            "openStage2=100",
            "openStage3=97",
            "openStage4=0",
            "openStage5=0",
            "openStage6=0",
            "closeStage1=100",
            "closeStage2=100",
            "closeStage3=100",
            "closeStage4=100",
            "closeStage5=100",
            "closeStage6=0"
        ]

        XCTAssertTrue(
            waitForElementValueContainingAllTokens(gestureSettingsScreen, tokens: expectedTokens, timeout: 8),
            "Gesture #7 payload did not arrive or was mapped to finger stages incorrectly"
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

    func testBottomNavigation_whenSwitchingTabs_thenIconAndTextColorsStayOnUbi4PaletteWithoutFlicker() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-ui-test-skip-synchronization",
            "-ui-test-tabbar-color-probe"
        ]
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDeviceElement = waitForDeviceElement(
            namedAnyOf: ["111111"],
            in: devicesTable,
            timeout: 60
        ) else {
            XCTFail("Could not find BLE device 111111 in scan list")
            return
        }
        targetDeviceElement.tap()

        let mainTabsRoot = app.otherElements[AccessibilityIdentifier.mainTabBarRoot]
        XCTAssertTrue(mainTabsRoot.waitForExistence(timeout: 12), "Main tabs did not open after connecting to BLE device 111111")

        let colorProbe = app.descendants(matching: .any)[AccessibilityIdentifier.mainTabBarColorProbe]
        XCTAssertTrue(colorProbe.waitForExistence(timeout: 3), "Tab bar color probe did not appear")

        guard let initialFrame = waitForTabBarColorFrame(colorProbe, timeout: 3) else {
            XCTFail("Tab bar color probe did not publish state; label=\(colorProbe.label); value=\(String(describing: colorProbe.value))")
            return
        }
        assertTabBarColorFrameIsStable(initialFrame, expectedSelectedTag: initialFrame.selectedTag)

        let buttons = sortedTabBarButtons(in: app)
        XCTAssertGreaterThanOrEqual(buttons.count, 3, "Expected at least three bottom navigation buttons")
        XCTAssertEqual(
            buttons.count,
            initialFrame.items.count,
            "Tab bar button count and color probe item count differ"
        )

        for targetIndex in buttons.indices {
            guard let currentFrame = waitForTabBarColorFrame(colorProbe, timeout: 1),
                  let targetItem = currentFrame.items.first(where: { $0.index == targetIndex })
            else {
                XCTFail("Could not resolve tab color state before tapping index \(targetIndex)")
                return
            }

            assertBottomNavigationColorTransition(
                tabRoot: colorProbe,
                targetButton: buttons[targetIndex],
                targetTag: targetItem.tag,
                startTag: currentFrame.selectedTag
            )
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
            "Spinner dropdown did not open (options near trigger button were not detected)"
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

    private func sortedTabBarButtons(in app: XCUIApplication) -> [XCUIElement] {
        app.tabBars.buttons.allElementsBoundByIndex
            .filter { $0.exists }
            .sorted { $0.frame.midX < $1.frame.midX }
    }

    private func assertBottomNavigationColorTransition(
        tabRoot: XCUIElement,
        targetButton: XCUIElement,
        targetTag: Int,
        startTag: Int,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        var samples: [TabBarColorFrame] = []
        if let beforeTap = tabBarColorFrame(from: tabRoot) {
            samples.append(beforeTap)
        }

        targetButton.tap()

        let deadline = Date().addingTimeInterval(0.8)
        var hasObservedTarget = false
        var didRollbackAfterTarget = false

        while Date() < deadline {
            if let frame = tabBarColorFrame(from: tabRoot) {
                if frame.selectedTag == targetTag {
                    hasObservedTarget = true
                } else if hasObservedTarget && frame.selectedTag == startTag && startTag != targetTag {
                    didRollbackAfterTarget = true
                }
                samples.append(frame)
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.016))
        }

        guard let finalFrame = waitForTabBarColorFrame(tabRoot, selectedTag: targetTag, timeout: 2) else {
            XCTFail("Bottom navigation did not end on selected tag \(targetTag)", file: file, line: line)
            return
        }
        samples.append(finalFrame)

        XCTAssertGreaterThanOrEqual(samples.count, 8, "Not enough color samples to validate tab switch", file: file, line: line)
        XCTAssertTrue(hasObservedTarget, "Bottom navigation did not expose target selection during sampled transition", file: file, line: line)
        XCTAssertFalse(didRollbackAfterTarget, "Bottom navigation selected tag rolled back during color transition", file: file, line: line)

        for frame in samples {
            assertTabBarColorFrameIsStable(frame, expectedSelectedTag: frame.selectedTag, file: file, line: line)
        }

        assertTabBarColorFrameIsStable(finalFrame, expectedSelectedTag: targetTag, file: file, line: line)
    }

    private func assertTabBarColorFrameIsStable(
        _ frame: TabBarColorFrame,
        expectedSelectedTag: Int,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let expectedWhite = "#FCFCFC"
        let expectedInactive = "#838383"
        let expectedAllowedColors: Set<String> = [expectedWhite, expectedInactive]
        let allowedColors = frame.allowedColors.isEmpty ? expectedAllowedColors : frame.allowedColors

        XCTAssertEqual(
            allowedColors,
            expectedAllowedColors,
            "Tab bar probe expected only ubi4_white and ubi4_deactivate_text",
            file: file,
            line: line
        )

        for item in frame.items {
            let observedColors = item.iconColors.union(item.textColors)
            XCTAssertFalse(
                observedColors.contains("#000000"),
                "Black flicker detected in bottom navigation item \(item.index): \(frameDebugDescription(frame))",
                file: file,
                line: line
            )
            XCTAssertTrue(
                observedColors.isSubset(of: expectedAllowedColors),
                "Unexpected bottom navigation color(s) \(observedColors) in item \(item.index): \(frameDebugDescription(frame))",
                file: file,
                line: line
            )
            XCTAssertFalse(
                item.isHighlighted,
                "Bottom navigation item \(item.index) became highlighted during tap; this can cause the black flash",
                file: file,
                line: line
            )
            XCTAssertEqual(
                item.iconCount,
                1,
                "Bottom navigation item \(item.index) has duplicated visible icons: \(frameDebugDescription(frame))",
                file: file,
                line: line
            )
            XCTAssertEqual(
                item.textCount,
                1,
                "Bottom navigation item \(item.index) has duplicated visible labels: \(frameDebugDescription(frame))",
                file: file,
                line: line
            )

            let expectedColor = item.tag == expectedSelectedTag ? expectedWhite : expectedInactive
            XCTAssertEqual(
                item.iconColors,
                Set([expectedColor]),
                "Bottom navigation icon color does not match selected state for item \(item.index): \(frameDebugDescription(frame))",
                file: file,
                line: line
            )
            XCTAssertEqual(
                item.textColors,
                Set([expectedColor]),
                "Bottom navigation text color does not match selected state for item \(item.index): \(frameDebugDescription(frame))",
                file: file,
                line: line
            )
        }
    }

    private func waitForTabBarColorFrame(
        _ tabRoot: XCUIElement,
        selectedTag: Int? = nil,
        timeout: TimeInterval
    ) -> TabBarColorFrame? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let frame = tabBarColorFrame(from: tabRoot),
               selectedTag == nil || frame.selectedTag == selectedTag {
                return frame
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }

        guard let frame = tabBarColorFrame(from: tabRoot) else { return nil }
        if let selectedTag, frame.selectedTag != selectedTag {
            return nil
        }
        return frame
    }

    private func tabBarColorFrame(from tabRoot: XCUIElement) -> TabBarColorFrame? {
        let value = (tabRoot.value as? String).flatMap { $0.isEmpty ? nil : $0 } ?? tabRoot.label
        guard !value.isEmpty else { return nil }

        var selectedTag: Int?
        var allowedColors = Set<String>()
        var items: [TabBarColorItem] = []

        for rawPart in value.split(separator: ";") {
            let part = String(rawPart)
            if part.hasPrefix("selectedTag=") {
                selectedTag = Int(part.dropFirst("selectedTag=".count))
            } else if part.hasPrefix("allowed=") {
                allowedColors = Set(
                    part
                        .dropFirst("allowed=".count)
                        .split(separator: ",")
                        .map(String.init)
                )
            } else if part.hasPrefix("items=") {
                items = part
                    .dropFirst("items=".count)
                    .split(separator: "|")
                    .compactMap(parseTabBarColorItem)
            }
        }

        guard let selectedTag, !items.isEmpty else { return nil }
        return TabBarColorFrame(
            selectedTag: selectedTag,
            allowedColors: allowedColors,
            items: items
        )
    }

    private func parseTabBarColorItem(_ rawItem: Substring) -> TabBarColorItem? {
        let fields = rawItem.split(separator: ",", omittingEmptySubsequences: false).map(String.init)
        guard fields.count == 8,
              let index = Int(fields[0]),
              let tag = Int(fields[1]),
              let iconCount = Int(fields[6]),
              let textCount = Int(fields[7])
        else {
            return nil
        }

        return TabBarColorItem(
            index: index,
            tag: tag,
            isSelected: fields[2] == "1",
            iconColors: colorSet(from: fields[3]),
            textColors: colorSet(from: fields[4]),
            isHighlighted: fields[5] == "1",
            iconCount: iconCount,
            textCount: textCount
        )
    }

    private func colorSet(from rawValue: String) -> Set<String> {
        if rawValue == "_" {
            return []
        }
        return Set(rawValue.split(separator: "+").map(String.init))
    }

    private func frameDebugDescription(_ frame: TabBarColorFrame) -> String {
        frame.items
            .map { item in
                "index=\(item.index),tag=\(item.tag),selected=\(item.isSelected),icons=\(Array(item.iconColors).sorted())/\(item.iconCount),texts=\(Array(item.textColors).sorted())/\(item.textCount),highlighted=\(item.isHighlighted)"
            }
            .joined(separator: " | ")
    }

    private func waitForDeviceElement(namedAnyOf candidates: [String], in table: XCUIElement, timeout: TimeInterval) -> XCUIElement? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            for name in candidates {
                let staticTextByLabel = table.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] %@", name)).firstMatch
                if staticTextByLabel.exists {
                    return staticTextByLabel
                }

                let cellByLabel = table.cells.matching(NSPredicate(format: "label CONTAINS[c] %@", name)).firstMatch
                if cellByLabel.exists {
                    return cellByLabel
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.4))
        }
        return nil
    }

    private func waitForAnyStaticText(
        containingAnyOf candidates: [String],
        in app: XCUIApplication,
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            for candidate in candidates {
                let element = app.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] %@", candidate)).firstMatch
                if element.exists {
                    return true
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.4))
        }
        return false
    }

    private func waitForAnyVisibleElement(
        containingAnyOf candidates: [String],
        in app: XCUIApplication,
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            for candidate in candidates {
                let predicate = NSPredicate(format: "label CONTAINS[c] %@", candidate)
                if app.staticTexts.matching(predicate).firstMatch.exists ||
                    app.buttons.matching(predicate).firstMatch.exists {
                    return true
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.4))
        }
        return false
    }

    private func legacyGestureSettingsButton(in app: XCUIApplication, deviceName: String) -> XCUIElement? {
        let deviceNameElement = app.staticTexts
            .matching(NSPredicate(format: "label CONTAINS[c] %@", deviceName))
            .firstMatch
        guard deviceNameElement.waitForExistence(timeout: 5) else { return nil }

        let markerFrame = deviceNameElement.frame
        return app.buttons.allElementsBoundByIndex
            .filter { button in
                button.exists &&
                button.frame.minX > markerFrame.maxX &&
                abs(button.frame.midY - markerFrame.midY) < 50 &&
                button.frame.width <= 80 &&
                button.frame.height <= 80
            }
            .sorted { $0.frame.minX < $1.frame.minX }
            .first
    }

    private func legacyGestureGearButton(in app: XCUIApplication, gestureLabel: String) -> XCUIElement? {
        if app.buttons["settings2"].exists {
            return app.buttons["settings2"]
        }

        let gestureButton = app.buttons
            .matching(NSPredicate(format: "label CONTAINS[c] %@", gestureLabel))
            .firstMatch
        guard gestureButton.waitForExistence(timeout: 5) else { return nil }

        let markerFrame = gestureButton.frame
        return app.buttons.allElementsBoundByIndex
            .filter { button in
                button.exists &&
                button.frame.width <= markerFrame.width * 0.45 &&
                abs(button.frame.midY - markerFrame.midY) < max(24, markerFrame.height * 0.5) &&
                button.frame.minX > markerFrame.midX &&
                button.frame.maxX <= markerFrame.maxX + 24
            }
            .sorted { $0.frame.minX > $1.frame.minX }
            .first
    }

    private func exerciseLegacyAdvancedSettingsControls(
        in app: XCUIApplication,
        runName: String,
        probeSession: String?
    ) {
        print("[BLE_COMMAND_TEST_BEGIN] run=\(runName)")
        let commandSettleTime: TimeInterval = 0
        let dialogCommandSettleTime: TimeInterval = 0
        let noCommandSettleTime: TimeInterval = 0
        var expectedCommands: [LegacyBleExpectedCommand] = []
        var forbiddenCommands: [LegacyBleForbiddenCommand] = []
        var noBleExpectedActions: [String] = []

        func expectCommand(
            actionName: String,
            expectedDescription: String,
            type: String = "WRITE",
            characteristics: [String],
            minimumCount: Int = 1,
            requiredBytes: String? = nil,
            minimumDistinctBytes: Int? = nil
        ) {
            expectedCommands.append(
                LegacyBleExpectedCommand(
                    actionName: actionName,
                    expectedDescription: expectedDescription,
                    type: type,
                    characteristics: characteristics,
                    minimumCount: minimumCount,
                    requiredBytes: requiredBytes,
                    minimumDistinctBytes: minimumDistinctBytes
                )
            )
        }

        func noteNoBleExpected(actionName: String, expectation: String) {
            noBleExpectedActions.append("\(actionName): \(expectation)")
        }

        func forbidCommand(
            actionName: String,
            reason: String,
            type: String = "WRITE",
            characteristics: [String]
        ) {
            forbiddenCommands.append(
                LegacyBleForbiddenCommand(
                    actionName: actionName,
                    reason: reason,
                    type: type,
                    characteristics: characteristics
                )
            )
        }

        scrollLegacyAdvancedSettingsToTop(in: app)
        attachLegacyVisibleState(in: app, runName: runName, phase: "top-before-actions")

        let probeStartCount: Int
        if let probeSession {
            _ = waitForLegacyBleProbeDrain(
                session: probeSession,
                in: app,
                runName: runName,
                quietPeriod: 0.5,
                timeout: 5.0
            )
            let probeValue = legacyBleProbeValue(session: probeSession, in: app) ?? "count=0 last=none"
            probeStartCount = legacyBleProbeCount(from: probeValue) ?? 0
            print("[BLE_COMMAND_TEST_BASELINE] run=\(runName) startCount=\(probeStartCount) value=\"\(probeValue)\"")
        } else {
            probeStartCount = 0
        }

        expectCommand(
            actionName: "shutdown current sliders 1-6",
            expectedDescription: "old source: SHUTDOWN_CURRENT_NEW_VM on every shutdown current slider stop",
            characteristics: ["4368000C-4D74-1001-726B-526F64696F6E"],
            minimumCount: 1,
            minimumDistinctBytes: 2
        )

        for index in 1...6 {
            performLegacyCommandProbeAction(
                runName: runName,
                actionName: "shutdown current \(index) slider",
                expectation: "BLE write: SHUTDOWN_CURRENT_NEW(_VM)",
                settleTime: commandSettleTime
            ) {
                guard let slider = legacySliderVisible(nearText: "shutdown current \(index)", in: app) else {
                    return false
                }
                slider.adjust(toNormalizedSliderPosition: 0.18 + CGFloat(index) * 0.08)
                return true
            }
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "swap button open close switch",
            expectation: "no BLE command expected: local state only",
            settleTime: noCommandSettleTime
        ) {
            noteNoBleExpected(
                actionName: "swap button open close switch",
                expectation: "old source only saves SWAP_BUTTONS_OPEN_CLOSE"
            )
            guard let control = legacySwitchVisible(nearText: "swap button open/close", in: app) else {
                return false
            }
            control.tap()
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "write serial number FEST-H-04921 accepted",
            expectation: "BLE write with confirmation: SERIAL_NUMBER_NEW_VM",
            settleTime: dialogCommandSettleTime
        ) {
            expectCommand(
                actionName: "write serial number FEST-H-04921 accepted",
                expectedDescription: "old source writes validationAndConversionSerialNumber(FEST-H-04921) to SERIAL_NUMBER_NEW_VM",
                characteristics: ["43680300-4D74-1001-726B-526F64696F6E"],
                requiredBytes: "464553542D58465448533034393231"
            )
            guard setVisibleLegacySerialNumber("FEST-H-04921", in: app),
                  tapLegacyButtonVisible(titled: "WRITE", in: app) else {
                return false
            }

            guard completeLegacyPasswordIfNeeded(in: app, password: "0889") else {
                return false
            }

            return tapDialogButton(titledAnyOf: ["OK", "ОК", "ok"], in: app)
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "prosthesis blocking switch enabled",
            expectation: "BLE write: ROTATION_GESTURE_NEW_VM",
            settleTime: commandSettleTime
        ) {
            expectCommand(
                actionName: "prosthesis blocking switch enabled",
                expectedDescription: "old source writes ROTATION_GESTURE_NEW_VM after prosthesis blocking switch",
                characteristics: ["43680400-4D74-1001-726B-526F64696F6E"]
            )
            guard tapLegacySwitchVisibleAndLeaveOn(nearText: "prosthesis blocking", in: app) else {
                return false
            }
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "hold to lock time slider",
            expectation: "BLE write: ROTATION_GESTURE_NEW_VM",
            settleTime: commandSettleTime
        ) {
            expectCommand(
                actionName: "hold to lock time slider",
                expectedDescription: "old source writes ROTATION_GESTURE_NEW_VM after hold-to-lock slider stop",
                characteristics: ["43680400-4D74-1001-726B-526F64696F6E"]
            )
            guard let slider = legacySliderVisible(
                nearAnyText: ["hold to lock time", "длина пика"],
                fallbackUserLabel: "timeForBlocking",
                in: app
            ) else {
                return false
            }
            slider.adjust(toNormalizedSliderPosition: 0.45)
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "single channel control switch",
            expectation: "BLE write: SET_ONE_CHANNEL_NEW(_VM)",
            settleTime: commandSettleTime
        ) {
            expectCommand(
                actionName: "single channel control switch",
                expectedDescription: "old source writes SET_ONE_CHANNEL_NEW_VM after single channel switch",
                characteristics: ["43680007-4D74-1001-726B-526F64696F6E"]
            )
            guard let control = legacySwitchVisible(nearText: "single channel control", in: app) else {
                return false
            }
            control.tap()
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "gesture switching by sensors switch",
            expectation: "BLE write: ROTATION_GESTURE_NEW(_VM)",
            settleTime: commandSettleTime
        ) {
            expectCommand(
                actionName: "gesture switching by sensors switch",
                expectedDescription: "old source writes ROTATION_GESTURE_NEW_VM after gesture switching by sensors switch",
                characteristics: ["43680400-4D74-1001-726B-526F64696F6E"]
            )
            guard tapLegacySwitchVisibleAndLeaveOn(
                nearAnyText: ["gesture switching by sensors", "переключение жеста сенсорами"],
                in: app
            ) else {
                return false
            }
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "time at rest slider",
            expectation: "BLE write: ROTATION_GESTURE_NEW_VM",
            settleTime: commandSettleTime
        ) {
            expectCommand(
                actionName: "time at rest slider",
                expectedDescription: "old source writes ROTATION_GESTURE_NEW_VM after time-at-rest slider stop",
                characteristics: ["43680400-4D74-1001-726B-526F64696F6E"]
            )
            guard let slider = legacySliderVisible(
                nearAnyText: ["time at rest", "время в упоре"],
                fallbackUserLabel: "timeAtRest",
                in: app
            ) else {
                return false
            }
            slider.adjust(toNormalizedSliderPosition: 0.55)
            return true
        }

        scrollLegacyAdvancedSettingsToBottomOnce(in: app)
        attachLegacyVisibleState(in: app, runName: runName, phase: "bottom-before-actions")

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "active gestures switch",
            expectation: "BLE write: SET_REVERSE_NEW_VM and ROTATION_GESTURE_NEW_VM",
            settleTime: commandSettleTime
        ) {
            expectCommand(
                actionName: "active gestures switch",
                expectedDescription: "old source writes SET_REVERSE_NEW_VM and then ROTATION_GESTURE_NEW_VM",
                characteristics: ["43680006-4D74-1001-726B-526F64696F6E"]
            )
            expectCommand(
                actionName: "active gestures switch",
                expectedDescription: "old source writes SET_REVERSE_NEW_VM and then ROTATION_GESTURE_NEW_VM",
                characteristics: ["43680400-4D74-1001-726B-526F64696F6E"]
            )
            guard let control = legacyActiveGesturesSwitchVisible(in: app) else {
                return false
            }
            control.tap()
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "calibration status button",
            expectation: "BLE read: STATUS_CALIBRATION_NEW_VM",
            settleTime: commandSettleTime
        ) {
            expectCommand(
                actionName: "calibration status button",
                expectedDescription: "old source reads STATUS_CALIBRATION_NEW_VM in calibration status dialog",
                type: "READ",
                characteristics: ["43680009-4D74-1001-726B-526F64696F6E"]
            )
            guard tapLegacyButtonVisible(titled: "CALIBRATION STATUS", in: app) else {
                return false
            }
            _ = tapDialogButton(titledAnyOf: ["OK", "ok", "close", "закрыть"], in: app)
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "calibration button accepted",
            expectation: "BLE write after dialog accept: CALIBRATION_NEW(_VM)",
            settleTime: dialogCommandSettleTime
        ) {
            expectCommand(
                actionName: "calibration button accepted",
                expectedDescription: "old source writes CALIBRATION_NEW_VM after calibration dialog accept",
                characteristics: ["43680008-4D74-1001-726B-526F64696F6E"]
            )
            guard tapLegacyButtonVisible(titled: "CALIBRATION", in: app) else {
                return false
            }
            return tapDialogButton(titledAnyOf: ["start", "START", "начать"], in: app)
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "hand side switch",
            expectation: "no immediate BLE command expected: local state used by calibration",
            settleTime: noCommandSettleTime
        ) {
            noteNoBleExpected(
                actionName: "hand side switch",
                expectation: "old source only saves HAND_SIDE; calibration later uses this value"
            )
            guard let control = legacySwitchVisible(nearText: "hand side", in: app) else {
                return false
            }
            control.tap()
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "fingers delay switch",
            expectation: "no BLE command expected: local state only",
            settleTime: noCommandSettleTime
        ) {
            noteNoBleExpected(
                actionName: "fingers delay switch",
                expectation: "old source only saves FINGERS_DELAY_SWITCH"
            )
            guard let control = legacySwitchVisible(nearText: "changing the fingers delay time", in: app) else {
                return false
            }
            control.tap()
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "smart connection switch",
            expectation: "no BLE command expected: local state only",
            settleTime: noCommandSettleTime
        ) {
            noteNoBleExpected(
                actionName: "smart connection switch",
                expectation: "old source only saves SMART_CONNECTION"
            )
            guard let control = legacySwitchVisible(nearText: "smart connection", occurrence: 0, in: app) else {
                return false
            }
            control.tap()
            return true
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "soft reset button cancelled",
            expectation: "no BLE command expected before reset dialog accept",
            settleTime: noCommandSettleTime
        ) {
            noteNoBleExpected(
                actionName: "soft reset button cancelled",
                expectation: "old source writes reset only after dialog accept; this test cancels"
            )
            forbidCommand(
                actionName: "soft reset button cancelled",
                reason: "cancel must not send RESET_TO_FACTORY_SETTINGS_NEW(_VM)",
                characteristics: [
                    "43680100-4D74-1001-726B-526F64696F6E",
                    "43686172-4D74-726B-0100-526F64696F6E"
                ]
            )
            guard tapLegacyButtonVisible(titled: "SOFT RESET TO FACTORY SETTINGS", in: app) else {
                return false
            }
            return tapDialogButton(titledAnyOf: ["cancel", "Cancel", "отмена"], in: app)
        }

        performLegacyCommandProbeAction(
            runName: runName,
            actionName: "hard reset button cancelled",
            expectation: "no BLE command expected before reset dialog accept",
            settleTime: noCommandSettleTime
        ) {
            noteNoBleExpected(
                actionName: "hard reset button cancelled",
                expectation: "old source writes reset only after dialog accept; this test cancels"
            )
            forbidCommand(
                actionName: "hard reset button cancelled",
                reason: "cancel must not send RESET_TO_FACTORY_SETTINGS_NEW(_VM)",
                characteristics: [
                    "43680100-4D74-1001-726B-526F64696F6E",
                    "43686172-4D74-726B-0100-526F64696F6E"
                ]
            )
            guard tapLegacyButtonVisible(titled: "RESET TO FACTORY SETTINGS", in: app) else {
                return false
            }
            return tapDialogButton(titledAnyOf: ["cancel", "Cancel", "отмена"], in: app)
        }

        let finalProbeValue: String?
        if let probeSession {
            finalProbeValue = waitForLegacyBleProbeDrain(
                session: probeSession,
                in: app,
                runName: runName,
                quietPeriod: 1.0,
                timeout: 20.0
            ) ?? legacyBleProbeValue(session: probeSession, in: app)
        } else {
            waitForLegacyBleQueueDrain(runName: runName)
            finalProbeValue = nil
        }

        if let probeSession, let finalProbeValue {
            let allCommands = legacyBleCommands(from: finalProbeValue)
            let actionCommands = allCommands.filter { $0.sequence > probeStartCount }
            let report = legacyBleCommandComparisonReport(
                runName: runName,
                session: probeSession,
                startCount: probeStartCount,
                finalProbeValue: finalProbeValue,
                commands: actionCommands,
                expectedCommands: expectedCommands,
                forbiddenCommands: forbiddenCommands,
                noBleExpectedActions: noBleExpectedActions
            )
            attachLegacyBleProbeValue(report, name: "\(runName)-advanced-settings-ble-command-history")

            let missingExpectations = legacyBleMissingExpectations(
                expectedCommands,
                in: actionCommands
            )
            XCTAssertTrue(
                missingExpectations.isEmpty,
                "Merged legacy advanced settings BLE commands missing/incorrect: \(missingExpectations.joined(separator: "; "))"
            )

            let forbiddenMatches = legacyBleForbiddenMatches(
                forbiddenCommands,
                in: actionCommands
            )
            XCTAssertTrue(
                forbiddenMatches.isEmpty,
                "Merged legacy advanced settings sent forbidden BLE commands: \(forbiddenMatches.joined(separator: "; "))"
            )
        } else if probeSession != nil {
            XCTFail("Legacy BLE command probe value was not available after advanced settings actions")
        }

        let stateDump = collectScrollableUIState(in: app, maxScrollPages: 2)
        let attachment = XCTAttachment(
            data: Data(stateDump.utf8),
            uniformTypeIdentifier: "public.plain-text"
        )
        attachment.name = "\(runName)-advanced-settings-state-after-command-probe"
        attachment.lifetime = .keepAlways
        add(attachment)
        print("[BLE_COMMAND_TEST_END] run=\(runName)")
    }

    private func performLegacyCommandProbeAction(
        runName: String,
        actionName: String,
        expectation: String,
        settleTime: TimeInterval,
        action: () -> Bool
    ) {
        print("[BLE_COMMAND_TEST_ACTION] run=\(runName) action=\"\(actionName)\" expectation=\"\(expectation)\" phase=begin")
        XCTAssertTrue(action(), "Could not perform legacy command probe action: \(actionName)")
        if settleTime > 0 {
            RunLoop.current.run(until: Date().addingTimeInterval(settleTime))
        }
        print("[BLE_COMMAND_TEST_ACTION] run=\(runName) action=\"\(actionName)\" phase=end")
    }

    private func waitForLegacyBleQueueDrain(runName: String) {
        print("[BLE_COMMAND_TEST_DRAIN] run=\(runName) phase=begin")
        RunLoop.current.run(until: Date().addingTimeInterval(8.0))
        print("[BLE_COMMAND_TEST_DRAIN] run=\(runName) phase=end")
    }

    private func waitForLegacyBleProbeDrain(
        session: String,
        in app: XCUIApplication,
        runName: String,
        quietPeriod: TimeInterval,
        timeout: TimeInterval
    ) -> String? {
        print("[BLE_COMMAND_TEST_DRAIN] run=\(runName) phase=probe-begin quietPeriod=\(quietPeriod) timeout=\(timeout)")
        let deadline = Date().addingTimeInterval(timeout)
        var lastValue = legacyBleProbeValue(session: session, in: app)
        var lastCount = lastValue.flatMap { legacyBleProbeCount(from: $0) } ?? 0
        var lastChange = Date()

        while Date() < deadline {
            if let value = legacyBleProbeValue(session: session, in: app) {
                let count = legacyBleProbeCount(from: value) ?? lastCount
                if count != lastCount || value != lastValue {
                    lastValue = value
                    lastCount = count
                    lastChange = Date()
                } else if Date().timeIntervalSince(lastChange) >= quietPeriod {
                    print("[BLE_COMMAND_TEST_DRAIN] run=\(runName) phase=probe-end count=\(lastCount)")
                    return value
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }

        print("[BLE_COMMAND_TEST_DRAIN] run=\(runName) phase=probe-timeout count=\(lastCount)")
        return lastValue
    }

    private func legacyBleCommands(from probeValue: String) -> [LegacyBleCapturedCommand] {
        let historyText: String
        if let historyRange = probeValue.range(of: " history=") {
            historyText = String(probeValue[historyRange.upperBound...])
        } else {
            historyText = probeValue
        }

        let pattern = #"seq=(\d+) type=([^ ]+) characteristic=([^ ]+) bytes=([^ ]*) case=([^|]*)"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else {
            return []
        }

        let nsRange = NSRange(historyText.startIndex..<historyText.endIndex, in: historyText)
        return regex.matches(in: historyText, range: nsRange).compactMap { match in
            guard
                let sequenceRange = Range(match.range(at: 1), in: historyText),
                let typeRange = Range(match.range(at: 2), in: historyText),
                let characteristicRange = Range(match.range(at: 3), in: historyText),
                let bytesRange = Range(match.range(at: 4), in: historyText),
                let caseRange = Range(match.range(at: 5), in: historyText),
                let sequence = Int(historyText[sequenceRange])
            else {
                return nil
            }

            return LegacyBleCapturedCommand(
                sequence: sequence,
                type: String(historyText[typeRange]),
                characteristic: String(historyText[characteristicRange]),
                bytes: String(historyText[bytesRange]),
                caseValue: String(historyText[caseRange]).trimmingCharacters(in: .whitespacesAndNewlines)
            )
        }
    }

    private func legacyBleCommandComparisonReport(
        runName: String,
        session: String,
        startCount: Int,
        finalProbeValue: String,
        commands: [LegacyBleCapturedCommand],
        expectedCommands: [LegacyBleExpectedCommand],
        forbiddenCommands: [LegacyBleForbiddenCommand],
        noBleExpectedActions: [String]
    ) -> String {
        var lines: [String] = []
        lines.append("run=\(runName)")
        lines.append("session=\(session)")
        lines.append("startCount=\(startCount)")
        lines.append("capturedActionCommandCount=\(commands.count)")
        lines.append("")
        lines.append("Expected old-code BLE commands:")
        for expectation in expectedCommands {
            let matches = legacyBleMatches(for: expectation, in: commands)
            let distinctBytes = Set(matches.map(\.bytes)).sorted()
            let status = legacyBleExpectationIsMet(expectation, matches: matches) ? "OK" : "MISSING"
            lines.append(
                "- \(status) action=\"\(expectation.actionName)\" type=\(expectation.type) characteristics=\(expectation.characteristics.joined(separator: ",")) count=\(matches.count) distinctBytes=\(distinctBytes.count) requiredBytes=\(expectation.requiredBytes ?? "none") expectation=\"\(expectation.expectedDescription)\""
            )
            if !distinctBytes.isEmpty {
                lines.append("  bytes=\(distinctBytes.joined(separator: ","))")
            }
        }
        lines.append("")
        lines.append("Forbidden BLE commands:")
        if forbiddenCommands.isEmpty {
            lines.append("- none")
        } else {
            for forbidden in forbiddenCommands {
                let matches = legacyBleMatches(for: forbidden, in: commands)
                let status = matches.isEmpty ? "OK_ABSENT" : "FORBIDDEN_SENT"
                lines.append(
                    "- \(status) action=\"\(forbidden.actionName)\" type=\(forbidden.type) characteristics=\(forbidden.characteristics.joined(separator: ",")) count=\(matches.count) reason=\"\(forbidden.reason)\""
                )
                for command in matches {
                    lines.append(
                        "  sent seq=\(command.sequence) characteristic=\(command.characteristic) bytes=\(command.bytes)"
                    )
                }
            }
        }
        lines.append("")
        lines.append("Controls where old source does not send an immediate BLE command:")
        if noBleExpectedActions.isEmpty {
            lines.append("- none")
        } else {
            for action in noBleExpectedActions {
                lines.append("- \(action)")
            }
        }
        lines.append("")
        lines.append("Captured commands after baseline:")
        if commands.isEmpty {
            lines.append("- none")
        } else {
            for command in commands {
                lines.append(
                    "- seq=\(command.sequence) type=\(command.type) characteristic=\(command.characteristic) bytes=\(command.bytes) case=\(command.caseValue)"
                )
            }
        }
        lines.append("")
        lines.append("Raw final probe value:")
        lines.append(finalProbeValue)
        return lines.joined(separator: "\n")
    }

    private func legacyBleMissingExpectations(
        _ expectations: [LegacyBleExpectedCommand],
        in commands: [LegacyBleCapturedCommand]
    ) -> [String] {
        expectations.compactMap { expectation in
            let matches = legacyBleMatches(for: expectation, in: commands)
            guard legacyBleExpectationIsMet(expectation, matches: matches) else {
                let distinctBytesCount = Set(matches.map(\.bytes)).count
                return "\(expectation.actionName) expected \(expectation.type) \(expectation.characteristics.joined(separator: ",")) minCount=\(expectation.minimumCount) actualCount=\(matches.count) distinctBytes=\(distinctBytesCount) requiredBytes=\(expectation.requiredBytes ?? "none")"
            }
            return nil
        }
    }

    private func legacyBleForbiddenMatches(
        _ forbiddenCommands: [LegacyBleForbiddenCommand],
        in commands: [LegacyBleCapturedCommand]
    ) -> [String] {
        forbiddenCommands.flatMap { forbidden in
            legacyBleMatches(for: forbidden, in: commands).map { command in
                "\(forbidden.actionName) sent forbidden \(command.type) \(command.characteristic) bytes=\(command.bytes) seq=\(command.sequence)"
            }
        }
    }

    private func legacyBleMatches(
        for expectation: LegacyBleExpectedCommand,
        in commands: [LegacyBleCapturedCommand]
    ) -> [LegacyBleCapturedCommand] {
        commands.filter { command in
            command.type.caseInsensitiveCompare(expectation.type) == .orderedSame &&
                expectation.characteristics.contains { characteristic in
                    command.characteristic.caseInsensitiveCompare(characteristic) == .orderedSame
                }
        }
    }

    private func legacyBleMatches(
        for forbidden: LegacyBleForbiddenCommand,
        in commands: [LegacyBleCapturedCommand]
    ) -> [LegacyBleCapturedCommand] {
        commands.filter { command in
            command.type.caseInsensitiveCompare(forbidden.type) == .orderedSame &&
                forbidden.characteristics.contains { characteristic in
                    command.characteristic.caseInsensitiveCompare(characteristic) == .orderedSame
                }
        }
    }

    private func legacyBleExpectationIsMet(
        _ expectation: LegacyBleExpectedCommand,
        matches: [LegacyBleCapturedCommand]
    ) -> Bool {
        guard matches.count >= expectation.minimumCount else {
            return false
        }
        if let requiredBytes = expectation.requiredBytes,
           !matches.contains(where: { $0.bytes.caseInsensitiveCompare(requiredBytes) == .orderedSame }) {
            return false
        }
        if let minimumDistinctBytes = expectation.minimumDistinctBytes,
           Set(matches.map(\.bytes)).count < minimumDistinctBytes {
            return false
        }
        return true
    }

    private func openLegacyAdvancedSettings(in app: XCUIApplication) -> Bool {
        _ = waitForLegacySynchronizationReady(in: app, timeout: 45)

        guard let advancedSettingsButton = legacyAdvancedSettingsButton(in: app) else {
            return false
        }

        advancedSettingsButton.tap()
        if waitForAnyVisibleElement(
            containingAnyOf: [
                "shutdown current",
                "single channel",
                "smart connection",
                "active gestures",
                "serial number",
                "ток отключения",
                "одноканальное",
                "серийный"
            ],
            in: app,
            timeout: 20
        ) {
            return true
        }

        if tapDialogButton(titledAnyOf: ["OK", "ОК", "ok", "close", "закрыть"], in: app) {
            _ = waitForLegacySynchronizationReady(in: app, timeout: 45)
            guard let retryButton = legacyAdvancedSettingsButton(in: app) else {
                return false
            }
            retryButton.tap()
            return waitForAnyVisibleElement(
                containingAnyOf: [
                    "shutdown current",
                    "single channel",
                    "smart connection",
                    "active gestures",
                    "serial number",
                    "ток отключения",
                    "одноканальное",
                    "серийный"
                ],
                in: app,
                timeout: 20
            )
        }

        return false
    }

    private func waitForLegacySynchronizationReady(in app: XCUIApplication, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let syncReady = app.staticTexts
                .matching(NSPredicate(format: "label CONTAINS[c] %@", "Sync 100"))
                .firstMatch
            if syncReady.exists {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.4))
        }
        return false
    }

    private func scrollLegacyAdvancedSettingsToTop(in app: XCUIApplication) {
        let scrollView = app.scrollViews.firstMatch
        guard scrollView.waitForExistence(timeout: 2) else { return }

        scrollView.swipeDown()
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))
    }

    private func scrollLegacyAdvancedSettingsToBottomOnce(in app: XCUIApplication) {
        let scrollView = app.scrollViews.firstMatch
        guard scrollView.waitForExistence(timeout: 2) else { return }

        let start = scrollView.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.88))
        let end = scrollView.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.08))
        start.press(forDuration: 0.01, thenDragTo: end)
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))
    }

    private func attachLegacyVisibleState(in app: XCUIApplication, runName: String, phase: String) {
        let attachment = XCTAttachment(
            data: Data(dumpUIStatePage(in: app, pageIndex: 0).utf8),
            uniformTypeIdentifier: "public.plain-text"
        )
        attachment.name = "\(runName)-advanced-settings-\(phase)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func legacySliderVisible(
        nearText text: String,
        occurrence: Int = 0,
        in app: XCUIApplication
    ) -> XCUIElement? {
        guard let label = legacyStaticTextVisibleNow(
            matching: NSPredicate(format: "label CONTAINS[c] %@", text),
            occurrence: occurrence,
            in: app
        ) else {
            return nil
        }

        let labelFrame = label.frame
        return app.sliders.allElementsBoundByIndex
            .filter { slider in
                slider.exists &&
                    isElementFrameVisible(slider.frame, in: app) &&
                    abs(slider.frame.midY - labelFrame.midY) < 60
            }
            .sorted { lhs, rhs in
                abs(lhs.frame.midY - labelFrame.midY) < abs(rhs.frame.midY - labelFrame.midY)
            }
            .first
    }

    private func legacySliderVisible(
        nearAnyText texts: [String],
        occurrence: Int = 0,
        fallbackUserLabel: String? = nil,
        in app: XCUIApplication
    ) -> XCUIElement? {
        for text in texts {
            if let slider = legacySliderVisible(nearText: text, occurrence: occurrence, in: app) {
                return slider
            }
        }

        if let fallbackUserLabel {
            let slider = app.sliders[fallbackUserLabel]
            if slider.exists && isElementFrameVisible(slider.frame, in: app) {
                return slider
            }
        }

        return nil
    }

    private func legacySwitchVisible(
        nearText text: String,
        occurrence: Int = 0,
        in app: XCUIApplication
    ) -> XCUIElement? {
        guard let label = legacyStaticTextVisibleNow(
            matching: NSPredicate(format: "label CONTAINS[c] %@", text),
            occurrence: occurrence,
            in: app
        ) else {
            return nil
        }

        let labelFrame = label.frame
        return app.switches.allElementsBoundByIndex
            .filter { control in
                control.exists &&
                    isElementFrameVisible(control.frame, in: app) &&
                    abs(control.frame.midY - labelFrame.midY) < 60
            }
            .sorted { lhs, rhs in
                abs(lhs.frame.midY - labelFrame.midY) < abs(rhs.frame.midY - labelFrame.midY)
            }
            .first
    }

    private func legacySwitchVisible(
        nearAnyText texts: [String],
        occurrence: Int = 0,
        in app: XCUIApplication
    ) -> XCUIElement? {
        for text in texts {
            if let control = legacySwitchVisible(nearText: text, occurrence: occurrence, in: app) {
                return control
            }
        }
        return nil
    }

    private func legacyActiveGesturesSwitchVisible(in app: XCUIApplication) -> XCUIElement? {
        legacySwitchVisible(nearText: "smart connection", occurrence: 1, in: app) ??
            legacySwitchVisible(nearText: "количество активных жестов", in: app) ??
            legacySwitchVisible(nearText: "active gestures", in: app)
    }

    private func tapLegacySwitchVisibleAndLeaveOn(nearText text: String, in app: XCUIApplication) -> Bool {
        guard let control = legacySwitchVisible(nearText: text, in: app) else {
            return false
        }
        return tapLegacySwitchVisibleAndLeaveOn(control)
    }

    private func tapLegacySwitchVisibleAndLeaveOn(nearAnyText texts: [String], in app: XCUIApplication) -> Bool {
        guard let control = legacySwitchVisible(nearAnyText: texts, in: app) else {
            return false
        }
        return tapLegacySwitchVisibleAndLeaveOn(control)
    }

    private func tapLegacySwitchVisibleAndLeaveOn(_ control: XCUIElement) -> Bool {
        guard control.exists else {
            return false
        }

        let wasOn = isSwitchOn(control)
        print("[BLE_COMMAND_TEST_SWITCH_INITIAL] value=\"\(wasOn ? "on" : "off")\"")
        control.tap()
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))

        if wasOn {
            control.tap()
            RunLoop.current.run(until: Date().addingTimeInterval(0.15))
        }

        return true
    }

    private func tapLegacyButtonVisible(titled title: String, in app: XCUIApplication) -> Bool {
        let predicate = NSPredicate(format: "label ==[c] %@", title)
        guard let button = legacyButtonVisibleNow(matching: predicate, in: app) else {
            return false
        }

        button.tap()
        return true
    }

    private func legacySlider(
        nearText text: String,
        occurrence: Int = 0,
        in app: XCUIApplication
    ) -> XCUIElement? {
        guard let label = legacyStaticText(containing: text, occurrence: occurrence, in: app),
              makeElementVisible(label, in: app) else {
            return nil
        }

        let labelFrame = label.frame
        return app.sliders.allElementsBoundByIndex
            .filter { slider in
                slider.exists &&
                isElementFrameVisible(slider.frame, in: app) &&
                abs(slider.frame.midY - labelFrame.midY) < 60
            }
            .sorted { lhs, rhs in
                abs(lhs.frame.midY - labelFrame.midY) < abs(rhs.frame.midY - labelFrame.midY)
            }
            .first
    }

    private func legacySlider(
        nearAnyText texts: [String],
        occurrence: Int = 0,
        fallbackUserLabel: String? = nil,
        in app: XCUIApplication
    ) -> XCUIElement? {
        for text in texts {
            if let slider = legacySlider(nearText: text, occurrence: occurrence, in: app) {
                return slider
            }
        }

        if let fallbackUserLabel {
            let slider = app.sliders[fallbackUserLabel]
            if slider.waitForExistence(timeout: 0.5),
               makeElementVisible(slider, in: app) {
                return slider
            }
        }

        return nil
    }

    private func legacySwitch(
        nearText text: String,
        occurrence: Int = 0,
        in app: XCUIApplication
    ) -> XCUIElement? {
        guard let label = legacyStaticText(containing: text, occurrence: occurrence, in: app),
              makeElementVisible(label, in: app) else {
            return nil
        }

        let labelFrame = label.frame
        return app.switches.allElementsBoundByIndex
            .filter { control in
                control.exists &&
                isElementFrameVisible(control.frame, in: app) &&
                abs(control.frame.midY - labelFrame.midY) < 60
            }
            .sorted { lhs, rhs in
                abs(lhs.frame.midY - labelFrame.midY) < abs(rhs.frame.midY - labelFrame.midY)
            }
            .first
    }

    private func legacySwitch(
        nearAnyText texts: [String],
        occurrence: Int = 0,
        in app: XCUIApplication
    ) -> XCUIElement? {
        for text in texts {
            if let control = legacySwitch(nearText: text, occurrence: occurrence, in: app) {
                return control
            }
        }
        return nil
    }

    private func legacyActiveGesturesSwitch(in app: XCUIApplication) -> XCUIElement? {
        legacySwitch(nearText: "smart connection", occurrence: 1, in: app) ??
            legacySwitch(nearText: "количество активных жестов", in: app) ??
            legacySwitch(nearText: "active gestures", in: app)
    }

    private func tapLegacySwitchAndLeaveOn(nearText text: String, in app: XCUIApplication) -> Bool {
        guard let control = legacySwitch(nearText: text, in: app) else {
            return false
        }
        return tapLegacySwitchAndLeaveOn(control, in: app)
    }

    private func tapLegacySwitchAndLeaveOn(nearAnyText texts: [String], in app: XCUIApplication) -> Bool {
        guard let control = legacySwitch(nearAnyText: texts, in: app) else {
            return false
        }
        return tapLegacySwitchAndLeaveOn(control, in: app)
    }

    private func tapLegacySwitchAndLeaveOn(_ control: XCUIElement, in app: XCUIApplication) -> Bool {
        guard makeElementVisible(control, in: app) else {
            return false
        }

        let wasOn = isSwitchOn(control)
        print("[BLE_COMMAND_TEST_SWITCH_INITIAL] value=\"\(wasOn ? "on" : "off")\"")
        control.tap()
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))

        if wasOn {
            control.tap()
            RunLoop.current.run(until: Date().addingTimeInterval(0.15))
        }

        return true
    }

    private func isSwitchOn(_ control: XCUIElement) -> Bool {
        if let value = control.value as? String {
            return value == "1" || value.lowercased() == "on"
        }
        return false
    }

    private func tapLegacyButton(titled title: String, in app: XCUIApplication) -> Bool {
        let predicate = NSPredicate(format: "label ==[c] %@", title)
        guard let button = legacyButton(matching: predicate, in: app),
              makeElementVisible(button, in: app) else {
            return false
        }

        button.tap()
        return true
    }

    private func tapDialogButton(titledAnyOf titles: [String], in app: XCUIApplication) -> Bool {
        for title in titles {
            let predicate = NSPredicate(format: "label ==[c] %@", title)
            let button = app.buttons.matching(predicate).firstMatch
            if button.waitForExistence(timeout: 1.5) {
                button.tap()
                return true
            }
        }
        return false
    }

    private func setLegacySerialNumber(_ serialNumber: String, in app: XCUIApplication) -> Bool {
        var textField = legacyVisibleTextField(
            containingAnyOf: ["serial number", "серийник протеза"],
            in: app
        )

        if textField == nil {
            let scrollView = app.scrollViews.firstMatch
            if scrollView.exists {
                scrollView.swipeDown()
                RunLoop.current.run(until: Date().addingTimeInterval(0.15))
            }
            textField = legacyVisibleTextField(
                containingAnyOf: ["serial number", "серийник протеза"],
                in: app
            )
        }

        guard let textField else {
            return false
        }

        textField.tap()
        clearAndType(serialNumber, in: textField)
        let returnButton = app.keyboards.buttons["Return"]
        if returnButton.exists {
            returnButton.tap()
        }
        return true
    }

    private func setVisibleLegacySerialNumber(_ serialNumber: String, in app: XCUIApplication) -> Bool {
        guard let textField = legacyVisibleTextField(
            containingAnyOf: ["serial number", "серийник протеза"],
            in: app
        ) else {
            return false
        }

        textField.tap()
        clearAndType(serialNumber, in: textField)
        let returnButton = app.keyboards.buttons["Return"]
        if returnButton.exists {
            returnButton.tap()
        }
        return true
    }

    private func completeLegacyPasswordIfNeeded(in app: XCUIApplication, password: String) -> Bool {
        if waitForLegacySetSerialNumberConfirmation(in: app, timeout: 1.0) {
            return true
        }

        if waitForAnyVisibleElement(
            containingAnyOf: ["Enter password", "Введите пароль"],
            in: app,
            timeout: 1.5
        ) {
            let passwordField = app.secureTextFields.firstMatch.exists
                ? app.secureTextFields.firstMatch
                : app.textFields.firstMatch
            guard passwordField.waitForExistence(timeout: 1.0) else {
                return false
            }
            passwordField.tap()
            clearAndType(password, in: passwordField)
            guard tapDialogButton(titledAnyOf: ["OK", "ОК", "ok"], in: app) else {
                return false
            }
        }

        return waitForLegacySetSerialNumberConfirmation(in: app, timeout: 2.0)
    }

    private func waitForLegacySetSerialNumberConfirmation(in app: XCUIApplication, timeout: TimeInterval) -> Bool {
        waitForAnyVisibleElement(
            containingAnyOf: [
                "Setting the serial number",
                "serial number of the prosthesis",
                "Установка серийного номера",
                "нового серийного номера"
            ],
            in: app,
            timeout: timeout
        )
    }

    private func legacyStaticText(
        containing text: String,
        occurrence: Int,
        in app: XCUIApplication
    ) -> XCUIElement? {
        let predicate = NSPredicate(format: "label CONTAINS[c] %@", text)
        if let match = legacyStaticTextVisibleNow(matching: predicate, occurrence: occurrence, in: app) {
            return match
        }

        let scrollView = app.scrollViews.firstMatch
        guard scrollView.waitForExistence(timeout: 1) else {
            return nil
        }

        scrollLegacyAdvancedSettingsToTop(in: app)
        for _ in 0..<2 {
            if let match = legacyStaticTextVisibleNow(matching: predicate, occurrence: occurrence, in: app) {
                return match
            }
            scrollView.swipeUp()
            RunLoop.current.run(until: Date().addingTimeInterval(0.15))
        }

        return nil
    }

    private func legacyStaticTextVisibleNow(
        matching predicate: NSPredicate,
        occurrence: Int,
        in app: XCUIApplication
    ) -> XCUIElement? {
        let matches = app.staticTexts
            .matching(predicate)
            .allElementsBoundByIndex
            .filter { $0.exists && isElementFrameVisible($0.frame, in: app) }
            .sorted { lhs, rhs in
                if lhs.frame.minY == rhs.frame.minY {
                    return lhs.frame.minX < rhs.frame.minX
                }
                return lhs.frame.minY < rhs.frame.minY
            }

        guard occurrence < matches.count else {
            return nil
        }
        return matches[occurrence]
    }

    private func legacyButton(matching predicate: NSPredicate, in app: XCUIApplication) -> XCUIElement? {
        if let button = legacyButtonVisibleNow(matching: predicate, in: app) {
            return button
        }

        let scrollView = app.scrollViews.firstMatch
        guard scrollView.waitForExistence(timeout: 1) else {
            return nil
        }

        scrollLegacyAdvancedSettingsToTop(in: app)
        for _ in 0..<2 {
            if let button = legacyButtonVisibleNow(matching: predicate, in: app) {
                return button
            }
            scrollView.swipeUp()
            RunLoop.current.run(until: Date().addingTimeInterval(0.15))
        }

        return nil
    }

    private func legacyButtonVisibleNow(matching predicate: NSPredicate, in app: XCUIApplication) -> XCUIElement? {
        app.buttons
            .matching(predicate)
            .allElementsBoundByIndex
            .filter { $0.exists && isElementFrameVisible($0.frame, in: app) }
            .sorted { lhs, rhs in
                if lhs.frame.minY == rhs.frame.minY {
                    return lhs.frame.minX < rhs.frame.minX
                }
                return lhs.frame.minY < rhs.frame.minY
            }
            .first
    }

    private func legacyTextField(nearText text: String, in app: XCUIApplication) -> XCUIElement? {
        guard let label = legacyStaticText(containing: text, occurrence: 0, in: app),
              makeElementVisible(label, in: app) else {
            return nil
        }

        let labelFrame = label.frame
        let textFields = app.textFields.allElementsBoundByIndex + app.secureTextFields.allElementsBoundByIndex
        return textFields
            .filter { textField in
                textField.exists &&
                isElementFrameVisible(textField.frame, in: app) &&
                abs(textField.frame.midY - labelFrame.midY) < 90
            }
            .sorted { lhs, rhs in
                abs(lhs.frame.midY - labelFrame.midY) < abs(rhs.frame.midY - labelFrame.midY)
            }
            .first
    }

    private func legacyVisibleTextField(containingAnyOf texts: [String], in app: XCUIApplication) -> XCUIElement? {
        let textFields = app.textFields.allElementsBoundByIndex + app.secureTextFields.allElementsBoundByIndex
        let visibleTextFields = textFields
            .filter { $0.exists && isElementFrameVisible($0.frame, in: app) }
            .sorted { lhs, rhs in
                if lhs.frame.minY == rhs.frame.minY {
                    return lhs.frame.minX < rhs.frame.minX
                }
                return lhs.frame.minY < rhs.frame.minY
            }

        for text in texts {
            if let textField = visibleTextFields.first(where: { textField in
                let label = textField.label.lowercased()
                let value = (textField.value as? String ?? "").lowercased()
                return label.contains(text.lowercased()) || value.contains(text.lowercased())
            }) {
                return textField
            }
        }

        return visibleTextFields.first
    }

    private func clearAndType(_ text: String, in element: XCUIElement) {
        if let currentValue = element.value as? String,
           !currentValue.isEmpty,
           !currentValue.lowercased().contains("serial number"),
           !currentValue.lowercased().contains("серийник") {
            element.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: currentValue.count))
        }
        element.typeText(text)
    }

    private func makeElementVisible(
        _ element: XCUIElement,
        in app: XCUIApplication,
        maxSwipes: Int = 2
    ) -> Bool {
        let scrollView = app.scrollViews.firstMatch
        guard scrollView.exists else {
            return element.exists && isElementFrameVisible(element.frame, in: app)
        }

        for _ in 0..<maxSwipes {
            guard element.exists else {
                scrollView.swipeUp()
                RunLoop.current.run(until: Date().addingTimeInterval(0.15))
                continue
            }

            if isElementFrameVisible(element.frame, in: app) {
                return true
            }

            let windowFrame = app.windows.firstMatch.frame
            if element.frame.minY < windowFrame.minY + 30 {
                scrollView.swipeDown()
            } else {
                scrollView.swipeUp()
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.15))
        }

        return element.exists && isElementFrameVisible(element.frame, in: app)
    }

    private func isElementFrameVisible(_ frame: CGRect, in app: XCUIApplication) -> Bool {
        guard !frame.isEmpty else { return false }
        let windowFrame = app.windows.firstMatch.frame
        return frame.maxY > windowFrame.minY + 10 &&
            frame.minY < windowFrame.maxY - 10 &&
            frame.maxX > windowFrame.minX + 10 &&
            frame.minX < windowFrame.maxX - 10
    }

    private func recordLegacyAdvancedSettingsState(in app: XCUIApplication, attachmentName: String) {
        XCTAssertTrue(openLegacyAdvancedSettings(in: app), "Legacy advanced settings screen did not open")

        RunLoop.current.run(until: Date().addingTimeInterval(5))
        let stateDump = collectScrollableUIState(in: app, maxScrollPages: 6)
        let attachment = XCTAttachment(
            data: Data(stateDump.utf8),
            uniformTypeIdentifier: "public.plain-text"
        )
        attachment.name = attachmentName
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func legacyAdvancedSettingsButton(in app: XCUIApplication) -> XCUIElement? {
        let directButton = app.buttons["settings"]
        if directButton.waitForExistence(timeout: 5) {
            return directButton
        }

        let settingsPredicate = NSPredicate(format: "label ==[c] %@", "settings")
        let labeledButton = app.buttons.matching(settingsPredicate).firstMatch
        if labeledButton.waitForExistence(timeout: 3) {
            return labeledButton
        }

        return nil
    }

    private func waitForLegacySensorsScreen(in app: XCUIApplication, timeout: TimeInterval) -> Bool {
        waitForAnyStaticText(
            containingAnyOf: ["Activity Sensors", "opening sensor sensitivity", "Driver", "Датчики"],
            in: app,
            timeout: timeout
        )
    }

    private func waitForLegacyDeviceElement(
        namedAnyOf candidates: [String],
        in app: XCUIApplication,
        timeout: TimeInterval
    ) -> XCUIElement? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            for name in candidates {
                let predicate = NSPredicate(format: "label CONTAINS[c] %@", name)
                let staticText = app.staticTexts.matching(predicate).firstMatch
                if staticText.exists {
                    return staticText
                }

                let cell = app.cells.matching(predicate).firstMatch
                if cell.exists {
                    return cell
                }

                let button = app.buttons.matching(predicate).firstMatch
                if button.exists {
                    return button
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.4))
        }
        return nil
    }

    private func dismissBluetoothPermissionIfNeeded() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: 2) else { return }

        for title in ["OK", "Allow", "Разрешить"] {
            let button = alert.buttons[title]
            if button.exists {
                button.tap()
                return
            }
        }
    }

    private func collectScrollableUIState(in app: XCUIApplication, maxScrollPages: Int) -> String {
        var pages: [String] = []
        var previousPageSignature: String?

        for pageIndex in 0..<maxScrollPages {
            RunLoop.current.run(until: Date().addingTimeInterval(0.8))
            pages.append(dumpUIStatePage(in: app, pageIndex: pageIndex))

            let currentSignature = visibleStateSignature(in: app)
            if currentSignature == previousPageSignature {
                break
            }
            previousPageSignature = currentSignature

            let scrollView = app.scrollViews.firstMatch
            guard scrollView.exists else {
                break
            }
            scrollView.swipeUp()
        }

        return pages.joined(separator: "\n\n")
    }

    private func dumpUIStatePage(in app: XCUIApplication, pageIndex: Int) -> String {
        let screenFrame = app.windows.firstMatch.frame
        var lines = [
            "page=\(pageIndex)",
            "timestamp=\(Date())",
            "screen=\(Int(screenFrame.width))x\(Int(screenFrame.height))",
            app.debugDescription
        ]

        return lines.joined(separator: "\n")
    }

    private func visibleStateSignature(in app: XCUIApplication) -> String {
        String(app.debugDescription.prefix(6000))
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

    private func waitForElementValueContainingAllTokens(
        _ element: XCUIElement,
        tokens: [String],
        timeout: TimeInterval
    ) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if let value = element.value as? String,
               tokens.allSatisfy({ value.contains($0) }) {
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
        let lowerMinY = triggerFrame.maxY + 4
        let lowerMaxY = triggerFrame.maxY + 420
        let upperMinY = triggerFrame.minY - 420
        let upperMaxY = triggerFrame.minY - 4

        return app.descendants(matching: .button)
            .allElementsBoundByIndex
            .filter { button in
                let isVerticallyNearTrigger =
                    (button.frame.minY >= lowerMinY && button.frame.maxY <= lowerMaxY) ||
                    (button.frame.minY >= upperMinY && button.frame.maxY <= upperMaxY)

                return button.exists &&
                isVerticallyNearTrigger &&
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
