import Foundation
import shared

struct TextInputListItemViewModelV3: Equatable, Hashable {
    static let maxInputBytesWithoutPrefix = TextInputNameLimitV3.maxBytes

    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let binding: WidgetV3BindingInfo?
    let placeholder: String
    let buttonTitle: String
}

extension TextInputListItemViewModelV3 {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.binding = WidgetV3Support.primaryBinding(from: widget)
        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let binding {
            self.identifier = "\(widgetPosition)-\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-\(binding.dataOffset)-text-input-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-text-input-v3"
        }
        let split = WidgetV3Support.splitTextInputTitle(widget.title ?? "")
        self.placeholder = split.placeholder
        self.buttonTitle = split.buttonTitle
    }

    func sendInput(_ input: String, currentFullName: String?) -> String? {
        guard let binding else { return nil }
        let cleaned = trimToByteLimit(input.trimmingCharacters(in: .whitespacesAndNewlines))
        guard !cleaned.isEmpty else { return nil }
        let resolvedCurrentFullName = resolveCurrentFullName(fallback: currentFullName)

        guard let transportName = DeviceNameBridgeV3.shared.transportName(
            rawName: cleaned,
            currentFullName: resolvedCurrentFullName
        ) else { return nil }
        guard let data = WidgetCommandBridgeV3.shared.buildSetText(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            deviceAddress: Int32(binding.deviceAddress),
            text: transportName
        ) else { return nil }

        NSLog(
            "[DeviceNameV3] TX current=\"%@\" input=\"%@\" transport=\"%@\"",
            resolvedCurrentFullName ?? "nil",
            cleaned,
            transportName
        )
        WidgetStateBridgeV3.shared.setTextValue(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            value: transportName
        )
        sendBytes(data)
        return transportName
    }

    func resolveCurrentFullName(fallback: String?) -> String? {
        guard let binding else { return fallback?.nilIfBlank }
        return WidgetStateBridgeV3.shared.getTextValue(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) ?? fallback?.nilIfBlank
    }

    func prefillDisplayName(storedFullName: String?) -> String {
        DeviceNameBridgeV3.shared.displayName(deviceName: storedFullName)
    }

    func trimToByteLimit(_ value: String) -> String {
        TextInputNameLimitV3.trimToLimit(value)
    }

    func isWithinLimit(_ value: String) -> Bool {
        value.utf8.count <= Self.maxInputBytesWithoutPrefix
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

    static func == (lhs: TextInputListItemViewModelV3, rhs: TextInputListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }
}

private extension String {
    var nilIfBlank: String? {
        let normalized = trimmingCharacters(in: .whitespacesAndNewlines)
        return normalized.isEmpty ? nil : normalized
    }
}
