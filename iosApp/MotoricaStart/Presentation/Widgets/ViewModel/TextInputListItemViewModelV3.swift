import Foundation
import shared

struct TextInputListItemViewModelV3: Equatable, Hashable {
    static let maxDeviceNameBytesWithoutPrefix = 10

    enum InputKind: Equatable {
        case deviceName
        case serialNumber
        case generic
    }

    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let binding: WidgetV3BindingInfo?
    let placeholder: String
    let buttonTitle: String

    var inputKind: InputKind {
        guard let binding else { return .generic }
        switch binding.dataCode {
        case 0x0D: return .deviceName
        case 0x0B: return .serialNumber
        default: return .generic
        }
    }
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

    func sendInput(_ input: String) -> String? {
        guard let binding else { return nil }
        let normalized = input.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleaned = inputKind == .deviceName ? trimToByteLimit(normalized) : normalized
        guard !cleaned.isEmpty else { return nil }

        let transportText = inputKind == .deviceName
            ? DeviceNameBridgeV3.shared.applyPrefixForTransport(rawName: cleaned)
            : cleaned
        guard let data = WidgetCommandBridgeV3.shared.buildSetText(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            deviceAddress: Int32(binding.deviceAddress),
            text: transportText
        ) else { return nil }

        let readAfterSet = inputKind == .serialNumber
            ? WidgetCommandBridgeV3.shared.buildReadRequest(
                parameterID: Int32(binding.parameterID),
                dataCode: Int32(binding.dataCode)
            )
            : nil
        sendBytes(data) {
            guard let readAfterSet else { return }
            self.sendBytes(readAfterSet)
        }
        return transportText
    }

    func prefillText(storedFullName: String?) -> String {
        switch inputKind {
        case .deviceName:
            return DeviceNameBridgeV3.shared.displayName(deviceName: storedFullName)
        case .serialNumber:
            guard let binding else { return "" }
            return WidgetStateBridgeV3.shared.getCurrent(
                addressDevice: Int32(binding.deviceAddress),
                parameterID: Int32(binding.parameterID),
                dataCode: Int32(binding.dataCode)
            )?.serializedValue ?? ""
        case .generic:
            return ""
        }
    }

    func trimToByteLimit(_ value: String) -> String {
        guard inputKind == .deviceName else { return value }
        return trimToUtf8ByteLimit(value, maxBytes: Self.maxDeviceNameBytesWithoutPrefix)
    }

    func isWithinLimit(_ value: String) -> Bool {
        inputKind != .deviceName || value.utf8.count <= Self.maxDeviceNameBytesWithoutPrefix
    }

    private func trimToUtf8ByteLimit(_ value: String, maxBytes: Int) -> String {
        var result = ""
        for scalar in value {
            let candidate = result + String(scalar)
            if candidate.utf8.count > maxBytes {
                break
            }
            result = candidate
        }
        return result
    }

    private func sendBytes(_ data: KotlinByteArray, onSent: @escaping () -> Void = {}) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.SERIALPORTCHAR_UUID,
            typeCommand: gatt.WRITE,
            onChunkSent: onSent
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
