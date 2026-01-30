//
//  WidgetsTabContainerViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 30.09.2025.
//

import SwiftUI
import UIKit

class WidgetsTabContainerViewController: UIViewController {
    private let contentViewController: WidgetsListViewController
    private let statusBarViewModel = StatusBarViewModel()
    private var statusBarHostingController: UIHostingController<StatusBarView>?
    private var statusBarHeightConstraint: NSLayoutConstraint?
    private var contentTopConstraint: NSLayoutConstraint?

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
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        title = contentViewController.title
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
}

final class GesturesTabViewController: WidgetsTabContainerViewController {}
final class SensorsTabViewController: WidgetsTabContainerViewController {}
final class TrainingTabViewController: WidgetsTabContainerViewController {}
final class SpecialSettingsTabViewController: WidgetsTabContainerViewController {}
