import Foundation
import shared

struct SwitcherListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
    private let smartConnectionStore = SmartConnectionSettingsStore()
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let binding: WidgetV3BindingInfo?
}

extension SwitcherListItemViewModelV3 {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.binding = WidgetV3Support.primaryBinding(from: widget)
        let baseStruct = WidgetMetadataExtractor.extractBaseStruct(from: widget.widget?.value)
        let widgetPosition = baseStruct?.widgetPosition ?? -1
        if let binding {
            self.identifier = "\(widgetPosition)-\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-\(binding.dataOffset)-switcher-v3"
        } else if baseStruct?.keyMobileSettings == SmartConnectionSettingsStore.mobileSettingsKeyAutoLogin {
            self.identifier = "mobile-\(SmartConnectionSettingsStore.mobileSettingsKeyAutoLogin)-switcher-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-switcher-v3"
        }
    }

    var isMobileSmartConnectionSetting: Bool {
        WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .keyMobileSettings == SmartConnectionSettingsStore.mobileSettingsKeyAutoLogin
    }

    func requestCurrent() {
        guard !isMobileSmartConnectionSetting else { return }
        guard let binding else { return }
        guard let data = WidgetCommandBridgeV3.shared.buildReadRequest(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return }
        sendBytes(data)
    }

    func sendState(_ isOn: Bool) {
        if isMobileSmartConnectionSetting {
            smartConnectionStore.setEnabled(isOn)
            print("[SWITCH][mobile-v3] set smart connection enabled=\(isOn)")
            return
        }
        guard let binding else { return }
        guard let data = WidgetCommandBridgeV3.shared.buildSetBoolean(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            deviceAddress: Int32(binding.deviceAddress),
            checked: isOn
        ) else { return }
        sendBytes(data)
    }

    func matches(snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard !isMobileSmartConnectionSetting else { return false }
        guard let binding else { return false }
        return snapshot.addressDevice == Int32(binding.deviceAddress)
            && snapshot.parameterID == Int32(binding.parameterID)
            && snapshot.dataCode == Int32(binding.dataCode)
    }

    func switchState(from snapshot: ParameterSnapshotV3Bridge) -> Bool? {
        V3SnapshotParser.boolField(from: snapshot.serializedValue, field: "checked")
    }

    func currentState() -> Bool? {
        if isMobileSmartConnectionSetting {
            return smartConnectionStore.isEnabled
        }
        guard let binding else { return nil }
        guard let snapshot = WidgetStateBridgeV3.shared.getCurrent(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return nil }
        return switchState(from: snapshot)
    }

    private func sendBytes(_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: SwitcherListItemViewModelV3, rhs: SwitcherListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }
}
