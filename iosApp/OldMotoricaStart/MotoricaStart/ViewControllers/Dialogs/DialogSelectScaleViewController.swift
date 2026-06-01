//
//  DialogSelectScaleViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 17.11.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
//

import UIKit


class DialogSelectScaleViewController: UIViewController {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    @IBOutlet weak var selectorScale: UISegmentedControl!
    private var testState = true
    private var scaleInt: UInt8 = 0x00

    private let sampleGattAttributes = SampleGattAttributes()
    private var savingParametrsMassString:[SaveObjectString]!
    var dataForSensorsViewController = ["":""]


    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
        
        loadDataString()
        initUI()
    }

    
    @IBAction func changeScale(_ sender: UISegmentedControl) {
        scaleInt = UInt8(sender.selectedSegmentIndex)
        print("select scale \(scaleInt)")
        saveDataString(key: sampleGattAttributes.SCALE, value: String(sender.selectedSegmentIndex))
    }
    @IBAction func acceptSelectScaleDialog(_ sender: Any) {
        dismiss(animated: true, completion: nil)
        dataForSensorsViewController["resultDialog"] = String("selectScaleAccept")
        NotificationCenter.default.post(name: .notificationDataDialogToSensorsView, object: nil, userInfo: self.dataForSensorsViewController)
    }
    
    
    private func initUI() {
        setupeSecetView(segmentedControl: selectorScale)
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.SCALE) {
                selectorScale.selectedSegmentIndex = Int(item.value) ?? -1
            }
        }
    }
    private func setupeSecetView (segmentedControl: UISegmentedControl) {
        segmentedControl.layer.cornerRadius = 5
        segmentedControl.layer.borderWidth = 2
        segmentedControl.layer.borderColor = UIColor.white.cgColor
        let titleTextAttributes = [NSAttributedStringKey.foregroundColor: UIColor.white]
        segmentedControl.setTitleTextAttributes(titleTextAttributes, for: .normal)
        segmentedControl.setTitleTextAttributes(titleTextAttributes, for: .selected)
    }

    
    // MARK: - работа с памятью
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
