//
//  BluetoothListViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import UIKit
import Combine
import Foundation
import SwiftUI
import shared

final class BluetoothListViewController: UIViewController {
    private enum Constants {
        static let rootTransitionDuration: TimeInterval = 0.35
        static let tableBottomInset: CGFloat = 16
    }

    static let storyboardID = "BluetoothListViewController"
    
    private var didTriggerFakeConnection = false
    private var isTransitioningToMainTabBar = false
    private var isUserInteractingWithDevicesList = false
    private var pendingDevicesReloadAfterInteraction = false
    private var lastRenderedDeviceIDs: [UUID] = []
    private var backgroundGradientLayer: CAGradientLayer?
    private weak var tableContainerView: UIView?
    private weak var legacySegmentedControl: UISegmentedControl?
    private let statusBarBackgroundView = UIView()
    
    private enum FilterIndex {
        static let allDevices = 0
        static let prosthetics = 1
    }

    private enum LegacySegmentIndex {
        static let prosthetics = 0
        static let allDevices = 1
    }

    private let filterSegmentTitles = ["Все устройства", "Протезы"]
    private let filterSegmentProvider = BluetoothFilterSegmentProvider(selectedSegmentIndex: 0)
    private lazy var bottomButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Нажми меня", for: .normal)
        button.addTarget(self, action: #selector(bottomButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    @IBOutlet private weak var containerView: UIView!
    @IBOutlet private weak var tableViewDevices: UITableView!
    @IBOutlet private weak var tableHeightConstraint: NSLayoutConstraint!
//    @IBOutlet private weak var tableBottomOfsetConstraint: NSLayoutConstraint!
//    private var devices = [BLEDevice]()
    
    private let viewModel: BluetoothListViewModel
    private var cancellables = Set<AnyCancellable>()
    private var scanAppearanceMode: MergedScanAppearanceMode { viewModel.initialScanAppearanceMode }

    // Инициализатор для UIStoryboard.instantiate
    init?(coder: NSCoder, viewModel: BluetoothListViewModel) {
        self.viewModel = viewModel
        super.init(coder: coder)
    }
    required init?(coder: NSCoder) { fatalError("Use init(coder:viewModel:)") }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
    }

