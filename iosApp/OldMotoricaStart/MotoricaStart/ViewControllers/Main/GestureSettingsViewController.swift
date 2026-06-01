//
//  Gesture Settings View Controller.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.06.2021.
//  Copyright © 2021 Brian Advent. All rights reserved.
//

import UIKit
            
@objc class GestureSettingsViewController: UIViewController, UIPickerViewDelegate, UIPickerViewDataSource {
    @IBOutlet var conteinerView: UIView!
    @IBOutlet weak var gestureRotationMainView: UIView!
    @IBOutlet weak var gesturesRotationView: UIView!
    @IBOutlet weak var peakLengthView: UIView!
    @IBOutlet weak var peakLengthSlide: UISlider!
    @IBOutlet weak var peakLengthNum: UILabel!
    @IBOutlet weak var switchingGestureMenuHight: NSLayoutConstraint!
    @IBOutlet weak var spaceBeforeGesturesButtonsBlockHight: NSLayoutConstraint!
    @IBOutlet weak var deviceName: UILabel!
    @IBOutlet weak var statusImage: UIImageView!
    @IBOutlet weak var switchingBySensorsSwitch: UISwitch!
    @IBOutlet weak var switchingBySensorsText: UILabel!
    @IBOutlet weak var startGestureButton: UIButton!
    @IBOutlet weak var endGestureButton: UIButton!
    @IBOutlet weak var startGestureNum: UILabel! //номер стартового жеста на ладошке на кнопке выбора
    @IBOutlet weak var endGestureNum: UILabel! //номер конечного жеста на ладошке на кнопке выбора
    @IBOutlet weak var gesture1: UIButton!
    @IBOutlet weak var gesture2: UIButton!
    @IBOutlet weak var gesture3: UIButton!
    @IBOutlet weak var gesture4: UIButton!
    @IBOutlet weak var gesture5: UIButton!
    @IBOutlet weak var gesture6: UIButton!
    @IBOutlet weak var gesture7: UIButton!
    @IBOutlet weak var gesture8: UIButton!
    @IBOutlet weak var gesture9: UIButton!
    @IBOutlet weak var gesture10: UIButton!
    @IBOutlet weak var gesture11: UIButton!
    @IBOutlet weak var gesture12: UIButton!
    @IBOutlet weak var gesture13: UIButton!
    @IBOutlet weak var gesture14: UIButton!
    @IBOutlet weak var gesture2settings: UIButton!
    @IBOutlet weak var gesture3settings: UIButton!
    @IBOutlet weak var gesture4settings: UIButton!
    @IBOutlet weak var gesture5settings: UIButton!
    @IBOutlet weak var gesture6settings: UIButton!
    @IBOutlet weak var gesture7settings: UIButton!
    @IBOutlet weak var gesture8settings: UIButton!
    @IBOutlet weak var gesture9settings: UIButton!
    @IBOutlet weak var gesture10settings: UIButton!
    @IBOutlet weak var gesture11settings: UIButton!
    @IBOutlet weak var gesture12settings: UIButton!
    @IBOutlet weak var gesture13settings: UIButton!
    @IBOutlet weak var gesture14settings: UIButton!
    @IBOutlet weak var gestureRotation: UIImageView!
    @IBOutlet weak var gesture2rotation: UIImageView!
    @IBOutlet weak var gesture3rotation: UIImageView!
    @IBOutlet weak var gesture4rotation: UIImageView!
    @IBOutlet weak var gesture5rotation: UIImageView!
    @IBOutlet weak var gesture6rotation: UIImageView!
    @IBOutlet weak var gesture7rotation: UIImageView!
    @IBOutlet weak var gesture8rotation: UIImageView!
    @IBOutlet weak var gesture9rotation: UIImageView!
    @IBOutlet weak var gesture10rotation: UIImageView!
    @IBOutlet weak var gesture11rotation: UIImageView!
    @IBOutlet weak var gesture12rotation: UIImageView!
    @IBOutlet weak var gesture13rotation: UIImageView!
    @IBOutlet weak var gesture14rotation: UIImageView!
    @IBOutlet weak var gestureReset: UIButton!
    
    
    @IBAction func unwindToThisGestureViewController (sender: UIStoryboardSegue){
        loadDataString()
        initUI()
        print("sGRG initUI() unwindToThisGestureViewController()")
    }
    private let sampleGattAttributes = SampleGattAttributes()
    private let screenWidth = UIScreen.main.bounds.width - 10
    private let screenHeight = UIScreen.main.bounds.height / 3
    private var savingParametrsMassString:[SaveObjectString]!
    private var dataForAdvancedSettingsViewController = ["deviceName":"lol"]
    @objc public var savingDeviceName: String = "...."
    let settingsDefault = UIImage(named:"settings")!
    let settingsActive = UIImage(named:"settings_active")!
    let loopGesture = UIImage(named:"gesture_loop")!
    let loopGestureActive = UIImage(named:"gesture_loop_active")!
    private var dataForCommunicate = ["byteArray": "01020304", "characteristic":"1", "type":"READ", "case":"0"]
    let connectStatus = UIImage(named:"connect_status")!
    let disconnectStatus = UIImage(named:"disconnect_status")!
    private var typeMultigrib: Bool = false
    private var typeMultigribNew: Bool = false
    private var typeMultigribNewVM: Bool = false
    private var activeGesture = 1
    private var activeGestures = 8
    
