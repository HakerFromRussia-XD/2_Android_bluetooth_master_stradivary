//
//  AdvancedSettinsFesthViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.02.2022.
//  Copyright © 2022 Brian Advent. All rights reserved.
import UIKit

class InstractionFesthViewController: UIViewController {
    
    
    @IBOutlet weak var back: UIButton!
    @IBOutlet weak var title_instraction: UILabel!
    @IBOutlet weak var controlInTheAppView: UIView!
    @IBOutlet weak var procthesisUseView: UIView!
    @IBOutlet weak var contactUsView: UIView!
    private let sampleGattAttributes = SampleGattAttributes()
    private var savingParametrsMassString:[SaveObjectString]!
    private var lockAdvancedSettings: Bool = false
    
    @IBAction func unwindToInstractionFesthViewController (sender: UIStoryboardSegue){}

    
    override func viewDidLoad() {
        super.viewDidLoad()
        //быстрый переход в помощь датчиков
//        performSegue(withIdentifier: "goToSensorsSettingsHelp", sender: nil)
//        performSegue(withIdentifier: "goToGesturesSettingsHelp", sender: nil)
//        performSegue(withIdentifier: "goToAdvancedSettingsHelp", sender: nil)
//        performSegue(withIdentifier: "goToAdvancedSettingsHelp", sender: nil)
//        performSegue(withIdentifier: "goToServiceWarrantyHelp", sender: nil)
        
        loadDataString()
        initUI()
    }
    

    

    // MARK: - обработка взаимодействия с UI
    private func initUI() {
        setupeCornerView(myView: controlInTheAppView)
        setupeCornerView(myView: procthesisUseView)
        setupeCornerView(myView: contactUsView)
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.OPEN_ADVANCED_SETTINGS) {
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
    
    
    
    @IBAction func perehodHowProsthesesWorksHelp(_ sender: Any) {
        performSegue(withIdentifier: "goToHowProsthesesWorksHelp", sender: nil)
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
    
    
    
    
    
    
    
    
    @IBAction func openPhone(_ sender: UITapGestureRecognizer) {
        guard let number = URL(string: "tel://" + "88007077197") else { return }
        UIApplication.shared.open(number)
    }
    @IBAction func openVk(_ sender: Any) {
        if let url = URL(string: "https://vk.com/motorica") {
                if #available(iOS 10, *){
                    UIApplication.shared.open(url)
                }else{
                    UIApplication.shared.openURL(url)
                }
            }
    }
    @IBAction func openTelegramm(_ sender: Any) {
        if let url = URL(string: "https://t.me/motoricans") {
                if #available(iOS 10, *){
                    UIApplication.shared.open(url)
                }else{
                    UIApplication.shared.openURL(url)
                }
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

extension UIImageView {
  func setImageColor(color: UIColor) {
    let templateImage = self.image?.withRenderingMode(.alwaysTemplate)
    self.image = templateImage
    self.tintColor = color
  }
}
