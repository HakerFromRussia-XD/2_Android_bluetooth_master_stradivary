//
//  testController.swift
//  MotoricaStart
//
//  Created by macbook on 14.04.2021.
//  Copyright © 2021 Brian Advent. All rights reserved.
//

import UIKit
import DGCharts


@objc class SensorsViewController: UIViewController {
    
    
    @IBOutlet weak var synchronizedText: UILabel!
    @IBOutlet weak var containerView: UIView!
    @IBOutlet weak var statusImage: UIImageView!
    @IBOutlet weak var deviceName: UILabel!
    @IBOutlet weak var startGestureNum: UILabel!
    @IBOutlet weak var activeGestureNum: UILabel!
    @IBOutlet weak var endGestureNum: UILabel!
    @IBOutlet weak var loopGestures: UIView!
    @IBOutlet weak var lineChartView: LineChartView!
    @IBOutlet weak var openThreshold: UISlider! {
        didSet {
            openThreshold.transform = CGAffineTransform(rotationAngle: CGFloat(-(Double.pi/2)))
        }
    }
    @IBOutlet weak var openThresholdView: UIView!
    @IBOutlet weak var closeThreshold: UISlider! {
        didSet {
            closeThreshold.transform = CGAffineTransform(rotationAngle: CGFloat(-(Double.pi/2)))
        }
    }
    @IBOutlet weak var closeThresholdView: UIView!
    @IBOutlet weak var openSensSlide: UISlider!
    @IBOutlet weak var openSensNum: UILabel!
    @IBOutlet weak var closeSensSlide: UISlider!
    @IBOutlet weak var closeSensNum: UILabel!
    @IBOutlet weak var swapSensText: UILabel!
    @IBOutlet weak var swapSensSwitch: UISwitch!
    @IBOutlet weak var blockSettingsSwitch: UISwitch!
    @IBOutlet weak var bloskSettingsText: UILabel!
    @IBOutlet weak var openBtn: UIButton!
    @IBOutlet weak var closeBtn: UIButton!
    @IBAction func unwindToThisAdvencedViewController (sender: UIStoryboardSegue){
        //тут выполняется код, при возвращении на это вью с помощью кнопок back от куда угодно
        loadDataString(key: "all", numberLoad: 8)
        initUI()
    }
    @IBOutlet weak var driverText: UILabel!
    @IBOutlet weak var bmsText: UILabel!
    @IBOutlet weak var sensersText: UILabel!
    @IBOutlet weak var gestureSettingsBtn: UIButton!
    @IBOutlet weak var advancedSettingsBtn: UIButton!
    @IBOutlet weak var unlockAdvancedSettings: UIButton!
    
    let connectStatus = UIImage(named:"connect_status")!
    let disconnectStatus = UIImage(named:"disconnect_status")!
    
    let values = (0..<1).map { (i) -> ChartDataEntry in
        let val = Double(arc4random_uniform(UInt32(1))+3)
        return ChartDataEntry(x: Double(i), y: val)
    }
    var firstInit: Bool = true

    var reseve_sensor_1_data: Int = 0
    var reseve_sensor_2_data: Int = 0
    var old_reseve_sensor_1_data: Int = 0
    var old_reseve_sensor_2_data: Int = 0
    var timerTiks: Int = 3
    private var current_sensor_1: Double = 0
    private var current_sensor_2: Double = 0
    private var start_sensor_1: Double = 0
    private var start_sensor_2: Double = 0
    private var target_sensor_1: Double = 0
    private var target_sensor_2: Double = 0
    private var rampTick: Int = 0
    private var chartTimer: Timer?
    
    var open_sens_slide: UInt8 = 0
    var close_sens_slide: UInt8 = 0
    

    var count: Int = 0
    var countTest: Int = 0
    static let sampleGattAttributes = SampleGattAttributes()
    static var dataForCommunicate = ["byteArray": "01020304", "characteristic":"1", "type":"READ", "case":"0"]
    private var savingParametrsMassString:[SaveObjectString]!
    private var blockChangeSettings: Bool = false
    private var lockAdvancedSettings: Bool = false
    private var startMoovSlide: Bool = true
    private var startValue: Float!
    private var swapButtonOpenClose: Bool = false
    static var flagReadData: Bool = true
    private var pauseReadData: Bool = true
    private var delayForReadData: UInt32 = 100000 //в микросекундах. Должно быть меньше, чем queueInterval. Чтобы не давать очереди
    static let queueInterval: UInt32 = 110000// большее количество команд, чем она отдаёт
    static let inactiveQueue = DispatchQueue(label: "My queue", attributes: [.concurrent, .initiallyInactive])
    static let semafore = DispatchSemaphore(value: 1)
    
    private var delayForReadData2: UInt32 = 1000000 //в микросекундах. Должно быть меньше, чем queueInterval. Чтобы не давать очереди
    static let queueInterval2: UInt32 = 1100000// большее количество команд, чем она отдаёт
    static let inactiveQueue2 = DispatchQueue(label: "My queue 2", attributes: [.concurrent, .initiallyInactive])
    static let semafore2 = DispatchSemaphore(value: 1)
    static var flagReadData2: Bool = true
    static var expectedReceiveConfirmation: Int = 0
    static var expectedIdCommand = "not set"
    var previosReceivedIdCommand = "not set"
    private static var oneAttempt = 0
    private static var twoAttempts = 0
    private static var threeAttempts = 0
    private static var fourAttempts = 0
    private static var fiveAttempts = 0
    private static var countAttempts = 0
    private static var versionDriverNum: Float = 0
    private var percentSyncronise = 0
    
    private var savingDeviceName: String = "...."
    private var typeMultigrib: Bool = false
    private var typeMultigribNew: Bool = false
    private var typeMultigribNewVM: Bool = false
    private var reseivedFirstNotifyDataBool: Bool = true
    public static var versionDriverGreaterThan237: Bool = false
    