    override var childForStatusBarStyle: UIViewController? {
        nil
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "BLE Devices"
        // Скрываем навигационную панель (верхнюю строку)
        navigationController?.navigationBar.isHidden = true
        setupStatusBarBackgroundView()

        let modernSegmentContainer = setupFilterControl()

        // настройка внешнего вида списка
        let tableContainer = UIView()
        tableContainerView = tableContainer
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
        registerDeviceCell()
        tableViewDevices.tableFooterView = UIView(frame: .zero)
        applyStaticAppearance(tableContainer: tableContainer)
        tableViewDevices.delaysContentTouches = false
        applyTableMetrics()
        tableViewDevices.accessibilityIdentifier = AccessibilityIdentifier.bleDevicesTable
        let tableTopConstraint: NSLayoutConstraint
        switch scanAppearanceMode {
        case .legacyBlue:
            tableTopConstraint = tableContainer.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 132)
        case .modernGray:
            guard let modernSegmentContainer else {
                assertionFailure("Modern scan layout requires segment container")
                tableTopConstraint = tableContainer.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 72)
                break
            }
            tableTopConstraint = tableContainer.topAnchor.constraint(equalTo: modernSegmentContainer.bottomAnchor, constant: 16)
        }
        // Assistant: ограничения для контейнера и таблицы
        NSLayoutConstraint.activate([
            tableContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            tableContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            tableTopConstraint,
            tableContainer.heightAnchor.constraint(equalTo: tableViewDevices.heightAnchor),
            
            tableViewDevices.leadingAnchor.constraint(equalTo: tableContainer.leadingAnchor),
            tableViewDevices.trailingAnchor.constraint(equalTo: tableContainer.trailingAnchor),
            tableViewDevices.topAnchor.constraint(equalTo: tableContainer.topAnchor),
            tableViewDevices.bottomAnchor.constraint(equalTo: tableContainer.bottomAnchor)
        ])
        // Кнопка отладочного действия не должна отображаться на экране сканирования.
        
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
                let currentDeviceIDs = devices.map { $0.id }

                if self.isTableInteractionInProgress {
                    self.logTouch("skipReload", details: "reason=interaction count=\(devices.count)")
                    self.pendingDevicesReloadAfterInteraction = true
                    return
                }

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
                self.updateTableHeight(animated: true)
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
    @objc private func bottomButtonTapped() {
        print("[BLE-CONNECT] Bottom button tapped")
        viewModel.sendBytes()
    }
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        switch scanAppearanceMode {
        case .legacyBlue:
            applyLegacyBlueBackground()
        case .modernGray:
            backgroundGradientLayer?.removeFromSuperlayer()
            backgroundGradientLayer = nil
            if let backgroundColor = UIColor(named: "ubi4_back") {
                containerView.backgroundColor = backgroundColor
            }
        }
        applyStatusBarBackgroundColor()
        view.bringSubviewToFront(statusBarBackgroundView)
        updateTableHeight()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        applyStatusBarBackgroundColor()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        applyStatusBarBackgroundColor()
    }

    private func setupStatusBarBackgroundView() {
        statusBarBackgroundView.translatesAutoresizingMaskIntoConstraints = false
        statusBarBackgroundView.isUserInteractionEnabled = false
        view.addSubview(statusBarBackgroundView)
        NSLayoutConstraint.activate([
            statusBarBackgroundView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            statusBarBackgroundView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            statusBarBackgroundView.topAnchor.constraint(equalTo: view.topAnchor),
            statusBarBackgroundView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor)
        ])
        applyStatusBarBackgroundColor()
    }

    private func applyStatusBarBackgroundColor() {
        let backgroundColor = statusBarBackgroundColor
        statusBarBackgroundView.backgroundColor = backgroundColor
        (UIApplication.shared.delegate as? AppDelegate)?.updateStatusBarOverlay(backgroundColor: backgroundColor)
    }

    private var statusBarBackgroundColor: UIColor {
        switch scanAppearanceMode {
        case .legacyBlue:
            return legacyTopColor
        case .modernGray:
            return UIColor(named: "ubi4_back") ?? .black
        }
    }

    private func setupFilterControl() -> UIView? {
        switch scanAppearanceMode {
        case .legacyBlue:
            setupLegacyFilterControl()
            return nil
        case .modernGray:
            return setupModernFilterControl()
        }
    }

    private func setupLegacyFilterControl() {
        let items = [
            NSLocalizedString("prosthetics", comment: ""),
            NSLocalizedString("all_devices", comment: "")
        ]
        let control = LegacyScanSegmentedControl(items: items)
        legacySegmentedControl = control
        control.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(control)
        NSLayoutConstraint.activate([
            control.heightAnchor.constraint(equalToConstant: 60),
            control.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            control.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            control.topAnchor.constraint(equalTo: view.topAnchor, constant: 80)
        ])

        control.selectedSegmentIndex = legacySegmentIndex(for: viewModel.currentFilterIndex)
        control.addTarget(self, action: #selector(legacyFilterChanged), for: .valueChanged)
        control.layer.borderWidth = 2
        control.layer.borderColor = UIColor(named: "backgroung_filter")?.cgColor
        control.backgroundColor = UIColor(named: "white")
        control.setTitleTextAttributes(
            [.foregroundColor: UIColor(named: "deselected_text_filter") ?? UIColor.darkGray],
            for: .normal
        )
        control.setTitleTextAttributes([.foregroundColor: UIColor.black], for: .selected)
    }

    private func setupModernFilterControl() -> UIView {
        let segmentContainer = UIView()
        segmentContainer.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(segmentContainer)
        segmentContainer.backgroundColor = UIColor.clear
        NSLayoutConstraint.activate([
            segmentContainer.heightAnchor.constraint(equalToConstant: 48),
            segmentContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            segmentContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            segmentContainer.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 8)
        ])
        let safeInitialIndex = min(max(viewModel.currentFilterIndex, 0), filterSegmentTitles.count - 1)
        filterSegmentProvider.selectedSegmentIndex = safeInitialIndex
        let segmentSelectorHost = UIHostingController(
            rootView: BluetoothSegmentSelectorView(
                provider: filterSegmentProvider,
                titles: filterSegmentTitles,
                appearanceMode: scanAppearanceMode
            )
        )
        addChild(segmentSelectorHost)
        segmentContainer.addSubview(segmentSelectorHost.view)
        segmentSelectorHost.view.backgroundColor = UIColor.clear
        segmentSelectorHost.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            segmentSelectorHost.view.leadingAnchor.constraint(equalTo: segmentContainer.leadingAnchor),
            segmentSelectorHost.view.trailingAnchor.constraint(equalTo: segmentContainer.trailingAnchor),
            segmentSelectorHost.view.topAnchor.constraint(equalTo: segmentContainer.topAnchor),
            segmentSelectorHost.view.bottomAnchor.constraint(equalTo: segmentContainer.bottomAnchor)
        ])
        segmentSelectorHost.didMove(toParent: self)
        filterSegmentProvider.$selectedSegmentIndex
            .removeDuplicates()
            .dropFirst()
            .sink { [weak self] selectedIndex in
                self?.viewModel.applyFilter(index: selectedIndex)
            }
            .store(in: &cancellables)
        return segmentContainer
    }

    @objc private func legacyFilterChanged(target: UISegmentedControl) {
        let filterIndex = target.selectedSegmentIndex == LegacySegmentIndex.prosthetics
            ? FilterIndex.prosthetics
            : FilterIndex.allDevices
        viewModel.applyFilter(index: filterIndex)
    }

    private func legacySegmentIndex(for filterIndex: Int) -> Int {
        filterIndex == FilterIndex.prosthetics ? LegacySegmentIndex.prosthetics : LegacySegmentIndex.allDevices
    }

    private func registerDeviceCell() {
        switch scanAppearanceMode {
        case .legacyBlue:
            tableViewDevices.register(LegacyScanDeviceCell.self, forCellReuseIdentifier: LegacyScanDeviceCell.identifier)
        case .modernGray:
            tableViewDevices.register(UINib(nibName: "DeviceCell", bundle: nil), forCellReuseIdentifier: DeviceCell.identifier)
        }
    }

    private func applyTableMetrics() {
        switch scanAppearanceMode {
        case .legacyBlue:
            tableViewDevices.rowHeight = 64
            tableViewDevices.estimatedRowHeight = 64
            tableViewDevices.separatorInset = .zero
        case .modernGray:
            tableViewDevices.rowHeight = 64
            tableViewDevices.estimatedRowHeight = 64
        }
    }

    func updateConstraints() {
        updateTableHeight(animated: true)
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
    private func updateTableHeight(animated: Bool = false) {
        let minHeight: CGFloat = 64
        let shouldKeepVisibleEmptyRow: Bool
        switch scanAppearanceMode {
        case .legacyBlue:
            shouldKeepVisibleEmptyRow = viewModel.currentFilterIndex == FilterIndex.allDevices || !viewModel.devices.isEmpty
        case .modernGray:
            shouldKeepVisibleEmptyRow = true
        }
        let minimumHeight = shouldKeepVisibleEmptyRow ? minHeight : 0
        let maxAllowedHeight = availableTableHeight(minimumHeight: minimumHeight)
        let contentHeight = max(tableViewDevices.contentSize.height, minimumHeight)
        let targetHeight = min(contentHeight, maxAllowedHeight)

        guard abs(tableHeightConstraint.constant - targetHeight) > 0.5 else { return }
        tableHeightConstraint.constant = targetHeight
        tableViewDevices.isScrollEnabled = tableViewDevices.contentSize.height > targetHeight + 0.5
        if animated {
            UIView.animate(
                withDuration: 0.3,
                delay: 0,
                options: [.curveEaseInOut, .beginFromCurrentState, .allowUserInteraction]
            ) {
                self.view.layoutIfNeeded()
            }
        } else {
            view.layoutIfNeeded()
        }
    }

    private func availableTableHeight(minimumHeight: CGFloat) -> CGFloat {
        guard view.bounds.height > 0 else { return max(tableHeightConstraint.constant, minimumHeight) }

        let tableTopY: CGFloat
        if let tableContainerView, tableContainerView.superview != nil {
            tableTopY = tableContainerView.frame.minY
        } else {
            tableTopY = tableViewDevices.convert(.zero, to: view).y
        }

        let bottomLimitY = view.bounds.height - view.safeAreaInsets.bottom - Constants.tableBottomInset
        return max(minimumHeight, bottomLimitY - tableTopY)
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
        updateTableHeight(animated: true)
        logTouch("flushPendingReload", details: "rows=\(tableViewDevices.numberOfRows(inSection: 0))")
    }
    
    private func refreshVisibleDeviceCells() {
        for cell in tableViewDevices.visibleCells {
            guard let indexPath = tableViewDevices.indexPath(for: cell),
                  viewModel.devices.indices.contains(indexPath.row) else { continue }
            let device = viewModel.devices[indexPath.row]
            if let legacyCell = cell as? LegacyScanDeviceCell {
                legacyCell.setupModel(model: device)
            } else if let modernCell = cell as? DeviceCell {
                modernCell.setupModel(model: device)
                if let connectedID = viewModel.connectedDeviceID, connectedID == device.id {
                    modernCell.backgroundColor = UIColor(named: "ubi4_active")
                } else {
                    modernCell.backgroundColor = defaultDeviceCellBackgroundColor
                }
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
        let device = viewModel.devices[indexPath.row]
        let cell: UITableViewCell
        switch scanAppearanceMode {
        case .legacyBlue:
            let legacyCell = tableView.dequeueReusableCell(
                withIdentifier: LegacyScanDeviceCell.identifier,
                for: indexPath
            ) as! LegacyScanDeviceCell
            legacyCell.setupModel(model: device)
            cell = legacyCell
        case .modernGray:
            let modernCell = tableView.dequeueReusableCell(withIdentifier: DeviceCell.identifier, for: indexPath) as! DeviceCell
            modernCell.setupModel(model: device) // Настройка данных в ячейке
            if let connectedID = viewModel.connectedDeviceID, connectedID == device.id {
                modernCell.backgroundColor = UIColor(named: "ubi4_active")
            } else {
                modernCell.backgroundColor = defaultDeviceCellBackgroundColor
            }
            cell = modernCell
        }
        cell.accessibilityIdentifier = "ble.deviceCell.\(indexPath.row)"
        cell.isAccessibilityElement = true
        cell.accessibilityLabel = DeviceNameBridgeV3.shared.displayName(deviceName: device.name)
        cell.accessibilityValue = "rssi=\(device.rssi);uuid=\(device.uuid.uuidString)"
        cell.accessibilityTraits.insert(.button)
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
            switch self.viewModel.family(for: selectedDevice) {
            case .newKmm:
                self.viewModel.connect(to: selectedDevice)
                guard self.openMainTabBar() else { return }
            case .oldLegacy:
                self.openLegacyBranch(for: selectedDevice)
            case .unknown:
                self.showConnectionToast("Неизвестное устройство")
                self.resetTransitionState()
            }
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
        if let statusBarColor = UIColor(named: "ubi4_back") {
            appDelegate.updateStatusBarOverlay(backgroundColor: statusBarColor)
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

    private func openLegacyBranch(for device: BLEDevice) {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate,
              let window = appDelegate.window else {
            resetTransitionState()
            return
        }

        viewModel.markOldSelection(device: device)
        viewModel.onDisappear()
        let legacyRoot = OldMotoricaStartLauncherAdapter().makeRootViewController(for: device)

        let transition = CATransition()
        transition.type = .push
        transition.subtype = .fromRight
        transition.duration = Constants.rootTransitionDuration
        transition.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
        window.layer.add(transition, forKey: kCATransition)
        window.rootViewController = legacyRoot
        window.makeKeyAndVisible()
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

    private var defaultDeviceCellBackgroundColor: UIColor {
        switch scanAppearanceMode {
        case .legacyBlue:
            return .white
        case .modernGray:
            return UIColor(named: "ubi4_gray") ?? .white
        }
    }

    private func applyStaticAppearance(tableContainer: UIView) {
        switch scanAppearanceMode {
        case .legacyBlue:
            tableViewDevices.backgroundColor = .white
            tableViewDevices.tintColor = .clear
            tableViewDevices.layer.borderWidth = 0
            tableViewDevices.separatorInset = .zero
            tableViewDevices.sectionIndexTrackingBackgroundColor = .clear
            tableContainer.layer.shadowOpacity = 0
        case .modernGray:
            tableViewDevices.backgroundColor = UIColor(named: "ubi4_gray")
            tableViewDevices.layer.borderColor = UIColor(named: "ubi4_gray_border")?.cgColor
            tableViewDevices.layer.borderWidth = 1
            tableContainer.layer.shadowOpacity = 0.25
        }
    }

    private var legacyTopColor: UIColor {
        UIColor(red: 0, green: 0.4745098039, blue: 0.568627451, alpha: 1)
    }

    private var legacyBottomColor: UIColor {
        UIColor(red: 0.2823529412, green: 0.6941176471, blue: 0.7490196078, alpha: 1)
    }

    private func applyLegacyBlueBackground() {
        containerView.backgroundColor = legacyTopColor
        let gradientLayer = backgroundGradientLayer ?? CAGradientLayer()
        gradientLayer.colors = [legacyTopColor.cgColor, legacyBottomColor.cgColor]
        gradientLayer.startPoint = CGPoint(x: 0.5, y: 0)
        gradientLayer.endPoint = CGPoint(x: 0.5, y: 1)
        gradientLayer.frame = containerView.bounds

        if backgroundGradientLayer == nil {
            containerView.layer.insertSublayer(gradientLayer, at: 0)
            backgroundGradientLayer = gradientLayer
        }
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

private final class LegacyScanDeviceCell: UITableViewCell {
    static let identifier = "LegacyScanDeviceCell"

    private let whiteContainerView = UIView()
    private let deviceNameText = UILabel()
    private let rssi = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupView()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    func setupModel(model: BLEDevice) {
        deviceNameText.text = LegacyScanDeviceNameFormatter.cleanName(deviceName: model.name)
        rssi.text = String(model.rssi)
    }

    private func setupView() {
        backgroundColor = .white
        contentView.backgroundColor = .white
        clipsToBounds = true
        preservesSuperviewLayoutMargins = true
        indentationWidth = 10

        whiteContainerView.translatesAutoresizingMaskIntoConstraints = false
        whiteContainerView.backgroundColor = UIColor(named: "white") ?? .white
        whiteContainerView.tintColor = .clear
        whiteContainerView.isUserInteractionEnabled = false
        contentView.addSubview(whiteContainerView)

        deviceNameText.translatesAutoresizingMaskIntoConstraints = false
        deviceNameText.textAlignment = .center
        deviceNameText.lineBreakMode = .byTruncatingTail
        deviceNameText.baselineAdjustment = .alignBaselines
        deviceNameText.adjustsFontSizeToFitWidth = false
        deviceNameText.font = UIFont(name: "OpenSans-Bold", size: 15)
            ?? UIFont(name: "OpenSansRoman-SemiBold", size: 15)
            ?? .boldSystemFont(ofSize: 15)
        deviceNameText.textColor = .black
        deviceNameText.backgroundColor = .clear
        deviceNameText.tintColor = .clear
        whiteContainerView.addSubview(deviceNameText)

        rssi.translatesAutoresizingMaskIntoConstraints = false
        rssi.textAlignment = .natural
        rssi.lineBreakMode = .byTruncatingTail
        rssi.baselineAdjustment = .alignBaselines
        rssi.font = UIFont(name: "OpenSans-Bold", size: 15)
            ?? UIFont(name: "OpenSansRoman-SemiBold", size: 15)
            ?? .boldSystemFont(ofSize: 15)
        rssi.textColor = UIColor(red: 0.7137254902, green: 0.7137254902, blue: 0.7137254902, alpha: 1)
        rssi.highlightedTextColor = UIColor(red: 0.4745131135, green: 0.4745037556, blue: 0.4745101333, alpha: 1)
        whiteContainerView.addSubview(rssi)

        let containerHeight = whiteContainerView.heightAnchor.constraint(equalToConstant: 64)
        containerHeight.priority = .defaultHigh

        NSLayoutConstraint.activate([
            whiteContainerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            whiteContainerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            whiteContainerView.topAnchor.constraint(equalTo: contentView.topAnchor),
            whiteContainerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
            containerHeight,

            deviceNameText.leadingAnchor.constraint(equalTo: whiteContainerView.leadingAnchor),
            deviceNameText.trailingAnchor.constraint(equalTo: whiteContainerView.trailingAnchor),
            deviceNameText.centerXAnchor.constraint(equalTo: whiteContainerView.centerXAnchor),
            deviceNameText.centerYAnchor.constraint(equalTo: whiteContainerView.centerYAnchor, constant: -1.5),

            rssi.trailingAnchor.constraint(equalTo: whiteContainerView.trailingAnchor, constant: -16),
            rssi.centerYAnchor.constraint(equalTo: whiteContainerView.centerYAnchor)
        ])
    }
}

private enum LegacyScanDeviceNameFormatter {
    static func cleanName(deviceName: String) -> String {
        guard deviceName.contains("FEST-X"), deviceName.count > 10, !deviceName.contains(" ") else {
            return deviceName
        }

        let prefixStart = deviceName.index(deviceName.startIndex, offsetBy: 6)
        let prefixEnd = deviceName.index(prefixStart, offsetBy: 4)
        let namePrefix = String(deviceName[prefixStart..<prefixEnd])
        let nameCode = String(deviceName[prefixEnd..<deviceName.endIndex])

        switch namePrefix {
        case "FTFS":
            return "FEST-F-\(nameCode)"
        case "FTHS":
            return "FEST-H-\(nameCode)"
        case "FTFO":
            return "FEST-FO-\(nameCode)"
        case "FTHO":
            return "FEST-HO-\(nameCode)"
        case "FTEP":
            return "FEST-EP-\(nameCode)"
        case "FTEB":
            return "FEST-EB-\(nameCode)"
        default:
            return deviceName
        }
    }
}

private final class LegacyScanSegmentedControl: UISegmentedControl {
    private let segmentInset: CGFloat = 5
    private let segmentImage: UIImage? = UIImage(color: UIColor.white)

    override func layoutSubviews() {
        super.layoutSubviews()

        layer.cornerRadius = bounds.height / 2
        let foregroundIndex = numberOfSegments
        if subviews.indices.contains(foregroundIndex),
           let foregroundImageView = subviews[foregroundIndex] as? UIImageView {
            foregroundImageView.bounds = foregroundImageView.bounds.insetBy(dx: segmentInset, dy: segmentInset)
            foregroundImageView.image = segmentImage
            foregroundImageView.layer.removeAnimation(forKey: "SelectionBounds")
            foregroundImageView.layer.masksToBounds = true
            foregroundImageView.layer.cornerRadius = foregroundImageView.bounds.height / 2
        }
    }
}

private final class BluetoothFilterSegmentProvider: ObservableObject {
    @Published var selectedSegmentIndex: Int

    init(selectedSegmentIndex: Int) {
        self.selectedSegmentIndex = selectedSegmentIndex
    }
}

private struct BluetoothSegmentSelectorView: View {
    @ObservedObject var provider: BluetoothFilterSegmentProvider
    let titles: [String]
    let appearanceMode: MergedScanAppearanceMode

    @State private var selectorDisplayOffset: CGFloat = 0
    @State private var isSelectorOffsetInitialized = false
    @State private var selectorAnimationTimer: Timer?

    private var animationDuration: Double { 0.3 }

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let segmentCount = CGFloat(max(titles.count, 1))
            let segmentWidth = width / segmentCount

            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(palette.background)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(palette.border, lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)

                RoundedRectangle(cornerRadius: 10)
                    .fill(palette.selectedBackground)
                    .padding(1)
                    .frame(width: segmentWidth)
                    .offset(x: selectorDisplayOffset)

                HStack(spacing: 0) {
                    ForEach(Array(titles.enumerated()), id: \.offset) { index, title in
                        Button(action: { select(index: index) }) {
                            Text(title)
                                .font(.system(size: 12, weight: .light))
                                .foregroundColor(index == provider.selectedSegmentIndex ? palette.selectedText : palette.normalText)
                                .animation(nil, value: provider.selectedSegmentIndex)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .contentShape(Rectangle())
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                        .animation(nil, value: provider.selectedSegmentIndex)
                        .buttonStyle(.plain)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(2)
            }
            .overlay(
                Color.clear
                    .accessibilityElement(children: .ignore)
                    .accessibilityIdentifier(AccessibilityIdentifier.bleFilterSegmentSelector)
                    .accessibilityValue("selectedIndex=\(clampedSelectedIndex)")
                    .allowsHitTesting(false)
            )
            .onAppear {
                initializeSelectorOffsetIfNeeded(segmentWidth: segmentWidth)
            }
            .onChange(of: segmentWidth) { newValue in
                updateSelectorOffsetForSegmentWidth(newValue)
            }
            .onChange(of: provider.selectedSegmentIndex) { _ in
                guard segmentWidth > 0 else { return }
                let targetOffset = segmentHighlightOffset(segmentWidth: segmentWidth)
                if isSelectorOffsetInitialized {
                    animateSelectorOffset(to: targetOffset)
                } else {
                    setSelectorOffsetImmediate(targetOffset)
                }
            }
        }
        .frame(height: 48)
        .onDisappear {
            selectorAnimationTimer?.invalidate()
            selectorAnimationTimer = nil
        }
    }

    private var clampedSelectedIndex: Int {
        guard !titles.isEmpty else { return 0 }
        return min(max(provider.selectedSegmentIndex, 0), titles.count - 1)
    }

    private var palette: SegmentPalette {
        switch appearanceMode {
        case .legacyBlue:
            return SegmentPalette(
                background: Color("white"),
                border: Color("backgroung_filter"),
                selectedBackground: Color("white"),
                selectedText: .black,
                normalText: Color("deselected_text_filter")
            )
        case .modernGray:
            return SegmentPalette(
                background: Color("ubi4_gray"),
                border: Color("ubi4_gray_border"),
                selectedBackground: Color("ubi4_back"),
                selectedText: .white,
                normalText: Color("ubi4_deactivate_text")
            )
        }
    }

    private func segmentHighlightOffset(segmentWidth: CGFloat) -> CGFloat {
        CGFloat(clampedSelectedIndex) * segmentWidth
    }

    private func select(index: Int) {
        guard provider.selectedSegmentIndex != index else { return }
        UIView.performWithoutAnimation {
            provider.selectedSegmentIndex = index
        }
    }

    private func initializeSelectorOffsetIfNeeded(segmentWidth: CGFloat) {
        guard segmentWidth > 0 else { return }
        let targetOffset = segmentHighlightOffset(segmentWidth: segmentWidth)
        guard !isSelectorOffsetInitialized else {
            if selectorAnimationTimer == nil {
                setSelectorOffsetImmediate(targetOffset)
            }
            return
        }
        setSelectorOffsetImmediate(targetOffset)
    }

    private func updateSelectorOffsetForSegmentWidth(_ segmentWidth: CGFloat) {
        guard segmentWidth > 0 else { return }
        let targetOffset = segmentHighlightOffset(segmentWidth: segmentWidth)
        guard isSelectorOffsetInitialized else {
            setSelectorOffsetImmediate(targetOffset)
            return
        }
        if selectorAnimationTimer == nil {
            setSelectorOffsetImmediate(targetOffset)
        } else {
            animateSelectorOffset(to: targetOffset)
        }
    }

    private func setSelectorOffsetImmediate(_ offset: CGFloat) {
        isSelectorOffsetInitialized = true
        selectorDisplayOffset = offset
    }

    private func animateSelectorOffset(to targetOffset: CGFloat) {
        let clampedTarget = targetOffset.isFinite ? targetOffset : 0
        let startOffset = selectorDisplayOffset
        let delta = clampedTarget - startOffset

        if abs(delta) < 0.5 {
            setSelectorOffsetImmediate(clampedTarget)
            return
        }

        selectorAnimationTimer?.invalidate()
        let startTime = CACurrentMediaTime()
        let duration = animationDuration
        let timer = Timer(timeInterval: 1.0 / 60.0, repeats: true) { timer in
            let elapsed = CACurrentMediaTime() - startTime
            let progress = min(max(elapsed / duration, 0), 1)
            let easedProgress = easeInOut(progress: progress)
            let nextOffset = startOffset + (delta * easedProgress)

            selectorDisplayOffset = nextOffset

            if progress >= 1 {
                timer.invalidate()
                selectorAnimationTimer = nil
                setSelectorOffsetImmediate(clampedTarget)
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        selectorAnimationTimer = timer
    }

    private func easeInOut(progress: Double) -> CGFloat {
        let easedValue: Double
        if progress < 0.5 {
            easedValue = 2 * progress * progress
        } else {
            easedValue = 1 - pow(-2 * progress + 2, 2) / 2
        }
        return CGFloat(easedValue)
    }
}

private struct SegmentPalette {
    let background: Color
    let border: Color
    let selectedBackground: Color
    let selectedText: Color
    let normalText: Color
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
