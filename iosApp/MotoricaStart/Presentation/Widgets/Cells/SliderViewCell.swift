import UIKit
import SwiftUI
import Combine
import shared

final class SliderViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing:SliderViewCell.self)
    static let height = CGFloat(130)
    private var widgetSliderInfo: WidgetSliderInfo?
    
    @IBOutlet private var widgetSliderTitleLabel: UILabel!
    @IBOutlet private var widgetSliderTitleLabel_2: UILabel!
    @IBOutlet private weak var progressSlider: UISlider!
    private var sliderHostingController: UIHostingController<CustomSlider>?
    @IBOutlet weak var containerView: UIView!
    
    private var viewModel: SliderListItemViewModel!
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
    func configure(with viewModel: SliderListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        print("requestSlider  title = \(viewModel.title)")
        viewModel.requestSlider()
        
        // 1. Создаём провайдер
        let provider = SliderProvider(
            value_1: .zero,
            title_1: viewModel.title,
            numLabel_1: viewModel.title_2,
            
            value_2: .zero,
            title_2: viewModel.title,
            numLabel_2: viewModel.title_2,
            isSecondSliderShow: viewModel.paramCount > 1,
            
            maxProgress: Float(viewModel.widget.sliderUnified?.maxProgress ?? 100),
            minProgress: Float(viewModel.widget.sliderUnified?.minProgress ?? 0)
        )
        self.provider = provider
        
        // 2. Вклеиваем SwiftUI контент
        contentConfiguration = UIHostingConfiguration {
            SliderRowView(
                provider: provider,
                onFirstSliderEditingEnded: { [weak self] _ in
                    self?.sliderEditingDidEnd()
                },
                onSecondSliderEditingEnded: { [weak self] _ in
                    self?.sliderEditingDidEnd()
                }
            )
        }
        numberCancellable?.cancel()
        
        // 3. Подписываемся на поток чисел и обновляем value
//        numberCancellable = NumberGenerator.shared.publisher
//            .receive(on: DispatchQueue.main)
//            .sink { [weak provider] value in
//                provider?.value_1 = Float(value.0)
//                provider?.value_2 = Float(value.1)
//            }
        
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
        guard ref.addressDevice == viewModel.widget.deviceAddress,
              ref.parameterID   == viewModel.widget.parameterID else { return }
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        
        let ordinal = Int(parameter.type)
//        print("[BLE-COMMUNICATION] in updateUI for ordinal = \(ordinal)")
        let entries = ParameterTypeEnum.values()
//        print("[BLE-COMMUNICATION] in updateUI for entries = \(entries)")
        let count = Int(entries.size)
//        print("[BLE-COMMUNICATION] in updateUI for count = \(count)")
        guard ordinal >= 0 && ordinal < count,
        let entry = entries.get(index: Int32(ordinal)) else { return }
//        print("[BLE-COMMUNICATION] in updateUI for entry = \(entry)")
        let sizeOf = Int(entry.sizeOf)
//        print("[BLE-COMMUNICATION] in updateUI for sizeOf = \(sizeOf)")
        let value = Int(String(parameter.data.prefix(sizeOf * 2)), radix: 16) ?? 0
//        let hex = parameter.data
//        let end = hex.index(hex.startIndex, offsetBy: sizeOf * 2)
//        let valueHex = String(hex[..<end])
//        let value = Int(valueHex, radix: 16) ?? 0
        
        print("[BLE-COMMUNICATION] SliderViewCell in updateUI deviceAddress = \(ref.addressDevice)   parameterID = \(ref.parameterID)")
        print("[BLE-COMMUNICATION] SliderViewCell in updateUI ref.addressDevice = \(ref.addressDevice)")
        print("[BLE-COMMUNICATION] SliderViewCell in updateUI parameterID = \(viewModel.widget.parameterID)")
        print("[BLE-COMMUNICATION] SliderViewCell in updateUI ref.parameterID = \(ref.parameterID)")
        print("[BLE-COMMUNICATION] SliderViewCell in updateUI value = \(value)")
//        let value2 = Int(String(parameter.data.dropFirst(sizeOf * 2).prefix(sizeOf * 2)), radix: 16) ?? 0

//        viewModel.widget.sliderUnified?.baseParameterWidgetStruct?.parameterInfoSet dataOffset.enumerated().forEach {}
//        viewModel.widget.sliderUnified?.baseParameterWidgetStruct?.dataOffset.enumerated().forEach { (index, it) in
//            let newValue = Int(parameter.data[(sizeOf * it) * 2..<(sizeOf * (it + 1) * 2)], radix: 16) ?? 0
//            let newByteValue = Int8(bitPattern: UInt8(newValue))
//        }
        

        DispatchQueue.main.async { [weak self] in
            self?.provider?.value_1 = Float(value)
//            self?.provider?.value_2 = Float(value2)
        }
    }
    
    private func sliderEditingDidEnd() {
        guard let provider else { return }
        let data: [KotlinInt] = [
            KotlinInt(int: Int32(provider.value_1)),
            KotlinInt(int: Int32(provider.value_2)),
            KotlinInt(int: 0),
            KotlinInt(int: 0),
            KotlinInt(int: 0),
            KotlinInt(int: 0)
        ]
        viewModel.sendSliderProgress(progress: data)
    }
}


final class WidgetSliderInfo {
    var addressDevice: Int = 0
    var parameterID: Int = 0
    var dataOffset: [Int] = []
    var minProgress: Int = 0
    var maxProgress: Int = 0
    var progress: [Int] = []
    var widgetSlidersSb: [UIProgressView] = []
    var widgetSliderNumTv: [UILabel] = []
    var widgetPosition: Int = 0
    var instanceId: Int = 0
    var responseReceived: Bool = false
    var loadingAnimators: [UIViewPropertyAnimator?] = []
    
    init(addressDevice: Int,
         parameterID: Int,
         dataOffset: [Int],
         minProgress: Int,
         maxProgress: Int,
         progress: [Int],
         widgetSlidersSb: [UIProgressView],
         widgetSliderNumTv: [UILabel],
         widgetPosition: Int,
         instanceId: Int,
         responseReceived: Bool,
         loadingAnimators: [UIViewPropertyAnimator?]
    ) {
        self.addressDevice = addressDevice
        self.parameterID = parameterID
        self.dataOffset = dataOffset
        self.minProgress = minProgress
        self.maxProgress = maxProgress
        self.progress = progress
        self.widgetSlidersSb = widgetSlidersSb
        self.widgetSliderNumTv = widgetSliderNumTv
        self.widgetPosition = widgetPosition
        self.instanceId = instanceId
        self.responseReceived = responseReceived
        self.loadingAnimators = loadingAnimators
    }
}
