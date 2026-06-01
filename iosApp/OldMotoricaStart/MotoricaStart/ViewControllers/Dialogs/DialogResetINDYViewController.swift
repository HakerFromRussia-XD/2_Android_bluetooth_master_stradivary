//
//  DialogResetINDYViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 18.11.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//


import UIKit


class DialogResetINDYViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    private var testState = true

    var dataForAdvancedSettingsViewController = ["":""]


    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }

    
    @IBAction func cancelResetINDYDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("resetCancel")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
    @IBAction func acceptResetINDYDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("resetAccept")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
}
