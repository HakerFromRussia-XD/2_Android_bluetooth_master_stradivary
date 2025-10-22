import UIKit
import SwiftUI
import Combine
import shared

final class SliderViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing:SliderViewCell.self)
    static let height = CGFloat(130)
    
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
        
        if let cachedValues = viewModel.cachedSliderValues() {
            if let first = cachedValues.first {
                provider.value_1 = first
            }
            if cachedValues.count > 1 {
                provider.value_2 = cachedValues[1]
            }
        }
        
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
        
        // 3. Запускаем подписку на поток
        job?.cancel(cause: nil)
        job = WidgetStateBridge.shared.observeSliders{ [weak self] paramRef in
            self?.updateUI(paramRef, viewModel: viewModel)
        }
        
        viewModel.requestSlider()
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
        guard let values = viewModel.sliderValues(from: parameter) else { return }
        print("[BLE-COMMUNICATION] SliderViewCell in updateUI values = \(values)")
        

        DispatchQueue.main.async { [weak self] in
            if let first = values.first {
                self?.provider?.value_1 = Float(first)
            }
            if values.count > 1 {
                self?.provider?.value_2 = Float(values[1])
            }
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
