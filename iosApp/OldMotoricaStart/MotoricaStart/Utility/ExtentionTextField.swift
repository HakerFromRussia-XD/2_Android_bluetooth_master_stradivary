//
//  ExtentionTextField.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 18.01.2024.
//  Copyright © 2024 Brian Advent. All rights reserved.
//

import UIKit

private var kAssociationKeyMaxLength: Int = 0
private var kAssociationKeyMaxLengthTextView: Int = 0

extension UITextField {
        @IBInspectable var maxLength: Int {
            get {
                if let length = objc_getAssociatedObject(self, &kAssociationKeyMaxLength) as? Int {
                    return length
                } else {
                    return Int.max
                }
            }
            set {
                objc_setAssociatedObject(self, &kAssociationKeyMaxLength, newValue, .OBJC_ASSOCIATION_RETAIN)
                addTarget(self, action: #selector(checkMaxLength), for: .editingChanged)
            }
        }

        @objc func checkMaxLength(textField: UITextField) {
            guard let prospectiveText = self.text,
                prospectiveText.count > maxLength
                else {
                    return
            }

            let selection = selectedTextRange

            let indexEndOfText = prospectiveText.index(prospectiveText.startIndex, offsetBy: maxLength)
            let substring = prospectiveText[..<indexEndOfText]
            text = String(substring)

            selectedTextRange = selection
        }
    }
