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
            app.otherElements[AccessibilityIdentifier.mainTabBarRoot].waitForExistence(timeout: 60),
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

        let versionLabel = app.staticTexts["\(AccessibilityIdentifier.accountBoardVersionPrefix).\(boardKey)"]
        XCTAssertTrue(versionLabel.waitForExistence(timeout: 5), "Version label for \(targetBoardName) was not found")
        let initialVersion = stringValue(versionLabel)
        guard let currentFirmwareVersion = firmwareVersion(from: initialVersion) else {
            XCTFail("Initial \(targetBoardName) firmware version is not parseable: \(initialVersion)")
            return
        }

        updateButton.tap()

        let filesDialog = app.otherElements[AccessibilityIdentifier.firmwareFilesDialog]
        XCTAssertTrue(filesDialog.waitForExistence(timeout: 5), "Firmware file selection dialog did not open")

        guard let firmware = firstFirmwareRowForFestHAndF(in: app, excludingVersion: currentFirmwareVersion) else {
            throw XCTSkip("No Fest H And F firmware .zip with version different from \(currentFirmwareVersion) was found in Motorica Start/Firmware")
        }
        let targetFirmwareVersion = firmware.version
        let firmwareRow = firmware.row
        firmwareRow.tap()

        let okButton = app.buttons[AccessibilityIdentifier.firmwareConfirmOkButton]
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
            waitForVersion(versionLabel, targetVersion: targetFirmwareVersion, timeout: 90),
            "\(targetBoardName) firmware version did not refresh to \(targetFirmwareVersion) after update"
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

    private func firstFirmwareRowForFestHAndF(
        in app: XCUIApplication,
        excludingVersion currentVersion: String
    ) -> (row: XCUIElement, version: String)? {
        let acceptedTokens = ["fest", "fh", "h_and_f", "h-and-f"]
        let elements = app.buttons.allElementsBoundByIndex + app.cells.allElementsBoundByIndex
        for element in elements {
            guard element.identifier.hasPrefix(AccessibilityIdentifier.firmwareFileRowPrefix) else {
                continue
            }
            let haystack = "\(element.identifier) \(element.label) \(stringValue(element))".lowercased()
            guard haystack.contains(".zip"), acceptedTokens.contains(where: { haystack.contains($0) }) else {
                continue
            }
            guard let version = firmwareVersion(from: haystack), version != currentVersion else {
                continue
            }
            return (element, version)
        }
        return nil
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

    private func waitForVersion(_ element: XCUIElement, targetVersion: String, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let currentVersion = stringValue(element)
            if firmwareVersion(from: currentVersion) == targetVersion {
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

    private func firmwareVersion(from text: String) -> String? {
        let pattern = #"(?i)v?(\d+)[._-](\d+)[._-](\d+)"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return nil }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        guard let match = regex.firstMatch(in: text, range: range), match.numberOfRanges == 4 else {
            return nil
        }
        let parts = (1..<4).compactMap { index -> String? in
            guard let range = Range(match.range(at: index), in: text) else { return nil }
            return String(Int(text[range]) ?? 0)
        }
        guard parts.count == 3 else { return nil }
        return parts.joined(separator: ".")
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
