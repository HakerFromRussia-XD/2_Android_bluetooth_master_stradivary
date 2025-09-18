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
    private var widgetsUpdateJob: Kotlinx_coroutines_coreJob?
    private let defaultDisplay: Int32 = 1
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
        let kotlinWidgets = dataFactory.prepareData(display: 1)
//        let kotlinWidgets = dataFactory.fakeData()
        print("[WIDGET_COORDINATOR] kotlinWidgets: \(kotlinWidgets)")
        
        // Преобразуем Kotlin-виджеты в DTO, помечая SliderItem как рекламу
        let widgetsDTO: [WidgetsResponseDTO.WidgetDTO] = kotlinWidgets
            .enumerated()
            .map { index, widget in
                print("[WIDGET_COORDINATOR] kotlinWidgets  index = \(index)   widget = \(widget)")
                var widgetType: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO?
                
                var title: String?
                var overview: String?
                var deviceAddress: Int?
                var parameterID: Int?
                
                switch widget {
//                    case is PlotItem:
//                        widgetType = .plotWidget
//                    case is SliderItem:
//                        widgetType = .sliderWidget
//                    case is OneButtonItem:
//                        widgetType = .commandWidget
//                    
//                    case is BaseParameterWidgetEStruct, is BaseParameterWidgetSStruct:
//                        widgetType = .commandWidget
//                    case is GestureOpticParameterWidgetEStruct:
//                        widgetType = .commandWidget
//                    case is GestureParameterWidgetEStruct:
//                        widgetType = .commandWidget
//                    case is OpticStartLearningWidgetEStruct, is OpticStartLearningWidgetSStruct:
//                        widgetType = .commandWidget
//                    case is PlotParameterWidgetEStruct, is PlotParameterWidgetSStruct:
//                        widgetType = .plotWidget
//                    case is SliderParameterWidgetEStruct, is SliderParameterWidgetSStruct:
//                        widgetType = .sliderWidget
//                    case is SpinnerParameterWidgetEStruct, is SpinnerParameterWidgetSStruct:
//                        widgetType = .commandWidget
//                    case is SwitchParameterWidgetEStruct, is SwitchParameterWidgetSStruct:
//                        widgetType = .commandWidget
//                    case is ThresholdParameterWidgetEStruct, is ThresholdParameterWidgetSStruct:
//                        widgetType = .commandWidget
////                    case is CommandParameterWidgetEStruct, is CommandParameterWidgetSStruct:
////                        widgetType = .commandWidget
//                    default:
//                        widgetType = .commandWidget
                    case let plotItem as PlotItem:
                        widgetType = .plotWidget
                        title = plotItem.title
                        let metadata = extractMetadata(from: plotItem.widget)
                        deviceAddress = metadata.deviceAddress
                        parameterID = metadata.parameterID
                    case let sliderItem as SliderItem:
                        widgetType = .sliderWidget
                        title = sliderItem.title
                        let metadata = extractMetadata(from: sliderItem.widget)
                        deviceAddress = metadata.deviceAddress
                        parameterID = metadata.parameterID
                    case let oneButtonItem as OneButtonItem:
                        widgetType = .commandWidget
                        title = oneButtonItem.title
                        if let descriptionValue = oneButtonItem.component2() as? String {
                            overview = descriptionValue
                        }
                        let metadata = extractMetadata(from: oneButtonItem.widget)
                        deviceAddress = metadata.deviceAddress
                        parameterID = metadata.parameterID
                    case is BaseParameterWidgetEStruct, is BaseParameterWidgetSStruct:
                        widgetType = .commandWidget
                        let metadata = extractMetadata(from: widget)
                        deviceAddress = metadata.deviceAddress
                        parameterID = metadata.parameterID
                    case is GestureOpticParameterWidgetEStruct:
                        widgetType = .commandWidget
                    case is GestureParameterWidgetEStruct:
                        widgetType = .commandWidget
                    case is OpticStartLearningWidgetEStruct, is OpticStartLearningWidgetSStruct:
                        widgetType = .commandWidget
                    case is PlotParameterWidgetEStruct, is PlotParameterWidgetSStruct:
                        widgetType = .plotWidget
                        let metadata = extractMetadata(from: widget)
                        deviceAddress = metadata.deviceAddress
                        parameterID = metadata.parameterID
                    case is SliderParameterWidgetEStruct, is SliderParameterWidgetSStruct:
                        widgetType = .sliderWidget
                        let metadata = extractMetadata(from: widget)
                        deviceAddress = metadata.deviceAddress
                        parameterID = metadata.parameterID
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
                
                
                if deviceAddress == nil || parameterID == nil {
                    let metadata = extractMetadata(from: widget)
                    if deviceAddress == nil { deviceAddress = metadata.deviceAddress }
                    if parameterID == nil { parameterID = metadata.parameterID }
                }
                
                return WidgetsResponseDTO.WidgetDTO(
                    id: index,
                    title: title,
                    widgetType: widgetType,
                    overview: overview,
                    releaseDate: nil,
                    isAd: false,
                    deviceAddress: deviceAddress,
                    parameterID: parameterID
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

    private func extractMetadata(from widget: Any?) -> (deviceAddress: Int?, parameterID: Int?) {
        guard let baseStruct = extractBaseStruct(from: widget) else {
            return (nil, nil)
        }

        var deviceAddress: Int? = Int(baseStruct.deviceId)
        var parameterID: Int?

        if let parameterInfo = firstParameterInfo(in: baseStruct.parameterInfoSet) {
            if let parameterIdValue = parameterInfo.value(forKey: "parameterID") as? KotlinInt {
                parameterID = Int(parameterIdValue.intValue)
            } else if let parameterIdNumber = parameterInfo.value(forKey: "parameterID") as? NSNumber {
                parameterID = parameterIdNumber.intValue
            }

            if let deviceAddressValue = parameterInfo.value(forKey: "deviceAddress") as? KotlinInt {
                deviceAddress = Int(deviceAddressValue.intValue)
            } else if let deviceAddressNumber = parameterInfo.value(forKey: "deviceAddress") as? NSNumber {
                deviceAddress = deviceAddressNumber.intValue
            }
        }

        return (deviceAddress, parameterID)
    }

    private func extractBaseStruct(from widget: Any?) -> BaseParameterWidgetStruct? {
        switch widget {
        case let baseStruct as BaseParameterWidgetStruct:
            return baseStruct
        case let baseEStruct as BaseParameterWidgetEStruct:
            return baseEStruct.baseParameterWidgetStruct
        case let baseSStruct as BaseParameterWidgetSStruct:
            return baseSStruct.baseParameterWidgetStruct
        case let commandEStruct as CommandParameterWidgetEStruct:
            return commandEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let commandSStruct as CommandParameterWidgetSStruct:
            return commandSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let plotEStruct as PlotParameterWidgetEStruct:
            return plotEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let plotSStruct as PlotParameterWidgetSStruct:
            return plotSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let sliderEStruct as SliderParameterWidgetEStruct:
            return sliderEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let sliderSStruct as SliderParameterWidgetSStruct:
            return sliderSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let spinnerEStruct as SpinnerParameterWidgetEStruct:
            return spinnerEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let spinnerSStruct as SpinnerParameterWidgetSStruct:
            return spinnerSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let switchEStruct as SwitchParameterWidgetEStruct:
            return switchEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let switchSStruct as SwitchParameterWidgetSStruct:
            return switchSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let opticStruct as OpticStartLearningWidgetEStruct:
            return opticStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let opticSStruct as OpticStartLearningWidgetSStruct:
            return opticSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        case let gestureOpticStruct as GestureOpticParameterWidgetEStruct:
            return gestureOpticStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let gestureStruct as GestureParameterWidgetEStruct:
            return gestureStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let thresholdStruct as ThresholdParameterWidgetEStruct:
            return thresholdStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct
        case let thresholdSStruct as ThresholdParameterWidgetSStruct:
            return thresholdSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct
        default:
            return nil
        }
    }

    private func firstParameterInfo(in set: Any?) -> NSObject? {
        guard let kotlinSet = set as? KotlinMutableSet else { return nil }
        let iterator = kotlinSet.iterator()
        while iterator.hasNext() {
            if let parameterInfo = iterator.next() as? NSObject { return parameterInfo }
        }
        return nil
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

