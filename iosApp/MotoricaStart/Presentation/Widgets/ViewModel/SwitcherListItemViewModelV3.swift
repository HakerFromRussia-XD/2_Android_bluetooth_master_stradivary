import Foundation
import shared

struct SwitcherListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
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
        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let binding {
            self.identifier = "\(widgetPosition)-\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-\(binding.dataOffset)-switcher-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-switcher-v3"
        }
    }

    func requestCurrent() {
        guard let binding else { return }
        guard let data = WidgetCommandBridgeV3.shared.buildReadRequest(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return }
        sendBytes(data)
    }

    func sendState(_ isOn: Bool) {
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
        guard let binding else { return false }
        return snapshot.addressDevice == Int32(binding.deviceAddress)
            && snapshot.parameterID == Int32(binding.parameterID)
            && snapshot.dataCode == Int32(binding.dataCode)
    }

    func switchState(from snapshot: ParameterSnapshotV3Bridge) -> Bool? {
        V3SnapshotParser.boolField(from: snapshot.serializedValue, field: "checked")
    }

    func currentState() -> Bool? {
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
