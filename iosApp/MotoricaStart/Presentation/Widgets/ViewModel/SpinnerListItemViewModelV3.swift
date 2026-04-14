import Foundation
import shared

struct SpinnerListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let binding: WidgetV3BindingInfo?
    let items: [String]
    let initialSelectedIndex: Int
}

extension SpinnerListItemViewModelV3 {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.binding = WidgetV3Support.primaryBinding(from: widget)
        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let binding {
            self.identifier = "\(widgetPosition)-\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-\(binding.dataOffset)-spinner-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-spinner-v3"
        }

        if let spinnerS = widget.widget?.value as? SpinnerParameterWidgetSStruct {
            self.items = spinnerS.dataSpinnerParameterWidgetStruct.spinnerItems.map { "\($0)" }
            self.initialSelectedIndex = Int(spinnerS.dataSpinnerParameterWidgetStruct.selectedIndex)
        } else if let spinnerE = widget.widget?.value as? SpinnerParameterWidgetEStruct {
            self.items = spinnerE.dataSpinnerParameterWidgetStruct.spinnerItems.map { "\($0)" }
            self.initialSelectedIndex = Int(spinnerE.dataSpinnerParameterWidgetStruct.selectedIndex)
        } else {
            self.items = []
            self.initialSelectedIndex = 0
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

    func sendSelectedIndex(_ index: Int) {
        guard let binding else { return }
        guard let data = WidgetCommandBridgeV3.shared.buildSetInt(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            deviceAddress: Int32(binding.deviceAddress),
            dataOffset: Int32(binding.dataOffset),
            value: Int32(index)
        ) else { return }
        sendBytes(data)
    }

    func matches(snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard let binding else { return false }
        return snapshot.addressDevice == Int32(binding.deviceAddress)
            && snapshot.parameterID == Int32(binding.parameterID)
            && snapshot.dataCode == Int32(binding.dataCode)
    }

    func selectedIndex(from snapshot: ParameterSnapshotV3Bridge) -> Int? {
        V3SnapshotParser.intField(from: snapshot.serializedValue, field: "spinnerValue")
    }

    func currentSelectedIndex() -> Int? {
        guard let binding else { return nil }
        guard let snapshot = WidgetStateBridgeV3.shared.getCurrent(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return nil }
        return selectedIndex(from: snapshot)
    }

    private func sendBytes(_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.SERIALPORTCHAR_UUID,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: SpinnerListItemViewModelV3, rhs: SpinnerListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }
}
