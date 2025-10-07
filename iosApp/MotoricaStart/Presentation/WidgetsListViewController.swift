import UIKit
import DGCharts
import shared

final class WidgetsListViewController: UIViewController, StoryboardInstantiable, Alertable {
    static var defaultFileName: String { "WidgetsListViewController" }
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

    private var widgetsTableViewController: WidgetsListTableViewController?
    private var widgetsUpdateJob: Kotlinx_coroutines_coreJob?
    var display: Int32 = 1
    var screenTitleOverride: String?
    let storage = CoreDataWidgetsResponseStorage()

    // MARK: - Lifecycle
    static func create(with viewModel: WidgetsListViewModel) -> WidgetsListViewController {
        let view = WidgetsListViewController.instantiateViewController()
        view.viewModel = viewModel
        return view
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        bind(to: viewModel)
        
        view.addSubview(bottomButton)
        NSLayoutConstraint.activate([
            bottomButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            bottomButton.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        print("[WIDGET_COORDINATOR] viewWillAppear")
        startObservingWidgetUpdates()
        reloadWidgetsFromShared()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        print("[WIDGET_COORDINATOR] viewWillDisappear")
        stopObservingWidgetUpdates()
    }
    
    deinit {
        stopObservingWidgetUpdates()
    }

    private func startObservingWidgetUpdates() {
        print("[WIDGET_COORDINATOR] startObservingWidgetUpdates")
        widgetsUpdateJob?.cancel(cause: nil)
        widgetsUpdateJob = UiStateBridge.shared.observeUpdates { [weak self] _ in
            self?.reloadWidgetsFromShared()
        }
    }

    private func stopObservingWidgetUpdates() {
        print("[WIDGET_COORDINATOR] stopObservingWidgetUpdates")
        widgetsUpdateJob?.cancel(cause: nil)
        widgetsUpdateJob = nil
    }

    private func reloadWidgetsFromShared() {
        print("[WIDGET_COORDINATOR] reloadWidgetsFromShared")
        let dataFactory = DataFactory()
        let kotlinWidgets = dataFactory.prepareData(display: display)
//        let kotlinWidgets = dataFactory.fakeData()
        print("[WIDGET_COORDINATOR] kotlinWidgets: \(kotlinWidgets)")
        
        // Преобразуем Kotlin-виджеты в DTO, помечая SliderItem как рекламу
        let widgetsDTO: [WidgetsResponseDTO.WidgetDTO] = kotlinWidgets
            .enumerated()
            .map { index, widget in
                print("[WIDGET_COORDINATOR] kotlinWidgets  index = \(index)   widget = \(widget)")
                var widgetType: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO?
                
                var title: String?
                var widgetObject: Any? = widget
                
                switch widget {
                    case let plotItem as PlotItem:
                        widgetType = .plotWidget
                        title = plotItem.title
                        widgetObject = plotItem.widget
                    case let sliderItem as SliderItem:
                        widgetType = .sliderWidget
                        title = sliderItem.title
                        widgetObject = sliderItem.widget
                    case let oneButtonItem as OneButtonItem:
                        widgetType = .commandWidget
                        title = oneButtonItem.title
                        widgetObject = oneButtonItem.widget
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
                        widgetObject = widget
                    case is SliderParameterWidgetEStruct, is SliderParameterWidgetSStruct:
                        widgetType = .sliderWidget
                        widgetObject = widget
                    case is SpinnerParameterWidgetEStruct, is SpinnerParameterWidgetSStruct:
                        widgetType = .commandWidget
                    case is SwitchParameterWidgetEStruct, is SwitchParameterWidgetSStruct:
                        widgetType = .commandWidget
                    case is ThresholdParameterWidgetEStruct, is ThresholdParameterWidgetSStruct:
                        widgetType = .commandWidget
                    default:
                        widgetType = .commandWidget
                    }

                    if title == nil {
                        title = extractTitle(from: widget) ?? "Widget \(index)"
                }
                
                return WidgetsResponseDTO.WidgetDTO(
                    id: index,
                    title: title,
                    widgetType: widgetType,
                    widget: AnyCodable(widgetObject)
                )
            }
        print("[WIDGET_COORDINATOR] widgetsDTO: \(widgetsDTO)")

        let mockResponseDTO = WidgetsResponseDTO(
            page: 1,
            totalPages: 1,
            widgets: widgetsDTO
        )
        
        
        let requestDTO = WidgetsRequestDTO(query: WidgetQuery(query: "My request").query, page: 1)
        storage.save(response: mockResponseDTO, for: requestDTO) { [weak self] responseDTO in
            self?.viewModel.update(with: responseDTO.toDomain())
        }
    }

    private func bind(to viewModel: WidgetsListViewModel) {
        viewModel.items.observe(on: self) { [weak self] _ in self?.updateItems() }
        viewModel.loading.observe(on: self) { [weak self] in self?.updateLoading($0) }
        viewModel.error.observe(on: self) { [weak self] in self?.showError($0) }
    }
    
    private func extractTitle(from widget: Any?) -> String? {
            switch widget {
            case let plotItem as PlotItem:
                return plotItem.title
            case let sliderItem as SliderItem:
                return sliderItem.title
            case let buttonItem as OneButtonItem:
                return buttonItem.title
            case let gesturesItem as GesturesItem:
                return gesturesItem.title
            case let switchItem as SwitchItem:
                return switchItem.title
            case let trainingItem as TrainingGestureItem:
                return trainingItem.title
            case let spinnerItem as SpinnerItem:
                return spinnerItem.title
            default:
                return nil
            }
        }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == String(describing: WidgetsListTableViewController.self),
            let destinationVC = segue.destination as? WidgetsListTableViewController {
            widgetsTableViewController = destinationVC
            widgetsTableViewController?.viewModel = viewModel
            viewModel.viewDidLoad()
        }
    }

    // MARK: - Private
    @objc private func bottomButtonTapped() {
        viewModel.requestInicializeInformation()
//        let data = BLECommands.shared.requestSlider(
//            addressDevice: Int32(widget.deviceAddress),
//            parameterID: Int32(widget.parameterID)
//        )
    }
    
    private func setupViews() {
        title = viewModel.screenTitle
        title = screenTitleOverride ?? viewModel.screenTitle
        emptyDataLabel.text = viewModel.emptyDataTitle
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

