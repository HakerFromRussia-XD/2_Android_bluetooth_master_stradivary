//
//  DialogCalibrationStatusViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 14.07.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//

import UIKit


class DialogCalibrationStatusViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    @IBOutlet weak var statusFinger1: UILabel!
    @IBOutlet weak var encoderSteps1: UILabel!
    @IBOutlet weak var current1: UILabel!
    @IBOutlet weak var statusFinger2: UILabel!
    @IBOutlet weak var encoderSteps2: UILabel!
    @IBOutlet weak var current2: UILabel!
    @IBOutlet weak var statusFinger3: UILabel!
    @IBOutlet weak var encoderSteps3: UILabel!
    @IBOutlet weak var current3: UILabel!
    @IBOutlet weak var statusFinger4: UILabel!
    @IBOutlet weak var encoderSteps4: UILabel!
    @IBOutlet weak var current4: UILabel!
    @IBOutlet weak var statusFinger5: UILabel!
    @IBOutlet weak var encoderSteps5: UILabel!
    @IBOutlet weak var current5: UILabel!
    @IBOutlet weak var statusFinger6: UILabel!
    @IBOutlet weak var encoderSteps6: UILabel!
    @IBOutlet weak var current6: UILabel!
    
    
    private var savingParametrsMassString:[SaveObjectString]!
    let sampleGattAttributes = SampleGattAttributes()
    private var dataForCommunicateRead = ["byteArray": "01020304", "characteristic":"1", "type":"READ"]
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;

        readDataFromFestX(characteristic: sampleGattAttributes.STATUS_CALIBRATION_NEW_VM)
        initUI()
        
        NotificationCenter.default.addObserver(self, selector: #selector(updatingUI), name: .notificationReseiveBLEDataCalibrationStatus, object: nil)
    }
    @IBAction func tapClose(_ sender: Any) {
        dismiss(animated: true, completion: nil)
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
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_FINGER+"1")) {
                statusFinger1.text = NSLocalizedString("status_finger_1", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_FINGER+"2")) {
                statusFinger2.text = NSLocalizedString("status_finger_2", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_FINGER+"3")) {
                statusFinger3.text = NSLocalizedString("status_finger_3", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_FINGER+"4")) {
                statusFinger4.text = NSLocalizedString("status_finger_4", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_FINGER+"5")) {
                statusFinger5.text = NSLocalizedString("status_finger_5", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_FINGER+"6")) {
                statusFinger6.text = NSLocalizedString("status_finger_6", comment: "") + item.value
            }
            
            
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_ENCODER_FINGER+"1")) {
                encoderSteps1.text = NSLocalizedString("encoder_steps", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_ENCODER_FINGER+"2")) {
                encoderSteps2.text = NSLocalizedString("encoder_steps", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_ENCODER_FINGER+"3")) {
                encoderSteps3.text = NSLocalizedString("encoder_steps", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_ENCODER_FINGER+"4")) {
                encoderSteps4.text = NSLocalizedString("encoder_steps", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_ENCODER_FINGER+"5")) {
                encoderSteps5.text = NSLocalizedString("encoder_steps", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_ENCODER_FINGER+"6")) {
                encoderSteps6.text = NSLocalizedString("encoder_steps", comment: "") + item.value
            }
            
            
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_CURRENT_FINGER+"1")) {
                current1.text = NSLocalizedString("current", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_CURRENT_FINGER+"2")) {
                current2.text = NSLocalizedString("current", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_CURRENT_FINGER+"3")) {
                current3.text = NSLocalizedString("current", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_CURRENT_FINGER+"4")) {
                current4.text = NSLocalizedString("current", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_CURRENT_FINGER+"5")) {
                current5.text = NSLocalizedString("current", comment: "") + item.value
            }
            if (item.key == (sampleGattAttributes.CALIBRATION_STATUS_CURRENT_FINGER+"6")) {
                current6.text = NSLocalizedString("current", comment: "") + item.value
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
        static let notificationReseiveBLEDataCalibrationStatus = Notification.Name(rawValue: "notificationReseiveBLEDataCalibrationStatus")
    }
