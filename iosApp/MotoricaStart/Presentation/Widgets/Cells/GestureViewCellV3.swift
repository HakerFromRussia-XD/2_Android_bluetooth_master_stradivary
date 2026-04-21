//
//  GestureViewCellV3.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 14.04.2026.
//

import SwiftUI
import Combine

final class GestureViewCellV3: UITableViewCell {
    static let reuseIdentifier = String(describing: GestureViewCellV3.self)

    private var viewModel: GestureListItemViewModel!
    private var cancellable: AnyCancellable?
    private var provider: GesturesProvider?
    private var updatesJob: Kotlinx_coroutines_coreJob?
    private let rotationDebouncer = Debouncer(delay: 1.0)

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        disableImplicitGeometryAnimations()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        disableImplicitGeometryAnimations()
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: GestureListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        let provider = viewModel.makeProvider()
        if provider.selectedSegment == .sprGroup {
            provider.selectedSegment = .collection
        }
        self.provider = provider
        cancellable?.cancel()
        preservesSuperviewLayoutMargins = false
        contentView.directionalLayoutMargins = .zero

        var configuration = UIHostingConfiguration {
            GesturesWidgetView(
                provider: provider,
                visibleSegments: [.collection, .rotationGroup],
                onSegmentChange: { [weak self] segment in
                    guard let self else { return }
                    switch segment {
                    case .collection:
                        break
                    case .rotationGroup:
                        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                            self?.viewModel.requestRotationGroup()
                        }
                    case .sprGroup:
                        break
                    }
                },
                onActiveGestureRequest: { [weak self] in
                    DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                        self?.viewModel.requestActiveGesture()
                    }
                },
                onFactoryGestureTap: { [weak self] item in
                    guard let self, let provider = self.provider else { return }
                    self.viewModel.selectFactoryGesture(item, provider: provider)
                },
                onCustomGestureTap: { [weak self] item in
                    guard let self, let provider = self.provider else { return }
                    self.viewModel.selectCustomGesture(item, provider: provider)
                },
                onCustomGestureSettingsTap: { [weak self] item in
                    self?.viewModel.openGestureSettings(for: item)
                },
                onRotationGestureRemove: { [weak self] index in
                    guard let self, let provider = self.provider else { return }
                    self.viewModel.removeRotationGesture(at: index, provider: provider)
                },
                onRotationGestureAdd: { [weak self] items in
                    guard let self, let provider = self.provider else { return }
                    self.viewModel.updateRotationGestures(items, provider: provider)
                },
                onRotationGesturesReorder: { [weak self] items in
                    guard let self, let provider = self.provider else { return }
                    self.rotationDebouncer.schedule { [weak self] in
                        guard let self else { return }
                        self.viewModel.updateRotationGestures(items, provider: provider)
                    }
                },
                onSprGestureAction: { _ in },
                onSprAddTap: {}
            )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration

        if let currentActiveGestureId = viewModel.currentActiveGestureId() {
            applyActiveGesture(currentActiveGestureId)
        }

        updatesJob?.cancel(cause: nil)
        updatesJob = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self else { return }

            if viewModel.matchesActiveGesture(snapshot: snapshot),
               let activeGestureId = viewModel.activeGestureId(from: snapshot) {
                DispatchQueue.main.async { [weak self] in
                    self?.applyActiveGesture(activeGestureId)
                }
            }

            if viewModel.matchesRotationGroup(snapshot: snapshot),
               let provider = self.provider,
               let rotationGroup = viewModel.rotationGroup(from: snapshot, provider: provider) {
                DispatchQueue.main.async { [weak self] in
                    self?.applyRotationGroupWithoutAnimation(rotationGroup)
                }
            }
        }

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.viewModel.requestActiveGesture()
            self?.viewModel.requestRotationGroup()
        }
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        updatesJob?.cancel(cause: nil)
        updatesJob = nil
        provider = nil
        contentConfiguration = nil
    }

    private func applyActiveGesture(_ activeGestureId: Int) {
        let activeGestureTitle =
            provider?.factoryGestures.first(where: { $0.id == activeGestureId })?.title ??
            provider?.customGestures.first(where: { $0.id == activeGestureId })?.title

        provider?.activeGestureId = activeGestureId
        provider?.activeGestureTitle = activeGestureTitle
    }

    private func applyRotationGroupWithoutAnimation(_ rotationGroup: [GesturesProvider.GestureDisplayItem]) {
        var transaction = Transaction(animation: nil)
        transaction.disablesAnimations = true
        withTransaction(transaction) { [weak self] in
            self?.provider?.rotationGroup = rotationGroup
        }
    }

    private func disableImplicitGeometryAnimations() {
        let actions: [String: CAAction] = [
            "bounds": NSNull(),
            "position": NSNull()
        ]
        layer.actions = actions
        contentView.layer.actions = actions
    }
}
