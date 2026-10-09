import Foundation
import shared

enum TextInputListItemActionV3 {
    case textChanged(String)
    case currentValueRequested(storedFullName: String?)
    case inputSubmitted(String)
}

enum TextInputSubmissionV3 {
    case empty
    case failed
    case deviceNameSaved
    case valueSent(String)
}

struct TextInputListItemUiStateV3 {
    let text: String
    let submission: TextInputSubmissionV3?
}

final class TextInputListItemViewModelV3: Equatable, Hashable {
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
    let deviceInfoField: V3DeviceInfoField?
    private let getDeviceInfoTextUseCase: GetDeviceInfoTextUseCaseV3
    private let editDeviceInfoTextUseCase: EditDeviceInfoTextUseCaseV3
    private let setDeviceInfoTextUseCase: SetDeviceInfoTextUseCaseV3
    private(set) var uiState = TextInputListItemUiStateV3(text: "", submission: nil)

    var inputKind: InputKind {
        guard let binding else { return .generic }
        switch binding.dataCode {
        case 0x0D: return .deviceName
        case 0x0B: return .serialNumber
        default: return .generic
        }
    }
    init(
        widget: Widget,
        bleManager: BleManagerKmm,
        getDeviceInfoTextUseCase: GetDeviceInfoTextUseCaseV3,
        editDeviceInfoTextUseCase: EditDeviceInfoTextUseCaseV3,
        setDeviceInfoTextUseCase: SetDeviceInfoTextUseCaseV3
    ) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.binding = WidgetV3Support.primaryBinding(from: widget)
        self.getDeviceInfoTextUseCase = getDeviceInfoTextUseCase
        self.editDeviceInfoTextUseCase = editDeviceInfoTextUseCase
        self.setDeviceInfoTextUseCase = setDeviceInfoTextUseCase
        self.deviceInfoField = WidgetV3Support.widgetCode(from: widget) == WidgetV3Support.WidgetCode.textInputV3
            && WidgetV3Support.parameterKey(for: binding, among: ["P_KEY_SET_DEVICE_NAME"]) != nil
            ? .deviceName : nil
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

    func onAction(_ action: TextInputListItemActionV3) {
        switch action {
        case .textChanged(let text):
            let value = deviceInfoField.map { editDeviceInfoTextUseCase.invoke(field: $0, text: text).text }
                ?? trimToByteLimit(text)
            uiState = TextInputListItemUiStateV3(text: value, submission: nil)
        case .currentValueRequested(let storedFullName):
            let value: String
            if let deviceInfoField {
                value = getDeviceInfoTextUseCase.invoke(field: deviceInfoField) ?? ""
            } else {
                value = prefillText(storedFullName: storedFullName)
            }
            uiState = TextInputListItemUiStateV3(text: value, submission: nil)
        case .inputSubmitted(let text):
            let normalized = text.trimmingCharacters(in: .whitespacesAndNewlines)
            let submission: TextInputSubmissionV3
            if normalized.isEmpty {
                submission = .empty
            } else if let deviceInfoField {
                let edit = editDeviceInfoTextUseCase.invoke(field: deviceInfoField, text: normalized)
                submission = setDeviceInfoTextUseCase.invoke(field: deviceInfoField, value: edit) == .sent
                    ? .deviceNameSaved : .failed
            } else if let value = sendInput(normalized) {
                submission = .valueSent(value)
            } else {
                submission = .failed
            }
            uiState = TextInputListItemUiStateV3(text: uiState.text, submission: submission)
        }
    }

    // Serial numbers and non-generated bindings retain their existing native commands.
    private func sendInput(_ input: String) -> String? {
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

    private func prefillText(storedFullName: String?) -> String {
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

    private func trimToByteLimit(_ value: String) -> String {
        guard inputKind == .deviceName else { return value }
        return Self.trimDeviceName(value)
    }

    static func trimDeviceName(_ value: String) -> String {
        var result = ""
        for scalar in value {
            let candidate = result + String(scalar)
            if candidate.utf8.count > maxDeviceNameBytesWithoutPrefix {
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
