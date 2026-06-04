//
//  AdvancedSettinsFesthViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.02.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
import UIKit

class AdvencedSettingsFesthViewController: UIViewController {
    
    @IBOutlet var conteinerView: UIView!
    @IBOutlet weak var deviceName: UILabel!
    @IBOutlet weak var statusImage: UIImageView!
    @IBOutlet weak var shutdownCurrentSlide: UISlider!
    @IBOutlet weak var shutdownCurrentSlide2: UISlider!
    @IBOutlet weak var shutdownCurrentSlide3: UISlider!
    @IBOutlet weak var shutdownCurrentSlide4: UISlider!
    @IBOutlet weak var shutdownCurrentSlide5: UISlider!
    @IBOutlet weak var shutdownCurrentSlide6: UISlider!
    @IBOutlet weak var shutdownCurrentNum: UILabel!
    @IBOutlet weak var shutdownCurrentNum2: UILabel!
    @IBOutlet weak var shutdownCurrentNum3: UILabel!
    @IBOutlet weak var shutdownCurrentNum4: UILabel!
    @IBOutlet weak var shutdownCurrentNum5: UILabel!
    @IBOutlet weak var shutdownCurrentNum6: UILabel!
    @IBOutlet weak var swapBtnSwitch: UISwitch!
    @IBOutlet weak var swapBtnText: UILabel!
    @IBOutlet weak var singleChannelControlSwitch: UISwitch!
    @IBOutlet weak var singleChannelControlText: UILabel!
    @IBOutlet weak var prosthesisBlockingDescription: UILabel!
    @IBOutlet weak var prosthesisBlockingText: UILabel!
    @IBOutlet weak var prosthesisBlockingSwitch: UISwitch!
    @IBOutlet weak var timeForBlockingNum: UILabel!
    @IBOutlet weak var timeForBlockingSlide: UISlider!
    @IBOutlet weak var timeForBlockingStack: UIStackView!
    @IBOutlet weak var switchingBySensorsSwitch: UISwitch!
    @IBOutlet weak var switchingBySensorsText: UILabel!
    @IBOutlet weak var mode: UISegmentedControl!
    @IBOutlet weak var peakTimeStack: UIStackView!
    @IBOutlet weak var peakTimeSlide: UISlider!
    @IBOutlet weak var peakTimeNum: UILabel!
    @IBOutlet weak var timeBetweenPeaksStack: UIStackView!
    @IBOutlet weak var timeBetweenPeaksSlide: UISlider!
    @IBOutlet weak var timeBetweenPeaksNum: UILabel!
    @IBOutlet weak var timeAtRestStack: UIStackView!
    @IBOutlet weak var timeAtRestSlide: UISlider!
    @IBOutlet weak var timeAtRestNum: UILabel!
    @IBOutlet weak var handSideSwitch: UISwitch!
    @IBOutlet weak var handSideText: UILabel!
    @IBOutlet weak var smartConnectionControlSwitch: UISwitch!
    @IBOutlet weak var smartConnectionControlText: UILabel!
    @IBOutlet weak var versionAppText: UILabel!
    
    @IBOutlet weak var fingersDelayDescription: UILabel!
    @IBOutlet weak var fingersDelayText: UILabel!
    @IBOutlet weak var fingersDelaySwitch: UISwitch!
    @IBOutlet weak var writeSerialNumber: UIButton!
    @IBOutlet weak var hardResetToFactorySettings: UIButton!
    @IBOutlet weak var softResetToFactorySettings: UIButton!
    @IBOutlet weak var calibration: UIButton!
    @IBOutlet weak var calibrationStatus: UIButton!
    @IBOutlet weak var spaceBeforeHandSide: NSLayoutConstraint!
    @IBOutlet weak var spaceBeforeGestureSwitchingBySensors: NSLayoutConstraint!
    @IBOutlet weak var spaceBeforeSmartConnection: NSLayoutConstraint!
    @IBOutlet weak var serialNumberTextField: UITextField!
    @IBOutlet weak var activeGesturesSwapText: UILabel!
    @IBOutlet weak var activeGesturesSwapSwitch: UISwitch!
    
    private let sampleGattAttributes = SampleGattAttributes()
    private var dataForCommunicate = ["byteArray": "01020304", "characteristic":"1", "type":"READ", "case":"0"]
    private var dataForAdvancedSettingsViewController = ["deviceName":"lol"]
    private var savingParametrsMassString:[SaveObjectString]!
    private var savingDeviceName: String = "...."
    let connectStatus = UIImage(named:"connect_status")!
    let disconnectStatus = UIImage(named:"disconnect_status")!
    var switchBySensors: UInt8 = 0x00
    var switchProsthesisBlocking: UInt8 = 0x00
    var modeInt: UInt8 = 0x00
    private var typeMultigribNewVM: Bool = false
    
