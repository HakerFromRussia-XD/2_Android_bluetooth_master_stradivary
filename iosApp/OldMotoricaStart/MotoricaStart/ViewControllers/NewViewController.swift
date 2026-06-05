//
//  NewViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.02.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//
import UIKit

class NewViewController: UIViewController {
    
    @IBOutlet var conteinerView: UIView!
//    @IBOutlet weak var statusImage: UIImageView!
//    @IBOutlet weak var shutdownCurrentSlide: UISlider!
//    @IBOutlet weak var shutdownCurrentNum: UILabel!
//    @IBOutlet weak var swapBtnSwitch: UISwitch!
//    @IBOutlet weak var swapBtnText: UILabel!
//    @IBOutlet weak var singleChannelControlSwitch: UISwitch!
//    @IBOutlet weak var singleChannelControlText: UILabel!
//    @IBOutlet weak var resetToFactorySettings: UIButton!
//    @IBOutlet weak var deviceName: UILabel!
    
//    @IBOutlet weak var deviceName: UILabel!
    
    private let sampleGattAttributes = SampleGattAttributes()
    private var dataForCommunicate = ["byteArray": "01020304", "characteristic":"1", "type":"READ", "case":"0"]
    private var dataForAdvancedSettingsViewController = ["deviceName":"lol"]
    private var savingParametrsMassString:[SaveObjectString]!
    private var savingDeviceName: String = "...."
    private var typeMultigrib: Bool = false
    let connectStatus = UIImage(named:"connect_status")!
    let disconnectStatus = UIImage(named:"disconnect_status")!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        loadDataString()
        initUI()
        NotificationCenter.default.addObserver(self, selector: #selector(checkStateConnection), name: .notificationCheckStateConnection, object: nil)
    }
    
    @objc func checkStateConnection(notification: Notification) {
//            guard let dataState = notification.userInfo,
//                let state = dataState["state"] as? String
//            else { return }
//            print("peredan state: \(state)")
//            if (state == "did Connect") {
//                statusImage.image = connectStatus
//            }
//            if (state == "did DISConnect") {
//                statusImage.image = disconnectStatus
//            }
        }

