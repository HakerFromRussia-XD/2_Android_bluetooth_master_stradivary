import UIKit
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
//    init?(coder: NSCoder, viewModel: WidgetsListViewModel) {
//        self.viewModel = viewModel
//        super.init(coder: coder)
//    }
//    required init?(coder: NSCoder) { fatalError("Use init(coder:viewModel:)") }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupViews()
        setupBehaviours()
        bind(to: viewModel)
        viewModel.viewDidLoad()
        
        let dataFactory = DataFactory()
        // например, берём список виджетов для display = 1
//        let kotlinWidgets = dataFactory.prepareData(display: 1)
        let kotlinWidgets = dataFactory.fakeData()
//        print("[WIDGET_COORDINATOR] kotlinWidgets: \(kotlinWidgets)")
        
        // Преобразуем Kotlin-виджеты в DTO, помечая SliderItem как рекламу
        let widgetsDTO: [WidgetsResponseDTO.WidgetDTO] = (kotlinWidgets as? [Any])?
            .enumerated()
            .map { index, widget in
                var isAd = false
                var widgetType: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO?
                
                switch widget {
                case is BaseParameterWidgetEStruct:
                    widgetType = .commandWidget
                case is SliderParameterWidgetEStruct, is SliderParameterWidgetSStruct:
                    widgetType = .sliderWidget
                case is PlotParameterWidgetEStruct, is PlotParameterWidgetSStruct:
                    widgetType = .plotWidget
                default:
                    widgetType = .unknown
                }
                
                return WidgetsResponseDTO.WidgetDTO(
                    id: index,
                    title: "Widget \(index)",
                    widgetType: widgetType,
                    posterPath: nil,
                    overview: nil,
                    releaseDate: nil,
                    isAd: isAd
                )
            } ?? []
        print("[WIDGET_COORDINATOR] widgetsDTO: \(widgetsDTO)")
        
        let mockResponseDTO = WidgetsResponseDTO(
            page: 2,
            totalPages: 5,
            widgets: widgetsDTO
        )
        
        
        let requestDTO = WidgetsRequestDTO(query: WidgetQuery(query: "My request").query, page: 1)
        storage.save(response: mockResponseDTO, for: requestDTO) { [weak self] in
            self?.viewModel.didSearch(query: "My request")             // ← чтение идёт уже из свежего кэша
        }
        
        view.addSubview(bottomButton)
        NSLayoutConstraint.activate([
            bottomButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            bottomButton.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
        
        //        let rawBytes: [UInt8] = [0x40, 0xFF, 0x0A, 0x40, 0xFF, 0x0A, 0x40, 0xFF, 0x0A, 0x40, 0xFF, 0x0A]
        //        bleManager.startScan { BleDevice in
        //            print("МЫ НАШЛИ УСТРОЙСТВО \(BleDevice.name)!!!")
        //        }
        
        //        let kotlinByteArray = KotlinByteArray(size: Int32(rawBytes.count))
        //        for (index, byte) in rawBytes.enumerated() {
        //            kotlinByteArray.set(index: Int32(index), value: Int8(bitPattern: byte))
        //        }
        //        parser.parseData(data: kotlinByteArray)
        //            } as! [WidgetsResponseDTO.WidgetDTO]
    }


    private func bind(to viewModel: WidgetsListViewModel) {
        viewModel.items.observe(on: self) { [weak self] _ in self?.updateItems() }
        viewModel.loading.observe(on: self) { [weak self] in self?.updateLoading($0) }
        viewModel.error.observe(on: self) { [weak self] in self?.showError($0) }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == String(describing: WidgetsListTableViewController.self),
            let destinationVC = segue.destination as? WidgetsListTableViewController {
            widgetsTableViewController = destinationVC
            widgetsTableViewController?.viewModel = viewModel
            widgetsTableViewController?.posterImagesRepository = posterImagesRepository
        }
    }

    // MARK: - Private
    @objc private func bottomButtonTapped() {
        viewModel.sendBytes()
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
            widgetsListContainer.isHidden = viewModel.isEmpty
            emptyDataLabel.isHidden = !viewModel.isEmpty
        }

        widgetsTableViewController?.updateLoading(loading)
    }

    private func showError(_ error: String) {
        guard !error.isEmpty else { return }
        showAlert(title: viewModel.errorTitle, message: error)
    }
}

