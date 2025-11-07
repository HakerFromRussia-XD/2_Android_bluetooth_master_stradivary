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
                onRotationGestureMoveUp: { [weak self] index in
//                    guard let self, let provider = self.provider else { return }
//                    self.viewModel.moveRotationGestureUp(at: index, provider: provider)
                },
                onRotationGestureMoveDown: { [weak self] index in
//                    guard let self, let provider = self.provider else { return }
//                    self.viewModel.moveRotationGestureDown(at: index, provider: provider)
                },
                onRotationGestureRemove: { [weak self] index in
//                    guard let self, let provider = self.provider else { return }
//                    self.viewModel.removeRotationGesture(at: index, provider: provider)
                },
                onRotationGestureAdd: { [weak self] in
//                    guard let self, let provider = self.provider else { return }
//                    self.viewModel.appendRotationGesture(provider: provider)
                },
                onRotationGesturesReorder: { [weak self] items in
//                    guard let self, let provider = self.provider else { return }
//                    self.viewModel.reorderRotationGestures(items, provider: provider)
                },
                onSprGestureAction: { _ in },
                onSprAddTap: { }
            )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration
            
        // 3. Запускаем подписку на поток
        job?.cancel(cause: nil)
        job = WidgetStateBridge.shared.observeSwitchers { [weak self] paramRef in
            self?.updateUI(paramRef, viewModel: viewModel)
        }
        
//        viewModel.requestActiveGesutre()
    }
        
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        job?.cancel(cause: nil)
        job = nil
        provider = nil
        contentConfiguration = nil
//        isProgrammaticUpdate = false
    }
        
        
    private func updateUI(_ ref: ParameterRef, viewModel: GestureListItemViewModel) {
        guard ref.addressDevice == viewModel.widget.deviceAddress,
              ref.parameterID   == viewModel.widget.parameterID else { return }
        
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        
//        guard let isOn = viewModel.switchValue(from: parameter) else { return }
//        
//        DispatchQueue.main.async { [weak self] in
//            guard let self else { return }
//            guard self.provider?.isOn != isOn else { return }
//            self.isProgrammaticUpdate = true
//            self.provider?.isOn = isOn
//            self.isProgrammaticUpdate = false
//        }
    }
}