     // MARK: - обработка взаимодействия с UI
    @IBAction func shutdownCurrentSlide(_ sender: UISlider) {
//        shutdownCurrentNum.text = String(Int(sender.value))
    }
    @IBAction func shutdownCurrentSlideStop(_ sender: UISlider) {
        let data = Data([UInt8(sender.value)])
        if (typeMultigrib) { sendDataToHC10(dataForWrite: data, characteristic:                                     sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(0))
        }
        else {
            sendDataToINDY(dataForWrite: data, characteristic: sampleGattAttributes.SHUTDOWN_CURRENT_HDLE)
        }
        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_HDLE, value: String(Int(sender.value)))
    }
    @IBAction func swapBtnSwitch(_ sender: UISwitch) {
//        if (sender.isOn) {
//            swapBtnText.text = "on"
//            saveDataString(key: sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE, value: String(1))
//        } else {
//            swapBtnText.text = "off"
//            saveDataString(key: sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE, value: String(0))
//        }
    }
    @IBAction func singleChannelControlSwitch(_ sender: UISwitch) {
//        if (sender.isOn) {
//            singleChannelControlText.text = "on"
//            let data = Data([0x01])
//            if (typeMultigrib) { sendDataToHC10(dataForWrite: data, characteristic:                                     sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(16))
//            }
//            else {
//                sendDataToINDY(dataForWrite: data, characteristic: sampleGattAttributes.SET_ONE_CHANNEL)
//            }
//            saveDataString(key: sampleGattAttributes.SET_ONE_CHANNEL, value: String(1))
//        } else {
//            singleChannelControlText.text = "off"
//            let data = Data([0x00])
//            if (typeMultigrib) { sendDataToHC10(dataForWrite: data, characteristic:                                     sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(16))
//            }
//            else {
//                sendDataToINDY(dataForWrite: data, characteristic: sampleGattAttributes.SET_ONE_CHANNEL)
//            }
//            saveDataString(key: sampleGattAttributes.SET_ONE_CHANNEL, value: String(0))
//        }
    }
    @IBAction func reset(_ sender: UIButton) {
        let data = Data([0x01])
        if (typeMultigrib) { sendDataToHC10(dataForWrite: data, characteristic:                                     sampleGattAttributes.FESTO_A_CHARACTERISTIC, myCase: String(15))
        }
        else { sendDataToINDY(dataForWrite: data, characteristic: sampleGattAttributes.RESET_TO_FACTORY_SETTINGS)
        }
        
        
//        swapBtnText.text = "off"
//        swapBtnSwitch.setOn(false, animated: true)
//        saveDataString(key: sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE, value: String(0))
//
//        shutdownCurrentNum.text = "80"
//        shutdownCurrentSlide.setValue(Float(80), animated: true)
//        saveDataString(key: sampleGattAttributes.SHUTDOWN_CURRENT_HDLE, value: String(80))
//
//        singleChannelControlText.text = "off"
//        singleChannelControlSwitch.setOn(false, animated: true)
        saveDataString(key: sampleGattAttributes.SET_ONE_CHANNEL, value: String(0))
    }
    private func sendDataToHC10 (dataForWrite: Data, characteristic: String, myCase: String) {
        self.dataForCommunicate["byteArray"] = dataForWrite.hexEncodedString()
        self.dataForCommunicate["characteristic"] = characteristic
        self.dataForCommunicate["type"] = sampleGattAttributes.WRITE_HC10
        self.dataForCommunicate["case"] = myCase
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: self.dataForCommunicate)
    }
    private func sendDataToINDY (dataForWrite: Data, characteristic: String) {
        self.dataForCommunicate["byteArray"] = dataForWrite.hexEncodedString()
        self.dataForCommunicate["characteristic"] = characteristic
        self.dataForCommunicate["type"] = sampleGattAttributes.WRITE
        NotificationCenter.default.post(name: .notificationFromSensorsViewController, object: nil, userInfo: self.dataForCommunicate)
    }
    @IBAction func perehod(_ sender: Any) {
        saveDataString(key: sampleGattAttributes.READ_THREAD_STRART, value: String(1))
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
    
    private func initUI() {
//        setupeButton(button: resetToFactorySettings)
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.SHUTDOWN_CURRENT_HDLE) {
//                shutdownCurrentNum.text = item.value
//                shutdownCurrentSlide.setValue(Float(item.value)!, animated: true)
            }
            if (item.key == sampleGattAttributes.SWAP_BUTTONS_OPEN_CLOSE) {
                if (Int(item.value) == 1) {
//                    swapBtnText.text = "on"
//                    swapBtnSwitch.setOn(true, animated: true)
                } else {
//                    swapBtnText.text = "off"
//                    swapBtnSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.SET_ONE_CHANNEL) {
                if (Int(item.value) == 1) {
//                    singleChannelControlText.text = "on"
//                    singleChannelControlSwitch.setOn(true, animated: true)
                } else {
//                    singleChannelControlText.text = "off"
//                    singleChannelControlSwitch.setOn(false, animated: true)
                }
            }
            if (item.key == sampleGattAttributes.DEVICE_NAME) {
//                deviceName.text = item.value
            }
            if (item.key == sampleGattAttributes.USE_MULTIGRAB) {
                if (item.value == sampleGattAttributes.USE) {
                    typeMultigrib = true
                }
                else { typeMultigrib = false }
            }
            if (item.key == sampleGattAttributes.STATUS_CONNECTION) {
                if (Int(item.value) == 1) {
//                    statusImage.image = connectStatus
                } else {
//                    statusImage.image = disconnectStatus
                }
            }
        }
    }
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
