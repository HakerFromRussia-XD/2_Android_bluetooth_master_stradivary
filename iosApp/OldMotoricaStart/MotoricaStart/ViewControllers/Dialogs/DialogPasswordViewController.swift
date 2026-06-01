//
//  DialogPasswordViewController.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 26.03.2024.
//  Copyright © 2024 Brian Advent. All rights reserved.
//

import UIKit


class DialogPasswordViewController: UIViewController, UITextFieldDelegate {
    @IBOutlet weak var backgraudView: UIVisualEffectView!
    @IBOutlet weak var passwordTextField: UITextField!
    private var passwordEnteredBefore: Bool = false

    private let sampleGattAttributes = SampleGattAttributes()
    private var savingParametrsMassString:[SaveObjectString]!
    var dataForAdvancedSettingsViewController = ["":""]


    override func viewDidLoad() {
        super.viewDidLoad()
        backgraudView.layer.cornerRadius = 10;
        backgraudView.layer.masksToBounds = true;
        passwordTextField.delegate = self
        
        loadDataString()
        initUI()
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.view.endEditing(true)
    }

    
    @IBAction func acceptPasswordDialog(_ sender: Any) {
        print("валидация пароля")
        self.view.endEditing(true)
        if (passwordTextField.text == sampleGattAttributes.PASSWORD_NUM) {
            dismiss(animated: true, completion: nil)
            dataForAdvancedSettingsViewController["resultDialog"] = String("passwordAccept")
            NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
            saveDataString(key: SensorsViewController.sampleGattAttributes.PASSWORD, value: String(1))
        } else {
            dataForAdvancedSettingsViewController["resultDialog"] = String("passwordCancel")
            NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
            dismiss(animated: true, completion: nil)
        }
    }
    
    
    private func initUI() {
        //проверить вводился ли пароль ранее
        for item in savingParametrsMassString
        {
            if (item.key == sampleGattAttributes.PASSWORD) {
                if (Int(item.value) == 1) {
                    //открываем следующий диалог подтверждения смены серийника
                    dataForAdvancedSettingsViewController["resultDialog"] = String("passwordAccept")
                    NotificationCenter.default.post(name: .notificationDataDialogs, object: nil, userInfo: self.dataForAdvancedSettingsViewController)
                    print("пароль вводился раньше")
                } else {
                    print("пароль не вводился раньше")
                }
            }
        }
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
