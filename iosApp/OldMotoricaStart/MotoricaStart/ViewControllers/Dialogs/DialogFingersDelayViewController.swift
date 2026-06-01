//
//  TestViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.06.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//

import UIKit


@objc class DialogFingersDelayViewController: UIViewController {
    @IBOutlet weak var timesSV: UIStackView!
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    @IBOutlet weak var titleFingersDelayDialog: UILabel!
    @IBOutlet weak var forefingerDelayNum: UILabel!
    @IBOutlet weak var middleFingerDelayNum: UILabel!
    @IBOutlet weak var ringFingerDelayNum: UILabel!
    @IBOutlet weak var littleFingerDelayNum: UILabel!
    @IBOutlet weak var bigFingerDelayNum: UILabel!
    @IBOutlet weak var rotationDelayNum: UILabel!
    @IBOutlet weak var slide1: UISlider!
    @IBOutlet weak var slide2: UISlider!
    @IBOutlet weak var slide3: UISlider!
    @IBOutlet weak var slide4: UISlider!
    @IBOutlet weak var slide5: UISlider!
    @IBOutlet weak var slide6: UISlider!
    private var savingParametrsMassString:[SaveObjectString]!
    let sampleGattAttributes = SampleGattAttributes()
    private var dataForCommunicate = ["byteArray": "01020304", "characteristic":"1", "type":"READ"]
    private var dataForCommunicateRead = ["byteArray": "01020304", "characteristic":"1", "type":"READ"]
    var stateGesture = 0
    
    private var fingerOpenStateDelay1 = 0
    private var fingerOpenStateDelay2 = 0
    private var fingerOpenStateDelay3 = 0
    private var fingerOpenStateDelay4 = 0
    private var fingerOpenStateDelay5 = 0
    private var fingerOpenStateDelay6 = 0

    private var fingerCloseStateDelay1 = 0
    private var fingerCloseStateDelay2 = 0
    private var fingerCloseStateDelay3 = 0
    private var fingerCloseStateDelay4 = 0
    private var fingerCloseStateDelay5 = 0
    private var fingerCloseStateDelay6 = 0
    
    private var openStage1: Int? = 0
    private var openStage2: Int? = 0
    private var openStage3: Int? = 0
    private var openStage4: Int? = 0
    private var openStage5: Int? = 0
    private var openStage6: Int? = 0
    
    private var closeStage1: Int? = 0
    private var closeStage2: Int? = 0
    private var closeStage3: Int? = 0
    private var closeStage4: Int? = 0
    private var closeStage5: Int? = 0
    private var closeStage6: Int? = 0
    
    private var gestureNumber = 0
    private var gestureTableStr = ""
    private var gestureTable: [String] = []
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
        timesSV.layer.cornerRadius = 10;
        timesSV.layer.masksToBounds = true;
        readDataFromFestX(characteristic: sampleGattAttributes.CHANGE_GESTURE_NEW_VM)
        initUI()
        
        NotificationCenter.default.addObserver(self, selector: #selector(updatingUI), name: .notificationReseiveBLEDataDelay, object: nil)
    }
    
