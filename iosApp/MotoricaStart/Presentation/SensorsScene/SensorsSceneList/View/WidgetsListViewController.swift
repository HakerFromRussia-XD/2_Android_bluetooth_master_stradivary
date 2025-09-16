import UIKit
import DGCharts
import shared

final class WidgetsListViewController: UIViewController, StoryboardInstantiable, Alertable {
    @IBOutlet private var contentView: UIView!
    @IBOutlet private var widgetsListContainer: UIView!
    @IBOutlet private(set) var suggestionsListContainer: UIView!
    @IBOutlet private var emptyDataLabel: UILabel!
    private lazy var bottomButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Нажми меня", for: .normal)
        button.addTarget(self, action: #selector(bottomButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private var viewModel: WidgetsListViewModel!
    private var posterImagesRepository: PosterImagesRepository?

    private var widgetsTableViewController: WidgetsListTableViewController?
    let storage = CoreDataWidgetsResponseStorage() 

    // MARK: - Lifecycle
    static func create(with viewModel: WidgetsListViewModel,posterImagesRepository: PosterImagesRepository?) -> WidgetsListViewController {
        let view = WidgetsListViewController.instantiateViewController()
        view.viewModel = viewModel
        view.posterImagesRepository = posterImagesRepository
        return view
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        setupBehaviours()
        bind(to: viewModel)
        
        view.addSubview(bottomButton)
        NSLayoutConstraint.activate([
            bottomButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            bottomButton.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // Обновить данные графиков, подписаться на потоки, перезагрузить таблицу
        // startSensorsStreaming()
        print("[WIDGET_COORDINATOR] viewWillAppear")
        let dataFactory = DataFactory()
        let kotlinWidgets = dataFactory.prepareData(display: 1)
//        let kotlinWidgets = dataFactory.fakeData()
        print("[WIDGET_COORDINATOR] kotlinWidgets: \(kotlinWidgets)")
        
        // Преобразуем Kotlin-виджеты в DTO, помечая SliderItem как рекламу
        let widgetsDTO: [WidgetsResponseDTO.WidgetDTO] = kotlinWidgets
            .enumerated()
            .map { index, widget in
                print("[WIDGET_COORDINATOR] kotlinWidgets  index = \(index)   widget = \(widget)")
                var widgetType: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO?
                
                switch widget {
                    case is PlotItem:
                        widgetType = .plotWidget
                    case is SliderItem:
                        widgetType = .sliderWidget
                    case is OneButtonItem:
                        widgetType = .commandWidget
                    
                    case is BaseParameterWidgetEStruct, is BaseParameterWidgetSStruct:
                        widgetType = .commandWidget
                    case is GestureOpticParameterWidgetEStruct:
                        widgetType = .commandWidget
                    case is GestureParameterWidgetEStruct:
                        widgetType = .commandWidget
                    case is OpticStartLearningWidgetEStruct, is OpticStartLearningWidgetSStruct:
                        widgetType = .commandWidget
                    case is PlotParameterWidgetEStruct, is PlotParameterWidgetSStruct:
                        widgetType = .plotWidget
                    case is SliderParameterWidgetEStruct, is SliderParameterWidgetSStruct:
                        widgetType = .sliderWidget
                    case is SpinnerParameterWidgetEStruct, is SpinnerParameterWidgetSStruct:
                        widgetType = .commandWidget
                    case is SwitchParameterWidgetEStruct, is SwitchParameterWidgetSStruct:
                        widgetType = .commandWidget
                    case is ThresholdParameterWidgetEStruct, is ThresholdParameterWidgetSStruct:
                        widgetType = .commandWidget
                    default:
                        widgetType = .commandWidget
                }
                
                return WidgetsResponseDTO.WidgetDTO(
                    id: index,
                    title: "Widget \(index)",
                    widgetType: widgetType,
                    overview: nil,
                    releaseDate: nil,
                    isAd: false
                )
            }
        print("[WIDGET_COORDINATOR] widgetsDTO: \(widgetsDTO)")

        let mockResponseDTO = WidgetsResponseDTO(
            page: 2,
            totalPages: 5,
            widgets: widgetsDTO
        )
        
        
        let requestDTO = WidgetsRequestDTO(query: WidgetQuery(query: "My request").query, page: 1)
        storage.save(response: mockResponseDTO, for: requestDTO) { [weak self] responseDTO in
            self?.viewModel.update(with: responseDTO.toDomain())
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        // Остановить таймеры, приостановить стримы, снять наблюдателей
        // stopSensorsStreaming()
        print("[WIDGET_COORDINATOR] viewWillDisappear")
    }

    private func bind(to viewModel: WidgetsListViewModel) {
        viewModel.items.observe(on: self) { [weak self] _ in self?.updateItems() }
        viewModel.loading.observe(on: self) { [weak self] in self?.updateLoading($0) }
        viewModel.error.observe(on: self) { [weak self] in self?.showError($0) }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == String(describing: WidgetsListTableViewController.self),
            let destinationVC = segue.destination as? WidgetsListTableViewController {
            widgetsTableViewController = destinationVC
            widgetsTableViewController?.viewModel = viewModel
            widgetsTableViewController?.posterImagesRepository = posterImagesRepository
            viewModel.viewDidLoad()
        }
    }

    // MARK: - Private
    @objc private func bottomButtonTapped() {
        viewModel.requestInicializeInformation()
    }
    
    private func setupViews() {
        title = viewModel.screenTitle
        emptyDataLabel.text = viewModel.emptyDataTitle
    }

    private func setupBehaviours() {
        addBehaviors([BackButtonEmptyTitleNavigationBarBehavior(),
                      BlackStyleNavigationBarBehavior()])
    }

    private func updateItems() {
        widgetsListContainer.isHidden = false
        emptyDataLabel.isHidden = true
        widgetsTableViewController?.reload()
    }

    private func updateLoading(_ loading: WidgetsListViewModelLoading?) {
        emptyDataLabel.isHidden = true
        widgetsListContainer.isHidden = true
        suggestionsListContainer.isHidden = true
        LoadingView.hide()

        switch loading {
        case .fullScreen: LoadingView.show()
        case .nextPage: widgetsListContainer.isHidden = false
        case .none:
            widgetsListContainer.isHidden = false
            emptyDataLabel.isHidden = !viewModel.isEmpty
        }
        

        widgetsTableViewController?.updateLoading(loading)
    }

    private func showError(_ error: String) {
        guard !error.isEmpty else { return }
        showAlert(title: viewModel.errorTitle, message: error)
    }
}

