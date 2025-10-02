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
    private var provider:   SliderProvider?
    private var job: Kotlinx_coroutines_coreJob?        // ссылка на корутину
    

    override func awakeFromNib() { super.awakeFromNib() }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: SwitchListItemViewModel) {
        self.viewModel = viewModel
        
        // Запускаем подписку на поток
        job?.cancel(cause: nil)
        job = WidgetStateBridge.shared.observeSliders{ [weak self] paramRef in
            self?.updateUI(paramRef, viewModel: viewModel)
        }
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        job?.cancel(cause: nil)        // прекращаем наблюдение
        job = nil
        provider    = nil
        contentConfiguration = nil
    }

    
    private func updateUI(_ ref: ParameterRef, viewModel: SwitchListItemViewModel) {

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