    @IBAction func forefingerDelaySlideStop(_ sender: UISlider) {
        if (stateGesture == 1) {
            fingerCloseStateDelay1 = Int(sender.value)
            saveDataString(key: "GESTURE_CLOSE_DELAY_FINGER1", value: String(fingerCloseStateDelay1))
        } else {
            fingerOpenStateDelay1 = Int(sender.value)
            saveDataString(key: "GESTURE_OPEN_DELAY_FINGER1", value: String(fingerOpenStateDelay1))
        }
        
        sendDataToFestX(characteristic: sampleGattAttributes.CHANGE_GESTURE_NEW_VM)
    }
    @IBAction func middleFingerDelaySlideStop(_ sender: UISlider) {
        if (stateGesture == 1) {
            fingerCloseStateDelay2 = Int(sender.value)
            saveDataString(key: "GESTURE_CLOSE_DELAY_FINGER2", value: String(fingerCloseStateDelay2))
        } else {
            fingerOpenStateDelay2 = Int(sender.value)
            saveDataString(key: "GESTURE_OPEN_DELAY_FINGER2", value: String(fingerOpenStateDelay2))
        }
        sendDataToFestX(characteristic: sampleGattAttributes.CHANGE_GESTURE_NEW_VM)
    }
    @IBAction func ringFingerDelaySlideStop(_ sender: UISlider) {
        if (stateGesture == 1) {
            fingerCloseStateDelay3 = Int(sender.value)
            saveDataString(key: "GESTURE_CLOSE_DELAY_FINGER3", value: String(fingerCloseStateDelay3))
        } else {
            fingerOpenStateDelay3 = Int(sender.value)
            saveDataString(key: "GESTURE_OPEN_DELAY_FINGER3", value: String(fingerOpenStateDelay3))
        }
        sendDataToFestX(characteristic: sampleGattAttributes.CHANGE_GESTURE_NEW_VM)
    }
    @IBAction func littleFingerDelaySlideStop(_ sender: UISlider) {
        if (stateGesture == 1) {
            fingerCloseStateDelay4 = Int(sender.value)
            saveDataString(key: "GESTURE_CLOSE_DELAY_FINGER4", value: String(fingerCloseStateDelay4))
        } else {
            fingerOpenStateDelay4 = Int(sender.value)
            saveDataString(key: "GESTURE_OPEN_DELAY_FINGER4", value: String(fingerOpenStateDelay4))
        }
        sendDataToFestX(characteristic: sampleGattAttributes.CHANGE_GESTURE_NEW_VM)
    }
    @IBAction func bigFingerDelaySlideStop(_ sender: UISlider) {
        if (stateGesture == 1) {
            fingerCloseStateDelay5 = Int(sender.value)
            saveDataString(key: "GESTURE_CLOSE_DELAY_FINGER5", value: String(fingerCloseStateDelay5))
        } else {
            fingerOpenStateDelay5 = Int(sender.value)
            saveDataString(key: "GESTURE_OPEN_DELAY_FINGER5", value: String(fingerOpenStateDelay5))
        }
        sendDataToFestX(characteristic: sampleGattAttributes.CHANGE_GESTURE_NEW_VM)
    }
    @IBAction func rotationDelaySlideStop(_ sender: UISlider) {
        if (stateGesture == 1) {
            fingerCloseStateDelay6 = Int(sender.value)
            saveDataString(key: "GESTURE_CLOSE_DELAY_FINGER6", value: String(fingerCloseStateDelay6))
        } else {
            fingerOpenStateDelay6 = Int(sender.value)
            saveDataString(key: "GESTURE_OPEN_DELAY_FINGER6", value: String(fingerOpenStateDelay6))
        }
        sendDataToFestX(characteristic: sampleGattAttributes.CHANGE_GESTURE_NEW_VM)
    }
    
    
    @IBAction func forefingerDelaySlide(_ sender: UISlider) {
        forefingerDelayNum.text = String(Int(sender.value)*10) + NSLocalizedString("_ms", comment: "")
    }
    @IBAction func middleFingerDelaySlide(_ sender: UISlider){
        middleFingerDelayNum.text = String(Int(sender.value)*10) + NSLocalizedString("_ms", comment: "")
    }
    @IBAction func ringFingerDelaySlide(_ sender: UISlider) {
        ringFingerDelayNum.text = String(Int(sender.value)*10) + NSLocalizedString("_ms", comment: "")
    }
    @IBAction func littleFingerDelaySlide(_ sender: UISlider) {
        littleFingerDelayNum.text = String(Int(sender.value)*10) + NSLocalizedString("_ms", comment: "")
    }
    @IBAction func bigFingerDelaySlide(_ sender: UISlider) {
        bigFingerDelayNum.text = String(Int(sender.value)*10) + NSLocalizedString("_ms", comment: "")
    }
    @IBAction func rotationDelaySlide(_ sender: UISlider) {
        rotationDelayNum.text = String(Int(sender.value)*10) + NSLocalizedString("_ms", comment: "")
    }
    
    @IBAction func tapClose(_ sender: UIButton) {
        dismiss(animated: true, completion: nil)
    }
    
    
    
