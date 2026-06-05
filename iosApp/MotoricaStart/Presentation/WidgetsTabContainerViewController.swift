//
//  WidgetsTabContainerViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 30.09.2025.
//

import SwiftUI
import UIKit
import shared

class WidgetsTabContainerViewController: UIViewController {
    private let tabsBackgroundColor = UIColor(named: "ubi4_back") ?? .black

    static let sharedStatusBarViewModel: StatusBarViewModel = {
        let initialState = Int(truncating: BLEStateBridge.shared.currentStateOrdinal() as NSNumber)
        return StatusBarViewModel(isConnected: initialState == 2)
    }()

    fileprivate let contentViewController: WidgetsListViewController
    private let statusBarViewModel = WidgetsTabContainerViewController.sharedStatusBarViewModel
    private let keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    private var statusBarHostingController: UIHostingController<StatusBarView>?
    private var statusBarHeightConstraint: NSLayoutConstraint?
    fileprivate var contentTopConstraint: NSLayoutConstraint?
    private var deviceNameObserver: NSObjectProtocol?
    private var bleStateJob: Kotlinx_coroutines_coreJob?
    private var batteryPercentJob: Kotlinx_coroutines_coreJob?

    private var isUiTestForceConnectedStatus: Bool {
        ProcessInfo.processInfo.arguments.contains("-ui-test-force-connected-status")
    }

    init(contentViewController: WidgetsListViewController) {
        self.contentViewController = contentViewController
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = tabsBackgroundColor
        view.isOpaque = true
        observeBleConnectionState()
        observeBatteryPercent()
        embedStatusBar()
        embedContentController()
        observeDeviceNameUpdates()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        title = contentViewController.title
        refreshStatusBarFromStorage()
    }

    private func embedContentController() {
        addChild(contentViewController)
        contentViewController.view.backgroundColor = tabsBackgroundColor
        contentViewController.view.isOpaque = true
        contentViewController.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(contentViewController.view)

        let guide = view.safeAreaLayoutGuide
        let topAnchor = statusBarHostingController?.view.bottomAnchor ?? guide.topAnchor
        contentTopConstraint = contentViewController.view.topAnchor.constraint(equalTo: topAnchor)
        NSLayoutConstraint.activate([
            contentViewController.view.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
            contentViewController.view.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
            contentTopConstraint,
            contentViewController.view.bottomAnchor.constraint(equalTo: guide.bottomAnchor)
        ].compactMap { $0 })

        contentViewController.didMove(toParent: self)
    }

    fileprivate var contentChromeTopAnchor: NSLayoutYAxisAnchor {
        statusBarHostingController?.view.bottomAnchor ?? view.safeAreaLayoutGuide.topAnchor
    }

    fileprivate func attachContentTop(to anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0) {
        contentTopConstraint?.isActive = false
        contentTopConstraint = contentViewController.view.topAnchor.constraint(equalTo: anchor, constant: constant)
        contentTopConstraint?.isActive = true
        view.setNeedsLayout()
    }
    
    private func embedStatusBar() {
        let hostingController = UIHostingController(
            rootView: StatusBarView(
                viewModel: statusBarViewModel,
                onAccountTap: { [weak self] in
                    self?.openAccount()
                },
                onHelpTap: { [weak self] in
                    self?.openHelp()
                },
                onDisconnectConfirmed: { [weak self] in
                    self?.handleDisconnectConfirmed()
                }
            )
        )
        statusBarHostingController = hostingController
        addChild(hostingController)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        hostingController.view.backgroundColor = .clear
        view.addSubview(hostingController.view)

        let guide = view.safeAreaLayoutGuide
        statusBarHeightConstraint = hostingController.view.heightAnchor.constraint(equalToConstant: StatusBarView.Constants.height)
        NSLayoutConstraint.activate([
            hostingController.view.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
            hostingController.view.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
            hostingController.view.topAnchor.constraint(equalTo: guide.topAnchor),
            statusBarHeightConstraint
        ].compactMap { $0 })

        hostingController.didMove(toParent: self)
    }

    private func openAccount() {
        if navigationController?.topViewController is AccountViewController { return }
        navigationController?.pushViewController(AccountViewController(), animated: true)
    }

    private func openHelp() {
        if navigationController?.topViewController is HelpViewController { return }
        navigationController?.pushViewController(HelpViewController(), animated: true)
    }

    func updateStatusBar(serialNumber: String? = nil, batteryLevel: Double? = nil, isConnected: Bool? = nil) {
        statusBarViewModel.update(serialNumber: serialNumber, batteryLevel: batteryLevel, isConnected: isConnected)
    }

    func setStatusBarVisible(_ isVisible: Bool) {
        statusBarHostingController?.view.isHidden = !isVisible
        statusBarHeightConstraint?.constant = isVisible ? StatusBarView.Constants.height : 0
        view.setNeedsLayout()
    }

    private func refreshStatusBarFromStorage() {
        let storedName = (try? keyValueStorage.load(for: BluetoothStorageKeys.selectedDeviceNameStorageKey)) ?? ""
        let displayName = DeviceNameBridgeV3.shared.displayName(deviceName: storedName)
        if !displayName.isEmpty {
            statusBarViewModel.update(serialNumber: displayName)
        }
    }