    private var startGestureInLoopNum = 0
    private var endGestureInLoopNum = 0
    private var gestureUseNum = 0
    private var setReverse = 0
    private var setModeProsthesis = 0
    private var maxStandCycles = 0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        loadDataString()
        initUI()
        
        
        //тест
//        performSegue(withIdentifier: "goToSetSerialNumber", sender: nil)
        
    
        NotificationCenter.default.addObserver(self, selector: #selector(checkStateConnection), name: .notificationCheckStateConnection, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(resultDialog), name: .notificationDataDialogs, object: nil)
    }
    @objc func checkStateConnection(notification: Notification) {
            guard let dataState = notification.userInfo,
                let state = dataState["state"] as? String
            else { return }
            print("peredan state: \(state)")
            if (state == "did Connect") {
                statusImage.image = connectStatus
            }
            if (state == "did DISConnect") {
                statusImage.image = disconnectStatus
            }
        }
    @objc func resultDialog(notification: Notification) {
        guard let dataState = notification.userInfo,
              let resultDialog = dataState["resultDialog"] as? String
        else {
            print("recalibrationResult else return")
            return
        }
        savingParametrsMassString = [SaveObjectString]()
                savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
                for item in savingParametrsMassString {
                    if (item.key == sampleGattAttributes.HAND_SIDE) {
                        if (resultDialog == "recalibrationCancel") {
                            if (Int(item.value) == 1) {
                                print("recalibrationResult cancel 1")
                                handSideSwitch.isOn = false
                                handSideText.text = NSLocalizedString("left", comment: "")
                                saveDataString(key: sampleGattAttributes.HAND_SIDE, value: String(0))
                            } else {
                                print("recalibrationResult cancel 0")
                                handSideSwitch.isOn = true
                                handSideText.text = NSLocalizedString("right", comment: "")
                                saveDataString(key: sampleGattAttributes.HAND_SIDE, value: String(1))
                            }
                        }
                        if (resultDialog == "recalibrationAccept") {
                            startCalibration()
                        }
                    }
                }
        
        if (resultDialog == "calibrationCancel") {
            print("calibrationResult cancel")
        }
        if (resultDialog == "calibrationAccept") {
            print("calibrationResult accept")
            startCalibration()
        }
        
        if (resultDialog == "hardResetCancel") {
            print("hardResetResult cancel")
        }
        if (resultDialog == "hardResetAccept") {
            print("hardResetResult accept")
            startReset(translateByte: Data([0x01]))
        }
        if (resultDialog == "softResetCancel") {
            print("softResetResult cancel")
        }
        if (resultDialog == "softResetAccept") {
            print("softResetResult accept")
            startReset(translateByte: Data([0x02]))
        }
        if (resultDialog == "passwordAccept") {
            print("пароль passwordResult accept")
            performSegue(withIdentifier: "goToSetSerialNumber", sender: nil)
        }
        if (resultDialog == "passwordCancel") {
            self.showToast(message:"пароль не верен", font: .systemFont(ofSize: 12.0))
        }
        if (resultDialog == "writeSerialNumberAccept") {
            print("passwordResult accept")
            startWriteSerialNumber()
        }
    }
    private func requestParametersAdvancedSettings() {
        if (typeMultigribNewVM) {
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
        } else {
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
    }

    
     // MARK: - обработка взаимодействия с UI
    @IBAction func perehod(_ sender: Any) {
        requestParametersAdvancedSettings()
        saveDataString(key: sampleGattAttributes.READ_THREAD_START, value: String(1))
    }
    @IBAction func shutdownCurrent1Slide(_ sender: UISlider) {
        shutdownCurrentNum.text = String(Int(sender.value))
    }
    @IBAction func shutdownCurrent1SlideStop(_ sender: UISlider) {
        let data = Data([UInt8(shutdownCurrentSlide.value), UInt8(shutdownCurrentSlide2.value), UInt8(shutdownCurrentSlide3.value), UInt8(shutdownCurrentSlide4.value), UInt8(shutdownCurrentSlide5.value), UInt8(shutdownCurrentSlide6.value)])
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SHUTDOWN_CURRENT_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_1, value: String(Int(sender.value)))
    }
    @IBAction func shutdownCurrent2Slide(_ sender: UISlider) {
        shutdownCurrentNum2.text = String(Int(sender.value))
    }
    @IBAction func shutdownCurrent2SlideStop(_ sender: UISlider) {
        let data = Data([UInt8(shutdownCurrentSlide.value), UInt8(shutdownCurrentSlide2.value), UInt8(shutdownCurrentSlide3.value), UInt8(shutdownCurrentSlide4.value), UInt8(shutdownCurrentSlide5.value), UInt8(shutdownCurrentSlide6.value)])
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SHUTDOWN_CURRENT_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_2, value: String(Int(sender.value)))
    }
    @IBAction func shutdownCurrent3Slide(_ sender: UISlider) {
        shutdownCurrentNum3.text = String(Int(sender.value))
    }
    @IBAction func shutdownCurrent3SlideStop(_ sender: UISlider){
        let data = Data([UInt8(shutdownCurrentSlide.value), UInt8(shutdownCurrentSlide2.value), UInt8(shutdownCurrentSlide3.value), UInt8(shutdownCurrentSlide4.value), UInt8(shutdownCurrentSlide5.value), UInt8(shutdownCurrentSlide6.value)])
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SHUTDOWN_CURRENT_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_3, value: String(Int(sender.value)))
    }
    @IBAction func shutdownCurrent4Slide(_ sender: UISlider) {
        shutdownCurrentNum4.text = String(Int(sender.value))
    }
    @IBAction func shutdownCurrent4SlideStop(_ sender: UISlider) {
        let data = Data([UInt8(shutdownCurrentSlide.value), UInt8(shutdownCurrentSlide2.value), UInt8(shutdownCurrentSlide3.value), UInt8(shutdownCurrentSlide4.value), UInt8(shutdownCurrentSlide5.value), UInt8(shutdownCurrentSlide6.value)])
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SHUTDOWN_CURRENT_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_4, value: String(Int(sender.value)))
    }
    @IBAction func shutdownCurrent5Slide(_ sender: UISlider) {
        shutdownCurrentNum5.text = String(Int(sender.value))
    }
    @IBAction func shutdownCurrent5SlideStop(_ sender: UISlider) {
        let data = Data([UInt8(shutdownCurrentSlide.value), UInt8(shutdownCurrentSlide2.value), UInt8(shutdownCurrentSlide3.value), UInt8(shutdownCurrentSlide4.value), UInt8(shutdownCurrentSlide5.value), UInt8(shutdownCurrentSlide6.value)])
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SHUTDOWN_CURRENT_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_5, value: String(Int(sender.value)))
    }
    @IBAction func shutdownCurrent6Slide(_ sender: UISlider) {
        shutdownCurrentNum6.text = String(Int(sender.value))
    }
    @IBAction func shutdownCurrent6SlideStop(_ sender: UISlider) {
        let data = Data([UInt8(shutdownCurrentSlide.value), UInt8(shutdownCurrentSlide2.value), UInt8(shutdownCurrentSlide3.value), UInt8(shutdownCurrentSlide4.value), UInt8(shutdownCurrentSlide5.value), UInt8(shutdownCurrentSlide6.value)])
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SHUTDOWN_CURRENT_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_6, value: String(Int(sender.value)))
    }
    @IBAction func swapBtnSwitch(_ sender: UISwitch) {
        if (sender.isOn) {
            swapBtnText.text = NSLocalizedString("on", comment: "")
            saveDataString(key: sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE, value: String(1))
        } else {
            swapBtnText.text = NSLocalizedString("off", comment: "")
            saveDataString(key: sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE, value: String(0))
        }
    }
    @IBAction func singleChannelControlSwitch(_ sender: UISwitch) {
        if (sender.isOn) {
            singleChannelControlText.text = NSLocalizedString("on", comment: "")
            let data = Data([0x01])
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL_NEW_VM, countRestart: 50)
            } else {
                SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SET_ONE_CHANNEL_NEW, type: sampleGattAttributes.WRITE, myCase: "")
            }
            saveDataString(key: sampleGattAttributes.SET_ONE_CHANNEL_NEW, value: String(1))
        } else {
            singleChannelControlText.text = NSLocalizedString("off", comment: "")
            let data = Data([0x00])
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.SET_ONE_CHANNEL_NEW_VM, countRestart: 50)
            } else {
                SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.SET_ONE_CHANNEL_NEW, type: sampleGattAttributes.WRITE, myCase: "")
            }
            saveDataString(key: sampleGattAttributes.SET_ONE_CHANNEL_NEW, value: String(0))
        }
    }
    @IBAction func prosthesisBlockingSwitch(_ sender: UISwitch) {
        let data: Data
        if (sender.isOn) {
            prosthesisBlockingText.text = NSLocalizedString("on", comment: "")
            switchProsthesisBlocking = 0x01
            data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value)])
            saveDataString(key: sampleGattAttributes.PROSTHESIS_BLOCKING, value: String(1))
            animatedShowOfFestxBlockingParameners()
        } else {
            prosthesisBlockingText.text = NSLocalizedString("off", comment: "")
            switchProsthesisBlocking = 0x00
            data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value)])
            saveDataString(key: sampleGattAttributes.PROSTHESIS_BLOCKING, value: String(0))
            animatedHideOfFestxBlockingParameners()
        }
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
    }
    @IBAction func timeForBlockingSlide(_ sender: UISlider) {
        timeForBlockingNum.text = String(Float(Int(sender.value)+1)/10) + NSLocalizedString("_s", comment: "")
    }
    @IBAction func timeForBlockingSlideStop(_ sender: UISlider) {
        let data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value)])
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
        saveDataString(key: sampleGattAttributes.TIME_FOR_BLOCKING, value: String(Int(sender.value)))
    }
    @IBAction func switchingBySensorsSwitch(_ sender: UISwitch) {
        let data: Data
        if (typeMultigribNewVM) {
            if (sender.isOn) {
                switchingBySensorsText.text = NSLocalizedString("on", comment: "")
                switchBySensors = 0x01
                animatedShowOfFestxTimeParameters()
            } else {
                switchingBySensorsText.text = NSLocalizedString("off", comment: "")
                switchBySensors = 0x00
                animatedHideOfFestxTimeParameters()
            }
            saveDataString(key: sampleGattAttributes.SWITCH_BY_SENSORS, value: String(Int(switchBySensors)))
            if (SensorsViewController.versionDriverGreaterThan237) {
                data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value)])
            } else {
                data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value)])
            }
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
        } else {
            if (sender.isOn) {
                switchingBySensorsText.text = NSLocalizedString("on", comment: "")
                switchBySensors = 0x01
                data = Data([switchBySensors, modeInt, UInt8(peakTimeSlide.value), UInt8(timeBetweenPeaksSlide.value)])
                saveDataString(key: sampleGattAttributes.SWITCH_BY_SENSORS, value: String(1))
                animatedShowOfFesthTimeParameters()
            } else {
                switchingBySensorsText.text = NSLocalizedString("off", comment: "")
                switchBySensors = 0x00
                data = Data([switchBySensors, modeInt, UInt8(peakTimeSlide.value), UInt8(timeBetweenPeaksSlide.value)])
                saveDataString(key: sampleGattAttributes.SWITCH_BY_SENSORS, value: String(0))
                animatedHideOfFesthTimeParameters()
            }
            SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.ROTATION_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
    }
    @IBAction func chengeMode(_ sender: UISegmentedControl) {
        modeInt = UInt8(sender.selectedSegmentIndex)
        let data = Data([switchBySensors, modeInt, UInt8(peakTimeSlide.value), UInt8(timeBetweenPeaksSlide.value)])
        SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.ROTATION_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        saveDataString(key: sampleGattAttributes.SET_MODE, value: String(sender.selectedSegmentIndex))
    }
    @IBAction func timeAtRestSlide(_ sender: UISlider) {
        timeAtRestNum.text = String(Float(Int(timeAtRestSlide.value)+1)/10)+NSLocalizedString("_s", comment: "")
    }
    @IBAction func timeAtRestSlideStop(_ sender: UISlider) {
        let data: Data
        if (SensorsViewController.versionDriverGreaterThan237) {
            data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value), UInt8(startGestureInLoopNum), UInt8(endGestureInLoopNum)])
        } else {
            data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value)])
        }
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
        saveDataString(key: sampleGattAttributes.TIME_AT_REST, value: String(Int(sender.value)))
    }
    @IBAction func peakTimeSlide(_ sender: UISlider) {
        if (Int(sender.value) % 2 == 1) {
            peakTimeNum.text = String(Float((Int(sender.value)+5)*5)/100)+"0"+NSLocalizedString("_s", comment: "")
        } else {
            peakTimeNum.text = String(Float((Int(sender.value)+5)*5)/100)+NSLocalizedString("_s", comment: "")
        }
    }
    @IBAction func peakTimeSlideStop(_ sender: UISlider) {
        let data = Data([switchBySensors, modeInt, UInt8(peakTimeSlide.value+5), UInt8(timeBetweenPeaksSlide.value+5)])
        SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.ROTATION_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        saveDataString(key: sampleGattAttributes.PEAK_TIME, value: String(Int(sender.value)))
    }
    @IBAction func timeBetweenPeaksSlide(_ sender: UISlider) {
        if (Int(sender.value) % 2 == 1) {
            timeBetweenPeaksNum.text = String(Float((Int(sender.value)+5)*5)/100)+"0"+NSLocalizedString("_s", comment: "")
        } else {
            timeBetweenPeaksNum.text = String(Float((Int(sender.value)+5)*5)/100)+NSLocalizedString("_s", comment: "")
        }
    }
    @IBAction func timeBetweenPeaksSlideStop(_ sender: UISlider) {
        let data = Data([switchBySensors, modeInt, UInt8(peakTimeSlide.value+5), UInt8(timeBetweenPeaksSlide.value+5)])
        SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.ROTATION_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        saveDataString(key: sampleGattAttributes.TIME_BETWEEN_PEAKS, value: String(Int(sender.value)))
    }
    @IBAction func handSideSwitch(_ sender: UISwitch) {
        if (sender.isOn) {
            handSideText.text = NSLocalizedString("right", comment: "")
            saveDataString(key: sampleGattAttributes.HAND_SIDE, value: String(1))
        } else {
            handSideText.text = NSLocalizedString("left", comment: "")
            saveDataString(key: sampleGattAttributes.HAND_SIDE, value: String(0))
        }
    }
    @IBAction func fingersDelaySwitch(_ sender: UISwitch) {
        if (sender.isOn) {
            fingersDelayText.text = NSLocalizedString("on", comment: "")
            saveDataString(key: sampleGattAttributes.FINGERS_DELAY_SWITCH, value: String(1))
        } else {
            fingersDelayText.text = NSLocalizedString("off", comment: "")
            saveDataString(key: sampleGattAttributes.FINGERS_DELAY_SWITCH, value: String(0))
        }
    }
    @IBAction func smartConnectionControlSwitch(_ sender: UISwitch) {
        if (sender.isOn) {
            smartConnectionControlText.text = NSLocalizedString("on", comment: "")
        } else {
            smartConnectionControlText.text = NSLocalizedString("off", comment: "")
        }
        LegacySmartConnectionStateStore.setEnabled(sender.isOn)
    }
    @IBAction func reset(_ sender: UIButton) {}
    @IBAction func calibration(_ sender: UIButton) {}
    @IBAction func startWriteSerialNumber(_ sender: UIButton) {
        print("startWriteSerialNumber "+NameUtil().validationAndConversionSerialNumber(serialNumber: serialNumberTextField.text ?? "null"))
        self.view.endEditing(true)
        if (NameUtil().validationAndConversionSerialNumber(serialNumber: serialNumberTextField.text ?? "null") == "false") {
            print("Error: "+NameUtil().getvalidationError(serialNumber: serialNumberTextField.text ?? "null"))
            self.showToast(message: NameUtil().getvalidationError(serialNumber: serialNumberTextField.text ?? "null"), font: .systemFont(ofSize: 12.0))
        } else {
            performSegue(withIdentifier: "goToPassword", sender: nil)
        }
    }
    private func startWriteSerialNumber() {
        print("пароль startWriteSerialNumber "+NameUtil().validationAndConversionSerialNumber(serialNumber: serialNumberTextField.text ?? "null"))
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data(NameUtil().validationAndConversionSerialNumber(serialNumber: serialNumberTextField.text ?? "null").utf8), characteristic: SensorsViewController.sampleGattAttributes.SERIAL_NUMBER_NEW_VM, countRestart: 50)
    }
    private func startCalibration() {
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.HAND_SIDE) {
                if (Int(item.value) == 1) {
                    let data = Data([0x09])
                    if (typeMultigribNewVM) {
                        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CALIBRATION_NEW_VM, countRestart: 50)
                    } else {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.CALIBRATION_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    }
                } else {
                    let data = Data([0x0a])
                    if (typeMultigribNewVM) {
                        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.CALIBRATION_NEW_VM, countRestart: 50)
                    } else {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: data, characteristic: sampleGattAttributes.CALIBRATION_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    }
                }
            }
        }
    }
    private func startReset(translateByte: Data) {
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: translateByte, characteristic: SensorsViewController.sampleGattAttributes.RESET_TO_FACTORY_SETTINGS_NEW_VM, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: translateByte, characteristic: sampleGattAttributes.RESET_TO_FACTORY_SETTINGS_NEW, type: sampleGattAttributes.WRITE, myCase: "")
        }
        
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_1, value: String(80))

        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_2, value: String(80))

        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_3, value: String(80))

        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_4, value: String(80))

        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_5, value: String(80))

        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_NEW_6, value: String(80))

        saveDataString(key: sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE, value: String(0))

        saveDataString(key: sampleGattAttributes.SET_ONE_CHANNEL_NEW, value: String(0))

