//
//  GestureService.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 12.01.2026.
//

import Foundation
import shared

extension Notification.Name {
    static let gestureSettingsDidUpdate = Notification.Name("GestureSettingsDidUpdate")
    static let gestureSettingsViewModelDidUpdate = Notification.Name("GestureSettingsViewModelDidUpdate")
    static let gestureSettingsDidUpdateV3 = Notification.Name("GestureSettingsV3DidUpdate")
    static let gestureSettingsViewModelDidUpdateV3 = Notification.Name("GestureSettingsV3ViewModelDidUpdate")
}

@objcMembers
final class GestureSettingsViewModel: NSObject {
    static let shared = GestureSettingsViewModel()
    private(set) var latestParameterRef: ParameterRef?


    private override init() {
        super.init()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleGestureSettingsUpdate(_:)),
            name: .gestureSettingsDidUpdate,
            object: nil
        )
    }

    @objc private func handleGestureSettingsUpdate(_ notification: Notification) {
        guard let parameterRef = notification.userInfo?["data"] as? ParameterRef else { return }
        latestParameterRef = parameterRef
        NotificationCenter.default.post(
            name: .gestureSettingsViewModelDidUpdate,
            object: self,
            userInfo: ["data": parameterRef]
        )
    }
}

@objcMembers
final class GestureService: NSObject {
    static let shared = GestureService()
    private let keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    private lazy var bleManager: BleManagerKmm = {
        _ = BLEComponents.shared
        return BleEnvironment.shared.getBleManager()
    }()
    
    override init() {
        super.init()
        _ = GestureSettingsViewModel.shared
    }
    
    @objc public func setNameGesture(numberGesture: Int, name: String) {
        let index = numberGesture - 64
        let names = updateName(name, at: index)
        print("Вызвана функция setNameGesture numberGesture = \(numberGesture)  name = \(name)  names = \(names)")
    }
    @objc public func getGestureName(numberGesture: Int) -> String {
        let index = numberGesture - 64
        let names = loadNames()
        guard names.indices.contains(index) else { return names.first ?? "" }
        print("Вызвана функция getGestureName numberGesture = \(numberGesture)  names = \(names)")
        print("Вызвана функция getGestureName name = \(names[index])")
        return names[index]
    }
    @objc public func getDeviceName() -> String {
        do {
            return try keyValueStorage.load(for: BluetoothStorageKeys.selectedDeviceNameStorageKey) ?? ""
        } catch {
            print("[Storage] failed to load selected device name: \(error)")
            return ""
        }
    }
    @objc public func getParameterData(deviceAddress: Int, parameterID: Int) -> NSString {
        let parameter = ParameterProvider.Companion().getParameter(deviceAddress: Int32(deviceAddress), parameterID: Int32(parameterID))
        return parameter.data as NSString
    }
    @objc public func getStatusConnection() -> Int { 1 }
    @objc public func getHandSide() -> Int { 1 }
    @objc public func decodeGestureSettings(raw: String) -> Gesture? {
//        print("Вызвана функция decodeGestureSettings  raw = \(raw)")
        guard !raw.isEmpty else { return nil }
        return SerializationObjects.shared.decodeGesture(raw: "\"\(raw)\"")
    }

    @objc public func getFingersDelaySwitch() -> Int { 1 }
    @objc public func sendDataToFest(dataForWrite: KotlinByteArray) {
        GestureListItemViewModel.sendFestData(
            data: dataForWrite,
            bleManager: bleManager
        )
    }

    func loadNames() -> [String] {
        let defaults = Self.defaultNames()
        let key = TypedStorageKey<[String]>(
            rawValue: BluetoothStorageKeys.customGestureNameStorageKey.rawValue + getDeviceName()
        )
        do {
            
            if var stored = try keyValueStorage.load(for: key) {
                if stored.count != defaults.count {
                    stored = Self.mergedNames(stored, defaults: defaults)
                    try? keyValueStorage.save(stored, for: key)
                }
                print ("loadNames 3")
                return stored
            }
        } catch {
            print ("loadNames 1")
        }

        try? keyValueStorage.save(defaults, for: key)
        print ("loadNames 2")
        return defaults
    }
    @discardableResult
    func updateName(_ name: String, at index: Int) -> [String] {
        let key = TypedStorageKey<[String]>(
            rawValue: BluetoothStorageKeys.customGestureNameStorageKey.rawValue + getDeviceName()
        )
        var names = loadNames()
        guard names.indices.contains(index) else { return names }
        names[index] = name
        print("Вызвана функция updateName names = \(names)")
        try? keyValueStorage.save(names, for: key)
        return names
    }
    private static func mergedNames(_ stored: [String], defaults: [String]) -> [String] {
        if stored.count >= defaults.count {
            return Array(stored.prefix(defaults.count))
        }
        return stored + Array(defaults[stored.count...])
    }
    private static func defaultNames() -> [String] {
        return [
            SharedRes.strings().gesture_1_btn,
            SharedRes.strings().gesture_2_btn,
            SharedRes.strings().gesture_3_btn,
            SharedRes.strings().gesture_4_btn,
            SharedRes.strings().gesture_5_btn,
            SharedRes.strings().gesture_6_btn,
            SharedRes.strings().gesture_7_btn,
            SharedRes.strings().gesture_8_btn,
            SharedRes.strings().gesture_9_btn,
            SharedRes.strings().gesture_10_btn,
            SharedRes.strings().gesture_11_btn,
            SharedRes.strings().gesture_12_btn,
            SharedRes.strings().gesture_13_btn,
            SharedRes.strings().gesture_14_btn
        ].map { $0.desc().localized() }
    }
    
    @objc func gestureStateOpen() -> String {
        return SharedRes.strings().gesture_state_open.desc().localized()
    }
    @objc func gestureStateClose() -> String {
        return SharedRes.strings().gesture_state_close.desc().localized()
    }
}
