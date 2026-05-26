import XCTest

final class FirmwareUpdateUITests: XCTestCase {
    private let targetDeviceName = "111111"
    private let targetBoardName = "Fest H And F"

    override func setUp() {
        continueAfterFailure = false
    }

    func testV3FirmwareUpdate_whenFestHAndFHasNewLocalFirmware_thenDfuProgressAndVersionRefresh() throws {
        let app = XCUIApplication()
        app.launch()

        let devicesTable = app.tables[AccessibilityIdentifier.bleDevicesTable]
        XCTAssertTrue(devicesTable.waitForExistence(timeout: 20), "BLE table did not appear")

        guard let targetDevice = waitForDevice(named: targetDeviceName, in: devicesTable, timeout: 60) else {
            XCTFail("Could not find BLE device \(targetDeviceName)")
            return
        }
        targetDevice.tap()

        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.mainTabBarRoot].waitForExistence(timeout: 15),
            "Main tabs did not open after connecting to \(targetDeviceName)"
        )

        let accountButton = app.buttons[AccessibilityIdentifier.statusBarAccountButton]
        XCTAssertTrue(accountButton.waitForExistence(timeout: 10), "Account button did not appear in status bar")
        accountButton.tap()

        XCTAssertTrue(
            app.otherElements[AccessibilityIdentifier.accountRoot].waitForExistence(timeout: 15),
            "Account screen did not open"
        )

        let boardKey = accessibilityKey(targetBoardName)
        let updateButton = app.buttons["\(AccessibilityIdentifier.accountBoardUpdateButtonPrefix).\(boardKey)"]
        XCTAssertTrue(updateButton.waitForExistence(timeout: 20), "Update button for \(targetBoardName) was not found")
        XCTAssertTrue(updateButton.isEnabled, "Update button for \(targetBoardName) is disabled")

        guard stringValue(updateButton).contains("updateAvailable=true") else {
            throw XCTSkip("No local firmware with a greater version for \(targetBoardName). Put a newer .zip into Motorica Start/Firmware.")
        }

        let versionLabel = app.staticTexts["\(AccessibilityIdentifier.accountBoardVersionPrefix).\(boardKey)"]
        XCTAssertTrue(versionLabel.waitForExistence(timeout: 5), "Version label for \(targetBoardName) was not found")
        let initialVersion = stringValue(versionLabel)
        XCTAssertFalse(initialVersion.isEmpty, "Initial \(targetBoardName) firmware version is empty")

        updateButton.tap()

        let filesDialog = app.otherElements[AccessibilityIdentifier.firmwareFilesDialog]
        XCTAssertTrue(filesDialog.waitForExistence(timeout: 5), "Firmware file selection dialog did not open")

        guard let firmwareRow = firstFirmwareRowForFestHAndF(in: app) else {
            throw XCTSkip("No Fest H And F firmware .zip was found in Motorica Start/Firmware")
        }
        firmwareRow.tap()

        let okButton = app.buttons["OK"]
        XCTAssertTrue(okButton.waitForExistence(timeout: 5), "Firmware update confirmation dialog did not appear")
        okButton.tap()

        let bootloaderIndicator = app.staticTexts["\(AccessibilityIdentifier.accountBoardBootloaderPrefix).\(boardKey)"]
        XCTAssertTrue(
            waitForElementToExist(bootloaderIndicator, timeout: 60),
            "\(targetBoardName) did not enter bootloader/DFU mode"
        )

        let progressDialog = app.otherElements[AccessibilityIdentifier.firmwareProgressDialog]
        XCTAssertTrue(progressDialog.waitForExistence(timeout: 20), "Firmware progress dialog did not appear")

        let progressBar = app.progressIndicators[AccessibilityIdentifier.firmwareProgressBar]
        XCTAssertTrue(progressBar.waitForExistence(timeout: 10), "Firmware progress bar did not appear")
        XCTAssertTrue(
            waitForProgressAdvance(progressBar, timeout: 90),
            "Firmware progress bar did not advance"
        )

        XCTAssertTrue(
            waitForElementToDisappear(progressDialog, timeout: 180),
            "Firmware progress dialog did not close after update"
        )
        XCTAssertTrue(
            waitForElementToDisappear(bootloaderIndicator, timeout: 120),
            "\(targetBoardName) did not leave bootloader/DFU mode"
        )
        XCTAssertTrue(
            waitForVersionChange(versionLabel, from: initialVersion, timeout: 90),
            "\(targetBoardName) firmware version did not refresh after update"
        )
    }

    private func waitForDevice(named name: String, in table: XCUIElement, timeout: TimeInterval) -> XCUIElement? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let cells = table.cells.allElementsBoundByIndex
            if let cell = cells.first(where: { elementContainsText($0, text: name) }) {
                return cell
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }
        return nil
    }

    private func firstFirmwareRowForFestHAndF(in app: XCUIApplication) -> XCUIElement? {
        let acceptedTokens = ["fest", "fh", "h_and_f", "h-and-f"]
        return app.descendants(matching: .any)
            .allElementsBoundByIndex
            .first { element in
                guard element.identifier.hasPrefix(AccessibilityIdentifier.firmwareFileRowPrefix) else { return false }
                let haystack = "\(element.identifier) \(element.label) \(stringValue(element))".lowercased()
                return haystack.contains(".zip") && acceptedTokens.contains { haystack.contains($0) }
            }
    }

    private func waitForElementToExist(_ element: XCUIElement, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if element.exists {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }
        return element.exists
    }

    private func waitForElementToDisappear(_ element: XCUIElement, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if !element.exists {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }
        return !element.exists
    }

    private func waitForVersionChange(_ element: XCUIElement, from initialVersion: String, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let currentVersion = stringValue(element)
            if !currentVersion.isEmpty, currentVersion != initialVersion {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.5))
        }
        return false
    }

    private func waitForProgressAdvance(_ element: XCUIElement, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        var minObserved: Double?
        var maxObserved: Double?

        while Date() < deadline {
            if let progress = progressValue(from: element) {
                minObserved = min(minObserved ?? progress, progress)
                maxObserved = max(maxObserved ?? progress, progress)
                if let minObserved, let maxObserved, maxObserved - minObserved >= 0.05 {
                    return true
                }
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }
        return false
    }

    private func progressValue(from element: XCUIElement) -> Double? {
        let value = stringValue(element)
        guard let range = value.range(of: "progress=") else { return nil }
        let raw = value[range.upperBound...]
            .prefix { character in character.isNumber || character == "." || character == "," }
            .replacingOccurrences(of: ",", with: ".")
        guard let parsed = Double(raw) else { return nil }
        return parsed > 1 ? parsed / 100 : parsed
    }

    private func elementContainsText(_ element: XCUIElement, text: String) -> Bool {
        if element.label.localizedCaseInsensitiveContains(text) {
            return true
        }
        return element.descendants(matching: .any)
            .allElementsBoundByIndex
            .contains { $0.label.localizedCaseInsensitiveContains(text) }
    }

    private func stringValue(_ element: XCUIElement) -> String {
        if let value = element.value as? String {
            return value
        }
        return element.label
    }

    private func accessibilityKey(_ value: String) -> String {
        let allowed = CharacterSet.alphanumerics
        let normalized = value.lowercased().unicodeScalars.map { scalar -> String in
            allowed.contains(scalar) ? String(scalar) : "-"
        }.joined()
        return normalized
            .split(separator: "-")
            .joined(separator: "-")
    }
}