//        saveDataString(key: sampleGattAttributes.SWITCH_BY_SENSORS, value: String(0))

        saveDataString(key: sampleGattAttributes.SET_MODE, value: String(-1))

        saveDataString(key: sampleGattAttributes.PEAK_TIME, value: String(15))

        saveDataString(key: sampleGattAttributes.TIME_BETWEEN_PEAKS, value: String(15))

        saveDataString(key: sampleGattAttributes.HAND_SIDE, value: String(0))
        
        loadDataString()
        initUI()
        
        requestParametersAdvancedSettings()
        print("ресет параметров")
        DispatchQueue.main.asyncAfter(deadline: .now() + 2)
        {
            print("обновление данных UI после ресета")
            self.loadDataString()
            self.initUI()
        }
    }
    @IBAction func activeGesturesSwapSwitch(_ sender: UISwitch) {
//        loadDataString()
//        initUI()
        
        var numActiveGestures = 8
        if (sender.isOn) {
            numActiveGestures = 14
        } else {
            //ограничиваем диапазон старт/стопа ргуппы ротации
            print("test gestures in loop  ASF startGestureInLoopNum=\(startGestureInLoopNum)  activeGestures - 1 =\(numActiveGestures - 1)")
            if (startGestureInLoopNum >= numActiveGestures) {
                startGestureInLoopNum = (numActiveGestures - 1)
                saveDataString(key: sampleGattAttributes.SELECTED_START_GESTURE, value: String(startGestureInLoopNum))
            }
            print("test gestures in loop  ASF endGestureInLoopNum=\(endGestureInLoopNum)  activeGestures - 1 =\(numActiveGestures - 1)")
            if (endGestureInLoopNum >= numActiveGestures) {
                endGestureInLoopNum = (numActiveGestures - 1)
                saveDataString(key: sampleGattAttributes.SELECTED_END_GESTURE, value: String(endGestureInLoopNum))
            }


            //если работающий в данный момент жест больше 8 то он устанавливается на 8
            if (gestureUseNum >= numActiveGestures) {
                //тут отправлять команду выбора работающего в данный момент жеста
                setUseGesture(numUseGesture: numActiveGestures)
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(numActiveGestures))
            }
        }
        activeGesturesSwapText.text = numActiveGestures.description
        saveDataString(key: sampleGattAttributes.NUM_ACTIVE_GESTURES, value: numActiveGestures.description)
        sendActiveGestures(numActiveGestures: UInt8(numActiveGestures))
        sendGestureRotation()
    }
    private func setUseGesture(numUseGesture: Int) {
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([UInt8(numUseGesture-1)]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
        }
    }
    private func sendActiveGestures(numActiveGestures: UInt8) {
        loadDataString()
        initUI()
        
        if (typeMultigribNewVM) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([UInt8(setReverse), numActiveGestures, UInt8(setModeProsthesis), UInt8(maxStandCycles-Int(UInt8(maxStandCycles/256))*256), UInt8(maxStandCycles/256)]), characteristic: sampleGattAttributes.SET_REVERSE_NEW_VM, countRestart: 50)
        }
    }
    private func sendGestureRotation() {
        loadDataString()
        initUI()
        
        
        let data: Data
        if (SensorsViewController.versionDriverGreaterThan237) {
            data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value), UInt8(startGestureInLoopNum), UInt8(endGestureInLoopNum)])
        } else {
            data = Data([switchBySensors, 0x00, UInt8(timeAtRestSlide.value), 0x00, switchProsthesisBlocking, UInt8(timeForBlockingSlide.value)])
        }
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
    }
    
    
    private func initUI() {
        setupeButton(button: writeSerialNumber)
        setupeButton(button: hardResetToFactorySettings)
        setupeButton(button: softResetToFactorySettings)
        setupeButton(button: calibration)
        setupeButton(button: calibrationStatus)
        setupeSecetView(segmentedControl: mode)
        versionAppText.text = Bundle.main.infoDictionary!["CFBundleShortVersionString"] as? String
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.DEVICE_NAME) {
                        deviceName.text = NameUtil().getCleanName(deviceName: item.value)
            }
            if (item.key == sampleGattAttributes.STATUS_CONNECTION) {
                if (Int(item.value) == 1) {
                    statusImage.image = connectStatus
                } else {
                    statusImage.image = disconnectStatus
                }
            }
            if (item.key == sampleGattAttributes.SHUTDOWN_CURRENT_NEW_1) {
                shutdownCurrentNum.text = item.value
                shutdownCurrentSlide.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SHUTDOWN_CURRENT_NEW_2) {
                shutdownCurrentNum2.text = item.value
                shutdownCurrentSlide2.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SHUTDOWN_CURRENT_NEW_3) {
                shutdownCurrentNum3.text = item.value
                shutdownCurrentSlide3.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SHUTDOWN_CURRENT_NEW_4) {
                shutdownCurrentNum4.text = item.value
                shutdownCurrentSlide4.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SHUTDOWN_CURRENT_NEW_5) {
                shutdownCurrentNum5.text = item.value
                shutdownCurrentSlide5.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SHUTDOWN_CURRENT_NEW_6) {
                shutdownCurrentNum6.text = item.value
                shutdownCurrentSlide6.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE){
                if (Int(item.value) == 1) {
                    swapBtnText.text = NSLocalizedString("on", comment: "")
                    swapBtnSwitch.setOn(true, animated: true)
                } else {
                    swapBtnText.text = NSLocalizedString("off", comment: "")
                    swapBtnSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.SET_ONE_CHANNEL_NEW) {
                if (Int(item.value) == 1) {
                    singleChannelControlText.text = NSLocalizedString("on", comment: "")
                    singleChannelControlSwitch.setOn(true, animated: true)
                } else {
                    singleChannelControlText.text = NSLocalizedString("off", comment: "")
                    singleChannelControlSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.SMART_CONNECTION) {
                if (Int(item.value) == 1) {
                    smartConnectionControlText.text = NSLocalizedString("on", comment: "")
                    smartConnectionControlSwitch.setOn(true, animated: true)
                } else {
                    smartConnectionControlText.text = NSLocalizedString("off", comment: "")
                    smartConnectionControlSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.SWITCH_BY_SENSORS) {
                if (Int(item.value) == 1) {
                    switchingBySensorsText.text = NSLocalizedString("on", comment: "")
                    switchingBySensorsSwitch.setOn(true, animated: true)
                    switchBySensors = 0x01
                } else {
                    switchingBySensorsText.text = NSLocalizedString("off", comment: "")
                    switchingBySensorsSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.PROSTHESIS_BLOCKING) {
                if (Int(item.value) == 1) {
                    prosthesisBlockingText.text = NSLocalizedString("on", comment: "")
                    prosthesisBlockingSwitch.setOn(true, animated: true)
                    switchProsthesisBlocking = 0x01
                } else {
                    prosthesisBlockingText.text = NSLocalizedString("off", comment: "")
                    prosthesisBlockingSwitch.setOn(false, animated: true)
                }
            }
        
            if (item.key == sampleGattAttributes.TIME_FOR_BLOCKING) {
                timeForBlockingNum.text = String(Float(Int(item.value)!+1)/10)+NSLocalizedString("_s", comment: "")
                timeForBlockingSlide.setValue(Float(item.value)!, animated: true)
            }
            
            if (item.key == sampleGattAttributes.SET_MODE) {
                mode.selectedSegmentIndex = Int(item.value) ?? -1
            }
            if (item.key == sampleGattAttributes.PEAK_TIME) {
                if ((Int(item.value) ?? 0) % 2 == 1) {
                    peakTimeNum.text = String(Float(((Int(item.value) ?? 0)+5)*5)/100)+"0"+NSLocalizedString("_s", comment: "")
                } else {
                    peakTimeNum.text = String(Float(((Int(item.value) ?? 0)+5)*5)/100)+NSLocalizedString("_s", comment: "")
                }
                peakTimeSlide.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.TIME_BETWEEN_PEAKS) {
                if ((Int(item.value) ?? 0) % 2 == 1) {
                    timeBetweenPeaksNum.text = String(Float(((Int(item.value) ?? 0)+5)*5)/100)+"0"+NSLocalizedString("_s", comment: "")
                } else {
                    timeBetweenPeaksNum.text = String(Float(((Int(item.value) ?? 0)+5)*5)/100)+NSLocalizedString("_s", comment: "")
                }
                timeBetweenPeaksSlide.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.HAND_SIDE){
                if (Int(item.value) == 1) {
                    handSideText.text = NSLocalizedString("right", comment: "")
                    handSideSwitch.setOn(true, animated: true)
                } else {
                    handSideText.text = NSLocalizedString("left", comment: "")
                    handSideSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.FINGERS_DELAY_SWITCH){
                if (Int(item.value) == 1) {
                    fingersDelayText.text = NSLocalizedString("on", comment: "")
                    fingersDelaySwitch.setOn(true, animated: true)
                } else {
                    fingersDelayText.text = NSLocalizedString("off", comment: "")
                    fingersDelaySwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.USE_MULTIGRAB_FESTX) {
                if (Int(item.value) == 1) {
                    typeMultigribNewVM = true
                }
            }
            if (item.key == sampleGattAttributes.TIME_AT_REST) {
                timeAtRestNum.text = String(Float(Int(item.value)!+1)/10)+NSLocalizedString("_s", comment: "")
                timeAtRestSlide.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SELECTED_START_GESTURE) {
                startGestureInLoopNum = Int(item.value) ?? 0
            }
            if (item.key == sampleGattAttributes.SELECTED_END_GESTURE) {
                endGestureInLoopNum = Int(item.value) ?? 0
            }
            if (item.key == sampleGattAttributes.NUM_ACTIVE_GESTURES) {
                print("данные количества активных жестов \(Int(item.value))")
                if (Int(item.value) ?? 8 == 14) {
                    activeGesturesSwapText.text = "14"
                    activeGesturesSwapSwitch.setOn(true, animated: true)
                } else {
                    activeGesturesSwapText.text = "8"
                    activeGesturesSwapSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.GESTURE_USE_NUM) {
                gestureUseNum = Int(item.value) ?? 0
            }
            if (item.key == sampleGattAttributes.SET_REVERSE) {
                setReverse = Int(item.value) ?? 0
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SET_MODE_PROSTHESIS) {
                setModeProsthesis = Int(item.value) ?? 0
            }
            if (item.key == SensorsViewController.sampleGattAttributes.MAX_STAND_CYCLES) {
                maxStandCycles = Int(item.value) ?? 0
                print ("AS Получили данные max_stand_cycles chislo data[4]=\(UInt8(maxStandCycles/256))")
                print ("AS Получили данные max_stand_cycles chislo data[3]=\(maxStandCycles-Int(UInt8(maxStandCycles/256))*256)")
            }
        }
        if (typeMultigribNewVM) {
            mode.isHidden = true
            peakTimeStack.isHidden = true
            timeBetweenPeaksStack.isHidden = true
            if (switchingBySensorsSwitch.isOn) {
                spaceBeforeHandSide.constant = 66
            } else {
                timeAtRestStack.isHidden = true
                spaceBeforeHandSide.constant = 16
            }
            if (prosthesisBlockingSwitch.isOn) {
                spaceBeforeGestureSwitchingBySensors.constant = 112
            } else {
                timeForBlockingStack.isHidden = true
                spaceBeforeGestureSwitchingBySensors.constant = 62
            }
        } else {
            prosthesisBlockingDescription.isHidden = true
            prosthesisBlockingText.isHidden = true
            prosthesisBlockingSwitch.isHidden = true
            timeForBlockingStack.isHidden = true
            spaceBeforeGestureSwitchingBySensors.constant = 16
            
            fingersDelayDescription.isHidden = true
            fingersDelayText.isHidden = true
            fingersDelaySwitch.isHidden = true
            spaceBeforeSmartConnection.constant = 16
            
            calibrationStatus.isHidden = true
            timeAtRestStack.isHidden = true
            if (!switchingBySensorsSwitch.isOn) {
                mode.isHidden = true
                peakTimeStack.isHidden = true
                timeBetweenPeaksStack.isHidden = true
                spaceBeforeHandSide.constant = 16
            }
        }
    }
    
    private func animatedShowOfFesthTimeParameters() {
        animatedFesthParameters(duration: 0.3, modeAlpha: 1, peakTimeStackAlpha: 1, timeBetweenPeaksStackAlpha: 1, spaceBeforeHandSideSize: 166)
    }
    private func animatedHideOfFesthTimeParameters() {
        animatedFesthParameters(duration: 0.3, modeAlpha: 0, peakTimeStackAlpha: 0, timeBetweenPeaksStackAlpha: 0, spaceBeforeHandSideSize: 16)
    }
    private func animatedFesthParameters(duration : Double, modeAlpha : CGFloat, peakTimeStackAlpha : CGFloat, timeBetweenPeaksStackAlpha : CGFloat, spaceBeforeHandSideSize : CGFloat) {
        mode.isHidden = false
        peakTimeStack.isHidden = false
        timeBetweenPeaksStack.isHidden = false
        UIView.animate(withDuration: duration, animations: {
            self.mode.alpha = modeAlpha
            self.peakTimeStack.alpha = peakTimeStackAlpha
            self.timeBetweenPeaksStack.alpha = timeBetweenPeaksStackAlpha
            self.spaceBeforeHandSide.constant = spaceBeforeHandSideSize
            self.view.layoutIfNeeded()
        })
    }
    
    private func animatedShowOfFestxBlockingParameners() {
        animatedBlockingParameners(duration: 0.3, timeForBlockingStackAlpha: 1, spaceBeforeGestureSwitchingBySensors: 112)
    }
    private func animatedHideOfFestxBlockingParameners() {
        animatedBlockingParameners(duration: 0.3, timeForBlockingStackAlpha: 0, spaceBeforeGestureSwitchingBySensors: 62)
    }
    private func animatedBlockingParameners (duration : Double, timeForBlockingStackAlpha : CGFloat, spaceBeforeGestureSwitchingBySensors : CGFloat) {
        timeForBlockingStack.isHidden = false
        UIView.animate(withDuration: duration, animations: {
            self.timeForBlockingStack.alpha = timeForBlockingStackAlpha
            self.spaceBeforeGestureSwitchingBySensors.constant = spaceBeforeGestureSwitchingBySensors
            self.view.layoutIfNeeded()
        })
    }
    
    private func animatedShowOfFestxTimeParameters() {
        animatedFestxParameters(duration: 0.3, timeAtRestStack: 1, spaceBeforeHandSideSize: 66)
    }
    private func animatedHideOfFestxTimeParameters() {
        animatedFestxParameters(duration: 0.3, timeAtRestStack: 0, spaceBeforeHandSideSize: 16)
    }
    private func animatedFestxParameters(duration : Double, timeAtRestStack : CGFloat, spaceBeforeHandSideSize : CGFloat) {
        self.timeAtRestStack.isHidden = false
        UIView.animate(withDuration: duration, animations: {
            self.timeAtRestStack.alpha = timeAtRestStack
            self.spaceBeforeHandSide.constant = spaceBeforeHandSideSize
            self.view.layoutIfNeeded()
        })
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
        gradientLayer.frame = conteinerView.bounds
        conteinerView.layer.insertSublayer(gradientLayer, at: 0)
    }
    private func setupeButton (button: UIButton) {
        button.layer.cornerRadius = 21
        button.layer.borderWidth = 2
        button.layer.borderColor = UIColor.white.cgColor
    }
    private func setupeSecetView (segmentedControl: UISegmentedControl) {
        segmentedControl.layer.cornerRadius = 5
        segmentedControl.layer.borderWidth = 2
        segmentedControl.layer.borderColor = UIColor.white.cgColor
        let titleTextAttributes = [NSAttributedStringKey.foregroundColor: UIColor.white]
        segmentedControl.setTitleTextAttributes(titleTextAttributes, for: .normal)
        segmentedControl.setTitleTextAttributes(titleTextAttributes, for: .selected)
    }
    
    
    // MARK: - работа с памятью
    private func loadDataString() {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            print("load   key: \(item.key) value: \(item.value)")
        }
    }
    private func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
}

extension Notification.Name {
        static let notificationDataDialogs = Notification.Name(rawValue: "notificationDataDialogs")
    }
extension UIViewController {
    func showToast(message : String, font: UIFont) {

        let toastLabel = UILabel(frame: CGRect(x: self.view.frame.size.width/2 - 150, y: self.view.frame.size.height-100, width: 300, height: 35))
        toastLabel.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        toastLabel.textColor = UIColor.white
        toastLabel.font = font
        toastLabel.textAlignment = .center;
        toastLabel.text = message
        toastLabel.alpha = 1.0
        toastLabel.layer.cornerRadius = 10;
        toastLabel.clipsToBounds  =  true
        self.view.addSubview(toastLabel)
        UIView.animate(withDuration: 4.0, delay: 0.1, options: .curveEaseOut, animations: {
             toastLabel.alpha = 0.0
        }, completion: {(isCompleted) in
            toastLabel.removeFromSuperview()
        })
    }
}
