//
//  ScanViewController_test.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 22.05.2023.
//  Copyright © 2023 Brian Advent. All rights reserved.
//
import UIKit
import CoreBluetooth
import Foundation

struct LegacyIndyPackedStatus {
    let driverNum: Int
    let bmsNum: Int
    let sensNum: Int
    let openThreshold: Int
    let closeThreshold: Int
    let openSensOption: Int
    let closeSensOption: Int
    let shutdownCurrent: Int
    let scaleFlags: Int

    var reverseSensors: Int {
        return scaleFlags >> 0 & 0b00000001
    }

    var oneChannel: Int {
        return scaleFlags >> 1 & 0b00000001
    }

    var sensorValues: [String: String] {
        return [
            "numderBytes": "12",
            "driver_num_string": String(driverNum),
            "bms_num": String(bmsNum),
            "sens_num": String(sensNum),
            "open_ch_num": String(openThreshold),
            "close_ch_num": String(closeThreshold),
            "corellator_noise_threshold_1_num": String(openSensOption),
            "corellator_noise_threshold_2_num": String(closeSensOption),
            "shutdown_current_num": String(shutdownCurrent),
            "scale_flags_and_revers_and_one_channel": String(scaleFlags),
            "set_reverse": String(reverseSensors),
            "set_one_channel": String(oneChannel)
        ]
    }
}

@objc class ScanViewController: UIViewController, CBCentralManagerDelegate, CBPeripheralDelegate {
    
    
    
    private var centralManager: CBCentralManager!
    private var devicesMass = [CBPeripheral]()
    private var servicesMass = [CBService]()
    private var characteristicsMass = [CBCharacteristic]()
    private var selectedDevice: CBPeripheral?
    
    private let items = [NSLocalizedString("prosthetics", comment: ""), NSLocalizedString("all_devices", comment: "")]
    lazy var segmentedConrol: CustomSegmentedControl = {
                let control = CustomSegmentedControl(items: items)
                return control
            }()
    @IBOutlet var containerView: UIView!
    @IBOutlet weak var tableHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var tableBottomOfsetConstraint: NSLayoutConstraint!
    @IBOutlet weak var tableViewDevices: UITableView!
    private var devices = [ScanItem]()
    private var allDevices = [ScanItem]()
    private var filteringDevices = [ScanItem]()
    private var savingParametrsMassString:[SaveObjectString]!
    private var previousConnectionUUID = ""
    private var smartConnectionActivate = true
    private var deactivateSmartConnection = false
    private var dataCount = 0
    private var versionDriverGreaterThan239: Bool = false

    
    var dataForSensorsViewController = ["numderBytes": "0", "sens_1": "123", "sens_2": "124", "synchronized": "0", "driver_num_string" : "not read", "bms_num": "0", "sens_num": "0", "open_ch_num": "0", "close_ch_num": "0", "corellator_noise_threshold_1_num": "0", "set_reverse":"0", "set_one_channel":"0", "add_gesture":"0", "corellator_noise_threshold_2_num": "0", "deviceName":"lol",  "reseivedFirstNotifyData":"false", "gesture_use_num":"0", "prosthesis_blocking_switching" : "0", "hold_to_lock_time":"13" , "gesture_switching_by_sensors":"0", "time_at_rest":"13","expected_id_command":"FFF0", "shutdown_current_num":"0", "scale_flags_and_revers_and_one_channel":"0", "start_gesture":"0", "end_gesture":"0", "num_active_gestures":"8", "set_mode_prothesis":"0", "max_stand_cycles":"0"]
    var dataForDelayFingersViewController = ["":""]
    var dataForCalibrationStatusViewController = ["":""]
    var dataState = ["state":"0"]
    fileprivate var cellIdentifire = String (describing: DeviceCell.self)
    let sampleGattAttributes = SampleGattAttributes()
    var reconecting: Bool = false
    var myTimer = Timer()
    var reseivedFirstNotifyData: Bool = false
    var selectedDeviceName = ""
    var directConnectionHint: OldMotoricaStartLauncher.ConnectionHint?
    private var didConsumeDirectConnectionHint = false
    private var didOpenDirectConnectionSensors = false
    @objc var gestureTable = [[[Int]]] (repeating: [[0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0]] , count: 7)
    @objc var gestureTableBig = [[[Int]]] (repeating: [[0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0]] , count: 13)
    @objc var byteEnabledGesture: Int = 0
    @objc var byteActiveGesture: Int = 0
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        tableViewDevices.layer.cornerRadius = 20;
        tableViewDevices.delegate = self
        tableViewDevices.dataSource = self
        tableViewDevices.register(UINib(nibName: "ScanDeviceNib", bundle: nil ) , forCellReuseIdentifier: cellIdentifire)
        tableViewDevices.tableFooterView = UIView (frame: CGRect.zero)
    
