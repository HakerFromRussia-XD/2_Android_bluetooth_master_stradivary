import UIKit
import SwiftUI
import Combine

final class SliderViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing:SliderViewCell.self)
    static let height = CGFloat(130)
    
//    @IBOutlet private var widgetSliderTitleLabel: UILabel!
//    @IBOutlet private var widgetSliderTitleLabel_2: UILabel!
//    @IBOutlet private weak var progressSlider: UISlider!
    private var sliderHostingController: UIHostingController<CustomSlider>?
//    @IBOutlet weak var containerView: UIView!
    
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
        // Assistant: базовая настройка слайдера
//        progressSlider.minimumValue = 0
//        progressSlider.maximumValue = 100
////         и визуальная настройка слайдера
////        progressSlider.setThumbImage(UIImage(), for: .normal)
//        let trackHeight: CGFloat = 9  // Высота трека (dp)
//        let activeColor = UIColor(named: "ubi4_active") ?? UIColor.green
//        let inactiveColor = UIColor(named: "ubi4_gray") ?? UIColor.gray
//        // Настройка кастомного изображения для трека
////        let minTrackImage = UIImage(color: activeColor, size: CGSize(width: 200, height: 9), lol: 1)
////        let maxTrackImage = UIImage(color: inactiveColor, size: CGSize(width: 200, height: 9), lol: 2)111
//        progressSlider.minimumTrackTintColor = activeColor
//        progressSlider.maximumTrackTintColor = inactiveColor
////        progressSlider.setMinimumTrackImage(UIImage(), for: .normal)
////        progressSlider.setMaximumTrackImage(UIImage(), for: .normal)
//        
////        let slider = CustomSlider(value: .constant(100),
////                     range: 0...100,
////                     trackHeight: 30,
////                     cornerRadius: 10,
////                     borderWidth: 1,
////                     activeColor: Color(UIColor(named: "ubi4_active") ?? UIColor.green),
////                     inactiveColor: Color(UIColor(named: "ubi4_gray") ?? UIColor.gray),
////                     borderColor: Color(UIColor(named: "ubi4_gray_border") ?? UIColor.gray))
////        sliderHostingController = UIHostingController(rootView: slider)
////        
////        // Вставляем SwiftUI представление в контейнер UIKit
////        if let hostingController = sliderHostingController {
////            // Мы добавляем контроллер к родительскому UIViewController
////            guard let parentVC = self.parentViewController else {
////                return
////            }
////            parentVC.addChild(hostingController)  // Добавляем дочерний контроллер
////            containerView.addSubview(hostingController.view)
////            
////            hostingController.view.frame = containerView.bounds
////            hostingController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
////            hostingController.didMove(toParent: parentVC)  // Уведомляем родителя, что дочерний контроллер был добавлен
////        }
        
        
        // новая попытка встроить SwiftUI
        
        
        
//        // Устанавливаем радиус скругления для углов
//        containerView.layer.cornerRadius = 10
//        containerView.layer.masksToBounds = true
//        // Дополнительная настройка границы
//        containerView.layer.borderWidth = 1.0 
//        containerView.layer.borderColor = UIColor(named: "ubi4_gray_border")?.cgColor//UIColor.gray.cgColor
        
        
        
//        subscribeToGenerator()
    }
    
//    private func subscribeToGenerator() {                  // Assistant: подписываемся на поток чисел
//        numberCancellable = NumberGenerator.shared.publisher
//            .receive(on: DispatchQueue.main)
//            .sink { [weak self] value in
//                self?.progressSlider.value = Float(value)
//            }
//    }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: AdListItemViewModel) {
//        self.viewModel = viewModel
//        widgetSliderTitleLabel.text = viewModel.title
//        widgetSliderTitleLabel_2.text = viewModel.title_2
        
        
        // 1. Создаём провайдер
        let provider = SliderRowProvider(
            title: viewModel.title,
            subtitle: viewModel.title_2
        )
        self.provider = provider
        
        // 2. Вклеиваем SwiftUI контент
        contentConfiguration = UIHostingConfiguration {
            SliderRowView(provider: provider)
        }
//        numberCancellable?.cancel()
        
        // 3. Подписываемся на поток чисел и обновляем value
//        numberCancellable = NumberGenerator.shared.publisher
//            .receive(on: DispatchQueue.main)
//            .sink { [weak provider] value in
//                provider?.value = Float(value)
//            }
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        provider    = nil
        contentConfiguration = nil
    }
    
//    private func setupConstraints() {
//        widgetSliderTitleLabel.translatesAutoresizingMaskIntoConstraints = false
//        NSLayoutConstraint.activate([
//            widgetSliderTitleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
//            widgetSliderTitleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
//            widgetSliderTitleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
//            widgetSliderTitleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8)
//        ])
//    }
}

//extension UIImage {
//    // Функция для создания изображения с заданным цветом, размером и скругленными углами
//    convenience init(color: UIColor, size: CGSize, lol: Int) {
//        let borderColor = UIColor(named: "ubi4_gray_border") ?? UIColor.gray
//        let activeColor = UIColor(named: "ubi4_active") ?? UIColor.green
//        let inactiveColor = UIColor(named: "ubi4_gray") ?? UIColor.gray
//        
//        let cornerRadius: CGFloat = 3 // Радиус скругления углов (dp)
//        let borderWidth: CGFloat = 1.0
//        
//        // Создаём контекст
//        UIGraphicsBeginImageContextWithOptions(size, false, 0)
//        
//        // Слой для рисования пути
//        let shapeLayer = CAShapeLayer()
//        shapeLayer.frame = CGRect(origin: .zero, size: size)
//        
//        // Рисуем путь с заданными скругленными углами
//        let path = UIBezierPath(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: cornerRadius)
//            
//        
//        // Заливка активным цветом
//        shapeLayer.fillColor = color.cgColor
//        shapeLayer.path = path.cgPath
//        
//        // Добавление обводки
//        if borderWidth > 0 {
//            shapeLayer.lineWidth = borderWidth
//            shapeLayer.strokeColor = borderColor.cgColor  // Цвет обводки
//            shapeLayer.lineJoin = .round  // Скругление углов обводки
//        }
//        
//        // Рисуем слой в контексте
//        shapeLayer.render(in: UIGraphicsGetCurrentContext()!)
//
//        
//        // Получаем изображение из контекста
//        let cgImage = UIGraphicsGetImageFromCurrentImageContext()!.cgImage!
//        UIGraphicsEndImageContext()
//        
//        self.init(cgImage: cgImage)
//    }
//}
//extension UIView {
//    var parentViewController: UIViewController? {
//        var parentResponder: UIResponder? = self
//        while parentResponder != nil {
//            parentResponder = parentResponder?.next
//            if let viewController = parentResponder as? UIViewController {
//                return viewController
//            }
//        }
//        return nil
//    }
//}

//struct CustomSlider_Previews: PreviewProvider {
//    static var previews: some View {
//        CustomSlider(value: .constant(100),
//                     range: 0...100,
//                     trackHeight: 30,
//                     cornerRadius: 10,
//                     borderWidth: 1,
//                     activeColor: Color(UIColor(named: "ubi4_active") ?? UIColor.green),
//                     inactiveColor: Color(UIColor(named: "ubi4_gray") ?? UIColor.gray),
//                     borderColor: Color(UIColor(named: "ubi4_gray_border") ?? UIColor.gray))
//            .frame(height: 30)
//            .padding()
//            .previewLayout(.sizeThatFits)
//    }
//}