    deinit {
        stopChartTimer()
        NotificationCenter.default.removeObserver(self)
        print("SensorsViewController_ is being deinitialized")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        UIApplication.shared.statusBarStyle = .lightContent
        saveDataString(key: SensorsViewController.sampleGattAttributes.READ_THREAD_START, value: String(0))
        saveDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB_FESTX, value: String(0))
        saveDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB_FESTH, value: String(0))
        initChart()
        loadDataString(key: "all", numberLoad: 9)
        initUI()
        
        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(longPerehod(gesture:)))
        unlockAdvancedSettings.addGestureRecognizer(longPress)
        
        NotificationCenter.default.addObserver(self, selector: #selector(updatingUINotification), name: .notificationReseiveBLEData, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(checkStateConnection), name: .notificationCheckStateConnection, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(updateUI), name: .notificationDataDialogToSensorsView, object: nil)
        
        print("Выполнился viewDidLoad")
        if (typeMultigrib) {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
        }
        
        
        //быстрый переход
//        performSegue(withIdentifier: "goToInstructionFest", sender: nil)
//        performSegue(withIdentifier: "goToGesturesSettings", sender: nil)
//        performSegue(withIdentifier: "goToAdvencesSettingsFesth", sender: nil)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if chartTimer == nil {
            startChartTimer()
        }
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        stopChartTimer()
    }
    
    
    @objc func updateUI(notification: Notification) {
        guard let dataState = notification.userInfo,
              let resultDialog = dataState["resultDialog"] as? String
        else {
            print("selectScaleResult else return")
            return
        }
        if (resultDialog == "selectScaleAccept") {
            performSegue(withIdentifier: "showSceleAcceptVC", sender: nil)
        }
        if (resultDialog == "acceptSelectScaleCancel") {
            performSegue(withIdentifier: "showSelectScaleVC", sender: nil)
        }
        if (resultDialog == "acceptSelectScaleAccept") {
            //отправка команды с размером протеза
            loadDataString(key: "all", numberLoad: 10)
            for item in savingParametrsMassString
            {
                if (item.key == SensorsViewController.sampleGattAttributes.SCALE) {
                    let scale: UInt8 = UInt8(item.value) ?? 0x04
                    let data = Data([scale])
                    sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_SELECT_SCALE)
                }
            }
        }
        if (resultDialog == "selectDisconnectAccept") {
            saveDataString(key: SensorsViewController.sampleGattAttributes.DEACTIVATE_SMART_CONNECTION, value: "1")
            NotificationCenter.default.post(name: .oldMotoricaStartDidRequestMergedScan, object: nil)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
                guard self?.view.window != nil else { return }
                self?.navigationController?.popToRootViewController(animated: true)
            }
        }
    }
    @objc func updatingUINotification(notification: Notification) {
        guard let dataForWrite = notification.userInfo,
            let numderBytes = dataForWrite["numderBytes"] as? String,
            let synchronized = dataForWrite["synchronized"] as? String,
            let driver_num_string = dataForWrite["driver_num_string"] as? String,
            let bms_num = dataForWrite["bms_num"] as? String,
            let sens_num = dataForWrite["sens_num"] as? String,
            let open_ch_num = dataForWrite["open_ch_num"] as? String,
            let close_ch_num = dataForWrite["close_ch_num"] as? String,
            let corellator_noise_threshold_1_num = dataForWrite["corellator_noise_threshold_1_num"] as? String,
            let corellator_noise_threshold_2_num = dataForWrite["corellator_noise_threshold_2_num"] as? String,
              
            let shutdown_current_num = dataForWrite["shutdown_current_num"] as? String,
            let scale_flags_and_revers_and_one_channel = dataForWrite["scale_flags_and_revers_and_one_channel"] as? String,
                
            let set_reverse = dataForWrite["set_reverse"] as? String,
            let set_one_channel = dataForWrite["set_one_channel"] as? String,
            let gesture_use_num = dataForWrite["gesture_use_num"] as? String,
            let prosthesis_blocking_switching = dataForWrite["prosthesis_blocking_switching"] as? String,
            let hold_to_lock_time = dataForWrite["hold_to_lock_time"] as? String,
            let gesture_switching_by_sensors = dataForWrite["gesture_switching_by_sensors"] as? String,
            let time_at_rest = dataForWrite["time_at_rest"] as? String,
            let sens_1 = dataForWrite["sens_1"] as? String,
            let sens_2 = dataForWrite["sens_2"] as? String,
            let reseivedFirstNotifyData = dataForWrite["reseivedFirstNotifyData"] as? String,
            let expected_id_command = dataForWrite["expected_id_command"] as? String,
            let start_gesture = dataForWrite["start_gesture"] as? String,
            let end_gesture = dataForWrite["end_gesture"] as? String,
            let num_active_gestures = dataForWrite["num_active_gestures"] as? String,
            let set_mode_prothesis = dataForWrite["set_mode_prothesis"] as? String,
            let max_stand_cycles = dataForWrite["max_stand_cycles"] as? String
        else { return }
        print("numderBytes = \(Int(numderBytes))")
        if (Int(numderBytes) ?? 0 >= 12) {
            for item in savingParametrsMassString
            {
                if (item.key == SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_HDLE && !typeMultigrib) {
                    if (Int(item.value) != Int(shutdown_current_num)) {
                        print("обновили ток отсечки INDY")
                        saveDataString(key: item.key, value: String(Int(shutdown_current_num)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        NotificationCenter.default.post(name: .notificationUpdateAdvancedSettings, object: nil, userInfo: nil)
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SET_REVERSE) {
                    //флаг установки реверса сенсоров
                    if ((Int(item.value) != (Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 0 & 0b00000001)) {
                        print("обновили реверс датчиков >12 numderBytes \((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 0 & 0b00000001)")
                        saveDataString(key: item.key, value: String((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 0 & 0b00000001))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL) {
                    //флаг одноканального режима
                    if ((Int(item.value) != (Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 1 & 0b00000001)) {
                        print("обновили одноканальный режим \((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 1 & 0b00000001)")
                        saveDataString(key: item.key, value: String((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 1 & 0b00000001))
                        loadDataString(key: item.key, numberLoad: 7)
                        NotificationCenter.default.post(name: .notificationUpdateAdvancedSettings, object: nil, userInfo: nil)
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SCALE && !typeMultigrib) {
                    //номер размера 0-S 1-M 2-L 3XL
                    if (Int(item.value) != ((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 2 & 0b00000011)) {
                        print("обновили установку размера INDY \((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 2 & 0b00000011)")
                        saveDataString(key: item.key, value: String((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 2 & 0b00000011))
                        loadDataString(key: item.key, numberLoad: 7)
                        NotificationCenter.default.post(name: .notificationUpdateAdvancedSettings, object: nil, userInfo: nil)
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SCALE_FIRST_SET && !typeMultigrib) {
                    //если это число = 1, то устанговка размера уже производилась
                    if (Int(item.value) != ((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 7 & 0b00000001)) {
                        print("обновили первую установку размера INDY \((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 7 & 0b00000001)")
                        saveDataString(key: item.key, value: String((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 7 & 0b00000001))
                        loadDataString(key: item.key, numberLoad: 7)
                        if ((Int(scale_flags_and_revers_and_one_channel) ?? 254) >> 7 & 0b00000001 == 0) {
                            print("показ диалогового окна с выбором размера")
                            performSegue(withIdentifier: "showSelectScaleVC", sender: nil)
                        }
                    }
                }
            }
        }
        if (Int(numderBytes) ?? 0 >= 10) {
            for item in savingParametrsMassString
            {
                if (item.key == SensorsViewController.sampleGattAttributes.DRIVER_NUM_STRING) {
                    if(item.value != driver_num_string) {
                        print("обновили версию драйвера строка" + driver_num_string)
                        saveDataString(key: item.key, value: String(driver_num_string))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.BMS_NUM) {
                    if (Int(item.value) != Int(bms_num)) {
                        print("обновили версию бмс")
                        saveDataString(key: item.key, value: String(Int(bms_num)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SENS_NUM) {
                    if (Int(item.value) != Int(sens_num)) {
                        print("обновили версию сенсоров")
                        saveDataString(key: item.key, value: String(Int(sens_num)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE) {
                    if (Int(item.value) != Int(open_ch_num)) {
                        print("обновили порог открытия")
                        saveDataString(key: item.key, value: String(Int(open_ch_num)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE) {
                    if (Int(item.value) != Int(close_ch_num)) {
                        print("обновили порог закрытия")
                        saveDataString(key: item.key, value: String(Int(close_ch_num)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"1") {
                    if (Int(item.value) != Int(corellator_noise_threshold_1_num)) {
                        print("обновили чувствительность открытия")
                        saveDataString(key: item.key, value: String(Int(corellator_noise_threshold_1_num)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"2") {
                    if (Int(item.value) != Int(corellator_noise_threshold_2_num)) {
                        print("обновили чувствительность закрытия")
                        saveDataString(key: item.key, value: String(Int(corellator_noise_threshold_2_num)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
                if (item.key == SensorsViewController.sampleGattAttributes.SET_REVERSE) {
                    if (Int(item.value) != Int(set_reverse)) {
                        print("обновили реверс датчиков >10 numderBytes")
                        saveDataString(key: item.key, value: String(Int(set_reverse)!))
                        loadDataString(key: item.key, numberLoad: 7)
                        initUI()
                    }
                }
            }
        }
        
        
        for item in savingParametrsMassString
        {
            if (item.key == SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL_NEW) {
                if (Int(item.value) != Int(set_one_channel)) {
                    print("обновили данные одноканального управления")
                    saveDataString(key: item.key, value: String(Int(set_one_channel)!))
                    loadDataString(key: item.key, numberLoad: 7)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_START_GESTURE) {
                if (Int(item.value) != Int(start_gesture)) {
                    print("обновили жест стартовый в группе ротации "+start_gesture)
                    saveDataString(key: item.key, value: String(Int(start_gesture)!))
                    loadDataString(key: item.key, numberLoad: 7)
                    initUI()
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_END_GESTURE) {
                if (Int(item.value) != Int(end_gesture)) {
                    print("обновили жест конечный в группе ротации "+end_gesture)
                    saveDataString(key: item.key, value: String(Int(end_gesture)!))
                    loadDataString(key: item.key, numberLoad: 7)
                    initUI()
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.GESTURE_USE_NUM) {
                if (Int(item.value) != Int(gesture_use_num)) {
                    print("обновили данные активного жеста")
                    saveDataString(key: item.key, value: String(Int(gesture_use_num)!))
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.PROSTHESIS_BLOCKING){
                if (Int(item.value) != Int(prosthesis_blocking_switching)) {
                    print("обновили данные блокировки (включение/отключение режима)")
                    saveDataString(key: item.key, value: String(Int(prosthesis_blocking_switching)!))
                    loadDataString(key: item.key, numberLoad: 7)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.TIME_FOR_BLOCKING){
                if (Int(item.value) != Int(hold_to_lock_time)) {
                    print("обновили данные блокировки слайдера")
                    saveDataString(key: item.key, value: String(Int(hold_to_lock_time)!))
                    loadDataString(key: item.key, numberLoad: 7)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SWITCH_BY_SENSORS) {
                if (Int(item.value) != Int(gesture_switching_by_sensors)) {
                    print("Int(item.value)=\(String(describing: Int(item.value)))   Int(gesture_switching_by_sensors)=\(String(describing: Int(gesture_switching_by_sensors)))")
                    print("обновили данные переключения жестов датчиками (включение/отключение режима)")
                    saveDataString(key: item.key, value: String(Int(gesture_switching_by_sensors)!))
                    loadDataString(key: item.key, numberLoad: 7)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.TIME_AT_REST) {
                if (Int(item.value) != Int(time_at_rest)) {
                    print("обновили данные переключения жестов датчиками (время зажима датчика открытия)")
                    saveDataString(key: item.key, value: String(Int(time_at_rest)!))
                    loadDataString(key: item.key, numberLoad: 7)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.NUM_ACTIVE_GESTURES) {
//                print("данные количества активных жестов \(Int(item.value))")
                if (Int(item.value) != Int(num_active_gestures)) {
                    print("обновили данные количества активных жестов")
                    saveDataString(key: item.key, value: String(Int(num_active_gestures)!))
                    loadDataString(key: item.key, numberLoad: 1)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SET_MODE_PROSTHESIS) {
                if (Int(item.value) != Int(set_mode_prothesis)) {
                    print("обновили данные режима работы протеза")
                    saveDataString(key: item.key, value: String(Int(set_mode_prothesis)!))
                    loadDataString(key: item.key, numberLoad: 1)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.MAX_STAND_CYCLES) {
                if (Int(item.value) != Int(max_stand_cycles)) {
                    print("обновили данные количества циклов стенда \(String(describing: Int(max_stand_cycles)))")
                    saveDataString(key: item.key, value: String(Int(max_stand_cycles)!))
                    loadDataString(key: item.key, numberLoad: 1)
                }
            }
        }
        percentSyncronise = Int(synchronized) ?? 0
        self.synchronizedText.text = NSLocalizedString("Sync", comment: "") + synchronized
        self.reseve_sensor_1_data = Int(sens_1) ?? 0
        self.reseve_sensor_2_data = Int(sens_2) ?? 0
        self.activeGestureNum.text = String((Int(gesture_use_num) ?? 0))
        
        if (self.previosReceivedIdCommand != expected_id_command) {
            self.previosReceivedIdCommand = expected_id_command
            if (SensorsViewController.expectedIdCommand == expected_id_command) {
                SensorsViewController.expectedReceiveConfirmation = 2
                self.previosReceivedIdCommand = "FFFF"
            }
        }
        
        //MARK: - reseivedFirstNotifyData = "true" только если протез FEST-H или FEST-X
        if (reseivedFirstNotifyData == "true") {
            if (reseivedFirstNotifyDataBool){
                reseivedFirstNotifyDataBool = false
                requestStartParameters()
                print ("Обновляем начальные данные")
            }
        }
    }
    @objc func checkStateConnection(notification: Notification) {
        guard let dataState = notification.userInfo,
            let state = dataState["state"] as? String
        else { return }
        print("peredan state: \(state)")
        if (state == "did Connect") {
            statusImage.image = connectStatus
            saveDataString(key: SensorsViewController.sampleGattAttributes.STATUS_CONNECTION, value: String(1))
        }
        if (state == "did DISConnect") {
            synchronizedText.text = NSLocalizedString("Sync", comment: "") + "0"
            statusImage.image = disconnectStatus
            saveDataString(key: SensorsViewController.sampleGattAttributes.STATUS_CONNECTION, value: String(0))
        }
    }
    @objc func longPerehod(gesture: UILongPressGestureRecognizer) {
        if gesture.state == UIGestureRecognizerState.began {
            print("Long Press")
        
            
            var titleDialog = ""
            if (lockAdvancedSettings) {
                titleDialog = NSLocalizedString("Do you want to unlock the advanced settings?", comment: "")
            } else { titleDialog = NSLocalizedString("Do you want to block advanced settings?", comment: "")}
            //1. Create the alert controller.
            let alert = UIAlertController(title: titleDialog, message: "", preferredStyle: .alert)
            

            // 2. Grab the value from the text field, and print it when the user clicks OK.
            alert.addAction(UIAlertAction(title: NSLocalizedString("Cancel", comment: ""), style: .default, handler: { (_) in
            }))
            alert.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: ""), style: .default, handler: { (_) in
                if (self.lockAdvancedSettings) {
                    self.saveDataString(key: SensorsViewController.sampleGattAttributes.OPEN_ADVANCED_SETTINGS, value: String(1))
                } else {
                    self.saveDataString(key: SensorsViewController.sampleGattAttributes.OPEN_ADVANCED_SETTINGS, value: String(0))
                }
                self.loadDataString(key: SensorsViewController.sampleGattAttributes.OPEN_ADVANCED_SETTINGS, numberLoad: 5)
                self.initUI()
            }))

            // 3. Present the alert.
            self.present(alert, animated: true, completion: nil)
        }
    }
    
    // MARK: - обработка взаимодействия с UI
    @IBAction func openThresholdSlide(_ sender: Any) {
        UIView.animate(withDuration: 0.06, animations: {
            self.openThresholdView.transform = CGAffineTransform.init(translationX: 0, y: -(CGFloat(self.openThreshold.value)/1.05))
        })
    }
    @IBAction func openThresholdSlideStop(_ sender: UISlider) {
        if (blockChangeSettings) {
            loadDataString(key: "all", numberLoad: 4)
            initUI()
        } else {
            let data = Data([UInt8(openThreshold.value)])
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_NEW_VM, countRestart: 50)
                
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) {
                        SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(4))
                    }
                    else {
                        sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE)
                    }
                }
            }
            saveDataString(key: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE, value: String(Int(openThreshold.value)))
//            loadDataString(key: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE, numberLoad: 3)
        }
    }
    @IBAction func closeThresholdSlide(_ sender: Any) {
        UIView.animate(withDuration: 0.06, animations: {
            self.closeThresholdView.transform = CGAffineTransform.init(translationX: 0, y: -(CGFloat(self.closeThreshold.value)/1.05))
        })
    }
    @IBAction func closeThresholdSlideStop(_ sender: UISlider) {
        if (blockChangeSettings) {
            loadDataString(key: "all", numberLoad: 4)
            initUI()
        } else {
            let data = Data([UInt8(closeThreshold.value)])
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(5)) }
                    else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE) }
                }
            }
            saveDataString(key: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE, value: String(Int(closeThreshold.value)))
//            loadDataString(key: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE, numberLoad: 3)
        }
    }
    @IBAction func openSensSlide(_ sender: UISlider) {
        openSensNum.text = String(Int(sender.value))
    }
    @IBAction func openSensSlideStop(_ sender: UISlider) {
        if (blockChangeSettings) {
            loadDataString(key: "all", numberLoad: 4)
            initUI()
        } else {
            let data = Data([0x01, (255 - UInt8(sender.value)), 0x01])
            open_sens_slide = (255 - UInt8(sender.value))
            print("Пишем число   open_sens_slide: " + String(open_sens_slide) + "   close_sens_slide: " + String(close_sens_slide))
            let data_new = Data([open_sens_slide, 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64, close_sens_slide, 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64])
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data_new, characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: data_new, characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(11)) }
                    else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS) }
                }
            }
            saveDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS + "1", value: String(Int(sender.value)))
//            loadDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS + "1", numberLoad: 3)
        }
    }
    @IBAction func closeSensSlide(_ sender: UISlider) {
        closeSensNum.text = String(Int(sender.value))
    }
    @IBAction func closeSensSlideStop(_ sender: UISlider) {
        if (blockChangeSettings) {
            loadDataString(key: "all", numberLoad: 4)
            initUI()
        } else {
            let data = Data([0x01, (255 - UInt8(sender.value)), 0x02])
            close_sens_slide = (255 - UInt8(sender.value))
            print("Пишем число   close_sens_slide: " + String(close_sens_slide) + "   open_sens_slide: " + String(open_sens_slide))
            let data_new = Data([open_sens_slide, 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64, close_sens_slide, 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64])
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data_new, characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: data_new, characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(11)) }
                    else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS) }
                }
            }
            saveDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS + "2", value: String(Int(sender.value)))
//            loadDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS + "2", numberLoad: 3)
        }
    }
    @IBAction func swapSensSwitch(_ sender: UISwitch) {
        if (blockChangeSettings) {
            loadDataString(key: "all",numberLoad: 4)
            initUI()
        } else {
            if (sender.isOn) {
                swapSensText.text = NSLocalizedString("on", comment: "")
                let data = Data([0x01])
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew){
                        SensorsViewController.myInteractiveQueueComand (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(14))}
                        else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE) }
                    }
                }
                saveDataString(key: SensorsViewController.sampleGattAttributes.SET_REVERSE, value: String(1))
                loadDataString(key: SensorsViewController.sampleGattAttributes.SET_REVERSE, numberLoad: 3)
            } else {
                swapSensText.text = NSLocalizedString("off", comment: "")
                let data = Data([0x00])
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew){
                        SensorsViewController.myInteractiveQueueComand (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(14))}
                        else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE) }
                    }
                }
                saveDataString(key: SensorsViewController.sampleGattAttributes.SET_REVERSE, value: String(0))
//                loadDataString(key: SensorsViewController.sampleGattAttributes.SET_REVERSE, numberLoad: 3)
            }
        }
    }
    @IBAction func blockSettingsSwitch(_ sender: UISwitch) {
        if (sender.isOn) {
            bloskSettingsText.text = NSLocalizedString("on", comment: "")
            blockChangeSettings = true
            saveDataString(key: SensorsViewController.sampleGattAttributes.SETTINGS_BLOKING, value: String(1))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SETTINGS_BLOKING, numberLoad: 3)
        } else {
            bloskSettingsText.text = NSLocalizedString("off", comment: "")
            blockChangeSettings = false
            saveDataString(key: SensorsViewController.sampleGattAttributes.SETTINGS_BLOKING, value: String(0))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SETTINGS_BLOKING, numberLoad: 3)
        }
    }
    @IBAction func startOpen(_ sender: Any) {
        let data = Data([0x01, 0x00])
        print("startOpen \(typeMultigribNewVM) \(typeMultigribNew) \(typeMultigrib)")
        if (swapButtonOpenClose) {
            if (typeMultigribNewVM) {
                print ("startClose")
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) {
                        SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(6))
                    }
                    else { 
                        print("startOpen sendDataToINDY!!!")
                        sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_HDLE) }
                }
            }
        } else {
            if (typeMultigribNewVM) {
                print ("startOpen")
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) {
                        SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(6))
                    }
                    else { 
                        print("startOpen sendDataToINDY!!!")
                        sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_HDLE) }
                }
            }
        }
        
    }
    @IBAction func stopOpen(_ sender: Any) {
        let data = Data([0x00, 0x00])
        if (swapButtonOpenClose) {
            if (typeMultigribNewVM) {
                print ("startOpen (stopClose)")
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) {
                        SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(6))
                    }
                    else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_HDLE) }
                }
            }
        } else {
            if (typeMultigribNewVM) {
                print ("startOpen (stop)")
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) {
                        SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(6))
                        
                    }
                    else { 
                        print("startOpen (stop) sendDataToINDY!!!")
                        sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_HDLE) }
                }
            }
        }
    }
    @IBAction func startClose(_ sender: Any) {
        let data = Data([0x01, 0x00])
        if (swapButtonOpenClose) {
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(7)) }
                    else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_HDLE) }
                }
            }
        } else {
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x01]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(7)) }
                    else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_HDLE) }
                }
            }
        }
    }
    @IBAction func stopClose(_ sender: Any) {
        let data = Data([0x00, 0x00])
        if (swapButtonOpenClose) {
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(7)) }
                    else {sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.OPEN_MOTOR_HDLE)  }
                }
            }
        } else {
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW_VM, countRestart: 50)
            } else {
                if (typeMultigribNew){
                    SensorsViewController.myInteractiveQueueComand (dataForWrite: Data([0x00]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_NEW, type: SensorsViewController.sampleGattAttributes.WRITE, myCase: "")
                } else {
                    if (typeMultigrib) { SensorsViewController.sendDataToHC10 (dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(7)) }
                    else { sendDataToINDY(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CLOSE_MOTOR_HDLE) }
                }
            }
        }
    }
    @IBAction func perehod(_ sender: Any) {
        print("выключили поток запросов")
        print("perehod  typeMultigribNew: \(typeMultigribNew)    typeMultigribNewVM: \(typeMultigribNewVM)")
        if (typeMultigribNew || typeMultigribNewVM) {
//            requestStartParameters()//без этой строчки синхронизация сбрасывалась к 50
            print("правда deviceName.text: ", deviceName.text ?? 0)
            if (percentSyncronise != 100) {
                performSegue(withIdentifier: "showWaiteVC", sender: nil)
                print("perehod showWaiteVC")
//                performSegue(withIdentifier: "goToAdvencesSettingsFesth", sender: nil)
            } else {
                performSegue(withIdentifier: "goToAdvencesSettingsFesth", sender: nil)
            }
        } else {
            performSegue(withIdentifier: "goToAdvencesSettings", sender: nil)
        }
        pauseReadData = false
        saveDataString(key: SensorsViewController.sampleGattAttributes.READ_THREAD_START, value: String(0))
        loadDataString(key: SensorsViewController.sampleGattAttributes.READ_THREAD_START, numberLoad: 3)
    }
    @IBAction func perehodInstruction(_ sender: Any) {
        if (typeMultigribNew || typeMultigribNewVM) {
            performSegue(withIdentifier: "goToInstructionFest", sender: nil)
        } else {
            performSegue(withIdentifier: "goToInstruction", sender: nil)
        }
    }
    @IBAction func perehod2(_ sender: Any) {
        print("выключили поток запросов")
//        requestStartParameters()
        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
        pauseReadData = false
        saveDataString(key: SensorsViewController.sampleGattAttributes.READ_THREAD_START, value: String(0))
        loadDataString(key: SensorsViewController.sampleGattAttributes.READ_THREAD_START, numberLoad: 3)
    }
    
    
    static func sendDataToHC10 (dataForWrite: Data, characteristic: String, myCase: String) {
        SensorsViewController.dataForCommunicate["byteArray"] = dataForWrite.hexEncodedString()
        SensorsViewController.dataForCommunicate["characteristic"] = characteristic
        SensorsViewController.dataForCommunicate["type"] = SensorsViewController.sampleGattAttributes.WRITE_HC10
        SensorsViewController.dataForCommunicate["case"] = myCase
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: SensorsViewController.dataForCommunicate)
    }
    static func sendDataToFESTH (dataForWrite: Data, characteristic: String) {
        self.dataForCommunicate["byteArray"] = dataForWrite.hexEncodedString()
        self.dataForCommunicate["characteristic"] = characteristic
        self.dataForCommunicate["type"] = SensorsViewController.sampleGattAttributes.WRITE
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: self.dataForCommunicate)
    }
    private func readDataFromFESTH (characteristic: String) {
        SensorsViewController.dataForCommunicate["characteristic"] = characteristic
        SensorsViewController.dataForCommunicate["type"] = SensorsViewController.sampleGattAttributes.READ
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: SensorsViewController.dataForCommunicate)
    }
    private func sendDataToINDY (dataForWrite: Data, characteristic: String) {
        SensorsViewController.dataForCommunicate["byteArray"] = dataForWrite.hexEncodedString()
        SensorsViewController.dataForCommunicate["characteristic"] = characteristic
        SensorsViewController.dataForCommunicate["type"] = SensorsViewController.sampleGattAttributes.WRITE
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: SensorsViewController.dataForCommunicate)
    }
    static func readDataFrom (characteristic: String) {
        SensorsViewController.dataForCommunicate["characteristic"] = characteristic
        SensorsViewController.dataForCommunicate["type"] = SensorsViewController.sampleGattAttributes.READ
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: SensorsViewController.dataForCommunicate)
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
    // MARK: - UI stule
    private func setupeButton (button: UIButton) {
        button.layer.cornerRadius = 21
        button.layer.borderWidth = 2
        button.layer.borderColor = UIColor.white.cgColor
    }
    // MARK: - работа с графиком
    func addEntry (sens1: Int, sens2: Int) {
        guard let lineChartView = lineChartView,
              let data = lineChartView.data else { return }

        var set1 : LineChartDataSet
        var set2 : LineChartDataSet
        
        if (firstInit) {
            set1 = createSet1(values: values)
            set2 = createSet2(values: values)
            data.append(set1)
            data.append(set2)
            firstInit = false
        } else {
            guard
                let existingSet1 = data[1] as? LineChartDataSet,
                let existingSet2 = data[2] as? LineChartDataSet
            else { return }
            set1 = existingSet1
            set2 = existingSet2
        }
        if (set1.count >= 600) {
            zaglushka(bool1: (set1.removeFirst()))
            zaglushka(bool1: (set2.removeFirst()))
        }
        
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens1)), toDataSet: 1)
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens2)), toDataSet: 2)
        
        data.notifyDataChanged()
        lineChartView.notifyDataSetChanged()
        lineChartView.setVisibleXRangeMaximum(600)
        lineChartView.moveViewToX(Double(set2.count - 600))
        self.count += 1
    }
    func initChart() {
        guard let lineChartView = lineChartView else { return }
        lineChartView.noDataText = "Нет данных"
        lineChartView.data = LineChartData()
        var data = lineChartView.data
        let set1 = LineChartDataSet(entries: [], label: "")
        data = LineChartData(dataSet: set1)
        var data2 = lineChartView.data
        let set2 = LineChartDataSet(entries: [], label: "")
        data2 = LineChartData(dataSet: set2)
        lineChartView.data = data
        lineChartView.data = data2
        
        lineChartView.highlightPerTapEnabled = false
        lineChartView.doubleTapToZoomEnabled = false
        lineChartView.isUserInteractionEnabled = false
        lineChartView.isExclusiveTouch = false
        lineChartView.isMultipleTouchEnabled = false
        lineChartView.dragEnabled = false
        lineChartView.dragDecelerationEnabled = false
        lineChartView.setScaleEnabled(false)
        lineChartView.drawGridBackgroundEnabled = false
        lineChartView.pinchZoomEnabled = false
        self.lineChartView.backgroundColor = UIColor(named: "transparent")
        lineChartView.legend.enabled = false
        lineChartView.animate(yAxisDuration: 0.7)
        
        let x: XAxis = self.lineChartView.xAxis
        x.labelTextColor = UIColor(named: "transparent")!
        x.drawGridLinesEnabled = false
        x.axisMaximum = 4000000
        x.avoidFirstLastClippingEnabled = true
        
        let y: YAxis = self.lineChartView.leftAxis
        y.axisMaximum = 255
        y.axisMinimum = 0
        y.labelTextColor = UIColor(named: "transparent")!
        y.drawGridLinesEnabled = true
        y.drawAxisLineEnabled = false
        y.gridColor = UIColor(named: "transparent")!
        self.lineChartView.rightAxis.axisLineColor = UIColor(named: "transparent")!
        self.lineChartView.rightAxis.labelTextColor = UIColor(named: "transparent")!
    }
    func createSet1(values: [ChartDataEntry]) -> LineChartDataSet {
        let set1 = LineChartDataSet(entries: [], label: "")
        set1.axisDependency = YAxis.AxisDependency.left
        set1.lineWidth = 2
        set1.setColor(UIColor(named: "lineColor_open")!)
        set1.mode = LineChartDataSet.Mode.cubicBezier
        set1.drawCirclesEnabled = false
        set1.drawValuesEnabled = false
        
        return set1
    }
    func createSet2(values: [ChartDataEntry]) -> LineChartDataSet {
        let set2 = LineChartDataSet(entries: [], label: "")
        set2.axisDependency = YAxis.AxisDependency.left
        set2.lineWidth = 2
        set2.setColor(UIColor(named: "lineColor_close")!)
        set2.mode = LineChartDataSet.Mode.cubicBezier
        set2.drawCirclesEnabled = false
        set2.drawValuesEnabled = false
        
        return set2
    }
    private func zaglushka(bool1: Bool) {    }

    private func startChartTimer() {
        stopChartTimer()
        let timer = Timer(timeInterval: 0.01, repeats: true) { [weak self] _ in
            self?.addSmoothedChartEntry()
        }
        RunLoop.main.add(timer, forMode: RunLoopMode.commonModes)
        chartTimer = timer
    }

    private func stopChartTimer() {
        chartTimer?.invalidate()
        chartTimer = nil
    }

    private func addSmoothedChartEntry() {
        let new1 = Double(reseve_sensor_1_data)
        let new2 = Double(reseve_sensor_2_data)

        if new1 != target_sensor_1 || new2 != target_sensor_2 {
            start_sensor_1 = current_sensor_1
            start_sensor_2 = current_sensor_2
            target_sensor_1 = new1
            target_sensor_2 = new2
            rampTick = 0
        }

        let ticks = max(1, timerTiks)
        let progress = min(1.0, Double(rampTick + 1) / Double(ticks))
        current_sensor_1 = start_sensor_1 + (target_sensor_1 - start_sensor_1) * progress
        current_sensor_2 = start_sensor_2 + (target_sensor_2 - start_sensor_2) * progress

        addEntry(
            sens1: Int(current_sensor_1.rounded()),
            sens2: Int(current_sensor_2.rounded())
        )

        if rampTick < ticks - 1 {
            rampTick += 1
        } else {
            old_reseve_sensor_1_data = Int(target_sensor_1)
            old_reseve_sensor_2_data = Int(target_sensor_2)
        }
    }

    private func requestStartParameters() {
        if (typeMultigribNew) {
            print("requestStartParameters 1 typeMultigribNew 1")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.DRIVER_VERSION_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SENS_VERSION_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.ADD_GESTURE_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.CALIBRATION_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SET_GESTURE_NEW, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
        }
        if (typeMultigribNewVM) {
            print("requestStartParameters 1 typeMultigribNew 2")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.DRIVER_VERSION_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SENS_VERSION_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SENS_OPTIONS_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SET_REVERSE_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.ADD_GESTURE_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.CALIBRATION_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.SET_GESTURE_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
            SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
        }
    }
    private func initUI() {
        setupeButton(button: openBtn)
        setupeButton(button: closeBtn)
        var driverStringOn: Bool = false
        var bmsOn: Bool = false
        var sensorsOn: Bool = false
        var openThresholdOn: Bool = false
        var closeThresholdOn: Bool = false
        var sensOptions1On: Bool = false
        var sensOptions2On: Bool = false
        var lockAdvancedSettingsOn: Bool = false
        var shutdownCurrentOn: Bool = false
        var reverseOn: Bool = false
        var oneChannelOn: Bool = false
        var scaleOn: Bool = false
        var scaleFirstSetOn: Bool = false
        var passwordFirstSetOn: Bool = false
        var startGestureNumOn: Bool = false
        var endGestureNumOn: Bool = false
        var activeGestureNumOn: Bool = false
        var switchBySensorsOn: Bool = false
        var numActiveGesturesOn: Bool = false
        var setModeProsthesisOn: Bool = false
        var maxStandCyclesOn: Bool = false
        
        // проверка, есть ли значения переменных в памяти
        for item in savingParametrsMassString
        {
            if (item.key == SensorsViewController.sampleGattAttributes.DRIVER_NUM_STRING) {
                driverStringOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.BMS_NUM) {
                bmsOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SENS_NUM) {
                sensorsOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE) {
                openThresholdOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE) {
                closeThresholdOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"1") {
                sensOptions1On = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"2") {
                sensOptions2On = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.OPEN_ADVANCED_SETTINGS) {
                lockAdvancedSettingsOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_HDLE) {
                shutdownCurrentOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SET_REVERSE) {
                reverseOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL) {
                oneChannelOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SCALE) {
                scaleOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SCALE_FIRST_SET) {
                scaleFirstSetOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.PASSWORD) {
                passwordFirstSetOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_START_GESTURE) {
                startGestureNumOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_END_GESTURE) {
                endGestureNumOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.GESTURE_USE_NUM) {
                activeGestureNumOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SWITCH_BY_SENSORS) {
                switchBySensorsOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.NUM_ACTIVE_GESTURES) {
                numActiveGesturesOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SET_MODE_PROSTHESIS) {
                setModeProsthesisOn = true
            }
            if (item.key == SensorsViewController.sampleGattAttributes.MAX_STAND_CYCLES) {
                maxStandCyclesOn = true
            }
        }
        
        // если переменная false, то её значения небыло в памяти телефона раньше и она
        // инициализируется дефолтным, заготовленным значением
        if (!driverStringOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.DRIVER_NUM_STRING, value: String(3))
            loadDataString(key: SensorsViewController.sampleGattAttributes.DRIVER_NUM_STRING, numberLoad: 1)
        }
        if (!bmsOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.BMS_NUM, value: String(1))
            loadDataString(key: SensorsViewController.sampleGattAttributes.BMS_NUM, numberLoad: 1)
        }
        if (!sensorsOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SENS_NUM, value: String(1))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SENS_NUM, numberLoad: 1)
        }
        if (!openThresholdOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE, value: String(30))
            loadDataString(key: SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE, numberLoad: 1)
        }
        if (!closeThresholdOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE, value: String(30))
            loadDataString(key: SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE, numberLoad: 1)
        }
        if (!sensOptions1On) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"1", value: String(22))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"1", numberLoad: 1)
        }
        if (!sensOptions2On) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"2", value: String(22))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"2", numberLoad: 1)
        }
        if (!lockAdvancedSettingsOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.OPEN_ADVANCED_SETTINGS, value: String(0))
            loadDataString(key: SensorsViewController.sampleGattAttributes.OPEN_ADVANCED_SETTINGS, numberLoad: 1)
        }
        if (!shutdownCurrentOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_HDLE, value: String(1))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_HDLE, numberLoad: 1)
        }
        if (!reverseOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SET_REVERSE, value: String(2))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SET_REVERSE, numberLoad: 1)
        }
        if (!oneChannelOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL, value: String(2))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL, numberLoad: 1)
        }
        if (!scaleOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SCALE, value: String(4))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SCALE, numberLoad: 1)
        }
        if (!scaleFirstSetOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SCALE_FIRST_SET, value: String(2))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SCALE_FIRST_SET, numberLoad: 1)
        }
        if (!passwordFirstSetOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.PASSWORD, value: String(0))
        }
        if (!startGestureNumOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SELECTED_START_GESTURE, value: String(0))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SELECTED_START_GESTURE, numberLoad: 1)
        }
        if (!endGestureNumOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SELECTED_END_GESTURE, value: String(0))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SELECTED_END_GESTURE, numberLoad: 1)
        }
        if (!activeGestureNumOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.GESTURE_USE_NUM, value: String(1))
            loadDataString(key: SensorsViewController.sampleGattAttributes.GESTURE_USE_NUM, numberLoad: 1)
        }
        if (!switchBySensorsOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SWITCH_BY_SENSORS, value: String(0))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SWITCH_BY_SENSORS, numberLoad: 1)
        }
        if (!numActiveGesturesOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.NUM_ACTIVE_GESTURES, value: String(8))
            loadDataString(key: SensorsViewController.sampleGattAttributes.NUM_ACTIVE_GESTURES, numberLoad: 1)
        }
        if (!setModeProsthesisOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.SET_MODE_PROSTHESIS, value: String(0))
            loadDataString(key: SensorsViewController.sampleGattAttributes.SET_MODE_PROSTHESIS, numberLoad: 1)
        }
        if (!maxStandCyclesOn) {
            saveDataString(key: SensorsViewController.sampleGattAttributes.MAX_STAND_CYCLES, value: String(0))
            loadDataString(key: SensorsViewController.sampleGattAttributes.MAX_STAND_CYCLES, numberLoad: 1)
        }
        
        
        // чтение переменных из памяти
        for item in savingParametrsMassString
        {
            if (item.key == SensorsViewController.sampleGattAttributes.DRIVER_NUM_STRING) {
                print("Получили данные из характеристики DRIVER_VERSION_NEW_VM чтение переменных из памяти строкой: " + item.value + "  " + String(item.value.count))
                if (item.value.count >= 5) {
                    if let number = Int(item.value[1..<2] + item.value[3..<5]) {
                        if (number >= 237) {
                            SensorsViewController.versionDriverGreaterThan237 = true
                        } else {
                            loopGestures.isHidden = true
                        }
                        print("Получили данные из характеристики DRIVER_VERSION_NEW_VM number = \(number)")
                    }
                }
                if (item.value.count == 3 ) {
                    //значит версия драйвера передаётся одним байтом
                    print("Получили данные из характеристики DRIVER_VERSION_NEW_VM чтение переменных из памяти: " + String((Float(item.value) ?? 1)/100))
                    
                    driverText.text = NSLocalizedString("driver", comment: "") + String((Float(item.value) ?? 1)/100)
                    SensorsViewController.versionDriverNum =  (Float(item.value) ?? 1)/100
                } else {
                    //значит версия драйвера передаётся строкой
                    print("Получили данные из характеристики DRIVER_VERSION_NEW_VM чтение переменных из памяти строкой 2: " + String((Float(item.value) ?? 1)/100))
                    driverText.text = NSLocalizedString("driver_without_v", comment: "") + item.value
                    SensorsViewController.versionDriverNum =  Float(item.value.parseToInt() ?? 25)/100
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.BMS_NUM) {
                bmsText.text = NSLocalizedString("bms", comment: "") + String(Float(item.value)!/100)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SENS_NUM) {
                sensersText.text = NSLocalizedString("sensors", comment: "") + String(Float(item.value)!/100)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.OPEN_THRESHOLD_HDLE) {
                openThreshold.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.CLOSE_THRESHOLD_HDLE) {
                closeThreshold.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"1") {
                openSensNum.text = String(255 - Int(item.value)!)
                openSensSlide.setValue(255 - Float(item.value)!, animated: true)
                open_sens_slide = UInt8(Int(item.value)!)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SENS_OPTIONS+"2") {
                closeSensNum.text = String(255 - Int(item.value)!)
                closeSensSlide.setValue(255 - Float(item.value)!, animated: true)
                close_sens_slide = UInt8(Int(item.value)!)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SET_REVERSE) {
                if (Int(item.value) == 1) {
                    swapSensText.text = NSLocalizedString("on", comment: "")
                    swapSensSwitch.setOn(true, animated: true)
                } else {
                    swapSensText.text = NSLocalizedString("off", comment: "")
                    swapSensSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SETTINGS_BLOKING) {
                if (Int(item.value) == 1) {
                    bloskSettingsText.text = NSLocalizedString("on", comment: "")
                    blockChangeSettings = true
                    blockSettingsSwitch.setOn(true, animated: true)
                } else {
                    bloskSettingsText.text = NSLocalizedString("off", comment: "")
                    blockChangeSettings = false
                    blockSettingsSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE) {
                if (Int(item.value) == 1) {
                    swapButtonOpenClose = true
                } else {
                    swapButtonOpenClose = false
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.READ_THREAD_START) {
                if (Int(item.value) == 1) {
                    pauseReadData = true
                } else {
                    pauseReadData = false
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.DEVICE_NAME){
                if (savingDeviceName != item.value) {
                    savingDeviceName = item.value
                    deviceName.text = NameUtil().getCleanName(deviceName: savingDeviceName)
                    if ( savingDeviceName.contains("FEST-A") || savingDeviceName.contains("BT05") || savingDeviceName.contains("Redmi") ||
                            savingDeviceName.contains("FEST-F") || savingDeviceName.contains(SensorsViewController.sampleGattAttributes.FESTH_NAME) || savingDeviceName.contains(SensorsViewController.sampleGattAttributes.FESTX_NAME)) {
                        typeMultigrib = true
                        if(savingDeviceName.contains(SensorsViewController.sampleGattAttributes.FESTX_NAME)) {
                            print("FEST-X activated")
                            typeMultigribNewVM = true
                            saveDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB_FESTX, value: String(1))
                        }
                        if(savingDeviceName.contains(SensorsViewController.sampleGattAttributes.FESTH_NAME)) {
                            typeMultigribNew = true
                            saveDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB_FESTH, value: String(1))
                        }
                        saveDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB, value: SensorsViewController.sampleGattAttributes.USE)
                        loadDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB, numberLoad: 2)
                        print("Multigrib mode activated!")
                    } else {
                        print("Mono mode activated!")
                        saveDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB, value: String(0))
                        loadDataString(key: SensorsViewController.sampleGattAttributes.USE_MULTIGRAB, numberLoad: 2)
                        gestureSettingsBtn.isHidden = true
                    }
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.OPEN_ADVANCED_SETTINGS) {
                if (Int(item.value) != 1) {
                    lockAdvancedSettings = true
                    advancedSettingsBtn.isHidden = true
                } else {
                    lockAdvancedSettings = false
                    advancedSettingsBtn.isHidden = false
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SCALE_FIRST_SET) {
                
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_START_GESTURE) {
                startGestureNum.text = String((Int(item.value) ?? 0) + 1)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_END_GESTURE) {
                endGestureNum.text = String((Int(item.value) ?? 0) + 1)
            }
            if (item.key == SensorsViewController.sampleGattAttributes.GESTURE_USE_NUM) {
                activeGestureNum.text = String((Int(item.value) ?? 0))
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SWITCH_BY_SENSORS) {
                if (Int(item.value) == 1) {
                    UIView.animate(withDuration: 0.3, animations: {
                        self.loopGestures.alpha = 1.0
                    })
                } else {
                    UIView.animate(withDuration: 0.3, animations: {
                        self.loopGestures.alpha = 0.0
                    })
                }
            }
        }
        
        openThresholdView.transform = CGAffineTransform.init(translationX: 0, y: -(CGFloat(openThreshold.value)/1.05))
        closeThresholdView.transform = CGAffineTransform.init(translationX: 0, y: -(CGFloat(closeThreshold.value)/1.05))
    }
    private func loadDataString(key: String, numberLoad: Int) {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        print("load number: \(numberLoad)   key: \(key)")
        for item in savingParametrsMassString {
            if (item.key == key) {
                print("load   key: \(item.key) value: \(item.value)")
            }
        }
    }
    private func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
    
    
    // MARK: - очередь команд
    @objc static func myInteractiveQueueComand(dataForWrite: Data, characteristic: String, type: String, myCase: String) {
        inactiveQueue.async {
            self.semafore.wait()
            self.flagReadData = false
            self.comandQueue(dataForWrite: dataForWrite, characteristic: characteristic, type: type, myCase: myCase)
            self.semafore.signal()
        }
        inactiveQueue.activate()
    }
    // MARK:  функция при вызове начинает генерировать команды
    // MARK:  на чтение данных и добавлять их в очередь команд
    func myInteractiveQueueReadData(dataForWrite: Data, characteristic: String, type: String, myCase: String) {
        SensorsViewController.inactiveQueue.async { [self] in
            while (SensorsViewController.flagReadData && pauseReadData) {
                usleep(delayForReadData)
                if (!typeMultigribNew) {
                    if (!typeMultigribNewVM) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: dataForWrite, characteristic: characteristic, type: type, myCase: myCase)
                    }
                }
            }
        }
        SensorsViewController.inactiveQueue.activate()
    }
    static let operationQueue = OperationQueue()
    static func comandQueue(dataForWrite: Data, characteristic: String, type: String, myCase: String) {
        let operation1 = BlockOperation() { [self] in
            usleep(queueInterval)
            //TODO код запроса информационных данных
            if (type == sampleGattAttributes.WRITE) {
//                print("write to FEST-H or FEST-X")
                sendDataToFESTH(dataForWrite: dataForWrite, characteristic: characteristic)
            }
            if (type == sampleGattAttributes.WRITE_HC10) {
//                print("write to HC10")
                sendDataToHC10 (dataForWrite: dataForWrite, characteristic: characteristic, myCase: myCase)
            } else if (type == sampleGattAttributes.READ) {
//                print("read")
                readDataFrom(characteristic: characteristic)
            }
        }
        operation1.completionBlock = { [self] in
            if (!flagReadData) {
                flagReadData = true
            }
        }
        operationQueue.addOperations([operation1], waitUntilFinished: true)
    }
    
    
    // MARK: - отправка команд с проверкой
    @objc static func myInteractiveQueueComandWithConfirmation(dataForWrite: Data, characteristic: String, countRestart: Int) {
        inactiveQueue2.async {
            self.semafore2.wait()
            self.comandQueue2(dataForWrite: dataForWrite, characteristic: characteristic, countRestart: countRestart)
            self.semafore2.signal()
        }
        inactiveQueue2.activate()
    }
    // MARK:  функция при вызове начинает генерировать команды
    // MARK:  на чтение данных и дбавлять их в очередь команд
    static let operationQueue2 = OperationQueue()
    static func comandQueue2(dataForWrite: Data, characteristic: String, countRestart: Int) {
        let operation1 = BlockOperation() { [self] in
            //TODO код запроса информационных данных
            self.expectedReceiveConfirmation = 1
            let formedId = characteristic.prefix(8).suffix(4)
            var countRestartLocal = countRestart
            var countAttempt = 0
            self.expectedIdCommand = String(formedId)
            
            while(self.expectedReceiveConfirmation != 2) {
                if (countRestartLocal > 0) {
                    
                    sendDataToFESTH(dataForWrite: dataForWrite, characteristic: characteristic)
                    usleep(100000)
                    countRestartLocal -= 1
                    countAttempt += 1
                    countAttempts = countAttempt
                    
                    if (countAttempt == 1) { oneAttempt += 1 }
                    if (countAttempt == 2) {
                        oneAttempt -= 1
                        twoAttempts += 1
                    }
                    if (countAttempt == 3) {
                        twoAttempts -= 1
                        threeAttempts += 1
                    }
                    if (countAttempt == 4) {
                        threeAttempts -= 1
                        fourAttempts += 1
                    }
                    if (countAttempt == 5) {
                        fourAttempts -= 1
                        fiveAttempts += 1
                    }
                    
                    //если протокол старый, то после отправки команды не ждём подтверждения
                    if (versionDriverNum < 2.34) {
                        countRestartLocal = 0
                    }
                } else {
                    return
                }
            }
        }
        operation1.completionBlock = {
            self.expectedReceiveConfirmation = 0
            print("==============================================================================")
            print("    one   attempt  = \(SensorsViewController.oneAttempt)    \n",
                  "    two   attempts = \(SensorsViewController.twoAttempts)   \n",
                  "    three attempts = \(SensorsViewController.threeAttempts) \n",
                  "    four  attempts = \(SensorsViewController.fourAttempts)  \n",
                  "    five  attempts = \(SensorsViewController.fiveAttempts)  \n",
                  "    all   attempts = \(SensorsViewController.countAttempts)   ")
            print("==============================================================================")
        }
        operationQueue2.addOperations([operation1], waitUntilFinished: true)
    }
}

