//
//  BluetoothListViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import UIKit
import Combine
import Foundation

final class BluetoothListViewController: UIViewController {
    static let storyboardID = "BluetoothListViewController"
    
    lazy var segmentedConrol: CustomSegmentedControl = {
        let items = ["Все устройства", "Протезы"]
        let control = CustomSegmentedControl(items: items)
        return control
    }()
//    var segmentedConrol = CustomSegmentedControl(titles: ["Коллекция жестов", "Группа ротации"])
    @IBOutlet private weak var containerView: UIView!
    @IBOutlet private weak var tableViewDevices: UITableView!
    @IBOutlet private weak var tableHeightConstraint: NSLayoutConstraint!
//    @IBOutlet private weak var tableBottomOfsetConstraint: NSLayoutConstraint!
    private var devices = [BLEDevice]()
    
    private let viewModel: BluetoothListViewModel
    private var cancellables = Set<AnyCancellable>()

    // Инициализатор для UIStoryboard.instantiate
    init?(coder: NSCoder, viewModel: BluetoothListViewModel) {
        self.viewModel = viewModel
        super.init(coder: coder)
    }
    required init?(coder: NSCoder) { fatalError("Use init(coder:viewModel:)") }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "BLE Devices"
        // Скрываем навигационную панель (верхнюю строку)
        navigationController?.navigationBar.isHidden = true
        
        // настройка внешнего вида списка фильтра устройств
        let segmentContainer = UIView()
        segmentContainer.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(segmentContainer)
        segmentContainer.layer.shadowColor = UIColor.black.cgColor
        segmentContainer.layer.shadowOpacity = 0.24
        segmentContainer.layer.shadowOffset = CGSize(width: 0, height: 2)
        segmentContainer.layer.shadowRadius = 3
        segmentContainer.layer.cornerRadius = 22
        segmentContainer.layer.masksToBounds = false
        segmentContainer.addSubview(segmentedConrol)
        segmentedConrol.translatesAutoresizingMaskIntoConstraints = true
        NSLayoutConstraint.activate([
           segmentContainer.heightAnchor.constraint(equalToConstant: 60),
           segmentContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
           segmentContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
           segmentContainer.topAnchor.constraint(equalTo: view.topAnchor, constant: 80)
        ])
        segmentedConrol.translatesAutoresizingMaskIntoConstraints = false
        segmentedConrol.leadingAnchor.constraint(equalTo: segmentContainer.leadingAnchor).isActive = true
        segmentedConrol.trailingAnchor.constraint(equalTo: segmentContainer.trailingAnchor).isActive = true
        segmentedConrol.topAnchor.constraint(equalTo: segmentContainer.topAnchor).isActive = true
        segmentedConrol.bottomAnchor.constraint(equalTo: segmentContainer.bottomAnchor).isActive = true
        segmentedConrol.layer.cornerRadius = 20
        segmentedConrol.layer.masksToBounds = true
        // style
        segmentedConrol.layer.borderWidth = 1
        segmentedConrol.layer.borderColor = UIColor(named: "ubi4_filter_gray_border")?.cgColor
        segmentedConrol.backgroundColor = UIColor(named: "ubi4_filter_back")
        // применяем фильтр при загрузке контроллера
        segmentedConrol.selectedSegmentIndex = UserDefaults.standard.integer(forKey: "selectedFilterIndex")
        segmentedConrol.addTarget(self, action: #selector(filterChange), for: .valueChanged)
        
        
        // настройка внешнего вида списка
        let tableContainer = UIView()
        tableContainer.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableContainer)
        tableContainer.layer.shadowColor = UIColor.black.cgColor
        tableContainer.layer.shadowOpacity = 0.24
        tableContainer.layer.shadowOffset = CGSize(width: 0, height: 2)
        tableContainer.layer.shadowRadius = 3
        tableContainer.layer.cornerRadius = 20
        // добавляем таблицу в контейнер
        if tableViewDevices.superview == view {
            tableViewDevices.removeFromSuperview()
        }
        tableContainer.addSubview(tableViewDevices)
        tableViewDevices.translatesAutoresizingMaskIntoConstraints = false
        // скругление содержимого таблицы и обрезка ячеек
        tableViewDevices.layer.cornerRadius = 20
        tableViewDevices.layer.masksToBounds = true
        // Настройка таблицы
        // (dataSource, delegate и регистрация ячейки остаются прежними)
        tableViewDevices.dataSource = self
        tableViewDevices.delegate = self
        tableViewDevices.register(UINib(nibName: "DeviceCell", bundle: nil), forCellReuseIdentifier: DeviceCell.identifier)
        tableViewDevices.tableFooterView = UIView(frame: .zero)
        tableViewDevices.backgroundColor = UIColor(named: "ubi4_gray")
        tableViewDevices.layer.borderColor = UIColor(named: "ubi4_gray_border")?.cgColor
        tableViewDevices.layer.borderWidth = 1
        // Assistant: ограничения для контейнера и таблицы
        NSLayoutConstraint.activate([
            tableContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            tableContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            tableContainer.topAnchor.constraint(equalTo: segmentedConrol.bottomAnchor, constant: 16),
            tableContainer.heightAnchor.constraint(equalTo: tableViewDevices.heightAnchor),
            
            tableViewDevices.leadingAnchor.constraint(equalTo: tableContainer.leadingAnchor),
            tableViewDevices.trailingAnchor.constraint(equalTo: tableContainer.trailingAnchor),
            tableViewDevices.topAnchor.constraint(equalTo: tableContainer.topAnchor),
            tableViewDevices.bottomAnchor.constraint(equalTo: tableContainer.bottomAnchor)
        ])
        
