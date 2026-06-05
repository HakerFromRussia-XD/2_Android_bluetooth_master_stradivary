//
//  DialogSetSerialNumberViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 26.03.2024.
//  Copyright © 2024 Brian Advent. All rights reserved.
//

import UIKit


class DialogSetSerialNumberViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    var dataForAdvancedSettingsViewController = ["":""]

    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }

    @IBAction func cancelSetSerialNumberDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }
    @IBAction func acceptSetSerialNumberDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForAdvancedSettingsViewController["resultDialog"] = String("writeSerialNumberAccept")
        NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
    }
}