        view.addSubview(segmentedConrol)
        segmentedConrol.translatesAutoresizingMaskIntoConstraints = false //set this for Auto Layout to work!
        segmentedConrol.heightAnchor.constraint(equalToConstant: 60).isActive = true
        segmentedConrol.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 16).isActive = true
        segmentedConrol.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -16).isActive = true
        segmentedConrol.topAnchor.constraint(equalTo: view.topAnchor, constant: 80).isActive = true
        segmentedConrol.selectedSegmentIndex = 1
        segmentedConrol.addTarget(self, action: #selector(filterChange), for: .valueChanged)
        //        style
        segmentedConrol.layer.borderWidth = 2
        segmentedConrol.layer.borderColor = UIColor(named: "backgroung_filter")?.cgColor
        segmentedConrol.backgroundColor = UIColor(named: "white")
        let titleTextAttributes = [NSAttributedStringKey.foregroundColor: UIColor(named: "deselected_text_filter")]
        UISegmentedControl.appearance().setTitleTextAttributes(titleTextAttributes as [AnyHashable : Any], for: .normal)
        let titleTextAttributes2 = [NSAttributedStringKey.foregroundColor: UIColor.black]
        UISegmentedControl.appearance().setTitleTextAttributes(titleTextAttributes2, for: .selected)
        NotificationCenter.default.addObserver(self, selector: #selector(readWriteToBLENotification), name: .notificationFromSensorsViewController, object: nil)
        LegacySmartConnectionStateStore.ensureDefaultIfNeeded()
        saveDataString(key: sampleGattAttributes.DEACTIVATE_SMART_CONNECTION, value: "0")
        print("TEST!!!! viewDidLoad()" )
        
        
        
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTHS00000"))
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTFO00123"))
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTFS12300"))
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTHO01230"))
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTEP14600"))
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTEB06540"))
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTEC12340"))
//        print("Тест работы с именем getCleanName(): "+NameUtil().getCleanName(deviceName: "FEST-XFTEB12345"))
        
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-H-1234"))
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-H-12345"))
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-H-123456"))
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-F-1234"))
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-F-12345"))
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-EP-12345"))
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-EB-12345"))
//        print("Тест работы с именем "+NameUtil().getCleanName(deviceName: "FEST-D-12345"))
        
        
        
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        allDevices = [ScanItem]()
        loadDataString()
        initUI()
        centralManager = CBCentralManager()
        centralManager.delegate = self
        print("TEST!!!! viewWillAppear()")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        openSensorsForDirectConnectionIfNeeded()
    }

    @objc private func filterChange(target: UISegmentedControl) {
        if target == self.segmentedConrol {
            devices = [ScanItem]()
            let segmentedIndex = target.selectedSegmentIndex
            
            if (segmentedIndex == 0) { devices = filteringDevices(devices: allDevices) }
            if (segmentedIndex == 1) { devices = allDevices }
            tableViewDevices.reloadData()
            updateConstraints()
            saveDataString(key: sampleGattAttributes.FILTER_POSITION, value: String(segmentedConrol.selectedSegmentIndex))
        }
    }
    
    func updateConstraints() {
        tableHeightConstraint.constant = tableViewDevices.contentSize.height
        
        if ((segmentedConrol.selectedSegmentIndex == 0 && (filteringDevices(devices: allDevices).count > 0)) || segmentedConrol.selectedSegmentIndex == 1) {
            //без этой строчки первый элемент пожованный в высоту
            if (tableViewDevices.contentSize.height < 64) {tableHeightConstraint.constant = 64}
        }
        
               
        UIView.animate(withDuration: 0.33,
                       delay: 0.0,
                       options: .curveEaseIn,
                       animations: {
                            self.view.layoutIfNeeded()
                        },
                       completion: nil)
    }
    
    // MARK: Запуск сканирования и подключения к выбранному устройству
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn
        {
            if connectDirectConnectionPeripheralIfAvailable() {
                return
            }

            centralManager.scanForPeripherals(withServices: nil,
                                              options: nil)
            print("TEST!!!! scanForPeripherals()" )
        }
    }
    func centralManager(_ central: CBCentralManager, didDiscover peripheral:
        CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {

        guard var title = peripheral.name else { return }
        if (peripheral.name == "HRSTM") { title = "INDY" }
        
        // mac-адрес на iOS получить невозможно, поэтому раскидываем имена по уникальным UUID, которые
        // сам iPhone присваивает каждому устройству
        print("UUID-addr test: \(String(describing: UIDevice.current.identifierForVendor?.uuidString))")
        print("UUID-addr Name: \(String(describing: peripheral.name))")
        loadDataString()
        var saveMacInMemory = true
        var resolvedDeviceName = title
        for item in savingParametrsMassString
        {
            if (item.key == peripheral.identifier.uuidString.uppercased()) {
                saveMacInMemory = false
                print("UUID-addr: " + peripheral.identifier.uuidString.uppercased() + " есть у нас в памяти  " + item.value)
            }
        }
        if (saveMacInMemory) {
            saveDataString(key: peripheral.identifier.uuidString.uppercased(), value: title)
        }
        
        
        var find: Bool = false
        for myDevicesMass in allDevices {
            if (myDevicesMass.title == title ) {
                find = true
            }
        }
        if (!find) {
            loadDataString()
            for item in savingParametrsMassString
            {
                if (item.key == peripheral.identifier.uuidString.uppercased()) {
                    selectedDeviceName = item.value
                    resolvedDeviceName = item.value
                    addBleDevice(title: item.value, RSSI: RSSI, peripheral: peripheral)
                }
            }
        }

        if handleDirectConnectionIfNeeded(
            peripheral: peripheral,
            deviceName: resolvedDeviceName
        ) {
            return
        }
        
        if (LegacySmartConnectionStateStore.isAutoConnectionAllowed()) {
            if (previousConnectionUUID == peripheral.identifier.uuidString.uppercased()) {
                beginConnection(
                    peripheral: peripheral,
                    deviceName: selectedDeviceName,
                    uuid: previousConnectionUUID,
                    shouldContinue: { LegacySmartConnectionStateStore.isAutoConnectionAllowed() }
                )
            }
        }
    }
    
    func addBleDevice(title: String, RSSI: NSNumber, peripheral: CBPeripheral) {
        allDevices.append(ScanItem(title: NameUtil().getCleanName(deviceName: title), rssi: RSSI, peripheral: peripheral))
        if (segmentedConrol.selectedSegmentIndex == 0) { devices = filteringDevices(devices: allDevices) }
        else { devices = allDevices }
        tableViewDevices.reloadData()
        updateConstraints()
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        print("did DISConnect")
        self.dataState["state"] = "did DISConnect"
        NotificationCenter.default.post(name: .notificationCheckStateConnection, object: nil, userInfo: self.dataState)
        myTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { (_) in
            print("did REConnect")
            self.centralManager.connect(peripheral, options: nil)
        }
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print("did FAILConnect")
        myTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { (_) in
            print("did REConnect")
            self.centralManager.connect(peripheral, options: nil)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral:
        CBPeripheral) {
        print("did Connect")
        self.dataState["state"] = "did Connect"
        NotificationCenter.default.post(name: .notificationCheckStateConnection, object: nil, userInfo: self.dataState)
        myTimer.invalidate()
        peripheral.delegate = self
        peripheral.discoverServices(nil)
    }
    
    //MARK: - работа с конкретным устройством
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error:
        Error?) {
        if let servicePeripherals = peripheral.services as [CBService]?
        {
            for service in servicePeripherals
            {
                self.servicesMass.append(service)
                peripheral.discoverCharacteristics(nil, for: service)
            }
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor
        service: CBService, error: Error?) {
        if let characterArray = service.characteristics as [CBCharacteristic]?
        {
            for cc in characterArray
            {
                self.characteristicsMass.append(cc)
                peripheral.setNotifyValue(true, for: cc) // нотификация
            }
        }
    }
    
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic:
        CBCharacteristic, error: Error?) {

        // Работа с характеристикой нотификации и чтения
        
//        print ("Имя устройства: " + (myDevice?.name ?? "lol"))
//        print ("Получили данные из характеристики: "+characteristic.uuid.uuidString)
//        print ("Что-то сделали с характеристикой: "+characteristic.uuid.uuidString+"   А НЕ НАДО БЫЛО!!!" )
        if let data = characteristic.value {
            dataCount = data.count
            data.withUnsafeBytes(
                {(bytes: UnsafePointer<UInt8>) -> Void in
                    if (!selectedDeviceMatches(sampleGattAttributes.FESTH_NAME) &&
                        !selectedDeviceMatches(sampleGattAttributes.FESTX_NAME)){
                        if (bytes[3] == 0 && bytes[4] == 0 && bytes[5] == 0 && bytes[6] == 0 && bytes[7] == 0 && bytes[8] == 0 && bytes[9] == 0) {
                            self.dataForSensorsViewController["numderBytes"] = String(Int(3))
                        } else {
                            let packedStatus = LegacyIndyPackedStatus(
                                driverNum: Int(bytes[3]),
                                bmsNum: Int(bytes[4]),
                                sensNum: Int(bytes[5]),
                                openThreshold: Int(bytes[6]),
                                closeThreshold: Int(bytes[7]),
                                openSensOption: Int(bytes[8]),
                                closeSensOption: Int(bytes[9]),
                                shutdownCurrent: Int(bytes[10]),
                                scaleFlags: Int(bytes[11])
                            )
                            for (key, value) in packedStatus.sensorValues {
                                self.dataForSensorsViewController[key] = value
                            }
                            print("Получили данные из характеристики DRIVER_VERSION_NEW_VM \(String(packedStatus.driverNum)) \(String(packedStatus.bmsNum)) \(String(packedStatus.sensNum))")
                        }
                        self.dataForSensorsViewController["synchronized"] = String(100)
                    }
                    if (selectedDeviceMatches(sampleGattAttributes.FESTH_NAME)){
                        if (characteristic.uuid.uuidString == sampleGattAttributes.MIO_MEASUREMENT_NEW) {
                            self.dataForSensorsViewController["sens_1"] = String(Int(bytes[0]))
                            self.dataForSensorsViewController["sens_2"] = String(Int(bytes[1]))
                            if (!reseivedFirstNotifyData) {
                                self.dataForSensorsViewController["synchronized"] = String(5)
                                self.dataForSensorsViewController["reseivedFirstNotifyData"] = "true"
                                reseivedFirstNotifyData = true
                                print("Первое получение нотификации 1")
                            }
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.DRIVER_VERSION_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(10)
                            self.dataForSensorsViewController["numderBytes"] = String(Int(10))
                            print ("Получили данные из характеристики DRIVER_VERSION_NEW: "+String(cString:bytes))
                            self.dataForSensorsViewController["driver_num_string"] = String(cString:bytes)
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.SENS_VERSION_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(15)
                            print ("Получили данные из характеристики SENS_VERSION_NEW: "+String(Int(bytes[0])))
                            self.dataForSensorsViewController["sens_num"] = String(Int(bytes[0]))
                            self.dataForSensorsViewController["bms_num"] = String(100)
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.OPEN_THRESHOLD_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(20)
                            self.dataForSensorsViewController["open_ch_num"] = String(Int(bytes[0]))
                            print ("Получили данные из характеристики OPEN_THRESHOLD_NEW: "+String(Int(bytes[0])))
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.CLOSE_THRESHOLD_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(25)
                            self.dataForSensorsViewController["close_ch_num"] = String(Int(bytes[0]))
                            print ("Получили данные из характеристики CLOSE_THRESHOLD_NEW: "+String(Int(bytes[0])))
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.SENS_OPTIONS_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(30)
                            self.dataForSensorsViewController["corellator_noise_threshold_1_num"] = String(Int(bytes[0]))
                            self.dataForSensorsViewController["corellator_noise_threshold_2_num"] = String(Int(bytes[13]))
                            print ("Получили данные из характеристики SENS_OPTIONS_NEW: "+String(Int(bytes[0])))
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.SET_REVERSE_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(35)
                            self.dataForSensorsViewController["set_reverse"] = String(Int(bytes[0]))
                            print ("Получили данные из характеристики SET_REVERSE_NEW: "+String(Int(bytes[0])))
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.SET_ONE_CHANNEL_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(40)
                            self.dataForSensorsViewController["set_one_channel"] = String(Int(bytes[0]))
                            print ("Получили данные из характеристики SET_ONE_CHANNEL_NEW: "+String(Int(bytes[0])))
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.ADD_GESTURE_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(45)
                            var data: String = ""
                            for i in 0...86 {
                                data += String(Int(bytes[i]))+" "
                            }
                            saveDataString(key: sampleGattAttributes.ADD_GESTURE_NEW, value: data)
                            self.dataForSensorsViewController["add_gesture"] = data
                            for i in 0...6 {
                                for j in 0...1 {
                                    for k in 0...5 {
                                        gestureTable[i][j][k] = Int(bytes[i*12 + j*6 + k])
                                        if(k == 4) { gestureTable[i][j][k] = Int(bytes[i * 12 + j * 6 + k]) }
                                        if(k == 5) { gestureTable[i][j][k] = Int(bytes[i * 12 + j * 6 + k]) }
                                    }
                                }
                            }
                            byteEnabledGesture = Int(bytes[84])
                            byteActiveGesture = Int(bytes[85])
                            print ("Получили данные из характеристики ADD_GESTURE_NEW: "+data)
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.CALIBRATION_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(50)
                            print ("Получили данные из характеристики CALIBRATION_NEW: ")
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.SHUTDOWN_CURRENT_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(55)
                            for i in 0...5 {
                                saveDataString(key: "SHUTDOWN_CURRENT_NEW_"+String(i+1), value: String(Int(bytes[i])))
                            }
                            print ("Получили данные из характеристики SHUTDOWN_CURRENT_NEW: ")
                        }
                        if (characteristic.uuid.uuidString == sampleGattAttributes.SET_GESTURE_NEW) {
                            self.dataForSensorsViewController["synchronized"] = String(100)
                            print ("Получили данные из характеристики SET_GESTURE_NEW: ")
                        }
                    } else {
                        if (selectedDeviceMatches(sampleGattAttributes.FESTX_NAME)) {
//                            print("Получение данных от FEST-X с характеристики: " + characteristic.uuid.uuidString)
                            
                            if (characteristic.uuid.uuidString == sampleGattAttributes.MIO_MEASUREMENT_NEW_VM) {
                                self.dataForSensorsViewController["sens_1"] = String(Int(bytes[0]))
                                self.dataForSensorsViewController["sens_2"] = String(Int(bytes[1]))
                                self.dataForSensorsViewController["gesture_use_num"] = String(Int(bytes[2])+1)
                                self.dataForSensorsViewController["expected_id_command"] = String(format: "%02X", bytes[11]) + String(format: "%02X", bytes[10])
                                
                                if (!reseivedFirstNotifyData) {
                                    self.dataForSensorsViewController["synchronized"] = String(5)
                                    print ("synchronized: 5")
                                    self.dataForSensorsViewController["reseivedFirstNotifyData"] = "true"
                                    reseivedFirstNotifyData = true
                                    print("Первое получение нотификации 2")
                                }
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.DRIVER_VERSION_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(10)
                                print ("synchronized: 10")
                                self.dataForSensorsViewController["numderBytes"] = String(Int(10))
                                print ("Получили данные из характеристики DRIVER_VERSION_NEW_VM: "+String(cString:bytes))
                                self.dataForSensorsViewController["driver_num_string"] = String(cString:bytes)
                                versionDriverGreaterThan239 = chekNewVersion(driverVersionString: String(cString:bytes))
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.SENS_VERSION_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(15)
                                print ("synchronized: 15")
                                print ("Получили данные из характеристики SENS_VERSION_NEW_VM: "+String(Int(bytes[0])))
                                self.dataForSensorsViewController["sens_num"] = String(Int(bytes[0]))
                                self.dataForSensorsViewController["bms_num"] = String(100)
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.OPEN_THRESHOLD_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(20)
                                print ("synchronized: 20")
                                self.dataForSensorsViewController["open_ch_num"] = String(Int(bytes[0]))
                                print ("Получили данные из характеристики OPEN_THRESHOLD_NEW: "+String(Int(bytes[0])))
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.CLOSE_THRESHOLD_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(25)
                                print ("synchronized: 25")
                                self.dataForSensorsViewController["close_ch_num"] = String(Int(bytes[0]))
                                print ("Получили данные из характеристики CLOSE_THRESHOLD_NEW: "+String(Int(bytes[0])))
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.SENS_OPTIONS_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(30)
                                print ("synchronized: 30")
                                self.dataForSensorsViewController["corellator_noise_threshold_1_num"] = String(Int(bytes[0]))
                                self.dataForSensorsViewController["corellator_noise_threshold_2_num"] = String(Int(bytes[13]))
                                print ("Получили данные из характеристики SENS_OPTIONS_NEW: "+String(Int(bytes[0])))
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.SET_REVERSE_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(35)
                                print ("synchronized: 35")
                                self.dataForSensorsViewController["set_reverse"] = String(Int(bytes[0]))
                                print ("Получили данные из характеристики SET_REVERSE_NEW: "+String(Int(bytes[0])))
                                
                                if (versionDriverGreaterThan239) {
                                    self.dataForSensorsViewController["num_active_gestures"] = String(Int(data[1]))
                                    self.dataForSensorsViewController["set_mode_prothesis"] = String(Int(data[2]))
                                    self.dataForSensorsViewController["max_stand_cycles"] = String(Int(data[3])+256*Int(data[4]))
                                    print ("Получили данные max_stand_cycles data[3]=\(data[3]) data[4]=\(data[4])")
                                    print ("Получили данные max_stand_cycles chislo=\(Int(data[3])+256*Int(data[4]))")
                                    print ("Получили данные max_stand_cycles chislo data[4]=\(UInt8((Int(data[3])+256*Int(data[4]))/256))")
                                    print ("Получили данные max_stand_cycles chislo data[3]=\((Int(data[3])+256*Int(data[4]))-Int(UInt8((Int(data[3])+256*Int(data[4]))/256))*256)")
                                }
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.SET_ONE_CHANNEL_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(40)
                                print ("synchronized: 40")
                                self.dataForSensorsViewController["set_one_channel"] = String(Int(bytes[0]))
                                print ("Получили данные из характеристики SET_ONE_CHANNEL_NEW: "+String(Int(bytes[0])))
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.ADD_GESTURE_NEW_VM) {
                                //TODO дописать сюда парсинг новой пачки жестов
                                self.dataForSensorsViewController["synchronized"] = String(45)
                                print ("synchronized: 45")
                                print ("Получили данные count = \(dataCount)")
                                var data: String = ""
                                if (dataCount == 87) {
                                    for i in 0...86 {
                                        data += String(Int(bytes[i]))+" "
                                    }
                                    for i in 0...6 {
                                        for j in 0...1 {
                                            for k in 0...5 {
                                                gestureTable[i][j][k] = Int(bytes[i*12 + j*6 + k])
                                            }
                                        }
                                    }
                                    byteEnabledGesture = Int(bytes[84])
                                    
                                    saveDataString(key: sampleGattAttributes.ADD_GESTURE_NEW, value: data)
                                }
                                
                                
                                if (dataCount == 159) {
                                    for i in 0...158 {
                                        data += String(Int(bytes[i]))+" "
                                    }
                                    for i in 0...12 {
                                        for j in 0...1 {
                                            for k in 0...5 {
                                                gestureTableBig[i][j][k] = Int(bytes[i*12 + j*6 + k])
                                            }
                                        }
                                    }
                                    
                                    saveDataString(key: sampleGattAttributes.ADD_GESTURE_NEW_BIG, value: data)
                                }
                                
                                
                                print ("Получили данные из характеристики ADD_GESTURE_NEW: "+data)
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.CALIBRATION_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(50)
                                print ("synchronized: 50")
                                print ("Получили данные из характеристики CALIBRATION_NEW: ")
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(55)
                                print ("synchronized: 55")
                                for i in 0...5 {
                                    saveDataString(key: "SHUTDOWN_CURRENT_NEW_"+String(i+1), value: String(Int(bytes[i])))
                                }
                                print ("Получили данные из характеристики SHUTDOWN_CURRENT_NEW: ")
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.SET_GESTURE_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(60)
                                print ("synchronized: 60")
                                print ("Получили данные из характеристики SET_GESTURE_NEW: ")
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.ROTATION_GESTURE_NEW_VM) {
                                self.dataForSensorsViewController["synchronized"] = String(100)
                                print ("synchronized: 100")
                                self.dataForSensorsViewController["gesture_switching_by_sensors"] = String(Int(bytes[0]))
                                self.dataForSensorsViewController["time_at_rest"] = String(Int(bytes[2]))
                                
                                
                                self.dataForSensorsViewController["start_gesture"] = String(Int(bytes[6]))
                                self.dataForSensorsViewController["end_gesture"] = String(Int(bytes[7]))
                                
                                
                                
                                print ("Получили данные из характеристики ROTATION_GESTURE_NEW_VM: gesture_switching_by_sensors=\(Int(bytes[0])) time_at_rest=\(Int(bytes[2]))")
                                self.dataForSensorsViewController["prosthesis_blocking_switching"] = String(Int(bytes[4]))
                                self.dataForSensorsViewController["hold_to_lock_time"] = String(Int(bytes[5]))
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.CHANGE_GESTURE_NEW_VM) {
                                print ("Получили данные из характеристики CHANGE_GESTURE_NEW_VM ")
                                for i in 13...18 {
                                    saveDataString(key: "GESTURE_OPEN_DELAY_FINGER"+String(i-12), value: String(Int(bytes[i])))
                                }
                                for i in 19...24 {
                                    saveDataString(key: "GESTURE_CLOSE_DELAY_FINGER"+String(i-18), value: String(Int(bytes[i])))
                                }
                                NotificationCenter.default.post(name: .notificationReseiveBLEDataDelay, object: nil, userInfo: self.dataForDelayFingersViewController)
                            }
                            if (characteristic.uuid.uuidString == sampleGattAttributes.STATUS_CALIBRATION_NEW_VM) {
                                var chislo: Int = 5
                                chislo = chislo << 8
                                print ("Получили данные из характеристики STATUS_CALIBRATION_NEW_VM \(chislo)")
                                
                                
                                for i in 0...5 {
                                    var temp: String = ""
                                    if (Int(bytes[36+i]) == 6) { temp = "calibrated" }
                                    if (Int(bytes[36+i]) == 5) { temp = "screw pulled" }
                                    if (Int(bytes[36+i]) == 4) { temp = "motor scrolls" }
                                    if (Int(bytes[36+i]) == 3) { temp = "there is no encoder" }
                                    if (Int(bytes[36+i]) == 2) { temp = "there is no motor" }
                                    if (Int(bytes[36+i]) == 1) { temp = "calibration in progress" }
                                    if (Int(bytes[36+i]) == 0) { temp = "not calibrated" }
                                    saveDataString(key: "CALIBRATION_STATUS_FINGER"+String(i+1), value: String(temp))
                                }
                                for i in 0...5 {
                                    let temp = Int(bytes[(i*4)]) +
                                               (Int(bytes[1+(i*4)]) << 8) +
                                               (Int(bytes[2+(i*4)]) << 16) +
                                               (Int(bytes[3+(i*4)]) << 24)
                                    saveDataString(key: "CALIBRATION_STATUS_ENCODER_FINGER"+String(i+1), value: String(temp))
                                }
                                for i in 0...5 {
                                    let temp = Int(bytes[24+(i*2)]) +
                                               (Int(bytes[25+(i*2)]) << 8)
                                    saveDataString(key: "CALIBRATION_STATUS_CURRENT_FINGER"+String(i+1), value: String(temp))
                                }
                                NotificationCenter.default.post(name: .notificationReseiveBLEDataCalibrationStatus, object: nil, userInfo: self.dataForCalibrationStatusViewController)
                            }
                        } else {
                            self.dataForSensorsViewController["sens_1"] = String(Int(bytes[1]))
                            self.dataForSensorsViewController["sens_2"] = String(Int(bytes[2]))
                        }
                    }
                    NotificationCenter.default.post(name: .notificationReseiveBLEData, object: nil, userInfo: self.dataForSensorsViewController)
            })
        }
    }
    
    
    private func filteringDevices (devices : [ScanItem]) -> [ScanItem] {
        filteringDevices = [ScanItem]()
        for device in devices {
            if ( device.title.contains("FEST") || device.title.contains("INDY") ) {
                filteringDevices.append(device)
            }
        }
        return filteringDevices
    }
    
    private func initUI() {
        var filterPositionOn: Bool = false
        var lastConnectionOn: Bool = false
        var smartConnectionActivateOn: Bool = false
        
        // проверка, есть ли значения переменных в памяти
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.FILTER_POSITION) {
                filterPositionOn = true
            }
            if (item.key == sampleGattAttributes.LAST_CONNECTION) {
                lastConnectionOn = true
            }
            if (item.key == sampleGattAttributes.SMART_CONNECTION) {
                smartConnectionActivateOn = true
            }
        }
        
        // если переменная false, то её значения небыло в памяти телефона раньше и она
        // инициализируется дефолтным, заготовленным значением
        if (!filterPositionOn) {
            saveDataString(key: sampleGattAttributes.FILTER_POSITION, value: "1")
            loadDataString()
        }
        if (!lastConnectionOn) {
            saveDataString(key: sampleGattAttributes.LAST_CONNECTION, value: "")
            loadDataString()
        }
        if (!smartConnectionActivateOn) {
            LegacySmartConnectionStateStore.setEnabled(true, notify: false)
            loadDataString()
        }
        
        
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.FILTER_POSITION) {
                segmentedConrol.selectedSegmentIndex = Int(item.value) ?? 1
            }
            if (item.key == sampleGattAttributes.LAST_CONNECTION) {
                previousConnectionUUID = item.value
            }
            if (item.key == sampleGattAttributes.SMART_CONNECTION) {
                smartConnectionActivate = LegacySmartConnectionStateStore.currentValue()
            }
            if (item.key == sampleGattAttributes.DEACTIVATE_SMART_CONNECTION) {
                deactivateSmartConnection = item.value.boolValue
            }
        }
        
    }
    
    // MARK: - работа с фоном
    override func viewDidLayoutSubviews() {
        let topColor: UIColor = #colorLiteral(red: 0, green: 0.4745098039, blue: 0.568627451, alpha: 1)
        let bottomColor: UIColor = #colorLiteral(red: 0.2823529412, green: 0.6941176471, blue: 0.7490196078, alpha: 1)
        
        let startPointX: CGFloat = 0.5
        let startPointY: CGFloat = 0
        let endPointX: CGFloat = 0.5
        let endPointY: CGFloat = 1
        
        let gradientLayer = CAGradientLayer()
        gradientLayer.colors = [topColor.cgColor, bottomColor.cgColor]
        gradientLayer.startPoint = CGPoint(x: startPointX, y: startPointY)
        gradientLayer.endPoint = CGPoint(x: endPointX, y: endPointY)
        gradientLayer.frame = containerView.bounds
        containerView.layer.insertSublayer(gradientLayer, at: 0)
    }
    
    // MARK: - работа с данными
    private func loadDataString() {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
    }
    private func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
    
    // MARK: -  Передача информации при помощи нотификации
    @objc func readWriteToBLENotification(notification: Notification) {
        guard let sendMassage = notification.userInfo,
        let data = sendMassage["byteArray"] as? String,
        let characteristic = sendMassage["characteristic"] as? String,
        let type = sendMassage["type"] as? String,
        let myCase = sendMassage["case"] as? String else { return }
//        print("readDataFromFestX 2 characteristic:\(characteristic)    type: \(type)")
//        print("sendDataToFestX 2 characteristic:\(characteristic)  type: \(type)")
        for characteristics in characteristicsMass {
//        print("характеристика в списке: \(characteristics.uuid.uuidString)  а мы ищем: \(characteristic)")
            if (characteristics.uuid.uuidString == characteristic) {
                print("попытались отправить данные type: \(type)")
                if (type == sampleGattAttributes.WRITE_HC10) {
                    let preambul = Data([0xAA, 0xAA])
                    let length = Data([UInt8(data.hexDecodedData().count + 2)])
                    let numberComand = Data([UInt8(Int(myCase)!)])
                    let forCalcCrc = preambul + length + numberComand + data.hexDecodedData()
                    let finalMassage = forCalcCrc + Data([crcCalc(data: forCalcCrc)])
                    
                    selectedDevice?.writeValue(finalMassage, for: characteristics, type: .withoutResponse)
                    print("посылка для HC10")
//                    selectedDevice?.services
                }
                if (type == sampleGattAttributes.WRITE) {
                    selectedDevice?.writeValue(data.hexDecodedData(), for: characteristics, type: .withResponse) //запись
                    print("сюда записали: \(characteristics.uuid)   \(data) \(data.count))")
                }
                if (type == sampleGattAttributes.READ) {
                    selectedDevice?.readValue(for: characteristics) //чтение
                    print("от сюда прочитали: \(characteristics.uuid)")
                }
            }
        }
    }

    func crcCalc(data: Data) -> UInt8 {
        var countLocal = data.count
        let crcTable = [UInt8](
            arrayLiteral: 0, 94, 188, 226, 97, 63, 221, 131, 194, 156, 126, 32, 163, 253, 31, 65,
               157, 195, 33, 127, 252, 162, 64, 30, 95, 1, 227, 189, 62, 96, 130, 220,
               35, 125, 159, 193, 66, 28, 254, 160, 225, 191, 93, 3, 128, 222, 60, 98,
               190, 224, 2, 92, 223, 129, 99, 61, 124, 34, 192, 158, 29, 67, 161, 255,
               70, 24, 250, 164, 39, 121, 155, 197, 132, 218, 56, 102, 229, 187, 89, 7,
               219, 133, 103, 57, 186, 228, 6, 88, 25, 71, 165, 251, 120, 38, 196, 154,
               101, 59, 217, 135, 4, 90, 184, 230, 167, 249, 27, 69, 198, 152, 122, 36,
               248, 166, 68, 26, 153, 199, 37, 123, 58, 100, 134, 216, 91, 5, 231, 185,
               140, 210, 48, 110, 237, 179, 81, 15, 78, 16, 242, 172, 47, 113, 147, 205,
               17, 79, 173, 243, 112, 46, 204, 146, 211, 141, 111, 49, 178, 236, 14, 80,
               175, 241, 19, 77, 206, 144, 114, 44, 109, 51, 209, 143, 12, 82, 176, 238,
               50, 108, 142, 208, 83, 13, 239, 177, 240, 174, 76, 18, 145, 207, 45, 115,
               202, 148, 118, 40, 171, 245, 23, 73, 8, 86, 180, 234, 105, 55, 213, 139,
               87, 9, 235, 181, 54, 104, 138, 212, 149, 203, 41, 119, 244, 170, 72, 22,
               233, 183, 85, 11, 136, 214, 52, 106, 43, 117, 151, 201, 74, 20, 246, 168,
               116, 42, 200, 150, 21, 75, 169, 247, 182, 232, 10, 84, 215, 137, 107, 53
           )
        var result: UInt8 = 0
        var i = 0
        while (countLocal != 0 ) {
            result = crcTable[Int(result ^ data[i])]
            i += 1
            countLocal -= 1
        }
        return result
    }
    
    func chekNewVersion(driverVersionString: String) -> Bool {
        if (driverVersionString.count >= 5) {
            if let number = Int(driverVersionString[1..<2] + driverVersionString[3..<5]) {
                if (number >= 239) {
                    return true
                }
            }
        }
        return false
    }
}