    private func handleDisconnectConfirmed() {
        StatusBarDisconnectCoordinator.disconnectAndShowScan(from: self)
    }

    private func observeDeviceNameUpdates() {
        deviceNameObserver = NotificationCenter.default.addObserver(
            forName: .v3DeviceNameDidUpdate,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let serial = notification.object as? String, !serial.isEmpty else { return }
            self?.statusBarViewModel.update(serialNumber: serial)
        }
    }

    private func observeBleConnectionState() {
        bleStateJob?.cancel(cause: nil)
        if isUiTestForceConnectedStatus {
            statusBarViewModel.update(isConnected: true)
            return
        }

        let initialState = Int(truncating: BLEStateBridge.shared.currentStateOrdinal() as NSNumber)
        statusBarViewModel.update(isConnected: initialState == 2)

        bleStateJob = BLEStateBridge.shared.observeState { [weak self] rawState in
            DispatchQueue.main.async {
                let state = Int(truncating: rawState)
                self?.statusBarViewModel.update(isConnected: state == 2)
            }
        }
    }

    private func observeBatteryPercent() {
        batteryPercentJob?.cancel(cause: nil)
        batteryPercentJob = WidgetStateBridge.shared.observeBatteryPercent { [weak self] rawPercent in
            DispatchQueue.main.async {
                let percent = max(0, min(100, Int(truncating: rawPercent as NSNumber)))
                self?.statusBarViewModel.update(batteryLevel: Double(percent) / 100.0)
            }
        }
    }

    deinit {
        if let deviceNameObserver {
            NotificationCenter.default.removeObserver(deviceNameObserver)
        }
        bleStateJob?.cancel(cause: nil)
        batteryPercentJob?.cancel(cause: nil)
    }
}

final class GesturesTabViewController: WidgetsTabContainerViewController {}
final class SensorsTabViewController: WidgetsTabContainerViewController {}
final class TrainingTabViewController: WidgetsTabContainerViewController {}

final class SpecialSettingsTabViewController: WidgetsTabContainerViewController {
    private var selectorHostingController: UIHostingController<SpecialSettingsSourceSelectorView>?
    private var selectorSource: SpecialSettingsSource = .prosthetic

    override func viewDidLoad() {
        super.viewDidLoad()
        installSelector()
        applySource(selectorSource, animated: false)
    }

    private func installSelector() {
        let selectorView = SpecialSettingsSourceSelectorView(
            selection: selectorSource,
            onSelectionChange: { [weak self] source in
                self?.applySource(source, animated: true)
            }
        )
        let hostingController = UIHostingController(rootView: selectorView)
        selectorHostingController = hostingController
        addChild(hostingController)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        hostingController.view.backgroundColor = .clear
        hostingController.view.isOpaque = false
        view.addSubview(hostingController.view)

        NSLayoutConstraint.activate([
            hostingController.view.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 16),
            hostingController.view.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -16),
            hostingController.view.topAnchor.constraint(equalTo: contentChromeTopAnchor, constant: 12),
            hostingController.view.heightAnchor.constraint(equalToConstant: 48)
        ])

        attachContentTop(to: hostingController.view.bottomAnchor, constant: 8)
        hostingController.didMove(toParent: self)
    }

    private func applySource(_ source: SpecialSettingsSource, animated: Bool) {
        selectorSource = source
        contentViewController.setSpecialSettingsSource(source)
    }
}

private struct SpecialSettingsSourceSelectorView: View {
    @State private var selection: SpecialSettingsSource
    let onSelectionChange: (SpecialSettingsSource) -> Void

    init(
        selection: SpecialSettingsSource,
        onSelectionChange: @escaping (SpecialSettingsSource) -> Void
    ) {
        _selection = State(initialValue: selection)
        self.onSelectionChange = onSelectionChange
    }

    var body: some View {
        UnifiedSegmentSelectorView(
            items: [
                UnifiedSegmentSelectorItem(
                    id: .prosthetic,
                    title: SharedRes.strings().prosthetic_settings.desc().localized(),
                    accessibilityIdentifier: AccessibilityIdentifier.specialSettingsProstheticButton
                ),
                UnifiedSegmentSelectorItem(
                    id: .mobile,
                    title: SharedRes.strings().mobile_settings.desc().localized(),
                    accessibilityIdentifier: AccessibilityIdentifier.specialSettingsMobileButton
                )
            ],
            selection: $selection,
            selectorAccessibilityLabel: "special.settings.segment.selector",
            selectorAccessibilityIdentifier: AccessibilityIdentifier.specialSettingsSelector,
            accessibilityValue: { source, offset, maxStep, steps, rollback in
                String(
                    format: "segment=%@;offset=%.3f;maxStep=%.3f;steps=%d;rollback=%@",
                    source.accessibilityValue,
                    offset,
                    maxStep,
                    steps,
                    rollback ? "true" : "false"
                )
            },
            onSelectionChange: onSelectionChange
        )
    }
}

private extension SpecialSettingsSource {
    var accessibilityValue: String {
        switch self {
        case .prosthetic:
            return "prosthetic"
        case .mobile:
            return "mobile"
        }
    }
}
