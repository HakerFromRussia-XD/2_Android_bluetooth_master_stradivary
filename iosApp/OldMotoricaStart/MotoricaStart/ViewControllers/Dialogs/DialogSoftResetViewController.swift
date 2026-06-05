//
//  DialogSoftResetViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 25.03.2024.
//  Copyright © 2024 Brian Advent. All rights reserved.
//

import UIKit


class DialogSoftResetViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    private var testState = true

    var dataForAdvancedSettingsViewController = ["":""]


    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }

    
    @IBAction func cancelSoftResetDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("softResetCancel")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
    @IBAction func acceptSoftResetDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("softResetAccept")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
}

