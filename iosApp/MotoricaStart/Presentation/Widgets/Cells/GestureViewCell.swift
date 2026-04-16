import UIKit
import SwiftUI
import Combine
import shared

final class GestureViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: GestureViewCell.self)
    
    private var viewModel: GestureListItemViewModel!
    private let mainQueue: DispatchQueueType = DispatchQueue.main
    private var cancellable: AnyCancellable?
    private var provider:   GesturesProvider?
//    private var job: Kotlinx_coroutines_coreJob?
    private var rotationJob: Kotlinx_coroutines_coreJob?
    private var bindingJob: Kotlinx_coroutines_coreJob?
    private var activeGestureJob: Kotlinx_coroutines_coreJob?
    private let rotationDebouncer = Debouncer(delay: 1.0)
//    var onOpenSettings: ((Int) -> Void)?
    
    
    // Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        disableImplicitGeometryAnimations()
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        disableImplicitGeometryAnimations()
    }
    
    override func awakeFromNib() { super.awakeFromNib() }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: GestureListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        
        // 1. Создаём провайдер
        let provider = viewModel.makeProvider()
        self.provider = provider
        cancellable?.cancel()
        preservesSuperviewLayoutMargins = false
        contentView.directionalLayoutMargins = .zero
        
        // 2. Вклеиваем SwiftUI контент
        var configuration = UIHostingConfiguration {
            GesturesWidgetView (
                provider: provider,
                onSegmentChange: { [weak self] segment in
                    guard let self else { return }
                    switch segment {
                    case .collection:
                        break
                    case .rotationGroup:
//                        self.viewModel.requestRotationGroup()
                        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                            self?.viewModel.requestRotationGroup()
                        }
                    case .sprGroup:
//                        self.viewModel.requestBindingGroup()
                        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                            self?.viewModel.requestBindingGroup()
                        }
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
                    print("onCustomGestureSettingsTap \(item)")
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
                onSprGestureAction: { [weak self] items in
                    guard let self, let provider = self.provider else { return }
                    self.viewModel.updateBindingGroup(provider: provider)
                    print("TODO: Implement")
                },
                onSprAddTap: { [weak self] in
                    guard let self, let provider = self.provider else { return }
                    self.viewModel.updateBindingGroup(provider: provider)
                }
            )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration
            
        // 3. Запускаем подписку на поток
        rotationJob?.cancel(cause: nil)
        rotationJob = WidgetStateBridge.shared.observeRotationGroup { [weak self] paramRef in
            self?.updateUI(paramRef, viewModel: viewModel)
        }
        bindingJob?.cancel(cause: nil)
        bindingJob = WidgetStateBridge.shared.observeBindingGroup { [weak self] paramRef in
            self?.updateBindingGestureUI(paramRef, viewModel: viewModel)
        }
        activeGestureJob?.cancel(cause: nil)
        activeGestureJob = WidgetStateBridge.shared.observeActiveGesture { [weak self] paramRef in
            self?.updateActiveGestureUI(paramRef, viewModel: viewModel)
        }
    }
        
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        rotationJob?.cancel(cause: nil)
        rotationJob = nil
        activeGestureJob?.cancel(cause: nil)
        activeGestureJob = nil
        provider = nil
        contentConfiguration = nil
    }
        
        
    private func updateUI(_ ref: ParameterRef, viewModel: GestureListItemViewModel) {
        guard ref.addressDevice == viewModel.widget.deviceAddress,
              ref.parameterID   == viewModel.widget.parameterID,
              let provider      = provider else { return }
        
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        
        let rotationGroup = viewModel.rotationGroup(from: parameter.data, provider: provider)

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.applyRotationGroupWithoutAnimation(rotationGroup)
        }
    }
    
    private func updateActiveGestureUI(_ ref: ParameterRef, viewModel: GestureListItemViewModel) {
        guard viewModel.contains(ref: ref) else { return }
        
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)

        let data = parameter.data
        let idHex = String(data.prefix(2))
        guard let activeGestureId = Int(idHex, radix: 16) else { return }
        
        let activeGestureTitle =
            provider?.factoryGestures.first(where: { $0.id == activeGestureId })?.title ??
            provider?.customGestures.first(where: { $0.id == activeGestureId })?.title
        
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.provider?.activeGestureId = activeGestureId
            self.provider?.activeGestureTitle = activeGestureTitle
        }
    }
    
    private func updateBindingGestureUI(_ ref: ParameterRef, viewModel: GestureListItemViewModel) {
        guard viewModel.contains(ref: ref) else { return }
        
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        print("Binding updateBindingGestureUI")
        
        let bindingGestures = viewModel.bindingGroup(from: parameter.data)

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.provider?.sprGestures = bindingGestures
        }
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
