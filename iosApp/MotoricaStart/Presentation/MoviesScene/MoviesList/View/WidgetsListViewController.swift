import UIKit
//import shared

final class WidgetsListViewController: UIViewController, StoryboardInstantiable, Alertable {
    
    @IBOutlet private var contentView: UIView!
    @IBOutlet private var widgetsListContainer: UIView!
    @IBOutlet private(set) var suggestionsListContainer: UIView!
    @IBOutlet private var emptyDataLabel: UILabel!
    
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
        viewModel.viewDidLoad()
        
        let mockResponseDTO = WidgetsResponseDTO(
            page: 2,
            totalPages: 5,
            widgets: [
                WidgetsResponseDTO.WidgetDTO(
                    id: 3,
                    title: "Пример виджета 2",
                    genre: .adventure,
                    posterPath: "/path/to/poster.jpg",
                    overview: "Описание виджета 1...",
                    releaseDate: "2023-10-01"
                ),
                WidgetsResponseDTO.WidgetDTO(
                    id: 1,
                    title: "isAd: true 1",
                    genre: .adventure,
                    posterPath: "/path/to/poster.jpg",
                    overview: "Описание виджета 2...",
                    releaseDate: "2023-10-01",
                    isAd: true
                ),
                WidgetsResponseDTO.WidgetDTO(
                    id: 2,
                    title: "Пример виджета 3",
                    genre: .adventure,
                    posterPath: "/path/to/poster.jpg",
                    overview: "Описание виджета 3...",
                    releaseDate: "2023-10-01"
                ),
                WidgetsResponseDTO.WidgetDTO(
                    id: 40,
                    title: "Пример виджета 4",
                    genre: .adventure,
                    posterPath: "/path/to/poster.jpg",
                    overview: "Описание виджета 4...",
                    releaseDate: "2023-10-01",
                    isAd: true
                ),
            ]
        )

        let requestDTO = WidgetsRequestDTO(query: WidgetQuery(query: "My request").query, page: 1)
        storage.save(response: mockResponseDTO, for: requestDTO) { [weak self] in
            self?.viewModel.didSearch(query: "My request")             // ← чтение идёт уже из свежего кэша
        }
        
//        let rawBytes: [UInt8] = [0x40, 0xFF, 0x0A, 0x40, 0xFF, 0x0A, 0x40, 0xFF, 0x0A, 0x40, 0xFF, 0x0A]
//        bleManager.startScan { BleDevice in
//            print("МЫ НАШЛИ УСТРОЙСТВО \(BleDevice.name)!!!")
//        }
        
//        let kotlinByteArray = KotlinByteArray(size: Int32(rawBytes.count))
//        for (index, byte) in rawBytes.enumerated() {
//            kotlinByteArray.set(index: Int32(index), value: Int8(bitPattern: byte))
//        }
//        parser.parseData(data: kotlinByteArray)
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

