import UIKit

final class LegacyBleCommandProbe {
    static let accessibilityIdentifier = "legacyBleCommandProbe"
    static let pasteboardName = UIPasteboard.Name("com.motorica.legacyBleCommandProbe")

    private static let shared = LegacyBleCommandProbe()

    private var observer: NSObjectProtocol?
    private var commandCount = 0
    private var lastValue = "count=0 last=none"
    private var recentSummaries: [String] = []
    private var historySummaries: [String] = []
    private var session = "default"
    private var windowProvider: (() -> UIWindow?)?
    private weak var probeView: UIView?

    static func startIfNeeded(windowProvider: @escaping () -> UIWindow?) {
        let processInfo = ProcessInfo.processInfo
        let isEnabled = processInfo.environment["MOTORICA_LEGACY_BLE_COMMAND_PROBE"] == "1" ||
            processInfo.arguments.contains("-legacy-ble-command-probe")
        guard isEnabled else {
            return
        }
        shared.start(windowProvider: windowProvider)
    }

    private func start(windowProvider: @escaping () -> UIWindow?) {
        guard observer == nil else {
            return
        }

        self.windowProvider = windowProvider
        session = ProcessInfo.processInfo.environment["MOTORICA_LEGACY_BLE_COMMAND_PROBE_SESSION"] ?? "default"
        lastValue = "session=\(session) count=0 last=none"
        recentSummaries = []
        historySummaries = []
        updateProbeView()
        updatePasteboard()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.updateProbeView()
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.updateProbeView()
        }
        observer = NotificationCenter.default.addObserver(
            forName: Notification.Name(rawValue: "notificationFromSensorsViewController"),
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.capture(notification)
        }
        NSLog("[BLE_COMMAND_CAPTURE_READY] \(lastValue)")
    }

    deinit {
        if let observer {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    private func capture(_ notification: Notification) {
        commandCount += 1

        let userInfo = notification.userInfo ?? [:]
        let type = Self.stringValue(userInfo["type"])
        let characteristic = Self.stringValue(userInfo["characteristic"])
        let bytes = Self.stringValue(userInfo["byteArray"])
        let caseValue = Self.stringValue(userInfo["case"])

        let summary = "seq=\(commandCount) type=\(type) characteristic=\(characteristic) bytes=\(bytes) case=\(caseValue)"
        recentSummaries.append(summary)
        if recentSummaries.count > 10 {
            recentSummaries.removeFirst(recentSummaries.count - 10)
        }
        historySummaries.append(summary)
        if historySummaries.count > 2_000 {
            historySummaries.removeFirst(historySummaries.count - 2_000)
        }
        lastValue = "session=\(session) count=\(commandCount) last=\(summary) recent=\(recentSummaries.joined(separator: " | ")) history=\(historySummaries.joined(separator: " | "))"
        updateProbeView()
        updatePasteboard()
        NSLog("[BLE_COMMAND_CAPTURE] \(summary)")
    }

    private func updatePasteboard() {
        UIPasteboard(name: Self.pasteboardName, create: true)?.string = lastValue
    }

    private func updateProbeView() {
        guard let window = windowProvider?() else {
            return
        }

        let parentView = window.rootViewController?.view ?? window
        let view: UIView
        if let probeView, probeView.superview === parentView {
            view = probeView
        } else {
            let probe = UIView(frame: CGRect(x: 1, y: 1, width: 1, height: 1))
            probe.backgroundColor = .clear
            probe.alpha = 0.01
            probe.isUserInteractionEnabled = false
            probe.isAccessibilityElement = true
            probe.accessibilityIdentifier = Self.accessibilityIdentifier
            probe.accessibilityTraits = [.staticText]
            parentView.addSubview(probe)
            probeView = probe
            view = probe
        }

        view.accessibilityLabel = "legacy ble command probe"
        view.accessibilityValue = lastValue
        parentView.bringSubviewToFront(view)
    }

    private static func stringValue(_ value: Any?) -> String {
        switch value {
        case let string as String:
            return string
        case let number as NSNumber:
            return number.stringValue
        case .some(let value):
            return String(describing: value)
        case .none:
            return "nil"
        }
    }
}