        let titleTextAttributes: [NSAttributedString.Key: Any] = [.foregroundColor: UIColor(named: "ubi4_deactivate_text") ?? UIColor.white]
        UISegmentedControl.appearance().setTitleTextAttributes(titleTextAttributes, for: .normal)
        let titleTextAttributes2: [NSAttributedString.Key: Any] = [.foregroundColor: UIColor(named: "ubi4_white") ?? UIColor.black]
        UISegmentedControl.appearance().setTitleTextAttributes(titleTextAttributes2, for: .selected)
        
        viewModel.$connectedDeviceID
            .receive(on: DispatchQueue.main)
            .sink { [weak self] uuid in
                print("[BLE-CONNECT] reloadData")
                guard
                    let self = self,
                    let uuid = uuid,
                    let device = self.viewModel.devices.first(where: { $0.id == uuid })
                else { return }
                self.tableViewDevices.reloadData() // перезагружаем строки, чтобы отобразить цвет подключения
                self.showConnectionToast("Подключено: \(device.name)")
            }
            .store(in: &cancellables)
        
        viewModel.$devices
            .receive(on: DispatchQueue.main)
            .throttle(for: .milliseconds(200), scheduler: DispatchQueue.main, latest: true)  // обновление не чаще раза в 0.2s
            .sink { devices in
                print("[BLE-VC] reload table with \(devices.count) items")
                if self.tableViewDevices.isDragging || self.tableViewDevices.isDecelerating {
                    return
                }
                UIView.performWithoutAnimation {
                    self.tableViewDevices.reloadData()
                    self.updateTableHeight()
                }
            }
            .store(in: &cancellables)
        viewModel.onAppear()
        
        
//        for family in UIFont.familyNames {
//            print(family)
//            for font in UIFont.fontNames(forFamilyName: family) {
//                print("ШРИФТЫ:  \(font)")
//            }
//        }
    }
    @objc private func filterChange(_ sender: UISegmentedControl) {
        // применяем фильтр при смене сегмента
        viewModel.applyFilter(index: sender.selectedSegmentIndex)
    }
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        if let backgroundColor = UIColor(named: "ubi4_back") {
            containerView.backgroundColor = backgroundColor
        }
    }

    func updateConstraints() {
        tableHeightConstraint.constant = tableViewDevices.contentSize.height
        UIView.animate(withDuration: 0.33,
                       delay: 0.0,
                       options: .curveEaseIn,
                       animations: {
                            self.view.layoutIfNeeded()
                        },
                       completion: nil)
    }
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        viewModel.onDisappear()
    }
    
    // Функция обновления высоты таблицы
    private func updateTableHeight() {
        // Устанавливаем высоту таблицы на основе ее контента
        tableHeightConstraint.constant = tableViewDevices.contentSize.height
        
        // Если контент слишком мал, устанавливаем минимальную высоту
        if tableViewDevices.contentSize.height < 64 {
            tableHeightConstraint.constant = 64
        }
        
        // Анимация для обновления UI
        UIView.animate(withDuration: 0.33, delay: 0.0, options: .curveEaseIn, animations: {
            self.view.layoutIfNeeded()
        }, completion: nil)
    }
}
// MARK: - UITableViewDataSource
extension BluetoothListViewController: UITableViewDataSource, UITableViewDelegate {
    private func showConnectionToast(_ message: String) {
            let toast = UILabel()
            toast.text = message
            toast.textAlignment = .center
            toast.font = UIFont.systemFont(ofSize: 14)
            toast.textColor = .white
            toast.backgroundColor = UIColor.black.withAlphaComponent(0.7)
            toast.layer.cornerRadius = 10
            toast.clipsToBounds = true

            // вычисляем ширину и позицию
            let padding: CGFloat = 16
            let maxWidth = view.bounds.width - padding * 2
            let textWidth = toast.intrinsicContentSize.width + padding
            let width = min(maxWidth, textWidth)
            toast.frame = CGRect(
                x: (view.bounds.width - width) / 2,
                y: view.safeAreaInsets.top + 16,
                width: width,
                height: 40
            )

            view.addSubview(toast)
            toast.alpha = 0

            // анимация появления и исчезновения
            UIView.animate(withDuration: 0.3, animations: {
                toast.alpha = 1
            }) { _ in
                UIView.animate(withDuration: 0.3, delay: 2.0, options: [], animations: {
                    toast.alpha = 0
                }) { _ in
                    toast.removeFromSuperview()
                }
            }
        }
    func numberOfSections(in tableView: UITableView) -> Int { return 1 }
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { viewModel.devices.count }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: DeviceCell.identifier, for: indexPath) as! DeviceCell
        let device = viewModel.devices[indexPath.row]
        cell.setupModel(model: device) // Настройка данных в ячейке
        if let connectedID = viewModel.connectedDeviceID, connectedID == device.id {
            cell.backgroundColor = UIColor(named: "ubi4_active")
        } else {
            cell.backgroundColor = UIColor(named: "ubi4_gray")
        }
        return cell
    }
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        print("[BLE-CONNECT] selectDeviceToConnect indexPath: \(indexPath)")
        viewModel.connectToDevice(at: indexPath.row)
        tableViewDevices.reloadRows(at: [indexPath], with: .none)
        
        // Запрос на открытие WidgetsListViewController через координатор
        openWidgetsList()
    }
    private func openWidgetsList() {
        // 1. DI‑контейнер Widgets‑сцены
        let widgetsDI = WidgetsSceneDIContainer(
            dependencies: makeWidgetsDependencies()
        )

        // 2. Минимальный набор действий (допустимы заглушки)
        let actions = WidgetsListViewModelActions(
            showWidgetDetails: { _ in },
            showWidgetQueriesSuggestions: { _ in },
            closeWidgetQueriesSuggestions: { }
        )
        
        let widgetsVC = widgetsDI.makeWidgetsListViewController(actions: actions)
        navigationController?.pushViewController(widgetsVC, animated: true)
    }
    private func makeWidgetsDependencies() -> WidgetsSceneDIContainer.Dependencies {                   // Assistant
        // -------- единый сетевой слой (можно разделить при желании) -------
        struct InlineConfig: NetworkConfigurable {
            let baseURL: URL
            let headers: [String:String]
            let queryParameters: [String:String] = [:]
        }
        let cfg = InlineConfig(
            baseURL: URL(string: "https://api.motorica.org/v1")!,
            headers: [:]
        )
        
        let network          = DefaultNetworkService(config: cfg)
        let dataTransfer     = DefaultDataTransferService(with: network)
        
        // ---------- возвращаем Dependencies контейнеру WidgetsScene -----------
         return WidgetsSceneDIContainer.Dependencies(
            apiDataTransferService:   dataTransfer,
            imageDataTransferService: dataTransfer
         )
    }
}
extension String {
    /// A data representation of the hexadecimal bytes in this string.
    func hexDecodedData() -> Data {
        // Get the UTF8 characters of this string
        let chars = Array(utf8)
        
        // Keep the bytes in an UInt8 array and later convert it to Data
        var bytes = [UInt8]()
        bytes.reserveCapacity(count / 2)
        
        // It is a lot faster to use a lookup map instead of strtoul
        let map: [UInt8] = [
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, // 01234567
            0x08, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 89:;<=>?
            0x00, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x00, // @ABCDEFG
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  // HIJKLMNO
        ]
        
        // Grab two characters at a time, map them and turn it into a byte
        for i in stride(from: 0, to: count, by: 2) {
            let index1 = Int(chars[i] & 0x1F ^ 0x10)
            let index2 = Int(chars[i + 1] & 0x1F ^ 0x10)
            bytes.append(map[index1] << 4 | map[index2])
        }
        
        return Data(bytes)
    }
    func parseToInt() -> Int? {
        return Int(self.components(separatedBy: CharacterSet.decimalDigits.inverted).joined())
     }
    var boolValue: Bool {
        return (self as NSString).boolValue
    }
}
extension UIImage{
    //creates a UIImage given a UIColor
    public convenience init?(color: UIColor, size: CGSize = CGSize(width: 1, height: 1)) {
        let rect = CGRect(origin: .zero, size: size)
        UIGraphicsBeginImageContextWithOptions(rect.size, false, 0.0)
        color.setFill()
        UIRectFill(rect)
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
    
        guard let cgImage = image?.cgImage else { return nil }
        self.init(cgImage: cgImage)
    }
}
private struct InlineNetworkConfig: NetworkConfigurable {
    let baseURL: URL
    let headers: [String : String]
    let queryParameters: [String : String] = [:]
}

class CustomSegmentedControl: UISegmentedControl{
    private let segmentInset: CGFloat = 5       //your inset amount
    private let segmentImage: UIImage? = UIImage(color: UIColor(named: "ubi4_back") ?? UIColor.white)
//    private let segmentImage: UIImage? = UIImage(color: UIColor.white)

    override func layoutSubviews(){
        super.layoutSubviews()

        //background
        layer.cornerRadius = 20
        //foreground
        let foregroundIndex = numberOfSegments
        if subviews.indices.contains(foregroundIndex), let foregroundImageView = subviews[foregroundIndex] as? UIImageView
        {
            foregroundImageView.bounds = foregroundImageView.bounds.insetBy(dx: segmentInset, dy: segmentInset)
            foregroundImageView.image = segmentImage    //substitute with our own colored image
            foregroundImageView.layer.removeAnimation(forKey: "SelectionBounds")
            foregroundImageView.layer.masksToBounds = true
            foregroundImageView.layer.borderWidth = 1
            foregroundImageView.layer.borderColor = UIColor(named: "ubi4_filter_gray_border")?.cgColor
            foregroundImageView.layer.cornerRadius = 18
        }
    }
}
