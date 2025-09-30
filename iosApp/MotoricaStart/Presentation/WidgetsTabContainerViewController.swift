//
//  WidgetsTabContainerViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 30.09.2025.
//

import UIKit

class WidgetsTabContainerViewController: UIViewController {
    private let contentViewController: WidgetsListViewController

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
        NSLayoutConstraint.activate([
            contentViewController.view.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
            contentViewController.view.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
            contentViewController.view.topAnchor.constraint(equalTo: guide.topAnchor),
            contentViewController.view.bottomAnchor.constraint(equalTo: guide.bottomAnchor)
        ])

        contentViewController.didMove(toParent: self)
    }
}

final class GesturesTabViewController: WidgetsTabContainerViewController {}
final class SensorsTabViewController: WidgetsTabContainerViewController {}
final class TrainingTabViewController: WidgetsTabContainerViewController {}
final class SpecialSettingsTabViewController: WidgetsTabContainerViewController {}
