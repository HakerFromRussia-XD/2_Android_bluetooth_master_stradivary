import UIKit
import SwiftUI
import Combine
import shared

final class SliderViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing:SliderViewCell.self)
    /// Identifier used when registering and dequeuing the cell
//    static var reuseIdentifier: String {
//        String(describing: self)
//    }
    static let height = CGFloat(130)
    
    @IBOutlet private var widgetSliderTitleLabel: UILabel!
    @IBOutlet private var widgetSliderTitleLabel_2: UILabel!
    @IBOutlet private weak var progressSlider: UISlider!
    private var sliderHostingController: UIHostingController<CustomSlider>?
    @IBOutlet weak var containerView: UIView!
    
    private var viewModel: SliderListItemViewModel!
    private let mainQueue: DispatchQueueType = DispatchQueue.main
    private var numberCancellable: AnyCancellable?

    // Assistant: Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    
    private var cancellable: AnyCancellable?
    private var provider:   SliderRowProvider?
    private var job: Kotlinx_coroutines_coreJob?        // ссылка на корутину
    

    override func awakeFromNib() { super.awakeFromNib() }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: SliderListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        
        // 1. Создаём провайдер
        let provider = SliderRowProvider(
            value_1: .zero,
            title_1: viewModel.title,
            numLabel_1: viewModel.title_2,
            
            value_2: .zero,
            title_2: viewModel.title,
            numLabel_2: viewModel.title_2,
            isSecondSliderShow: viewModel.showSecondSlider,
        )
        self.provider = provider
        
        // 2. Вклеиваем SwiftUI контент
        contentConfiguration = UIHostingConfiguration {
            SliderRowView(provider: provider)
        }
        numberCancellable?.cancel()
        
//         3. Подписываемся на поток чисел и обновляем value
        numberCancellable = NumberGenerator.shared.publisher
            .receive(on: DispatchQueue.main)
            .sink { [weak provider] value in
                provider?.value_1 = Float(value.0)
                provider?.value_2 = Float(value.1)
            }
        
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
    
    private func setupConstraints() {
        widgetSliderTitleLabel.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            widgetSliderTitleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            widgetSliderTitleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            widgetSliderTitleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            widgetSliderTitleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8)
        ])
    }
    
    private func updateUI(_ ref: ParameterRef, viewModel: SliderListItemViewModel) {
        guard ref.addressDevice == viewModel.deviceAddress,
              ref.parameterID   == viewModel.parameterID else { return }

        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)

//        let typeIndex = Int(parameter.type)
//        let sizeOf = ParameterTypeEnum.values()[Int(parameter.type)].sizeOf
//        let sizeOf = ParameterTypeEnum.values()[Int(parameter.type)].allSatisfy({ $0.hashValue == Int(parameter.type).hashValue }) ? Int(parameter.type) : 1
//        let sizeOf = PreferenceKeysUbi4ParameterTypeEnum.values()[Int(parameter.type)].sizeOf //PreferenceKeysUbi4ParameterTypeEnum().entries[Int(parameter.type)].sizeOf
        guard let typeEnum = ParameterTypeEnum(rawValue: Int32(parameter.type)) else { return }
        let sizeOf = Int(typeEnum.sizeOf)

        let hex = parameter.data
        let end = hex.index(hex.startIndex, offsetBy: sizeOf * 2)
        let valueHex = String(hex[..<end])
        let value = Int(valueHex, radix: 16) ?? 0

        DispatchQueue.main.async { [weak self] in
            self?.provider?.value_1 = Float(value)
        }
    }
}
