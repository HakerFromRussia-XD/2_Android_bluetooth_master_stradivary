//
//  DialogWaiteViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 18.05.2023.
//  Copyright © 2023 Brian Advent. All rights reserved.
//

import UIKit


class DialogWaitViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!


    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
    }


    @IBAction func acceptSelectScaleDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }

}
