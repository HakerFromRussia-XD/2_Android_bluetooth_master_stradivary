//
//  BluetoothListViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import UIKit
import Combine
import Foundation
import shared

final class BluetoothListViewController: UIViewController {
    private enum Constants {
        static let rootTransitionDuration: TimeInterval = 0.35
    }

    static let storyboardID = "BluetoothListViewController"
    
    private var didTriggerFakeConnection = false
    private var isTransitioningToMainTabBar = false
    private var isUserInteractingWithDevicesList = false
    private var pendingDevicesReloadAfterInteraction = false
    private var lastRenderedDeviceIDs: [UUID] = []
    
    lazy var segmentedConrol: CustomSegmentedControl = {
        let items = ["Все устройства", "Протезы"]
        let control = CustomSegmentedControl(items: items)
        return control
    }()
    private lazy var bottomButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Нажми меня", for: .normal)
        button.addTarget(self, action: #selector(bottomButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
//    var segmentedConrol = CustomSegmentedControl(titles: ["Коллекция жестов", "Группа ротации"])
    @IBOutlet private weak var containerView: UIView!
    @IBOutlet private weak var tableViewDevices: UITableView!
    @IBOutlet private weak var tableHeightConstraint: NSLayoutConstraint!
//    @IBOutlet private weak var tableBottomOfsetConstraint: NSLayoutConstraint!
//    private var devices = [BLEDevice]()
    
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
        
        // настройка внешнего вида фильтра списка устройств
        let segmentContainer = UIView()
        segmentContainer.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(segmentContainer)
        segmentContainer.layer.shadowColor = UIColor.black.cgColor
        segmentContainer.layer.shadowOpacity = 0.25
        segmentContainer.layer.shadowOffset = CGSize(width: 0, height: 2)
        segmentContainer.layer.shadowRadius = 3
        segmentContainer.layer.cornerRadius = 22
        segmentContainer.layer.masksToBounds = false
        segmentContainer.addSubview(segmentedConrol)
        segmentedConrol.translatesAutoresizingMaskIntoConstraints = true
        NSLayoutConstraint.activate([
           segmentContainer.heightAnchor.constraint(equalToConstant: 54),
           segmentContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
           segmentContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
           segmentContainer.topAnchor.constraint(equalTo: view.topAnchor, constant: 47)
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
        segmentedConrol.selectedSegmentIndex = viewModel.currentFilterIndex
        segmentedConrol.addTarget(self, action: #selector(filterChange), for: .valueChanged)

        let font = UIFont(name: "SFProDisplay-Light", size: 14)

        segmentedConrol.setTitleTextAttributes([
            .foregroundColor: UIColor(named: "ubi4_deactivate_text") ?? .white,
            .font: font ?? UIFont.systemFont(ofSize: 14, weight: .semibold)
        ], for: .normal)

        segmentedConrol.setTitleTextAttributes([
            .foregroundColor: UIColor(named: "ubi4_white") ?? .black,
            .font: font ?? UIFont.systemFont(ofSize: 14, weight: .semibold)
        ], for: .selected)
        
        
        // настройка внешнего вида списка
        let tableContainer = UIView()
        tableContainer.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableContainer)
        tableContainer.layer.shadowColor = UIColor.black.cgColor
        tableContainer.layer.shadowOpacity = 0.25
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
        tableViewDevices.delaysContentTouches = false
        tableViewDevices.rowHeight = 64
        tableViewDevices.estimatedRowHeight = 64
        tableViewDevices.accessibilityIdentifier = AccessibilityIdentifier.bleDevicesTable
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
        view.addSubview(bottomButton)
        NSLayoutConstraint.activate([
            bottomButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            bottomButton.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
        
        let titleTextAttributes: [NSAttributedString.Key: Any] = [.foregroundColor: UIColor(named: "ubi4_deactivate_text") ?? UIColor.white]
        UISegmentedControl.appearance().setTitleTextAttributes(titleTextAttributes, for: .normal)
        let titleTextAttributes2: [NSAttributedString.Key: Any] = [.foregroundColor: UIColor(named: "ubi4_white") ?? UIColor.black]
        UISegmentedControl.appearance().setTitleTextAttributes(titleTextAttributes2, for: .selected)
        
        viewModel.$connectedDeviceID
            .receive(on: DispatchQueue.main)
            .dropFirst()
            .sink { [weak self] uuid in
                print("[BLE-CONNECT] reloadData uuid: \(String(describing: uuid))")
                guard
                    let self = self,
                    !self.isTransitioningToMainTabBar,
                    let uuid = uuid,
                    let device = self.viewModel.devices.first(where: { $0.id == uuid })
                else { return }
                self.logTouch("connectionCallback", details: "uuid=\(uuid.uuidString)")
                self.tableViewDevices.reloadData() // перезагружаем строки, чтобы отобразить цвет подключения
                let displayName = DeviceNameBridgeV3.shared.displayName(deviceName: device.name)
                self.showConnectionToast("Подключено: \(displayName)")
                print("[BLE-CONNECT] Подключено: \(displayName)")
            }
            .store(in: &cancellables)
        
        viewModel.$devices
            .receive(on: DispatchQueue.main)
            .throttle(for: .milliseconds(200), scheduler: DispatchQueue.main, latest: true)  // обновление не чаще раза в 0.2s
            .sink { [weak self] devices in
                guard let self = self else { return }
                guard !self.isTransitioningToMainTabBar else { return }
                print("[BLE-VC] reload table with \(devices.count) items")
                if self.isTableInteractionInProgress {
                    self.logTouch("skipReload", details: "reason=interaction count=\(devices.count)")
                    self.pendingDevicesReloadAfterInteraction = true
                    return
                }
                
                let currentDeviceIDs = devices.map { $0.id }
                if currentDeviceIDs == self.lastRenderedDeviceIDs {
                    self.logTouch("skipReload", details: "reason=same-ids count=\(devices.count)")
                    self.refreshVisibleDeviceCells()
                    return
                }
                
                self.lastRenderedDeviceIDs = currentDeviceIDs
                self.pendingDevicesReloadAfterInteraction = false
                UIView.performWithoutAnimation {
                    self.tableViewDevices.reloadData()
                }
                self.updateTableHeight()
            }
            .store(in: &cancellables)
        viewModel.onAppear()
        
        

//        for family in UIFont.familyNames {
//            print("Family: \(family)")
//            for name in UIFont.fontNames(forFamilyName: family) {
//                print("Font: \(name)")
//            }
//        }
    }
    @objc private func filterChange(_ sender: UISegmentedControl) {
        // применяем фильтр при смене сегмента
        viewModel.applyFilter(index: sender.selectedSegmentIndex)
    }
    @objc private func bottomButtonTapped() {
        print("[BLE-CONNECT] Bottom button tapped")
        viewModel.sendBytes()
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
    
    //TODO: тут можно включать автоконнекшн (1)
    //комментим viewDidAppear если не нужен автоконнекшн к fakeData
//    override func viewDidAppear(_ animated: Bool) {
//        super.viewDidAppear(animated)
//        guard !didTriggerFakeConnection else { return }
//        didTriggerFakeConnection = true
//        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
//            self?.simulateAutoConnection()
//        }
//    }
    //комментим viewDidAppear если не нужен автоконнекшн к UBI4_Roman (недоделано)
//    override func viewDidAppear(_ animated: Bool) {
//        super.viewDidAppear(animated)
//        guard !didTriggerFakeConnection else { return }
//        didTriggerFakeConnection = true
//        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
//            self?.autoConnection()
//        }
//    }
    
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        viewModel.onDisappear()
    }
    
    
    // Функция обновления высоты таблицы
    private func updateTableHeight() {
        let targetHeight = max(tableViewDevices.contentSize.height, 64)
        guard abs(tableHeightConstraint.constant - targetHeight) > 0.5 else { return }
        tableHeightConstraint.constant = targetHeight
        view.layoutIfNeeded()
    }
    
    private var isTableInteractionInProgress: Bool {
        isUserInteractingWithDevicesList || tableViewDevices.isTracking || tableViewDevices.isDragging || tableViewDevices.isDecelerating
    }
    
    private func flushPendingDevicesReloadIfNeeded() {
        guard pendingDevicesReloadAfterInteraction else { return }
        pendingDevicesReloadAfterInteraction = false
        lastRenderedDeviceIDs = viewModel.devices.map(\.id)
        UIView.performWithoutAnimation {
            tableViewDevices.reloadData()
        }
        updateTableHeight()
        logTouch("flushPendingReload", details: "rows=\(tableViewDevices.numberOfRows(inSection: 0))")
    }
    
    private func refreshVisibleDeviceCells() {
        for case let cell as DeviceCell in tableViewDevices.visibleCells {
            guard let indexPath = tableViewDevices.indexPath(for: cell),
                  viewModel.devices.indices.contains(indexPath.row) else { continue }
            let device = viewModel.devices[indexPath.row]
            cell.setupModel(model: device)
            if let connectedID = viewModel.connectedDeviceID, connectedID == device.id {
                cell.backgroundColor = UIColor(named: "ubi4_active")
            } else {
                cell.backgroundColor = UIColor(named: "ubi4_gray")
            }
        }
    }
    
    private func logTouch(_ event: String, details: String = "") {
        let timestamp = String(format: "%.3f", Date().timeIntervalSince1970)
        let suffix = details.isEmpty ? "" : " \(details)"
        let message = "[BLE-TAP-TRACE] t=\(timestamp) event=\(event)\(suffix)"
        NSLog("%@", message)
        print(message)
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
        cell.accessibilityIdentifier = "ble.deviceCell.\(indexPath.row)"
        if let connectedID = viewModel.connectedDeviceID, connectedID == device.id {
            cell.backgroundColor = UIColor(named: "ubi4_active")
        } else {
            cell.backgroundColor = UIColor(named: "ubi4_gray")
        }
        return cell
    }
    
    func tableView(_ tableView: UITableView, shouldHighlightRowAt indexPath: IndexPath) -> Bool {
        isUserInteractingWithDevicesList = true
        logTouch("shouldHighlight", details: "row=\(indexPath.row) devices=\(viewModel.devices.count)")
        return true
    }
    
    func tableView(_ tableView: UITableView, didHighlightRowAt indexPath: IndexPath) {
        isUserInteractingWithDevicesList = true
        logTouch("didHighlight", details: "row=\(indexPath.row)")
    }
    
    func tableView(_ tableView: UITableView, didUnhighlightRowAt indexPath: IndexPath) {
        if !tableView.isTracking && !isTransitioningToMainTabBar {
            isUserInteractingWithDevicesList = false
            flushPendingDevicesReloadIfNeeded()
        }
        logTouch("didUnhighlight", details: "row=\(indexPath.row) tracking=\(tableView.isTracking)")
    }
    
    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        logTouch("willSelect", details: "row=\(indexPath.row)")
        return indexPath
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        guard !isTransitioningToMainTabBar else { return }
        isTransitioningToMainTabBar = true
        isUserInteractingWithDevicesList = false
        tableView.isUserInteractionEnabled = false
        tableView.allowsSelection = false
        logTouch("didSelect", details: "row=\(indexPath.row)")
        print("[BLE-CONNECT] selectDeviceToConnect indexPath: \(indexPath)")
        print("[BLE-TAP] 2 indexPath: \(indexPath)")
        tableView.deselectRow(at: indexPath, animated: true)
        handleDeviceSelection(at: indexPath)
    }
    
    private func handleDeviceSelection(at indexPath: IndexPath) {
        guard let selectedDevice = viewModel.device(at: indexPath.row) else {
            resetTransitionState()
            return
        }
        
        // Запрос на открытие WidgetsListViewController через координатор
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.logTouch(
                "connectStart",
                details: "row=\(indexPath.row) name=\(selectedDevice.name) uuid=\(selectedDevice.uuid.uuidString)"
            )
            UiStateBridge.shared.resetWidgetsState()
            WidgetsListViewController.resetGlobalSynchronizationState()
            self.viewModel.connect(to: selectedDevice)
            guard self.openMainTabBar() else { return }
        }
    }
    
    @discardableResult
    private func openMainTabBar() -> Bool {
        guard
            let appDelegate = UIApplication.shared.delegate as? AppDelegate,
            let navigationController = navigationController
        else {
            resetTransitionState()
            return false
        }
        let tabBarController = MainTabBarController(appDIContainer: appDelegate.appDIContainer)
        if let window = appDelegate.window {
            let transition = CATransition()
            transition.type = .push
            transition.subtype = .fromRight
            transition.duration = Constants.rootTransitionDuration
            transition.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
            window.layer.add(transition, forKey: kCATransition)
        }
        navigationController.setViewControllers([tabBarController], animated: false)
        return true
    }
    
    private func resetTransitionState() {
        isTransitioningToMainTabBar = false
        isUserInteractingWithDevicesList = false
        tableViewDevices.isUserInteractionEnabled = true
        tableViewDevices.allowsSelection = true
        flushPendingDevicesReloadIfNeeded()
    }
    
    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        isUserInteractingWithDevicesList = true
        logTouch("scrollBegin")
    }
    
    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate {
            isUserInteractingWithDevicesList = false
            flushPendingDevicesReloadIfNeeded()
        }
        logTouch("scrollEndDragging", details: "decelerate=\(decelerate)")
    }
    
    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        isUserInteractingWithDevicesList = false
        flushPendingDevicesReloadIfNeeded()
        logTouch("scrollEndDecelerating")
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
            imageDataTransferService: dataTransfer,
            bleManager: viewModel.bleManager
         )
    }
    private func simulateAutoConnection() {
        guard let index = viewModel.prepareFakeDeviceForTesting() else { return }
        tableViewDevices.reloadData()
        viewModel.connectToDevice(at: index)
        openMainTabBar()
    }
    private func autoConnection() {
//        guard let index = viewModel.prepareFakeDeviceForTesting() else { return }
//        tableViewDevices.reloadData()
//        viewModel.connectToDevice(at: index)
//        openMainTabBar()
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
    private let segmentInset: CGFloat = 3       //your inset amount
    private let segmentImage: UIImage? = UIImage(color: UIColor(named: "ubi4_back") ?? UIColor.white)
//    private let segmentImage: UIImage? = UIImage(color: UIColor.white)

    override func layoutSubviews(){
        super.layoutSubviews()

        //background
        layer.cornerRadius = 16
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
            foregroundImageView.layer.cornerRadius = 14
        }
    }
}
