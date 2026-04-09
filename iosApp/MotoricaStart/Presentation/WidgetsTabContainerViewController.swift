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
    private let contentViewController: WidgetsListViewController
    private let statusBarViewModel = StatusBarViewModel()
    private let keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    private var statusBarHostingController: UIHostingController<StatusBarView>?
    private var statusBarHeightConstraint: NSLayoutConstraint?
    private var contentTopConstraint: NSLayoutConstraint?
    private var deviceNameObserver: NSObjectProtocol?

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
    
    private func embedStatusBar() {
        let hostingController = UIHostingController(rootView: StatusBarView(viewModel: statusBarViewModel))
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

    deinit {
        if let deviceNameObserver {
            NotificationCenter.default.removeObserver(deviceNameObserver)
        }
    }
}

final class GesturesTabViewController: WidgetsTabContainerViewController {}
final class SensorsTabViewController: WidgetsTabContainerViewController {}
final class TrainingTabViewController: WidgetsTabContainerViewController {}
final class SpecialSettingsTabViewController: WidgetsTabContainerViewController {}
