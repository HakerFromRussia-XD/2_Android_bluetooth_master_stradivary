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
    static let customGestureNamesDidUpdate = Notification.Name("CustomGestureNamesDidUpdate")
}

private func intValue(from value: Any?) -> Int? {
    switch value {
    case let kotlinInt as KotlinInt:
        return Int(kotlinInt.intValue)
    case let kotlinLong as KotlinLong:
        return Int(kotlinLong.intValue)
    case let kotlinUInt as KotlinUInt:
        return Int(kotlinUInt.intValue)
    case let number as NSNumber:
        return number.intValue
    default:
        return nil
    }
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
final class GestureSettingsViewModelV3: NSObject {
    static let shared = GestureSettingsViewModelV3()
    private(set) var latestParameterRef: ParameterRef?
    private(set) var latestParameterData: String?

    private override init() {
        super.init()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleGestureSettingsUpdate(_:)),
            name: .gestureSettingsDidUpdateV3,
            object: nil
        )
    }

    @objc private func handleGestureSettingsUpdate(_ notification: Notification) {
        guard let parameterInfo = notification.userInfo?["dataV3"] as? ParameterInfo<AnyObject, AnyObject, AnyObject, AnyObject> else {
            return
        }
        guard
            let parameterID = intValue(from: parameterInfo.parameterID),
            let dataCode = intValue(from: parameterInfo.dataCode),
            let deviceAddress = intValue(from: parameterInfo.deviceAddress)
        else {
            return
        }

        let parameterRef = ParameterRef(
            addressDevice: Int32(deviceAddress),
            parameterID: Int32(parameterID),
            dataCode: Int32(dataCode)
        )

        var parameterData = WidgetStateBridgeV3.shared.getCurrent(
            addressDevice: Int32(deviceAddress),
            parameterID: Int32(parameterID),
            dataCode: Int32(dataCode)
        )?.serializedValue

        if parameterData == nil,
           let typedParameterInfo = parameterInfo as? ParameterInfo<KotlinInt, KotlinInt, KotlinInt, KotlinInt> {
            parameterData = ParameterProvider.Companion().getParameterV3(parameterInfo: typedParameterInfo).data
        }

        guard let resolvedParameterData = parameterData else {
            return
        }

        latestParameterRef = parameterRef
        latestParameterData = resolvedParameterData
        NotificationCenter.default.post(
            name: .gestureSettingsViewModelDidUpdateV3,
            object: self,
            userInfo: [
                "data": parameterRef,
                "parameterData": resolvedParameterData
            ]
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
        _ = GestureSettingsViewModelV3.shared
    }
    
    @objc public func setNameGesture(numberGesture: Int, name: String) {
        let index = numberGesture - 64
        let names = updateName(name, at: index)
        print("Вызвана функция setNameGesture numberGesture = \(numberGesture)  name = \(name)  names = \(names)")
        NotificationCenter.default.post(
            name: .customGestureNamesDidUpdate,
            object: self,
            userInfo: [
                "gestureId": numberGesture,
                "name": name,
                "names": names
            ]
        )
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
    @objc public func getHandSide() -> Int {
        V3HandSideProvider.shared.startObserving()
        return V3HandSideProvider.shared.currentSide
    }
    @objc public func decodeGestureSettings(raw: String) -> Gesture? {
//        print("Вызвана функция decodeGestureSettings  raw = \(raw)")
        guard !raw.isEmpty else { return nil }
        return SerializationObjects.shared.decodeGesture(raw: "\"\(raw)\"")
    }
    @objc(decodeGestureSettingsV3WithRaw:)
    public func decodeGestureSettingsV3(raw: String) -> Gesture? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        guard
            let data = trimmed.data(using: .utf8),
            let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }

        let keys = [
            "gestureId",
            "openPosition1", "openPosition2", "openPosition3", "openPosition4", "openPosition5", "openPosition6",
            "closePosition1", "closePosition2", "closePosition3", "closePosition4", "closePosition5", "closePosition6",
            "openToCloseTimeShift1", "openToCloseTimeShift2", "openToCloseTimeShift3", "openToCloseTimeShift4", "openToCloseTimeShift5", "openToCloseTimeShift6",
            "closeToOpenTimeShift1", "closeToOpenTimeShift2", "closeToOpenTimeShift3", "closeToOpenTimeShift4", "closeToOpenTimeShift5", "closeToOpenTimeShift6"
        ]

        let hex = keys
            .map { key -> String in
                let value = max(0, min(255, intValue(from: json[key]) ?? 0))
                return String(format: "%02X", value)
            }
            .joined()

        return SerializationObjects.shared.decodeGesture(raw: "\"\(hex)\"")
    }

    @objc public func getFingersDelaySwitch() -> Int { 1 }
    @objc public func sendDataToFest(dataForWrite: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: dataForWrite,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }
    @objc(sendDataToFestV3WithDataForWrite:)
    public func sendDataToFestV3(dataForWrite: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: dataForWrite,
            command: gatt.SERIALPORTCHAR_UUID,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }
    @objc(requestGestureSettingsV3WithGestureId:)
    public func requestGestureSettingsV3(gestureId: Int) {
        let data = BLECommandsV3.shared.requestGestureInfo(gestureId: Int32(gestureId))
        sendDataToFestV3(dataForWrite: data)
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
