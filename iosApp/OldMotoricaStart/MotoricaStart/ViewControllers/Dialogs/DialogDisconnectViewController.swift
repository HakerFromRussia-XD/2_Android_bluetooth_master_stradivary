//
//  DialogDisconnectViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 26.05.2023.
//  Copyright © 2023 Brian Advent. All rights reserved.
//

import UIKit


class DialogDisconnectViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    var dataForSensorsViewController = ["":""]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }
    
    @IBAction func cancelAcceptedScaleDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)

    }
    @IBAction func acceptAcceptedScaleDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForSensorsViewController["resultDialog"] = String("selectDisconnectAccept")
        NotificationCenter.default.post(name: .notificationDataDialogToSensorsView, object: nil, userInfo: self.dataForSensorsViewController)
    }
}
