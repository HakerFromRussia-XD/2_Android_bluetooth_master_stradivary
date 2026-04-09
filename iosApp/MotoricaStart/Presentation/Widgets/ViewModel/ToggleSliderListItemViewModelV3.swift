import Foundation
import shared

struct ToggleSliderListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let binding: WidgetV3BindingInfo?
    let minProgress: Int
    let maxProgress: Int
    let increment: Float
    let unitLabel: String
}

extension ToggleSliderListItemViewModelV3 {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.binding = WidgetV3Support.primaryBinding(from: widget)
        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let binding {
            self.identifier = "\(widgetPosition)-\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-\(binding.dataOffset)-toggle-slider-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-toggle-slider-v3"
        }

        if let toggleS = widget.widget?.value as? ToggleSliderParameterWidgetSStruct {
            self.minProgress = Int(toggleS.minProgress)
            self.maxProgress = Int(toggleS.maxProgress)
            self.increment = toggleS.increment
            self.unitLabel = toggleS.unitLabel
        } else if let toggleE = widget.widget?.value as? ToggleSliderParameterWidgetEStruct {
            self.minProgress = Int(toggleE.minProgress)
            self.maxProgress = Int(toggleE.maxProgress)
            self.increment = toggleE.increment
            self.unitLabel = ""
        } else {
            self.minProgress = 0
            self.maxProgress = 100
            self.increment = 1
            self.unitLabel = ""
        }
    }

    var uiRange: ClosedRange<Float> {
        let upper = max(maxProgress, minProgress + 1)
        return Float(minProgress)...Float(upper)
    }

    func requestCurrent() {
        guard let binding else { return }
        guard let data = WidgetCommandBridgeV3.shared.buildReadRequest(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return }
        sendBytes(data)
    }

    func sendValue(enabled: Bool, progress: Int) {
        guard let binding else { return }
        let packed = pack(enabled: enabled, progress: progress)
        guard let data = WidgetCommandBridgeV3.shared.buildSetInt(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            deviceAddress: Int32(binding.deviceAddress),
            dataOffset: Int32(binding.dataOffset),
            value: Int32(packed)
        ) else { return }
        sendBytes(data)
    }

    func matches(snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard let binding else { return false }
        return snapshot.addressDevice == Int32(binding.deviceAddress)
            && snapshot.parameterID == Int32(binding.parameterID)
            && snapshot.dataCode == Int32(binding.dataCode)
    }

    func unpack(snapshot: ParameterSnapshotV3Bridge) -> (enabled: Bool, progress: Int)? {
        guard let packed = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "toggleValue") else {
            return nil
        }
        return unpack(packed: packed)
    }

    func currentValue() -> (enabled: Bool, progress: Int)? {
        guard let binding else { return nil }
        guard let snapshot = WidgetStateBridgeV3.shared.getCurrent(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else { return nil }
        return unpack(snapshot: snapshot)
    }

    func unpack(packed: Int) -> (enabled: Bool, progress: Int) {
        let enabled = (packed & 0x80) != 0
        let value = packed & 0x7F
        let clamped = min(max(value, minProgress), maxProgress)
        return (enabled, clamped)
    }

    func pack(enabled: Bool, progress: Int) -> Int {
        let normalized = min(max(progress, minProgress), maxProgress)
        let lowerBits = normalized & 0x7F
        let enabledBit = enabled ? 0x80 : 0x00
        return enabledBit | lowerBits
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

    static func == (lhs: ToggleSliderListItemViewModelV3, rhs: ToggleSliderListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }
}