    private var listGesturesName = [
        NSLocalizedString("gesture_1", comment: ""),
        NSLocalizedString("gesture_2", comment: ""),
        NSLocalizedString("gesture_3", comment: ""),
        NSLocalizedString("gesture_4", comment: ""),
        NSLocalizedString("gesture_5", comment: ""),
        NSLocalizedString("gesture_6", comment: ""),
        NSLocalizedString("gesture_7", comment: ""),
        NSLocalizedString("gesture_8", comment: ""),
        NSLocalizedString("gesture_9", comment: ""),
        NSLocalizedString("gesture_10", comment: ""),
        NSLocalizedString("gesture_11", comment: ""),
        NSLocalizedString("gesture_12", comment: ""),
        NSLocalizedString("gesture_13", comment: ""),
        NSLocalizedString("gesture_14", comment: "")]
    private var oldListGesturesName = [
        NSLocalizedString("gesture_1", comment: ""),
        NSLocalizedString("gesture_2", comment: ""),
        NSLocalizedString("gesture_3", comment: ""),
        NSLocalizedString("gesture_4", comment: ""),
        NSLocalizedString("gesture_5", comment: ""),
        NSLocalizedString("gesture_6", comment: ""),
        NSLocalizedString("gesture_7", comment: ""),
        NSLocalizedString("gesture_8", comment: "")]
    private var startGestureSelectedRow = 0 //номер стартового жеста для передачи
    private var endGestureSelectedRow = 0 //номер финального жеста для передачи
    private var switchBySensors: UInt8 = 0x00
    private var timeAtRest: UInt8 = 0x00
    private var switchProsthesisBlocking: UInt8 = 0x00
    private var timeForBlocking: UInt8 = 0x00
    
