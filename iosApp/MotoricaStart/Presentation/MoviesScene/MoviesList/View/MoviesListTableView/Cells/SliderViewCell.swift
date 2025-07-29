import UIKit
import SwiftUI
import Combine

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
    
    private var viewModel: AdListItemViewModel!
    private let mainQueue: DispatchQueueType = DispatchQueue.main
    private var numberCancellable: AnyCancellable?

    // Assistant: Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    
    // новая попытка встроить SwiftUI
    private var cancellable: AnyCancellable?
    private var provider:   SliderRowProvider?
    
    
    
    
    

    override func awakeFromNib() {
        super.awakeFromNib()
    }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: AdListItemViewModel) {
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
            isSecondSliderHidden: viewModel.hideSecondSlider,
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
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
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
}
