import UIKit
import SwiftUI
import Combine
import shared

final class SwitchViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing:SwitchViewCell.self)
    private var widgetSwitchInfo: WidgetSwitchInfo?
    
    private var viewModel: SwitchListItemViewModel!
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
    private var provider:   SwitchProvider?
    private var job: Kotlinx_coroutines_coreJob?        // ссылка на корутину
    private var isProgrammaticUpdate = false
    
    override func awakeFromNib() { super.awakeFromNib() }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: SwitchListItemViewModel) {
        self.viewModel = viewModel
        
        // 1. Создаём провайдер
        let provider = SwitchProvider(
            isOn: viewModel.cachedSwitchValue() ?? viewModel.widget.switchUnified?.isChecked ?? false,
            title: viewModel.title
        )
        self.provider = provider
        cancellable?.cancel()
        isProgrammaticUpdate = true
        cancellable = provider.$isOn
            .dropFirst()
            .removeDuplicates()
            .sink { [weak self] isOn in
                self?.handleSwitchChange(isOn: isOn)
            }
        
        // 2. Вклеиваем SwiftUI контент
        contentConfiguration = UIHostingConfiguration {
            SwitchRowView(provider: provider)
        }
        numberCancellable?.cancel()
            
        // 3. Запускаем подписку на поток
        job?.cancel(cause: nil)
        job = WidgetStateBridge.shared.observeSwitchers { [weak self] paramRef in
            self?.updateUI(paramRef, viewModel: viewModel)
        }
        
        viewModel.requestSwitch()
    }
        
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        job?.cancel(cause: nil)        // прекращаем наблюдение
        job = nil
        provider    = nil
        contentConfiguration = nil
        isProgrammaticUpdate = false
    }
        
        
    private func updateUI(_ ref: ParameterRef, viewModel: SwitchListItemViewModel) {
        guard ref.addressDevice == viewModel.widget.deviceAddress,
              ref.parameterID   == viewModel.widget.parameterID else { return }
        
        let parameter = ParameterProvider.Companion().getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        
        guard let isOn = viewModel.switchValue(from: parameter) else { return }
        
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            guard self.provider?.isOn != isOn else { return }
            self.isProgrammaticUpdate = true
            defer { self.isProgrammaticUpdate = false }
            self.provider?.isOn = isOn
        }
    }
}
    
private extension SwitchViewCell {
    func handleSwitchChange(isOn: Bool) {
        let isProgrammatic = isProgrammaticUpdate
        isProgrammaticUpdate = false
        guard !isProgrammatic else { return }
        viewModel.sendSwitchState(isOn: isOn)
    }
}



final class WidgetSwitchInfo {
    var addressDevice: Int = 0
    var parameterID: Int = 0

    
    init(addressDevice: Int,
         parameterID: Int,
    ) {
        self.addressDevice = addressDevice
        self.parameterID = parameterID
    }
}
