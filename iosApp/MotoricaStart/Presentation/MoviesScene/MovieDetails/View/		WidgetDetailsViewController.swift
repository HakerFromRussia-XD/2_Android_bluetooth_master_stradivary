import UIKit

final class         WidgetDetailsViewController: UIViewController, StoryboardInstantiable {

    @IBOutlet private var posterImageView: UIImageView!
    @IBOutlet private var overviewTextView: UITextView!

    // MARK: - Lifecycle

    private var viewModel: WidgetDetailsViewModel!
    
    static func create(with viewModel: WidgetDetailsViewModel) ->         WidgetDetailsViewController {
        let view =         WidgetDetailsViewController.instantiateViewController()
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

    private func bind(to viewModel: WidgetDetailsViewModel) {
        viewModel.posterImage.observe(on: self) { [weak self] in self?.posterImageView.image = $0.flatMap(UIImage.init) }
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        viewModel.updatePosterImage(width: Int(posterImageView.imageSizeAfterAspectFit.scaledSize.width))
    }

    // MARK: - Private
    private func setupViews() {
        title = viewModel.title
        overviewTextView.text = viewModel.overview
        posterImageView.isHidden = viewModel.isPosterImageHidden
        view.accessibilityIdentifier = AccessibilityIdentifier.widgetDetailsView
    }
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
}