    override func viewDidLoad() {
        super.viewDidLoad()
        loadDataString()
        initUI()
        print("sGRG initUI() viewDidLoad()")
        NotificationCenter.default.addObserver(self, selector: #selector(checkStateConnection), name: .notificationCheckStateConnection, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(updatingUINotification), name: .notificationReseiveBLEData, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(resultDialog), name: .notificationDataDialogs, object: nil)
    }
    @objc func resultDialog(notification: Notification) {
        guard let dataState = notification.userInfo,
              let resultDialog = dataState["resultDialog"] as? String
        else {
            print("recalibrationResult else return")
            return
        }
        
        
        if (resultDialog == "gesturesResetCancel") {
            print("gesturesResetResult cancel")
        }
        if (resultDialog == "gesturesResetAccept") {
            print("gesturesResetResult accept")
            startReset(translateByte: Data([0x03]))
        }
    }
    @objc private func checkStateConnection(notification: Notification) {
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
    
    private func animatedShowLoopGesturesParameters() {
        print("sGRG animatedShowLoopGesturesParameters")
        animatedLoopGesturesParameters(duration: 0.3, alpha: 1, switchingGestureMenuHight: 200)
        if (SensorsViewController.versionDriverGreaterThan237) {
//            print("2 sGRG animatedShowLoopGesturesParameters startGestureInLoopNum = \(startGestureSelectedRow + 1) endGestureInLoopNum = \(endGestureSelectedRow + 1)")
            setGesturesRotationGroup(startGestureInLoopNum: startGestureSelectedRow, endGestureInLoopNum: endGestureSelectedRow, withSave: false)
        } else {
            animatedHideLoopGesturesParameters()
        }
    }
    private func animatedHideLoopGesturesParameters() {
        print("sGRG animatedHideLoopGesturesParameters")
        animatedLoopGesturesParameters(duration: 0.3, alpha: 0, switchingGestureMenuHight: 42)
        let gesturesLoopMass = [gestureRotation, gesture2rotation, gesture3rotation, gesture4rotation, gesture5rotation, gesture6rotation, gesture7rotation, gesture8rotation, gesture9rotation, gesture10rotation, gesture11rotation,gesture12rotation, gesture13rotation, gesture14rotation]
        for item in gesturesLoopMass {
                    item?.isHidden = true
                }
    }
    private func animatedLoopGesturesParameters(duration : Double, alpha : CGFloat, switchingGestureMenuHight : CGFloat) {
        if (SensorsViewController.versionDriverGreaterThan237) {
            UIView.animate(withDuration: duration, animations: {
                self.switchingGestureMenuHight.constant = switchingGestureMenuHight
                self.gesturesRotationView.alpha = alpha
                self.peakLengthView.alpha = alpha
                self.view.layoutIfNeeded()
            })
        } else {
            self.switchingGestureMenuHight.constant = 0
        }
        
    }
    
    
    
    // MARK: - обработка взаимодействия с UI
    @IBAction func peakLengthSlide(_ sender: UISlider) {
        peakLengthNum.text = String(Float(Int(peakLengthSlide.value)+1)/10)+NSLocalizedString("_s", comment: "")
    }
    @IBAction func peakLengthSlideStop(_ sender: UISlider) {
        let data = Data([switchBySensors, 0x00, UInt8(peakLengthSlide.value), 0x00, switchProsthesisBlocking, timeForBlocking, UInt8(startGestureSelectedRow), UInt8(endGestureSelectedRow)])
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
        saveDataString(key: sampleGattAttributes.TIME_AT_REST, value: String(Int(sender.value)))
    }
    @IBAction func switchingBySensorsSwitch(_ sender: UISwitch) {
        let data: Data
        if (sender.isOn) {
            switchingBySensorsText.text = NSLocalizedString("on", comment: "")
            switchBySensors = 0x01
            data = Data([0x01, 0x00, timeAtRest, 0x00, switchProsthesisBlocking, timeForBlocking, UInt8(startGestureSelectedRow), UInt8(endGestureSelectedRow)])
            saveDataString(key: sampleGattAttributes.SWITCH_BY_SENSORS, value: String(1))
            animatedShowLoopGesturesParameters()
        } else {
            switchBySensors = 0x00
            switchingBySensorsText.text = NSLocalizedString("off", comment: "")
            data = Data([0x00, 0x00, timeAtRest, 0x00, switchProsthesisBlocking, timeForBlocking, UInt8(startGestureSelectedRow), UInt8(endGestureSelectedRow)])
            saveDataString(key: sampleGattAttributes.SWITCH_BY_SENSORS, value: String(0))
            animatedHideLoopGesturesParameters()
        }
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
    }
    @IBAction func gestureStartPopUp(_ sender: Any) {
        print("sGRG gestureStartPopUp")
        let vc = UIViewController()
        vc.preferredContentSize = CGSize(width: screenWidth, height: screenHeight)
        let pikerView = UIPickerView(frame: CGRect(x: 0, y: 0, width: screenWidth, height: screenHeight))
        pikerView.dataSource = self
        pikerView.delegate = self
        
        pikerView.selectRow(startGestureSelectedRow, inComponent: 0, animated: false)
        vc.view.addSubview(pikerView)
        pikerView.centerXAnchor.constraint(equalTo: vc.view.centerXAnchor).isActive = true
        pikerView.centerYAnchor.constraint(equalTo: vc.view.centerYAnchor).isActive = true
        
        let alert = UIAlertController(
            title: NSLocalizedString("Select start gesture", comment: ""),
            message: "", preferredStyle: .actionSheet)
        
        alert.popoverPresentationController?.sourceView = startGestureButton
        alert.popoverPresentationController?.sourceRect = startGestureButton.bounds
        
        alert.setValue(vc, forKey: "contentViewController")
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { UIAlertAction in
            
        }))
        alert.addAction(UIAlertAction(title: "Select", style: .default, handler: { [self] UIAlertAction in
            print("sGRG gestureStartPopUp  Select")
            self.startGestureSelectedRow = pikerView.selectedRow(inComponent: 0)//тут запись
            let selected = Array(self.listGesturesName)[self.startGestureSelectedRow]
            let name = selected
            self.startGestureButton.setTitle(name, for: .normal)
            self.startGestureNum.text = "\(self.startGestureSelectedRow + 1)"
            
            self.saveDataString(key: self.sampleGattAttributes.SELECTED_START_GESTURE, value: String(self.startGestureSelectedRow))
//            print("1 sGRG startGestureInLoopNum = \(startGestureSelectedRow + 1) endGestureInLoopNum = \(endGestureSelectedRow + 1)")
            self.setGesturesRotationGroup(startGestureInLoopNum: startGestureSelectedRow, endGestureInLoopNum: endGestureSelectedRow, withSave: false)
            let data = Data([switchBySensors, 0x00, UInt8(peakLengthSlide.value), 0x00, switchProsthesisBlocking, timeForBlocking, UInt8(startGestureSelectedRow), UInt8(endGestureSelectedRow)])
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
        }))
        
        self.present(alert, animated: true, completion: nil)
    }
    @IBAction func gestureEndPopUp(_ sender: Any) {
        print("sGRG gestureEndPopUp")
        let vc = UIViewController()
        vc.preferredContentSize = CGSize(width: screenWidth, height: screenHeight)
        let pikerView = UIPickerView(frame: CGRect(x: 0, y: 0, width: screenWidth, height: screenHeight))
        pikerView.dataSource = self
        pikerView.delegate = self
        
        pikerView.selectRow(endGestureSelectedRow, inComponent: 0, animated: false)
        vc.view.addSubview(pikerView)
        pikerView.centerXAnchor.constraint(equalTo: vc.view.centerXAnchor).isActive = true
        pikerView.centerYAnchor.constraint(equalTo: vc.view.centerYAnchor).isActive = true
        
        let alert = UIAlertController(
            title: NSLocalizedString("Select end gesture", comment: ""),
            message: "", preferredStyle: .actionSheet)
        
        alert.popoverPresentationController?.sourceView = endGestureButton
        alert.popoverPresentationController?.sourceRect = endGestureButton.bounds
        
        alert.setValue(vc, forKey: "contentViewController")
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { UIAlertAction in
        }))
        alert.addAction(UIAlertAction(title: "Select", style: .default, handler: { [self] UIAlertAction in
            print("sGRG gestureEndPopUp  Select")
            self.endGestureSelectedRow = pikerView.selectedRow(inComponent: 0)
            let name = Array(self.listGesturesName)[self.endGestureSelectedRow]
            self.endGestureButton.setTitle(name, for: .normal)
            self.endGestureNum.text = "\(self.endGestureSelectedRow + 1)"
            
            self.saveDataString(key: self.sampleGattAttributes.SELECTED_END_GESTURE, value: String(self.endGestureSelectedRow))
            self.setGesturesRotationGroup(startGestureInLoopNum: startGestureSelectedRow, endGestureInLoopNum: endGestureSelectedRow, withSave: false)
            let data = Data([switchBySensors, 0x00, UInt8(peakLengthSlide.value), 0x00, switchProsthesisBlocking, timeForBlocking, UInt8(startGestureSelectedRow), UInt8(endGestureSelectedRow)])
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: data, characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, countRestart: 50)
        }))
        
        self.present(alert, animated: true, completion: nil)
    }
    private func setGesturesRotationGroup (startGestureInLoopNum: Int, endGestureInLoopNum: Int, withSave: Bool) {
        print("sGRG setGesturesRotationGroup startGestureInLoopNum = \(startGestureInLoopNum + 1) endGestureInLoopNum = \(endGestureInLoopNum + 1)")
        if (SensorsViewController.versionDriverGreaterThan237) {
            //создаём массив картинок символизирующих ротацию
            let gesturesLoopMass = [gestureRotation, gesture2rotation, gesture3rotation, gesture4rotation, gesture5rotation, gesture6rotation, gesture7rotation, gesture8rotation, gesture9rotation, gesture10rotation, gesture11rotation,gesture12rotation, gesture13rotation, gesture14rotation]
            //скрываем видимость всех этих картинок
            for item in gesturesLoopMass {
                item?.isHidden = true
            }
            //если конечный жест стал меньше начального то рокируем
            if (endGestureInLoopNum < startGestureInLoopNum) {
                let oldState = endGestureSelectedRow
                endGestureSelectedRow = startGestureSelectedRow
                startGestureSelectedRow = oldState
                var name = Array(listGesturesName)[startGestureSelectedRow]
                startGestureButton.setTitle(name, for: .normal)
                startGestureNum.text = "\(startGestureSelectedRow + 1)"
                name = Array(listGesturesName)[endGestureSelectedRow]
                endGestureButton.setTitle(name, for: .normal)
                endGestureNum.text = "\(endGestureSelectedRow + 1)"
                //показываем картинки циклов в диапазоне
                for item in endGestureInLoopNum...startGestureInLoopNum {
                    gesturesLoopMass[item]?.isHidden = false
                }
            } else {
                for item in startGestureInLoopNum...endGestureInLoopNum {
                    gesturesLoopMass[item]?.isHidden = false
                }
            }
            
            //если требуется запись новых границ групп ротации
            if (withSave){
                saveDataString(key: sampleGattAttributes.SELECTED_START_GESTURE, value: String(startGestureSelectedRow)) //тут запись
                saveDataString(key: sampleGattAttributes.SELECTED_END_GESTURE, value: String(endGestureSelectedRow)) //тут запись
            }
        } else {
            //если версия ниже, то просто скрываем выделение групп ротации
            let gesturesLoopMass = [gestureRotation, gesture2rotation, gesture3rotation, gesture4rotation, gesture5rotation, gesture6rotation, gesture7rotation, gesture8rotation, gesture9rotation, gesture10rotation, gesture11rotation,gesture12rotation, gesture13rotation, gesture14rotation]
            for item in gesturesLoopMass {
                item?.isHidden = true
            }
        }
    }
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    func pickerView(_ pickerView: UIPickerView, viewForRow row: Int, forComponent component: Int, reusing view: UIView?) -> UIView {
        let label = UILabel(frame: CGRect(x: 0, y: 0, width: screenWidth, height: 42))
        if (activeGestures == 8) {
            label.text = Array(oldListGesturesName)[row]
        } else {
            label.text = Array(listGesturesName)[row]
        }
        
        label.sizeToFit()
        return label
    }
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        if (activeGestures == 8) {
            oldListGesturesName.count
        } else {
            listGesturesName.count
        }
    }
    func pickerView(_ pickerView: UIPickerView, rowHeightForComponent component: Int) -> CGFloat {
        return 42
    }
    
    @IBAction func goToGripperSettings(_ sender: UIButton) {
        switch sender {
            case gesture2settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(2))
            case gesture3settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(3))
            case gesture4settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(4))
            case gesture5settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(5))
            case gesture6settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(6))
            case gesture7settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(7))
            case gesture8settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(8))
            case gesture9settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(9))
            case gesture10settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(10))
            case gesture11settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(11))
            case gesture12settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(12))
            case gesture13settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(13))
            case gesture14settings:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(14))
            default:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(1))
        }
        if (typeMultigrib) {
            print("правда deviceName.text: ", deviceName.text ?? 0)
            // останавливаем работу протеза от датчиков
            if (typeMultigribNew) {
                SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x00]), characteristic: sampleGattAttributes.SENS_ENABLED_NEW, type: sampleGattAttributes.WRITE, myCase: "") }
            if (typeMultigribNewVM) {
                SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x00]), characteristic: sampleGattAttributes.SENS_ENABLED_NEW_VM, countRestart: 50)
            }
            performSegue(withIdentifier: "go3DGripperSettings", sender: nil)
        } else {
            print("ложь deviceName.text: ", deviceName.text!)
            performSegue(withIdentifier: "goGripperSettings", sender: nil)
        }
        
    }
    @IBAction func perehod(_ sender: Any) {
        saveDataString(key: sampleGattAttributes.READ_THREAD_START, value: String(1))
        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([]), characteristic: SensorsViewController.sampleGattAttributes.ROTATION_GESTURE_NEW_VM, type: SensorsViewController.sampleGattAttributes.READ, myCase: "")
    }
    @IBAction func selectGesture(_ sender: UIButton) {
        switch sender {
            case gesture1:
//                    setupeButtonActiveWithoutSettings(button: gesture1)
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x00]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x00]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x00]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(1))
            case gesture2:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x01]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x01]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x01]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(2))
            case gesture3:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x02]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x02]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x02]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(3))
            case gesture4:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x03]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x03]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x03]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(4))
            case gesture5:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x04]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x04]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x04]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(5))
            case gesture6:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x05]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x05]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x05]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(6))
            case gesture7:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x06]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x06]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x06]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(7))
            case gesture8:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x07]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x07]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x07]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(8))
            case gesture9:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x08]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x08]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x08]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(9))
            case gesture10:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x09]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x09]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x09]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(10))
            case gesture11:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x0A]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x0A]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x0A]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(11))
            case gesture12:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x0B]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x0B]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x0B]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(12))
            case gesture13:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x0C]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x0C]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x0C]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(13))
            case gesture14:
                if (typeMultigribNewVM) {
                    SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: Data([0x0D]), characteristic: sampleGattAttributes.SET_GESTURE_NEW_VM, countRestart: 50)
                } else {
                    if (typeMultigribNew) {
                        SensorsViewController.myInteractiveQueueComand(dataForWrite: Data([0x0D]), characteristic: sampleGattAttributes.SET_GESTURE_NEW, type: sampleGattAttributes.WRITE, myCase: "")
                    } else {
                        sendDataToHC10(dataForWrite: Data([0x0D]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(13))
                    }
                }
                saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(14))
            default:
                saveDataString(key: sampleGattAttributes.GESTURE_EDITING_NUM, value: String(0))
        }
        loadDataString()
        initUI()
        print("sGRG initUI() selectGesture()")
    }
    private func sendDataToHC10 (dataForWrite: Data, characteristic: String, myCase: String) {
        self.dataForCommunicate["byteArray"] = dataForWrite.hexEncodedString()
        self.dataForCommunicate["characteristic"] = characteristic
        self.dataForCommunicate["type"] = sampleGattAttributes.WRITE_HC10
        self.dataForCommunicate["case"] = myCase
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: self.dataForCommunicate)
    }
    @objc public func sendDataToFest (dataForWrite: Data, characteristic: String, typeFestX: Bool) {
        //определить FEST-H или FEST-X подключён
        print("Вызвана функция sendDataToFest  typeFestX = \(typeFestX)")
        if (typeFestX) {
            SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: dataForWrite, characteristic: characteristic, countRestart: 50)
        } else {
            SensorsViewController.myInteractiveQueueComand(dataForWrite: dataForWrite, characteristic: characteristic, type: sampleGattAttributes.WRITE, myCase: "")
        }
    }
    @objc public func setNameGesture(numberGesture: Int, name: String) {
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.GESTURES_NAMES) {
                var gesturesNamesArr = item.value.split{$0 == ";"}.map(String.init)
                let buttonsGestureArr = [gesture1, gesture2, gesture3, gesture4, gesture5, gesture6, gesture7, gesture8, gesture9, gesture10, gesture11, gesture12, gesture13, gesture14]
                var newGesturesNamesStr = ""
                for (count, gestureName) in gesturesNamesArr.enumerated() {
                    if (count == numberGesture-1) {
                        gesturesNamesArr[count] = name
                        buttonsGestureArr[count]?.setTitle(gestureName, for: .normal)
                    }
                    newGesturesNamesStr += gesturesNamesArr[count]+";"
                }
                saveDataString(key: SensorsViewController.sampleGattAttributes.GESTURES_NAMES, value: newGesturesNamesStr)
                loadDataString()
            }
        }
    }
    @objc public func getDeviceName() -> String {
        var textName: String = "";
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.DEVICE_NAME) {
                textName = NameUtil().getCleanName(deviceName: item.value)
            }
        }
        return textName;
    }
    @objc public func getStatusConnection() -> Int {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.STATUS_CONNECTION) {
                return Int(item.value)!;
            }
        }
        return 0;
    }
    @objc public func getGestureNum() -> Int {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.GESTURE_EDITING_NUM) {
                return Int(item.value)!;
            }
        }
        return 0;
    }
    @objc public func getGestureName(numberGesture: Int) -> String {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.GESTURES_NAMES) {
                let gesturesNamesArr = item.value.split{$0 == ";"}.map(String.init)
                return gesturesNamesArr[numberGesture-1];
            }
        }
        return "0";
    }
    @objc public func getUseFestX() -> Int {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.USE_MULTIGRAB_FESTX) {
                return Int(item.value)!;
            }
        }
        return 0;
    }
    @objc public func getHandSide() -> Int {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.HAND_SIDE) {
                return Int(item.value)!;
            }
        }
        return 0;
    }
    @objc public func getGestureTable() -> String {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.ADD_GESTURE_NEW) {
                return item.value;
            }
        }
        return ""
    }
    @objc public func getGestureTableBig() -> String {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.ADD_GESTURE_NEW_BIG) {
                return item.value;
            }
        }
        return ""
    }
    @objc public func getFingersDelay() -> String {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        var figersDelay = [String] (repeating: "", count: 12)
        for item in savingParametrsMassString {
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"1")) {
                figersDelay[0] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"2")) {
                figersDelay[1] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"3")) {
                figersDelay[2] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"4")) {
                figersDelay[3] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"5")) {
                figersDelay[4] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"6")) {
                figersDelay[5] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"1")) {
                figersDelay[6] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"2")) {
                figersDelay[7] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"3")) {
                figersDelay[8] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"4")) {
                figersDelay[9] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"5")) {
                figersDelay[10] = item.value
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"6")) {
                figersDelay[11] = item.value
            }
        }
        var data: String = ""
        for i in 0...11 {
            data += figersDelay[i]+" "
        }
        return data
    }
    @objc public func getFingersDelaySwitch() -> Int {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
        for item in savingParametrsMassString {
            if (item.key == sampleGattAttributes.FINGERS_DELAY_SWITCH) {
                return Int(item.value)!;
            }
        }
        return 0;
    }
    @objc public func getVersionDriverGreaterThan237() -> Bool {
        return SensorsViewController.versionDriverGreaterThan237;
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
    private func setupeButton (button: UIButton, buttonSettings: UIButton, gestureLoopImage: UIImageView) {
        button.layer.cornerRadius = 21
        button.layer.borderWidth = 2
        button.layer.borderColor = UIColor.white.cgColor
        button.setTitleColor(UIColor.white, for: UIControlState.normal)
        button.layer.backgroundColor = UIColor(named: "lineColor_open")?.cgColor
        buttonSettings.setImage(settingsDefault, for: .normal)
        gestureLoopImage.image = loopGesture
    }
    private func setupeButton (button: UIButton) {
        button.layer.cornerRadius = 21
        button.layer.borderWidth = 2
        button.layer.borderColor = UIColor.white.cgColor
    }
    private func setupeColorBorderView(view: UIView) {
        view.layer.borderColor = UIColor.white.cgColor
    }
    private func setupeButtonActive (button: UIButton, buttonSettings: UIButton, gestureLoopImage: UIImageView) {
        button.layer.cornerRadius = 21
        button.layer.borderWidth = 2
        button.layer.borderColor = UIColor(named: "lineColor_open")?.cgColor
        button.setTitleColor(UIColor(named: "lineColor_open"), for: UIControlState.normal)
        button.layer.backgroundColor = UIColor.white.cgColor
        buttonSettings.setImage(settingsActive, for: .normal)
        gestureLoopImage.image = loopGestureActive
    }
    private func setupeButtonActiveWithoutSettings (button: UIButton, gestureLoopImage: UIImageView) {
        button.layer.cornerRadius = 21
        button.layer.borderWidth = 2
        button.layer.borderColor = UIColor(named: "lineColor_open")?.cgColor
        button.setTitleColor(UIColor(named: "lineColor_open"), for: UIControlState.normal)
        button.layer.backgroundColor = UIColor.white.cgColor
        gestureLoopImage.image = loopGestureActive
    }
    
    private func initUI() {
//        print("sGRG initUI()")
        setupeColorBorderView(view: gesturesRotationView)
        setupeColorBorderView(view: startGestureButton)
        setupeColorBorderView(view: endGestureButton)
        var gesturesNamesOn: Bool = false
        if (SensorsViewController.versionDriverGreaterThan237) {
            gesture9.isHidden = false
            gesture9settings.isHidden = false
            gesture10.isHidden = false
            gesture10settings.isHidden = false
            gesture11.isHidden = false
            gesture11settings.isHidden = false
            gesture12.isHidden = false
            gesture12settings.isHidden = false
            gesture13.isHidden = false
            gesture13settings.isHidden = false
            gesture14.isHidden = false
            gesture14settings.isHidden = false
        } else {
            gestureReset.isHidden = true
            gestureRotationMainView.isHidden = true
            switchingGestureMenuHight.constant = 0
            spaceBeforeGesturesButtonsBlockHight.constant = 0
        }
        
        // проверка, есть ли значения переменных в памяти
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.GESTURES_NAMES) {
                gesturesNamesOn = true
            }
        }
        
        // если переменная false, то её значения небыло в памяти телефона раньше и она
        // инициализируется дефолтным, заготовленным значением
        if (!gesturesNamesOn) {
            var summStr = ""
            for name in listGesturesName {
                summStr += name+";"
            }
            saveDataString(key: SensorsViewController.sampleGattAttributes.GESTURES_NAMES, value:summStr)
            loadDataString()
        }
        
        print("GESTURES_NAMES init UI")
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.DEVICE_NAME) {
                deviceName.text = NameUtil().getCleanName(deviceName: item.value)
            }
            if (item.key == sampleGattAttributes.GESTURES_NAMES) {
                let gesturesNamesArr = item.value.split{$0 == ";"}.map(String.init)
                let buttonsGestureArr = [gesture1, gesture2, gesture3, gesture4, gesture5, gesture6, gesture7, gesture8, gesture9, gesture10, gesture11, gesture12, gesture13, gesture14]
                for (count, gestureName) in gesturesNamesArr.enumerated() {
                    print("GESTURES_NAMES \(count)  "+gestureName+" name.characters.first?.description = /"+(gestureName.characters.first?.description ?? "null")+"/")
                
                    buttonsGestureArr[count]?.setTitle(gestureName, for: .normal)
                }
            }
            if (item.key == sampleGattAttributes.STATUS_CONNECTION) {
                if (Int(item.value) == 1) {
                    statusImage.image = connectStatus
                } else {
                    statusImage.image = disconnectStatus
                }
            }
            if (item.key == sampleGattAttributes.GESTURE_USE_NUM) {
                activeGesture = Int(item.value) ?? 1
                setActiveGesture(activeGesture: activeGesture)
                print("GESTURE_USE_NUM activeGesture = \(activeGesture)")
            }
            if (item.key == sampleGattAttributes.USE_MULTIGRAB) {
                if (item.value == sampleGattAttributes.USE) {
                    typeMultigrib = true
                }
            }
            if (item.key == sampleGattAttributes.USE_MULTIGRAB_FESTH) {
                if (Int(item.value) == 1) {
                    typeMultigribNew = true
                }
            }
            if (item.key == sampleGattAttributes.USE_MULTIGRAB_FESTX) {
                if (Int(item.value) == 1) {
                    typeMultigribNewVM = true
//                    print("FEST-X activated")
                }
            }
            if (item.key == sampleGattAttributes.SWITCH_BY_SENSORS) {
                if (Int(item.value) == 1) {
                    switchingBySensorsText.text = NSLocalizedString("on", comment: "")
                    switchingBySensorsSwitch.setOn(true, animated: true)
                    switchBySensors = 0x01
                    
                    switchingGestureMenuHight.constant = 200
                    gesturesRotationView.alpha = 1.0
                    peakLengthView.alpha = 1.0
                } else {
                    switchingBySensorsText.text = NSLocalizedString("off", comment: "")
                    switchingBySensorsSwitch.setOn(false, animated: true)
                    
                    switchingGestureMenuHight.constant = 42
                    gesturesRotationView.alpha = 0
                    peakLengthView.alpha = 0
                }
            }
            if (item.key == sampleGattAttributes.SELECTED_START_GESTURE) {
                startGestureSelectedRow = Int(item.value) ?? 0 //тут запись
                print("00 sGRG startGestureInLoopNum = \(startGestureSelectedRow + 1)")
                let name = Array(listGesturesName)[startGestureSelectedRow]
                startGestureButton.setTitle(name, for: .normal)
                startGestureNum.text = "\(startGestureSelectedRow + 1)"
                if (switchBySensors == 1) {
                    setGesturesRotationGroup(startGestureInLoopNum: startGestureSelectedRow, endGestureInLoopNum: endGestureSelectedRow, withSave: false)
                }
            }
            if (item.key == sampleGattAttributes.SELECTED_END_GESTURE) {
                endGestureSelectedRow = Int(item.value) ?? 0
                print("00 sGRG endGestureInLoopNum = \(endGestureSelectedRow + 1)")
                let name = Array(listGesturesName)[endGestureSelectedRow]
                endGestureButton.setTitle(name, for: .normal)
                endGestureNum.text = "\(endGestureSelectedRow + 1)"
                if (switchBySensors == 1) {
                    setGesturesRotationGroup(startGestureInLoopNum: startGestureSelectedRow, endGestureInLoopNum: endGestureSelectedRow, withSave: false)
                }
            }
            if (item.key == sampleGattAttributes.PROSTHESIS_BLOCKING) {
                switchProsthesisBlocking = UInt8(item.value) ?? 0x00
            }
            if (item.key == sampleGattAttributes.TIME_FOR_BLOCKING) {
                timeForBlocking = UInt8(item.value) ?? 0x00
            }
            if (item.key == sampleGattAttributes.TIME_AT_REST) {
                peakLengthNum.text =  String(Float(Int(item.value)!+1)/10)+NSLocalizedString("_s", comment: "")
                peakLengthSlide.setValue(Float(item.value)!, animated: true)
                timeAtRest = UInt8(item.value) ?? 0x00
                print("timeAtRest = \(timeAtRest)")
            }
            if (item.key == sampleGattAttributes.NUM_ACTIVE_GESTURES) {
                print("GS данные количества активных жестов \(Int(item.value))")
                activeGestures = Int(item.value) ?? 8
                if (Int(item.value) ?? 8 == 8) {
                    setOldGesturesNum()
                }
            }
        }
    }
    private func setOldGesturesNum() {
        gesture9.isHidden = true
        gesture9settings.isHidden = true
        gesture10.isHidden = true
        gesture10settings.isHidden = true
        gesture11.isHidden = true
        gesture11settings.isHidden = true
        gesture12.isHidden = true
        gesture12settings.isHidden = true
        gesture13.isHidden = true
        gesture13settings.isHidden = true
        gesture14.isHidden = true
        gesture14settings.isHidden = true
        
        //устанавливать тут ограничения на выбор иного количества жестов
        
    }
    private func setActiveGesture(activeGesture: Int) {
        setupeButton(button: gesture1, buttonSettings: gesture2settings, gestureLoopImage: gestureRotation)
        setupeButton(button: gesture2, buttonSettings: gesture2settings, gestureLoopImage: gesture2rotation)
        setupeButton(button: gesture3, buttonSettings: gesture3settings, gestureLoopImage: gesture3rotation)
        setupeButton(button: gesture4, buttonSettings: gesture4settings, gestureLoopImage: gesture4rotation)
        setupeButton(button: gesture5, buttonSettings: gesture5settings, gestureLoopImage: gesture5rotation)
        setupeButton(button: gesture6, buttonSettings: gesture6settings, gestureLoopImage: gesture6rotation)
        setupeButton(button: gesture7, buttonSettings: gesture7settings, gestureLoopImage: gesture7rotation)
        setupeButton(button: gesture8, buttonSettings: gesture8settings, gestureLoopImage: gesture8rotation)
        setupeButton(button: gesture9, buttonSettings: gesture9settings, gestureLoopImage: gesture9rotation)
        setupeButton(button: gesture10, buttonSettings: gesture10settings, gestureLoopImage: gesture10rotation)
        setupeButton(button: gesture11, buttonSettings: gesture11settings, gestureLoopImage: gesture11rotation)
        setupeButton(button: gesture12, buttonSettings: gesture12settings, gestureLoopImage: gesture12rotation)
        setupeButton(button: gesture13, buttonSettings: gesture13settings, gestureLoopImage: gesture13rotation)
        setupeButton(button: gesture14, buttonSettings: gesture14settings, gestureLoopImage: gesture14rotation)
        setupeButton(button: gestureReset)
        if (activeGesture == 1) { setupeButtonActiveWithoutSettings(button: gesture1, gestureLoopImage: gestureRotation) }
        if (activeGesture == 2) { setupeButtonActive(button: gesture2, buttonSettings: gesture2settings, gestureLoopImage: gesture2rotation) }
        if (activeGesture == 3) { setupeButtonActive(button: gesture3, buttonSettings: gesture3settings, gestureLoopImage: gesture3rotation) }
        if (activeGesture == 4) { setupeButtonActive(button: gesture4, buttonSettings: gesture4settings, gestureLoopImage: gesture4rotation) }
        if (activeGesture == 5) { setupeButtonActive(button: gesture5, buttonSettings: gesture5settings, gestureLoopImage: gesture5rotation) }
        if (activeGesture == 6) { setupeButtonActive(button: gesture6, buttonSettings: gesture6settings, gestureLoopImage: gesture6rotation) }
        if (activeGesture == 7) { setupeButtonActive(button: gesture7, buttonSettings: gesture7settings, gestureLoopImage: gesture7rotation) }
        if (activeGesture == 8) { setupeButtonActive(button: gesture8, buttonSettings: gesture8settings, gestureLoopImage: gesture8rotation) }
        if (activeGesture == 9) { setupeButtonActive(button: gesture9, buttonSettings: gesture9settings, gestureLoopImage: gesture9rotation) }
        if (activeGesture == 10) { setupeButtonActive(button: gesture10, buttonSettings: gesture10settings, gestureLoopImage: gesture10rotation) }
        if (activeGesture == 11) { setupeButtonActive(button: gesture11, buttonSettings: gesture11settings, gestureLoopImage: gesture11rotation) }
        if (activeGesture == 12) { setupeButtonActive(button: gesture12, buttonSettings: gesture12settings, gestureLoopImage: gesture12rotation) }
        if (activeGesture == 13) { setupeButtonActive(button: gesture13, buttonSettings: gesture13settings, gestureLoopImage: gesture13rotation) }
        if (activeGesture == 14) { setupeButtonActive(button: gesture14, buttonSettings: gesture14settings, gestureLoopImage: gesture14rotation) }
    }
    private func startReset(translateByte: Data) {
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: translateByte, characteristic: SensorsViewController.sampleGattAttributes.RESET_TO_FACTORY_SETTINGS_NEW_VM, countRestart: 50)
 
        loadDataString()
        initUI()
    }
    private func loadDataString() {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
//        for item in savingParametrsMassString {
//            print("load   key: \(item.key) value: \(item.value)")
//        }
    }
    @objc func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
    @objc func updatingUINotification(notification: Notification) {
        //переключение активного жеста в мгновение, когда он меняется в нотификации
        guard let dataForWrite = notification.userInfo,
            let gesture_use_num = dataForWrite["gesture_use_num"] as? String,
            let gesture_switching_by_sensors = dataForWrite["gesture_switching_by_sensors"] as? String,
            let time_at_rest = dataForWrite["time_at_rest"] as? String,
            let start_gesture = dataForWrite["start_gesture"] as? String,
            let end_gesture = dataForWrite["end_gesture"] as? String
        else { return }
        var update = false
        for item in savingParametrsMassString
        {
            if (item.key == SensorsViewController.sampleGattAttributes.GESTURE_USE_NUM) {
                if (Int(item.value) != Int(gesture_use_num)) {
                    saveDataString(key: sampleGattAttributes.GESTURE_USE_NUM, value: String(Int(gesture_use_num)!))
                    update = true
                    print("sGRG initUI() GESTURE_USE_NUM было:\(Int(item.value))  стало:\(Int(gesture_use_num))")
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SWITCH_BY_SENSORS) {
                if (Int(item.value) != Int(gesture_switching_by_sensors)) {
                    saveDataString(key: sampleGattAttributes.SWITCH_BY_SENSORS, value: String(Int(gesture_switching_by_sensors)!))
                    update = true
                    print("sGRG initUI() SWITCH_BY_SENSORS было:\(Int(item.value))  стало:\(Int(gesture_switching_by_sensors))")
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.TIME_AT_REST) {
                if (Int(item.value) != Int(time_at_rest)) {
                    saveDataString(key: sampleGattAttributes.TIME_AT_REST, value: String(Int(time_at_rest)!))
                    update = true
                    print("sGRG initUI() TIME_AT_REST было:\(Int(item.value))  стало:\(Int(gesture_switching_by_sensors))")
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_START_GESTURE) {
                if (Int(item.value) != Int(start_gesture)) {
                    saveDataString(key: sampleGattAttributes.SELECTED_START_GESTURE, value: String(Int(start_gesture)!))
                    update = true
                    print("sGRG initUI() SELECTED_START_GESTURE было:\(Int(item.value))  стало:\(Int(start_gesture))")
                }
            }
            if (item.key == SensorsViewController.sampleGattAttributes.SELECTED_END_GESTURE) {
                if (Int(item.value) != Int(end_gesture)) {
                    saveDataString(key: sampleGattAttributes.SELECTED_END_GESTURE, value: String(Int(end_gesture)!))
                    update = true
                    print("sGRG initUI() SELECTED_END_GESTURE было:\(Int(item.value))  стало:\(Int(end_gesture))")
                }
            }
        }
        if (update){
            update = false
            loadDataString()
            initUI()
        }
    }
}