    //MARK: отправка/запрос ble команд
    private func sendDataToFestX (characteristic: String) {
        let dataForWrite = Data([UInt8(gestureNumber-1), UInt8(openStage4!),UInt8(openStage3!), UInt8(openStage2!),
                         UInt8(openStage1!), UInt8(openStage5!), UInt8(openStage6!), UInt8(closeStage4!),
                         UInt8(closeStage3!), UInt8(closeStage2!), UInt8(closeStage1!), UInt8(closeStage5!),
                         UInt8(closeStage6!), UInt8(fingerOpenStateDelay1), UInt8(fingerOpenStateDelay2),
                         UInt8(fingerOpenStateDelay3), UInt8(fingerOpenStateDelay4), UInt8(fingerOpenStateDelay5),
                         UInt8(fingerOpenStateDelay6), UInt8(fingerCloseStateDelay1), UInt8(fingerCloseStateDelay2),
                         UInt8(fingerCloseStateDelay3), UInt8(fingerCloseStateDelay4), UInt8(fingerCloseStateDelay5),
                         UInt8(fingerCloseStateDelay6)])
        SensorsViewController.myInteractiveQueueComandWithConfirmation(dataForWrite: dataForWrite, characteristic: characteristic, countRestart: 50)
    }
    private func readDataFromFestX (characteristic: String) {
        self.dataForCommunicateRead["characteristic"] = characteristic
        self.dataForCommunicateRead["type"] = sampleGattAttributes.READ
        self.dataForCommunicateRead["case"] = "0"
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: self.dataForCommunicateRead)
    }
    
    
    private func initUI() {
        loadDataString()
        for item in savingParametrsMassString
        {
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"1")) {
                fingerOpenStateDelay1 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"2")) {
                fingerOpenStateDelay2 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"3")) {
                fingerOpenStateDelay3 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"4")) {
                fingerOpenStateDelay4 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"5")) {
                fingerOpenStateDelay5 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_OPEN_DELAY_FINGER+"6")) {
                fingerOpenStateDelay6 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"1")) {
                fingerCloseStateDelay1 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"2")) {
                fingerCloseStateDelay2 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"3")) {
                fingerCloseStateDelay3 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"4")) {
                fingerCloseStateDelay4 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"5")) {
                fingerCloseStateDelay5 = Int(item.value) ?? 15
            }
            if (item.key == (sampleGattAttributes.GESTURE_CLOSE_DELAY_FINGER+"6")) {
                fingerCloseStateDelay6 = Int(item.value) ?? 15
            }
            if (item.key == sampleGattAttributes.ADD_GESTURE_NEW) {
                gestureTableStr = item.value;
                gestureTable = gestureTableStr.components(separatedBy: " ")
            }
        }
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.STATE_GESTURE) {
                if (Int(item.value) == 1) {
                    titleFingersDelayDialog.text = NSLocalizedString("title_fingers_delay_dialog_close", comment: "")
                    slide1.value = Float(fingerCloseStateDelay1)
                    slide2.value = Float(fingerCloseStateDelay2)
                    slide3.value = Float(fingerCloseStateDelay3)
                    slide4.value = Float(fingerCloseStateDelay4)
                    slide5.value = Float(fingerCloseStateDelay5)
                    slide6.value = Float(fingerCloseStateDelay6)
                    stateGesture = 1
                } else {
                    titleFingersDelayDialog.text = NSLocalizedString("title_fingers_delay_dialog_open", comment: "")
                    slide1.value = Float(fingerOpenStateDelay1)
                    slide2.value = Float(fingerOpenStateDelay2)
                    slide3.value = Float(fingerOpenStateDelay3)
                    slide4.value = Float(fingerOpenStateDelay4)
                    slide5.value = Float(fingerOpenStateDelay5)
                    slide6.value = Float(fingerOpenStateDelay6)
                    stateGesture = 0
                }
                forefingerDelayNum.text = String(Int(slide1.value)*10) + NSLocalizedString("_ms", comment: "")
                middleFingerDelayNum.text = String(Int(slide2.value)*10) + NSLocalizedString("_ms", comment: "")
                ringFingerDelayNum.text = String(Int(slide3.value)*10) + NSLocalizedString("_ms", comment: "")
                littleFingerDelayNum.text = String(Int(slide4.value)*10) + NSLocalizedString("_ms", comment: "")
                bigFingerDelayNum.text = String(Int(slide5.value)*10) + NSLocalizedString("_ms", comment: "")
                rotationDelayNum.text = String(Int(slide6.value)*10) + NSLocalizedString("_ms", comment: "")
            }
            if (item.key == sampleGattAttributes.GESTURE_EDITING_NUM) {
                gestureNumber = Int(item.value)!;
                openStage4 = Int(gestureTable[12*(gestureNumber-1)+0]);
                openStage3 = Int(gestureTable[12*(gestureNumber-1)+1]);
                openStage2 = Int(gestureTable[12*(gestureNumber-1)+2]);
                openStage1 = Int(gestureTable[12*(gestureNumber-1)+3]);
                openStage5 = Int(gestureTable[12*(gestureNumber-1)+4]);
                openStage6 = Int(gestureTable[12*(gestureNumber-1)+5]);
                
                closeStage4 = Int(gestureTable[12*(gestureNumber-1)+6]);
                closeStage3 = Int(gestureTable[12*(gestureNumber-1)+7]);
                closeStage2 = Int(gestureTable[12*(gestureNumber-1)+8]);
                closeStage1 = Int(gestureTable[12*(gestureNumber-1)+9]);
                closeStage5 = Int(gestureTable[12*(gestureNumber-1)+10]);
                closeStage6 = Int(gestureTable[12*(gestureNumber-1)+11]);
                
            }
        }
    }
    private func loadDataString() {
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
    }
    private func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
    @objc func updatingUI(notification: Notification) {
        //обновление задержек пальцев после чтения с протеза
            initUI()
        
    }
}


extension Notification.Name {
        static let notificationReseiveBLEDataDelay = Notification.Name(rawValue: "notificationReseiveBLEDataDelay")
    }