extension Notification.Name {
    static let notificationFromSensorsViewController = Notification.Name(rawValue: "notificationFromSensorsViewController")
}
class CustomSegmentedControl: UISegmentedControl{
    private let segmentInset: CGFloat = 5       //your inset amount
    private let segmentImage: UIImage? = UIImage(color: UIColor.white)    //your color

    override func layoutSubviews(){
        super.layoutSubviews()

        //background
        layer.cornerRadius = bounds.height/2
        //foreground
        let foregroundIndex = numberOfSegments
        if subviews.indices.contains(foregroundIndex), let foregroundImageView = subviews[foregroundIndex] as? UIImageView
        {
            foregroundImageView.bounds = foregroundImageView.bounds.insetBy(dx: segmentInset, dy: segmentInset)
            foregroundImageView.image = segmentImage    //substitute with our own colored image
            foregroundImageView.layer.removeAnimation(forKey: "SelectionBounds")
            foregroundImageView.layer.masksToBounds = true
            foregroundImageView.layer.cornerRadius = foregroundImageView.bounds.height/2
        }
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
extension ScanViewController: UITableViewDelegate, UITableViewDataSource{
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return devices.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: cellIdentifire, for: indexPath) as! DeviceCell
        cell.setupModel(model: devices[indexPath.row])
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        selectDeviceToConnect(indexPath)
    }
    func selectDeviceToConnect(_ indexPath:IndexPath) {
        let device = devices[indexPath.row]
        beginConnection(
            peripheral: device.peripheral,
            deviceName: device.title,
            uuid: device.peripheral.identifier.uuidString.uppercased()
        )
    }
}

