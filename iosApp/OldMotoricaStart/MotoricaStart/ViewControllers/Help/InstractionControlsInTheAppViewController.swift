//
//  InstractionSensorsSettingsViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 20.11.2023.
//  Copyright © 2023 Brian Advent. All rights reserved.
//

import UIKit

class InstractionControlsInTheAppViewController: UIViewController {
    
    
    @IBOutlet weak var back: UIButton!
    @IBOutlet weak var title_instraction: UILabel!
    @IBOutlet weak var mainView: UIView!
    @IBOutlet weak var controlInTheAppView: UIView!
    private let sampleGattAttributes = SampleGattAttributes()
    private var savingParametrsMassString:[SaveObjectString]!
    private var lockAdvancedSettings: Bool = false

    
    override func viewDidLoad() {
        super.viewDidLoad()

        loadDataString()
        initUI()
    }
    

    

    // MARK: - обработка взаимодействия с UI
    private func initUI() {
        setupeCornerView(myView: controlInTheAppView)
        setupeCornerView(myView: mainView)
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.OPEN_ADVANCED_SETTINGS) {
                print("OPEN_ADVANCED_SETTINGS  item.value = "+item.value)
                if (Int(item.value) == 1) {
                    lockAdvancedSettings = true
                }
            }
        }

        back.setTitleColor(UIColor.black, for: .normal)
        title_instraction.textColor = UIColor.black
    }
    @IBAction func perehodSensorsSettingsHelp(_ sender: Any) {
        performSegue(withIdentifier: "goToSensorsSettingsHelp", sender: nil)
    }
    @IBAction func perehodGesturesSettingsHelp(_ sender: Any) {
        performSegue(withIdentifier: "goToGesturesSettingsHelp", sender: nil)
    }
    @IBAction func perehodAdvancedSettingsHelp(_ sender: Any) {
        if (lockAdvancedSettings){
            performSegue(withIdentifier: "goToAdvancedSettingsHelp", sender: nil)
        } else {
            let titleDialog = NSLocalizedString("You do not have access to this section of the manual!", comment: "")
            //1. Create the alert controller.
            let alert = UIAlertController(title: titleDialog, message: "", preferredStyle: .alert)
            

            // 2. Grab the value from the text field, and print it when the user clicks OK.
            alert.addAction(UIAlertAction(title: NSLocalizedString("Ok", comment: ""), style: .default, handler: { (_) in
            }))
            
            // 3. Present the alert.
            self.present(alert, animated: true, completion: nil)
        }
    }
    
    
    
    
    // MARK: - работа с фоном
    override func viewDidLayoutSubviews() {}
    
    // MARK: - UI stule
    private func setupeCornerView (myView: UIView) {
        myView.layer.cornerRadius = 21
        myView.layer.shadowColor = UIColor.black.cgColor
        myView.layer.masksToBounds = false;
        myView.layer.shadowOffset = CGSizeMake(0, 4);
        myView.layer.shadowRadius = 2;
        myView.layer.shadowOpacity = 0.08;
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
