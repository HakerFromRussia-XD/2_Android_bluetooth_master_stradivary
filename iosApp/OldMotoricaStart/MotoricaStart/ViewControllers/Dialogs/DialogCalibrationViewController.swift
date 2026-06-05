//
//  DialogCalibrationViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 15.11.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//

import UIKit


class DialogCalibrationViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    private var testState = true

    var dataForAdvancedSettingsViewController = ["":""]


    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }

    
    @IBAction func cancelCalibrationDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("calibrationCancel")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
    @IBAction func acceptCalibrationDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("calibrationAccept")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
    


    private func saveDataString(key: String, value: String) {
        let saveObjectString = SaveObjectString(key: key, value: value)
        print("save   key: \(key) value: \(value)")
        DataManager.save(saveObjectString, with: key)
    }
}
