//
//  DialogRecalibrationViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 11.11.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//

import UIKit


class DialogRecalibrationViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    private var testState = true
    
    var dataForAdvancedSettingsViewController = ["":""]
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }
    
    

 
    @IBAction func cancelRecalibrationDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        print("recalibrationResult cancel from dialog")
        dataForAdvancedSettingsViewController["resultDialog"] = String("recalibrationCancel")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
    @IBAction func acceptRecalibrationDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        print("recalibrationResult accept from dialog")
        dataForAdvancedSettingsViewController["resultDialog"] = String("recalibrationAccept")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
    
    
    
    
    private func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
}
