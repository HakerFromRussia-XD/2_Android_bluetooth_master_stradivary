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
    private let statusBarHostingController: StatusBarHostingController
    private var statusBarHeightConstraint: NSLayoutConstraint?

//    init(contentViewController: WidgetsListViewController) {
//        self.contentViewController = contentViewController
//        super.init(nibName: nil, bundle: nil)
//    }
    init(
        contentViewController: WidgetsListViewController,
        statusBarViewModel: StatusBarViewModel = .shared
    ) {
        self.contentViewController = contentViewController
        self.statusBarHostingController = StatusBarHostingController(viewModel: statusBarViewModel)
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
    
    func setStatusBarHidden(_ hidden: Bool, animated: Bool = true) {
        statusBarHeightConstraint?.constant = hidden ? 0 : StatusBarView.height
        statusBarHostingController.view.isHidden = hidden

        guard animated else {
            view.layoutIfNeeded()
            return
        }

        UIView.animate(withDuration: 0.2) {
            self.view.layoutIfNeeded()
        }
    }

    private func embedStatusBar() {
        addChild(statusBarHostingController)
        let statusBarView = statusBarHostingController.view
        statusBarView?.translatesAutoresizingMaskIntoConstraints = false
        statusBarView?.backgroundColor = .clear

        if let statusBarView {
            view.addSubview(statusBarView)
            let guide = view.safeAreaLayoutGuide
            statusBarHeightConstraint = statusBarView.heightAnchor.constraint(equalToConstant: StatusBarView.height)

            NSLayoutConstraint.activate([
                statusBarView.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
                statusBarView.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
                statusBarView.topAnchor.constraint(equalTo: guide.topAnchor),
                statusBarHeightConstraint
            ].compactMap { $0 })
        }

        statusBarHostingController.didMove(toParent: self)
    }

    private func embedContentController() {
        addChild(contentViewController)
        contentViewController.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(contentViewController.view)

        let guide = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            contentViewController.view.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
            contentViewController.view.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
            contentViewController.view.topAnchor.constraint(equalTo: guide.topAnchor),
            contentViewController.view.topAnchor.constraint(equalTo: statusBarHostingController.view.bottomAnchor),
        ])

        contentViewController.didMove(toParent: self)
    }
}

final class GesturesTabViewController: WidgetsTabContainerViewController {}
final class SensorsTabViewController: WidgetsTabContainerViewController {}
final class TrainingTabViewController: WidgetsTabContainerViewController {}
final class SpecialSettingsTabViewController: WidgetsTabContainerViewController {}
