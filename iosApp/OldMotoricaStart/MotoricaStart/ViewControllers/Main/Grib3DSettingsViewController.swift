//
//  Grib3DSettingsViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 16.01.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//

import UIKit

class Grib3DSettingsViewController: UIViewController {
    @IBOutlet var containerView: UIView!
    @IBOutlet weak var deviceName: UILabel!
    @IBOutlet weak var numberGesture: UILabel!
    @IBOutlet weak var statusImage: UIImageView!
    @IBOutlet weak var saveBtn: UIButton!
    
    var numberGestureNum: UInt8 = 0
    private let sampleGattAttributes = SampleGattAttributes()
    private var savingParametrsMassString:[SaveObjectString]!
    private var dataForCommunicate = ["byteArray": "01020304", "characteristic":"1", "type":"READ", "case":"0"]
    private let openState: UInt8 = 1
    private let closeState: UInt8 = 0
    private var openStage = 0b00000000
    private var closeStage = 0b00000000
    private var oldOpenStage = 0b00000000
    private var oldCloseStage = 0b00000000
    let connectStatus = UIImage(named:"connect_status")!
    let disconnectStatus = UIImage(named:"disconnect_status")!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        loadDataString()
        initUI()
        NotificationCenter.default.addObserver(self, selector: #selector(checkStateConnection), name: .notificationCheckStateConnection, object: nil)
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
    
    
    // MARK: - обработка взаимодействия с UI
    @IBAction func changeOpenState(_ sender: UISwitch) {
        switch sender {
//            case openLittleFinger:
//                if (sender.isOn) {
//                    openStage = openStage | 0b00000001
//                    print("openStage = \(openStage)")
//                } else {
//                    openStage = openStage & 0b11111110
//                    print("openStage = \(openStage)")
//                }
//            case openRingFinger:
//                if (sender.isOn) {
//                    openStage = openStage | 0b00000010
//                    print("openStage = \(openStage)")
//                } else {
//                    openStage = openStage & 0b11111101
//                    print("openStage = \(openStage)")
//                }
//            case openMiddleFinger:
//                if (sender.isOn) {
//                    openStage = openStage | 0b00000100
//                    print("openStage = \(openStage)")
//                } else {
//                    openStage = openStage & 0b11111011
//                    print("openStage = \(openStage)")
//                }
//            case openForefinger:
//                if (sender.isOn) {
//                    openStage = openStage | 0b00001000
//                    print("openStage = \(openStage)")
//                } else {
//                    openStage = openStage & 0b11110111
//                    print("openStage = \(openStage)")
//                }
//            case openThumb:
//                if (sender.isOn) {
//                    openStage = openStage | 0b00010000
//                    print("openStage = \(openStage)")
//                } else {
//                    openStage = openStage & 0b11101111
//                    print("openStage = \(openStage)")
//                }
//            case openThumbRotation:
//                if (sender.isOn) {
//                    openStage = openStage | 0b00100000
//                    print("openStage = \(openStage)")
//                } else {
//                    openStage = openStage & 0b11011111
//                    print("openStage = \(openStage)")
//                }
            default:
                print("tup default")
                
        }
        saveDataString(key: String(numberGestureNum) + sampleGattAttributes.GESTURE_OPEN_STAGE, value: "\(openStage)")
        sendDataToHC10(dataForWrite: Data([numberGestureNum, UInt8(openStage), UInt8(closeStage), openState]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(12))
    }
    @IBAction func changeCloseState(_ sender: UISwitch) {
        switch sender {
//            case closeLittleFinger:
//                if (sender.isOn) {
//                    closeStage = closeStage | 0b00000001
//                    print("closeStage = \(closeStage)")
//                } else {
//                    closeStage = closeStage & 0b11111110
//                    print("closeStage = \(closeStage)")
//                }
//            case closeRingFinger:
//                if (sender.isOn) {
//                    closeStage = closeStage | 0b00000010
//                    print("closeStage = \(closeStage)")
//                } else {
//                    closeStage = closeStage & 0b11111101
//                    print("closeStage = \(closeStage)")
//                }
//            case closeMiddleFinger:
//                if (sender.isOn) {
//                    closeStage = closeStage | 0b00000100
//                    print("closeStage = \(closeStage)")
//                } else {
//                    closeStage = closeStage & 0b11111011
//                    print("closeStage = \(closeStage)")
//                }
//            case closeForefinger:
//                if (sender.isOn) {
//                    closeStage = closeStage | 0b00001000
//                    print("closeStage = \(closeStage)")
//                } else {
//                    closeStage = closeStage & 0b11110111
//                    print("closeStage = \(closeStage)")
//                }
//            case closeThumb:
//                if (sender.isOn) {
//                    closeStage = closeStage | 0b00010000
//                    print("closeStage = \(closeStage)")
//                } else {
//                    closeStage = closeStage & 0b11101111
//                    print("closeStage = \(closeStage)")
//                }
//            case closeThumbRotation:
//                if (sender.isOn) {
//                    closeStage = closeStage | 0b00100000
//                    print("closeStage = \(closeStage)")
//                } else {
//                    closeStage = closeStage & 0b11011111
//                    print("closeStage = \(closeStage)")
//                }
            default:
                print("tup default")
        }
        saveDataString(key: String(numberGestureNum) + sampleGattAttributes.GESTURE_CLOSE_STAGE, value: "\(closeStage)")
        sendDataToHC10(dataForWrite: Data([numberGestureNum, UInt8(openStage), UInt8(closeStage), closeState]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(12))
    }
    @IBAction func perehod(_ sender: Any) {
        saveDataString(key: String(numberGestureNum) + sampleGattAttributes.GESTURE_OPEN_STAGE, value: "\(oldOpenStage)")
        saveDataString(key: String(numberGestureNum) + sampleGattAttributes.GESTURE_CLOSE_STAGE, value: "\(oldCloseStage)")
        sendDataToHC10(dataForWrite: Data([numberGestureNum, UInt8(oldOpenStage), UInt8(oldCloseStage), openState]), characteristic: sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(12))
    }
    
    
    private func sendDataToHC10 (dataForWrite: Data, characteristic: String, myCase: String) {
        self.dataForCommunicate["byteArray"] = dataForWrite.hexEncodedString()
        self.dataForCommunicate["characteristic"] = characteristic
        self.dataForCommunicate["type"] = sampleGattAttributes.WRITE_HC10
        self.dataForCommunicate["case"] = myCase
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: self.dataForCommunicate)
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
    private func setupeButton (button: UIButton) {
        button.layer.cornerRadius = 21
        button.layer.borderWidth = 2
        button.layer.borderColor = UIColor.white.cgColor
    }
    
    private func initUI() {
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.DEVICE_NAME) {
                deviceName.text = item.value
            }
            if (item.key == sampleGattAttributes.GESTURE_EDITING_NUM) {
                numberGesture.text = numberGesture.text! + "  №" + item.value
                numberGestureNum = UInt8(Int(item.value)!-1)
            }
            if (item.key == sampleGattAttributes.STATUS_CONNECTION) {
                if (Int(item.value) == 1) {
                    statusImage.image = connectStatus
                } else {
                    statusImage.image = disconnectStatus
                }
            }
        }
        for item in savingParametrsMassString
        {
            if (item.key == (String(numberGestureNum) + sampleGattAttributes.GESTURE_OPEN_STAGE)) {
                openStage = Int(item.value)!
                oldOpenStage = openStage
                print("openStage: \(openStage)")
//                openLittleFinger.isOn = (openStage >> 0 & 0b00000001 == 1)
//                openRingFinger.isOn = (openStage >> 1 & 0b00000001 == 1)
//                openMiddleFinger.isOn = (openStage >> 2 & 0b00000001 == 1)
//                openForefinger.isOn = (openStage >> 3 & 0b00000001 == 1)
//                openThumb.isOn = (openStage >> 4 & 0b00000001 == 1)
//                openThumbRotation.isOn = (openStage >> 5 & 0b00000001 == 1)
            }
            if (item.key == (String(numberGestureNum) + sampleGattAttributes.GESTURE_CLOSE_STAGE)) {
                closeStage = Int(item.value)!
                oldCloseStage = closeStage
//                closeLittleFinger.isOn = (closeStage >> 0 & 0b00000001 == 1)
//                closeRingFinger.isOn = (closeStage >> 1 & 0b00000001 == 1)
//                closeMiddleFinger.isOn = (closeStage >> 2 & 0b00000001 == 1)
//                closeForefinger.isOn = (closeStage >> 3 & 0b00000001 == 1)
//                closeThumb.isOn = (closeStage >> 4 & 0b00000001 == 1)
//                closeThumbRotation.isOn = (closeStage >> 5 & 0b00000001 == 1)
            }
        }
    }
    private func loadDataString() {
        setupeButton(button: saveBtn)
        savingParametrsMassString = [SaveObjectString]()
        savingParametrsMassString = DataManager.loadAll(SaveObjectString.self)
//        for item in savingParametrsMassString {
//            print("load   key: \(item.key) value: \(item.value)")
//        }
    }
    private func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
}