private extension ScanViewController {
    func openSensorsForDirectConnectionIfNeeded() {
        guard let hint = directConnectionHint,
              !didOpenDirectConnectionSensors else {
            return
        }

        didOpenDirectConnectionSensors = true
        view.accessibilityIdentifier = "legacy.directConnectionHost"
        saveDataString(key: sampleGattAttributes.DEVICE_NAME, value: hint.deviceName)
        saveDataString(key: sampleGattAttributes.DEVICE_MAC, value: hint.deviceUUID.uppercased())
        saveDataString(key: sampleGattAttributes.LAST_CONNECTION, value: hint.deviceUUID.uppercased())
        showDirectConnectionSensorsScreen()
        clearDirectConnectionHintIfFinished()
    }

    func connectDirectConnectionPeripheralIfAvailable() -> Bool {
        guard let hint = directConnectionHint,
              !didConsumeDirectConnectionHint,
              let uuid = UUID(uuidString: hint.deviceUUID),
              let peripheral = centralManager.retrievePeripherals(withIdentifiers: [uuid]).first else {
            return false
        }

        didConsumeDirectConnectionHint = true
        beginConnection(
            peripheral: peripheral,
            deviceName: hint.deviceName,
            uuid: uuid.uuidString.uppercased(),
            openSensorsScreen: false
        )
        clearDirectConnectionHintIfFinished()
        return true
    }

