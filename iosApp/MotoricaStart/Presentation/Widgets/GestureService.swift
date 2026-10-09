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

func gestureSettingsIntValue(from value: Any?) -> Int? {
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

private struct LegacySavedString: Decodable {
    let key: String
    let value: String
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

enum V3GestureEditorAction {
    case settingsRequested(Int32)
    case settingsWriteRequested(V3GestureSettings, V3GestureCommand, String)
    case settingsReceived(V3GestureSettingsResponse)
    case nameChanged(Int, String)
}

struct V3GestureEditorUiState {
    let parameterRef: ParameterRef?
    let parameterData: String?
    let customGestureNames: [String]
}

@objcMembers
final class GestureSettingsViewModelV3: NSObject {
    static let shared = WidgetsSceneDIContainer.makeGestureSettingsViewModelV3()
    private let requestGestureSettingsUseCase: RequestGestureSettingsUseCaseV3
    private let writeGestureSettingsUseCase: WriteGestureSettingsUseCaseV3
    private let getGestureEditorHandSideUseCase: GetGestureEditorHandSideUseCaseV3
    private let getCustomGestureNamesUseCase: GetCustomGestureNamesUseCaseV3
    private let renameCustomGestureUseCase: RenameCustomGestureUseCaseV3
    private var stopSettingsObservation: (() -> Void)?
    @nonobjc private(set) var uiState: V3GestureEditorUiState?
    var latestParameterRef: ParameterRef? { uiState?.parameterRef }
    var latestParameterData: String? { uiState?.parameterData }
    var currentHandSide: Int { Int(getGestureEditorHandSideUseCase.invoke()) }

    init(
        requestGestureSettingsUseCase: RequestGestureSettingsUseCaseV3,
        writeGestureSettingsUseCase: WriteGestureSettingsUseCaseV3,
        observeGestureSettingsUseCase: ObserveGestureSettingsUseCaseV3,
        getGestureEditorHandSideUseCase: GetGestureEditorHandSideUseCaseV3,
        getCustomGestureNamesUseCase: GetCustomGestureNamesUseCaseV3,
        renameCustomGestureUseCase: RenameCustomGestureUseCaseV3
    ) {
        self.requestGestureSettingsUseCase = requestGestureSettingsUseCase
        self.writeGestureSettingsUseCase = writeGestureSettingsUseCase
        self.getGestureEditorHandSideUseCase = getGestureEditorHandSideUseCase
        self.getCustomGestureNamesUseCase = getCustomGestureNamesUseCase
        self.renameCustomGestureUseCase = renameCustomGestureUseCase
        super.init()
        stopSettingsObservation = observeGestureSettingsUseCase.observeResponses { [weak self] response in
            self?.onAction(.settingsReceived(response))
        }
    }

    deinit {
        stopSettingsObservation?()
    }

    @nonobjc func loadCustomGestureNames() -> [String] {
        let names = getCustomGestureNamesUseCase.invoke().names
        updateNamesState(names)
        return names
    }

    private func updateNamesState(_ names: [String]) {
        uiState = V3GestureEditorUiState(parameterRef: uiState?.parameterRef,
                                       parameterData: uiState?.parameterData,
                                       customGestureNames: names)
    }

    @nonobjc func onAction(_ action: V3GestureEditorAction) {
        switch action {
        case .settingsRequested(let gestureId):
            requestGestureSettingsUseCase.requestNow(gestureId: gestureId)
        case .settingsWriteRequested(let settings, let command, let name):
            writeGestureSettingsUseCase.invoke(settings: settings, command: command, name: name)
        case .settingsReceived(let response):
            let parameterRef = ParameterRef(addressDevice: response.addressDevice,
                                            parameterID: response.parameterID,
                                            dataCode: response.dataCode)
            let state = V3GestureEditorUiState(
                parameterRef: parameterRef,
                parameterData: response.serializedSettings,
                customGestureNames: uiState?.customGestureNames ?? []
            )
            uiState = state
            // Reentrant responses must not replace this event's captured pair.
            NotificationCenter.default.post(
                name: .gestureSettingsViewModelDidUpdateV3,
                object: self,
                userInfo: ["data": parameterRef, "parameterData": response.serializedSettings]
            )
        case .nameChanged(let gestureNumber, let name):
            let names = renameCustomGestureUseCase.invoke(index: Int32(clamping: gestureNumber - 64), name: name)
            updateNamesState(names)
        }
    }
}

@objcMembers
final class GestureService: NSObject {
    static let shared = GestureService()
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
        notifyNameChanged(numberGesture: numberGesture, name: name, names: names)
    }

    @objc(setNameGestureV3WithNumberGesture:name:)
    public func setNameGestureV3(numberGesture: Int, name: String) {
        let viewModel = GestureSettingsViewModelV3.shared
        viewModel.onAction(.nameChanged(numberGesture, name))
        notifyNameChanged(numberGesture: numberGesture, name: name, names: viewModel.uiState?.customGestureNames ?? [])
    }

    private func notifyNameChanged(numberGesture: Int, name: String, names: [String]) {
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
        gestureName(numberGesture: numberGesture, names: loadNames())
    }

    @objc(getGestureNameV3WithNumberGesture:)
    public func getGestureNameV3(numberGesture: Int) -> String {
        gestureName(numberGesture: numberGesture, names: GestureSettingsViewModelV3.shared.loadCustomGestureNames())
    }

    private func gestureName(numberGesture: Int, names: [String]) -> String {
        let index = numberGesture - 64
        guard names.indices.contains(index) else { return names.first ?? "" }
        print("Вызвана функция getGestureName numberGesture = \(numberGesture)  names = \(names)")
        print("Вызвана функция getGestureName name = \(names[index])")
        return names[index]
    }
    @objc public func getDeviceName() -> String {
        V3CustomGestureNamesRepositoryImpl.shared.getDeviceName()
    }
    @objc public func getParameterData(deviceAddress: Int, parameterID: Int) -> NSString {
        let parameter = ParameterProvider.Companion().getParameter(deviceAddress: Int32(deviceAddress), parameterID: Int32(parameterID))
        return parameter.data as NSString
    }
    @objc public func getStatusConnection() -> Int { 1 }
    @objc public func getHandSide() -> Int {
        V3HandSideProvider.shared.startObserving()
        return GestureSettingsViewModelV3.shared.currentHandSide
    }
    @objc public func getLegacyHandSide() -> Int {
        let handSideKey = "HAND_SIDE"
        guard
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first,
            let fileURLs = try? FileManager.default.contentsOfDirectory(
                at: documentsURL,
                includingPropertiesForKeys: nil
            )
        else {
            return 0
        }

        let decoder = JSONDecoder()
        for fileURL in fileURLs {
            guard
                let data = try? Data(contentsOf: fileURL),
                let item = try? decoder.decode(LegacySavedString.self, from: data),
                item.key == handSideKey,
                let side = Int(item.value),
                side == 0 || side == 1
            else {
                continue
            }
            return side
        }
        return 0
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
                let value = max(0, min(255, gestureSettingsIntValue(from: json[key]) ?? 0))
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
        GestureSettingsViewModelV3.shared.onAction(.settingsRequested(Int32(gestureId)))
    }

    @objc(sendGestureSettingsV3WithGestureWithAddress:)
    public func sendGestureSettingsV3(gestureWithAddress: GestureWithAddress) {
        guard let command = V3GestureCommand.entries.first(where: { $0.code == gestureWithAddress.gestureState }) else {
            return
        }
        let gesture = gestureWithAddress.gesture
        // The renderer already stores these fields in device order; keep their raw values.
        let settings = V3GestureSettings(
            gestureId: gesture.gestureId,
            openPositions: [gesture.openPosition1, gesture.openPosition2, gesture.openPosition3,
                            gesture.openPosition4, gesture.openPosition5, gesture.openPosition6].map { KotlinInt(int: $0) },
            closePositions: [gesture.closePosition1, gesture.closePosition2, gesture.closePosition3,
                             gesture.closePosition4, gesture.closePosition5, gesture.closePosition6].map { KotlinInt(int: $0) },
            openToCloseDelays: [gesture.openToCloseTimeShift1, gesture.openToCloseTimeShift2, gesture.openToCloseTimeShift3,
                               gesture.openToCloseTimeShift4, gesture.openToCloseTimeShift5, gesture.openToCloseTimeShift6].map { KotlinInt(int: $0) },
            closeToOpenDelays: [gesture.closeToOpenTimeShift1, gesture.closeToOpenTimeShift2, gesture.closeToOpenTimeShift3,
                               gesture.closeToOpenTimeShift4, gesture.closeToOpenTimeShift5, gesture.closeToOpenTimeShift6].map { KotlinInt(int: $0) }
        )
        GestureSettingsViewModelV3.shared.onAction(.settingsWriteRequested(settings, command, gesture.gestureName))
    }

    func loadNames() -> [String] {
        V3CustomGestureNamesRepositoryImpl.shared.loadNames()
    }

    @discardableResult
    func updateName(_ name: String, at index: Int) -> [String] {
        V3CustomGestureNamesRepositoryImpl.shared.updateName(name, at: index)
    }
    
    @objc func gestureStateOpen() -> String {
        return SharedRes.strings().gesture_state_open.desc().localized()
    }
    @objc func gestureStateClose() -> String {
        return SharedRes.strings().gesture_state_close.desc().localized()
    }
}
