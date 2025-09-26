import UIKit

final class WidgetDetailsViewController: UIViewController, StoryboardInstantiable {

    // MARK: - Lifecycle

    private var viewModel: WidgetDetailsViewModel!
    
    static func create(with viewModel: WidgetDetailsViewModel) -> WidgetDetailsViewController {
        let view = WidgetDetailsViewController.instantiateViewController()
        view.viewModel = viewModel
        return view
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        bind(to: viewModel)
        if let windowScene = view.window?.windowScene {
            // Получаем frame статусбара
            if let statusBarFrame = windowScene.statusBarManager?.statusBarFrame {
                // Создаём кастомный UIView для имитации фонового цвета статусбара
                let statusBarView = UIView(frame: statusBarFrame)
                statusBarView.backgroundColor = UIColor(named: "ubi4_back") ?? UIColor.black // Используйте свой цвет фона
                
                // Добавляем его на главный view
                view.addSubview(statusBarView)
                
                // Размещаем его поверх всех элементов, если нужно
                view.bringSubviewToFront(statusBarView)
            }
        }
    }
    
    private func bind(to  viewModel: WidgetDetailsViewModel) {}
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
    }

    // MARK: - Private
    private func setupViews() {
        title = viewModel.title
        view.accessibilityIdentifier = AccessibilityIdentifier.widgetDetailsView
    }
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
}
