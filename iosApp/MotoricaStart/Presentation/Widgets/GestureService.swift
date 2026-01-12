//
//  GestureService.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 12.01.2026.
//

import Foundation

@objcMembers
final class GestureService: NSObject {
    private let gestureNamesStorage = CustomGestureNamesStorage.shared

    @objc public func setNameGesture(numberGesture: Int, name: String) {
        let index = numberGesture - 64
        let names = gestureNamesStorage.updateName(name, at: index)
        print("Вызвана функция setNameGesture numberGesture = \(numberGesture)  name = \(name)  names = \(names)")
    }

    @objc public func getGestureName(numberGesture: Int) -> String {
        let index = numberGesture
        let names = gestureNamesStorage.loadNames()
        guard names.indices.contains(index) else { return names.first ?? "" }
        print("Вызвана функция getGestureName numberGesture = \(numberGesture)  names = \(names)")
        print("Вызвана функция getGestureName name = \(names[index])")
        return names[index]
    }
    @objc public func getDeviceName() -> String { "" }
    @objc public func getStatusConnection() -> Int { 0 }
    @objc public func getGestureNum() -> Int {
        let index = 64
        let names = gestureNamesStorage.loadNames()
//        let testVal = names.indices.contains(index)
        print("Вызвана функция getGestureNum names = \(names)")
        return 64
    }
    @objc public func getUseFestX() -> Int { 0 }
    @objc public func getHandSide() -> Int { 0 }
    @objc public func getGestureTable() -> String { "" }
    @objc public func getGestureTableBig() -> String { "" }

    @objc public func getFingersDelay() -> String {
        let data: String = ""
        return data
    }

    @objc public func getFingersDelaySwitch() -> Int { 0 }
    @objc public func getVersionDriverGreaterThan237() -> Bool { true }

    @objc public func sendDataToFest(dataForWrite: Data, characteristic: String, typeFestX: Bool) {
        print("Вызвана функция sendDataToFest  typeFestX = \(typeFestX)")
    }

    @objc func saveDataString(key: String, value: String) {
        print("save   key: \(key) value: \(value)")
    }
}
