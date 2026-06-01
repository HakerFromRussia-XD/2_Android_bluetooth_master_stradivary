//
//  DialogResetViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 15.11.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//

import UIKit


class DialogResetViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    private var testState = true

    var dataForAdvancedSettingsViewController = ["":""]


    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }

    
    @IBAction func cancelResetDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("hardResetCancel")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
    @IBAction func acceptResetDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("hardResetAccept")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
}