    func handleDirectConnectionIfNeeded(peripheral: CBPeripheral, deviceName: String) -> Bool {
        guard let hint = directConnectionHint,
              !didConsumeDirectConnectionHint,
              matchesDirectConnectionHint(hint, peripheral: peripheral, deviceName: deviceName) else {
            return false
        }

        didConsumeDirectConnectionHint = true
        beginConnection(
            peripheral: peripheral,
            deviceName: deviceName,
            uuid: peripheral.identifier.uuidString.uppercased(),
            openSensorsScreen: false
        )
        clearDirectConnectionHintIfFinished()
        return true
    }

    func matchesDirectConnectionHint(
        _ hint: OldMotoricaStartLauncher.ConnectionHint,
        peripheral: CBPeripheral,
        deviceName: String
    ) -> Bool {
        let peripheralUUID = peripheral.identifier.uuidString.uppercased()
        if peripheralUUID == hint.deviceUUID.uppercased() {
            return true
        }

        let hintNames = normalizedDirectConnectionNames(for: hint.deviceName)
        let candidateNames = normalizedDirectConnectionNames(for: deviceName)
            .union(normalizedDirectConnectionNames(for: peripheral.name ?? ""))
        return !hintNames.isDisjoint(with: candidateNames)
    }

    func normalizedDirectConnectionNames(for rawName: String) -> Set<String> {
        let cleanName = NameUtil().getCleanName(deviceName: rawName)
        return Set([rawName, cleanName].map {
            $0.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        }.filter { !$0.isEmpty })
    }

