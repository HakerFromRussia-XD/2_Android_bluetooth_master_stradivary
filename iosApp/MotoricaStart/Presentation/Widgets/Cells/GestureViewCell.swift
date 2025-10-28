import UIKit
import SwiftUI
import Combine
import shared

final class GestureOpticViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: GestureOpticViewCell.self)
    
    private var viewModel: GestureOpticListItemViewModel!
    private let mainQueue: DispatchQueueType = DispatchQueue.main
    private var numberCancellable: AnyCancellable?
    
    // Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    private var cancellable: AnyCancellable?
    private var provider:   GestureOpticProvider?
    private var job: Kotlinx_coroutines_coreJob?        // ссылка на корутину
    
    override func awakeFromNib() { super.awakeFromNib() }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: GestureOpticListItemViewModel) {
        self.viewModel = viewModel
        
        // 1. Создаём провайдер
        let provider = GestureOpticProvider(
            title: viewModel.title
        )
        self.provider = provider
        cancellable?.cancel()
        
        // 2. Вклеиваем SwiftUI контент
//        contentConfiguration = UIHostingConfiguration {
//            GestureOpticProvider(provider: provider, title: <#String#>)
//        }
//        numberCancellable?.cancel()
//            
//        // 3. Запускаем подписку на поток
//        job?.cancel(cause: nil)
//        job = WidgetStateBridge.shared.observeSwitchers { [weak self] paramRef in
//            self?.updateUI(paramRef, viewModel: viewModel)
//        }
        
        viewModel.requestActiveGesutre()
    }
        
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
//        job?.cancel(cause: nil)        // прекращаем наблюдение
//        job = nil
//        provider    = nil
//        contentConfiguration = nil
//        isProgrammaticUpdate = false
    }
        
        
    private func updateUI(_ ref: ParameterRef, viewModel: SwitchListItemViewModel) {
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
