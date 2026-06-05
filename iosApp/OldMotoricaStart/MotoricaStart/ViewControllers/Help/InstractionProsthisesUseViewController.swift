//
//  InstractionProsthisesUseViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 21.11.2023.
//  Copyright © 2023 Brian Advent. All rights reserved.
//

import UIKit

class InstractionProsthisesUseViewController: UIViewController {
    
    
    @IBOutlet weak var back: UIButton!
    @IBOutlet weak var title_instraction: UILabel!
    @IBOutlet weak var mainView: UIView!
    @IBOutlet weak var prosthisesUseView: UIView!
    private let sampleGattAttributes = SampleGattAttributes()
    private var savingParametrsMassString:[SaveObjectString]!
    private var typeMultigribNewVM: Bool = false
    private var typeMultigribNew: Bool = false

    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        loadDataString()
        initUI()
    }
    

    

    // MARK: - обработка взаимодействия с UI
    private func initUI() {
        setupeCornerView(myView: prosthisesUseView)
        setupeCornerView(myView: mainView)
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.USE_MULTIGRAB_FESTX) {
                print("AnyperehodServiceWarrantyHelp USE_MULTIGRAB_FESTX item="+item.value)
                if (Int(item.value) == 1) {
                    typeMultigribNewVM = true
                }
            }
            if (item.key == sampleGattAttributes.USE_MULTIGRAB_FESTH) {
                print("AnyperehodServiceWarrantyHelp USE_MULTIGRAB_FESTH item="+item.value)
                if (Int(item.value) == 1) {
                    typeMultigribNew = true
                }
            }
        }

        back.setTitleColor(UIColor.black, for: .normal)
        title_instraction.textColor = UIColor.black
    }
    @IBAction func perehodHowProsthesesWorksHelp(_ sender: Any) {
        if (typeMultigribNew || typeMultigribNewVM) {
            print("AnyperehodServiceWarrantyHelp goToHowProsthesesWorksHelp")
            performSegue(withIdentifier: "goToHowProsthesesWorksHelp", sender: nil)
        } else {
            print("AnyperehodServiceWarrantyHelp goToHowProsthesesWorksMonoHelp")
            performSegue(withIdentifier: "goToHowProsthesesWorksMonoHelp", sender: nil)
        }
    }
    @IBAction func perehodProsthesesSocketHelp(_ sender: Any) {
            performSegue(withIdentifier: "goToProsthesesSocketHelp", sender: nil)
    }
    @IBAction func perehodCompleteSetHelp(_ sender: Any) {
            performSegue(withIdentifier: "goToCompleteSetHelp", sender: nil)
    }
    @IBAction func perehodChargingTheProsthesesHelp(_ sender: Any) {
            performSegue(withIdentifier: "goToChargingTheProsthesesHelp", sender: nil)
    }
    @IBAction func perehodProsthesesCareHelp(_ sender: Any) {
            performSegue(withIdentifier: "goToProsthesesCareHelp", sender: nil)
    }
    @IBAction func perehodServiceWarrantyHelp(_ sender: Any) {
            performSegue(withIdentifier: "goToServiceWarrantyHelp", sender: nil)
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