extension Notification.Name {
    static let notificationReseiveBLEData = Notification.Name(rawValue: "notificationReseiveBLEData")
}

extension Notification.Name {
    static let notificationCheckStateConnection = Notification.Name(rawValue: "notificationCheckStateConnection")
}

extension Notification.Name {
    static let notificationUpdateAdvancedSettings = Notification.Name(rawValue: "notificationUpdateAdvancedSettings")
}

extension Notification.Name {
    static let notificationDataDialogToSensorsView = Notification.Name(rawValue: "notificationDataDialogToSensorsView")
}

extension Notification.Name {
    static let oldMotoricaStartDidRequestMergedScan = Notification.Name(rawValue: "OldMotoricaStart.didRequestMergedScan")
}

extension Data {
    /// A hexadecimal string representation of the bytes.
    func hexEncodedString() -> String {
        let hexDigits = Array("0123456789abcdef".utf16)
        var hexChars = [UTF16.CodeUnit]()
        hexChars.reserveCapacity(count * 2)
        
        for byte in self {
            let (index1, index2) = Int(byte).quotientAndRemainder(dividingBy: 16)
            hexChars.append(hexDigits[index1])
            hexChars.append(hexDigits[index2])
        }
        
        return String(utf16CodeUnits: hexChars, count: hexChars.count)
    }
}

extension String {
    subscript(_ range: CountableRange<Int>) -> String {
        let start = index(startIndex, offsetBy: max(0, range.lowerBound))
        let end = index(start, offsetBy: min(self.count - range.lowerBound,
                                             range.upperBound - range.lowerBound))
        return String(self[start..<end])
    }

    subscript(_ range: CountablePartialRangeFrom<Int>) -> String {
        let start = index(startIndex, offsetBy: max(0, range.lowerBound))
         return String(self[start...])
    }
}
