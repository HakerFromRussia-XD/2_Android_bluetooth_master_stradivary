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
    private var job: Kotlinx_coroutines_coreJob?
    
    // Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
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
                        self.viewModel.requestRotationGroup()
                    case .sprGroup:
                        self.viewModel.requestBindingGroup()
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
                onRotationGesturesReorder: {_ in },
                onSprGestureAction: { _ in },
                onSprAddTap: { }
            )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration
            
        // 3. Запускаем подписку на поток
        job?.cancel(cause: nil)
        job = WidgetStateBridge.shared.observeRotationGroup { [weak self] paramRef in
            self?.updateUI(paramRef, viewModel: viewModel)
        }
    }
        
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        job?.cancel(cause: nil)
        job = nil
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
            self.provider?.rotationGroup = rotationGroup
        }
    }
}
