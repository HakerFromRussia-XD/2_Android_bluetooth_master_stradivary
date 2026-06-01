//
//  NameUtil.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 21.03.2024.
//  Copyright © 2024 Brian Advent. All rights reserved.
//

import Foundation

class NameUtil {
    let sampleGattAttributes = SampleGattAttributes()
    var newName = ""
    var validationError = ""
    
    func getCleanName (deviceName: String) -> String {
        newName = deviceName
        if (deviceName.contains(sampleGattAttributes.FESTX_NAME)) {
            if (deviceName.count > 6) {
                if (deviceName.contains(" ")) {
                    return deviceName
                }
                

                let namePrefix = deviceName[6 ..< 10]
                let nameCode = deviceName[10 ..< deviceName.count]
                print("Test getCleanName() deviceName: \(deviceName)")
//                print("Test getCleanName() namePrefix: \(namePrefix)")
                print("Test getCleanName() nameCode: \(nameCode)")

                switch namePrefix {
                    case sampleGattAttributes.NEW_DEVICE_TYPE_FEST_F:
                        newName = "FEST-F-\(nameCode)"
                    case sampleGattAttributes.NEW_DEVICE_TYPE_FEST_H:
                        newName = "FEST-H-\(nameCode)"
                    case sampleGattAttributes.NEW_DEVICE_TYPE_FEST_F_O:
                        newName = "FEST-FO-\(nameCode)"
                    case sampleGattAttributes.NEW_DEVICE_TYPE_FEST_H_O:
                        newName = "FEST-HO-\(nameCode)"
                    case sampleGattAttributes.NEW_DEVICE_TYPE_FEST_EP:
                        newName = "FEST-EP-\(nameCode)"
                    case sampleGattAttributes.NEW_DEVICE_TYPE_FEST_EB:
                        newName = "FEST-EB-\(nameCode)"
                    default:
                        newName = deviceName
                }
            }
        }
        return newName
    }
    func validationAndConversionSerialNumber(serialNumber: String)-> String {
        print("serialNumber: "+serialNumber)//"FEST-F-11111"

        if (serialNumber.contains("-") && (serialNumber.count >= 12)){
            let namePrefix = serialNumber.components(separatedBy:"-")[0]+"-"+serialNumber.components(separatedBy:"-")[1]
            print("namePrefix = "+namePrefix+"    sampleGattAttributes.DEVICE_TYPE_INDY_H = "+sampleGattAttributes.DEVICE_TYPE_INDY_H)
            
            if (namePrefix == sampleGattAttributes.DEVICE_TYPE_INDY_H) {
                print("namePrefix == DEVICE_TYPE_INDY_H")
            } else {
                print("namePrefix != DEVICE_TYPE_INDY_H")
            }
//            switch "test3" {
//            case "test1":
//                print("test 1")
//                break
//            case "test2":
//                print("test 2")
//                break
//            case "test3":
//                print("test 3")
//                break
//            default:
//                print("test default")
//            }
            
            switch namePrefix {
            case sampleGattAttributes.DEVICE_TYPE_FEST_F:
                break
            case sampleGattAttributes.DEVICE_TYPE_FEST_H: 
                break
            case sampleGattAttributes.DEVICE_TYPE_FEST_H_EP: 
                break
            case sampleGattAttributes.DEVICE_TYPE_FEST_H_EB: 
                break
            case sampleGattAttributes.DEVICE_TYPE_INDY_F: 
                print("test INDY_F")
                break
            case sampleGattAttributes.DEVICE_TYPE_INDY_H: 
                print("test INDY_H")
                break
            case sampleGattAttributes.DEVICE_TYPE_INDY_H_EP: 
                print("test INDY__H_EP")
                break
            case sampleGattAttributes.DEVICE_TYPE_INDY_H_EB:
                print("test INDY__H_EB")
                break
            default:
                print("test default")
                validationError = "В нашей линейке продуктов нет: "+namePrefix
            }

            var nameBridge: String = serialNumber[6 ..< 7]
            if (serialNumber.components(separatedBy:"-")[1].count == 2) { nameBridge = serialNumber[7..<8]}
            if (nameBridge != "-") {
                validationError = "Буквенную и числовую части должен разделять дефис"
                return "false"
            }

            if ((serialNumber.components(separatedBy:"-")[1].count == 1) && serialNumber.count < 12 || ((serialNumber.components(separatedBy:"-")[1].count == 2) && serialNumber.count < 13)) {
                validationError = "Вы ввели слишком короткий серийный номер"
                return "false"
            } else {
                if (((serialNumber.components(separatedBy:"-")[1].count == 1) && serialNumber.count > 12) || ((serialNumber.components(separatedBy:"-")[1].count == 2) && serialNumber.count > 13)) {
                validationError = "Вы ввели слишком длинный серийный номер"
                return "false"
              }
            }

            var nameCode: String = serialNumber[7 ..< serialNumber.count]
            if (serialNumber.components(separatedBy:"-")[1].count == 2) { nameCode = serialNumber[8 ..< serialNumber.count] }
            let x: Int? = Int(nameCode)

            if (x?.description == nil) {
                validationError = "Вторая часть серийного номера не число: "+nameCode
                return "false"
            } else {
                validationError = "Типо ошибок нет"
            }
            print("nameCode = "+nameCode+"  "+(x?.description ?? "ne chislo"))

            
            switch namePrefix {
            case sampleGattAttributes.DEVICE_TYPE_FEST_F:
                return sampleGattAttributes.FESTX_NAME+sampleGattAttributes.NEW_DEVICE_TYPE_FEST_F+nameCode
            case sampleGattAttributes.DEVICE_TYPE_FEST_H:
                return sampleGattAttributes.FESTX_NAME+sampleGattAttributes.NEW_DEVICE_TYPE_FEST_H+nameCode
            case sampleGattAttributes.DEVICE_TYPE_FEST_H_EP:
                return sampleGattAttributes.FESTX_NAME+sampleGattAttributes.NEW_DEVICE_TYPE_FEST_EP+nameCode
            case sampleGattAttributes.DEVICE_TYPE_FEST_H_EB:
                return sampleGattAttributes.FESTX_NAME+sampleGattAttributes.NEW_DEVICE_TYPE_FEST_EB+nameCode
                
                
            case sampleGattAttributes.DEVICE_TYPE_INDY_F:
                return sampleGattAttributes.DEVICE_TYPE_INDY_F+"-"+nameCode
            case sampleGattAttributes.DEVICE_TYPE_INDY_H:
                return sampleGattAttributes.DEVICE_TYPE_INDY_H+"-"+nameCode
            case sampleGattAttributes.DEVICE_TYPE_INDY_H_EP:
                return sampleGattAttributes.DEVICE_TYPE_INDY_H_EP+"-"+nameCode
            case sampleGattAttributes.DEVICE_TYPE_INDY_H_EB:
                return sampleGattAttributes.DEVICE_TYPE_INDY_H_EB+"-"+nameCode
                
                
            default:
                validationError = "В нашей линейке продуктов нет: "+namePrefix
            }
        } else {
            validationError = "Вы ввели некорректный серийный номер"
        }
        
        return "false"
      }
    
    func getvalidationError(serialNumber: String) -> String {
        validationAndConversionSerialNumber(serialNumber: serialNumber)
        return validationError
    }
}