    func selectedDeviceMatches(_ needle: String) -> Bool {
        let names = [
            selectedDevice?.name,
            selectedDeviceName,
            directConnectionHint?.deviceName
        ].compactMap { $0 }
        return names.contains { $0.contains(needle) }
    }

    func clearDirectConnectionHintIfFinished() {
        guard didOpenDirectConnectionSensors && didConsumeDirectConnectionHint else {
            return
        }
        directConnectionHint = nil
    }

    func showDirectConnectionSensorsScreen() {
        if let sensorsViewController = storyboard?.instantiateViewController(withIdentifier: "VC2"),
           let navigationController = navigationController {
            navigationController.pushViewController(sensorsViewController, animated: false)
            return
        }

        UIView.performWithoutAnimation {
            performSegue(withIdentifier: "goSensorsSettings", sender: nil)
        }
    }

    func beginConnection(
        peripheral: CBPeripheral,
        deviceName: String,
        uuid: String,
        openSensorsScreen: Bool = true,
        shouldContinue: @escaping () -> Bool = { true }
    ) {
        centralManager.stopScan()
        myTimer.invalidate()
        selectedDevice = peripheral
        selectedDeviceName = deviceName

        saveDataString(key: sampleGattAttributes.DEVICE_NAME, value: deviceName)
        saveDataString(key: sampleGattAttributes.DEVICE_MAC, value: uuid)
        saveDataString(key: sampleGattAttributes.LAST_CONNECTION, value: uuid)

        guard shouldContinue() else {
            return
        }

        print("did Connect request")
        centralManager.connect(peripheral, options: nil)
        myTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] (_) in
            guard let self else { return }
            guard shouldContinue() else {
                self.myTimer.invalidate()
                return
            }
            print("did REConnect")
            self.centralManager.connect(peripheral, options: nil)
        }
        if openSensorsScreen {
            performSegue(withIdentifier: "goSensorsSettings", sender: nil)
        }
    }
}
