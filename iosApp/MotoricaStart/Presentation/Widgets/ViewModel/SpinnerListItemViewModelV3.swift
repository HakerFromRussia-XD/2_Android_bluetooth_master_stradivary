import Foundation
import shared

enum SpinnerListItemActionV3 {
    case currentValueRequested
    case selectionRequested(Int)
    case rolePinChecked(String)
    case selectedIndexChanged(Int, pin: String? = nil)
}

struct SpinnerListItemUiStateV3 {
    let isPinRequired: Bool
    let isPinValid: Bool?
}

final class SpinnerListItemViewModelV3: Equatable, Hashable {
    private enum HandSideBinding {
        static let parameterID = 0x10
        static let dataCode = 0x0E
    }

    private let identifier: String
    private let getSpinnerSettingsUseCase: GetSpinnerSettingsUseCaseV3
    private let observeSpinnerResponsesUseCase: ObserveSpinnerResponsesUseCaseV3
    private let requestSpinnerValueUseCase: RequestSpinnerValueUseCaseV3
    private let setSpinnerValueUseCase: SetSpinnerValueUseCaseV3
    private let getDeviceRoleUseCase: GetDeviceRoleUseCaseV3
    private let changeDeviceRoleUseCase: ChangeDeviceRoleUseCaseV3
    private let getSettingsParameterUseCase: GetSettingsParameterUseCaseV3
    private let requestSettingsParameterUseCase: RequestSettingsParameterUseCaseV3
    private let sendSpinnerSelectionUseCase: SendSpinnerSelectionUseCaseV3
    private let applyHandSideDeviceValueUseCase: ApplyHandSideDeviceValueUseCaseV3
    private(set) var uiState = SpinnerListItemUiStateV3(isPinRequired: false, isPinValid: nil)
    let title: String
    let widget: Widget
    let binding: WidgetV3BindingInfo?
    let items: [String]
    let initialSelectedIndex: Int
    var isHandSideSelector: Bool {
        binding?.parameterID == HandSideBinding.parameterID &&
            binding?.dataCode == HandSideBinding.dataCode
    }

    init(
        widget: Widget,
        getSpinnerSettingsUseCase: GetSpinnerSettingsUseCaseV3,
        observeSpinnerResponsesUseCase: ObserveSpinnerResponsesUseCaseV3,
        requestSpinnerValueUseCase: RequestSpinnerValueUseCaseV3,
        setSpinnerValueUseCase: SetSpinnerValueUseCaseV3,
        getDeviceRoleUseCase: GetDeviceRoleUseCaseV3,
        changeDeviceRoleUseCase: ChangeDeviceRoleUseCaseV3,
        getSettingsParameterUseCase: GetSettingsParameterUseCaseV3,
        requestSettingsParameterUseCase: RequestSettingsParameterUseCaseV3,
        sendSpinnerSelectionUseCase: SendSpinnerSelectionUseCaseV3,
        applyHandSideDeviceValueUseCase: ApplyHandSideDeviceValueUseCaseV3
    ) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.getSpinnerSettingsUseCase = getSpinnerSettingsUseCase
        self.observeSpinnerResponsesUseCase = observeSpinnerResponsesUseCase
        self.requestSpinnerValueUseCase = requestSpinnerValueUseCase
        self.setSpinnerValueUseCase = setSpinnerValueUseCase
        self.getDeviceRoleUseCase = getDeviceRoleUseCase
        self.changeDeviceRoleUseCase = changeDeviceRoleUseCase
        self.getSettingsParameterUseCase = getSettingsParameterUseCase
        self.requestSettingsParameterUseCase = requestSettingsParameterUseCase
        self.sendSpinnerSelectionUseCase = sendSpinnerSelectionUseCase
        self.applyHandSideDeviceValueUseCase = applyHandSideDeviceValueUseCase
        self.binding = WidgetV3Support.primaryBinding(from: widget)
        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let binding {
            self.identifier = "\(widgetPosition)-\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-\(binding.dataOffset)-spinner-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-spinner-v3"
        }

        let storedRole: () -> Int = {
            Int(getDeviceRoleUseCase.invoke().wireValue)
        }
        if let spinnerS = widget.widget?.value as? SpinnerParameterWidgetSStruct {
            self.items = spinnerS.dataSpinnerParameterWidgetStruct.spinnerItems.map { "\($0)" }
            self.initialSelectedIndex = binding.map { UserFirmwareRoleAccess.isRoleSelector(parameterID: $0.parameterID, dataCode: $0.dataCode) } == true
                ? storedRole() : Int(spinnerS.dataSpinnerParameterWidgetStruct.selectedIndex)
        } else if let spinnerE = widget.widget?.value as? SpinnerParameterWidgetEStruct {
            self.items = spinnerE.dataSpinnerParameterWidgetStruct.spinnerItems.map { "\($0)" }
            self.initialSelectedIndex = binding.map { UserFirmwareRoleAccess.isRoleSelector(parameterID: $0.parameterID, dataCode: $0.dataCode) } == true
                ? storedRole() : Int(spinnerE.dataSpinnerParameterWidgetStruct.selectedIndex)
        } else {
            self.items = []
            self.initialSelectedIndex = 0
        }
    }

    func onAction(_ action: SpinnerListItemActionV3) {
        switch action {
        case .currentValueRequested:
            requestCurrent()
        case .selectionRequested(let index):
            let required: Bool
            if let role = Self.role(for: index), roleParameterKey != nil || isDeviceRoleSelector {
                required = changeDeviceRoleUseCase.requiresPin(role: role)
            } else {
                required = false
            }
            uiState = SpinnerListItemUiStateV3(isPinRequired: required, isPinValid: nil)
        case .rolePinChecked(let pin):
            let valid = changeDeviceRoleUseCase.isPinValid(pin: pin)
            uiState = SpinnerListItemUiStateV3(isPinRequired: uiState.isPinRequired, isPinValid: valid)
        case .selectedIndexChanged(let index, let pin):
            sendSelectedIndex(index, pin: pin)
        }
    }

    private func requestCurrent() {
        if let roleParameterKey {
            requestSpinnerValueUseCase.invoke(parameterKey: roleParameterKey)
            return
        }
        if let parameterKey = spinnerParameterKey {
            requestSpinnerValueUseCase.invoke(parameterKey: parameterKey)
            return
        }
        guard let binding else { return }
        requestSettingsParameterUseCase.invoke(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        )
    }

    private func sendSelectedIndex(_ index: Int, pin: String?) {
        if roleParameterKey != nil, let role = Self.role(for: index) {
            _ = changeDeviceRoleUseCase.invoke(role: role, pin: pin)
            return
        }
        if let parameterKey = spinnerParameterKey {
            setSpinnerValueUseCase.invoke(parameterKey: parameterKey, value: Int32(index))
            return
        }
        guard let binding else { return }
        sendSpinnerSelectionUseCase.invoke(
            deviceAddress: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            dataOffset: Int32(binding.dataOffset),
            index: Int32(index)
        )
    }

    func applyHandSideDeviceSnapshot(_ index: Int) {
        guard isHandSideSelector else { return }
        applyHandSideDeviceValueUseCase.invoke(index: Int32(index))
    }

    func observeValueResponses(onResponse: @escaping (Int?) -> Void) -> Kotlinx_coroutines_coreJob? {
        guard let binding else { return nil }
        return observeSpinnerResponsesUseCase.invoke(
            deviceAddress: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) { value in
            onResponse(value?.intValue)
        }
    }

    func selectedIndex(fromResponse value: Int?) -> Int? {
        if roleParameterKey != nil { return Int(getDeviceRoleUseCase.invoke().wireValue) }
        if spinnerParameterKey == V3ParameterKeys.shared.P_KEY_SETTINGS_PROFILE {
            return value
        }
        if let parameterKey = spinnerParameterKey {
            // RX JSON omitted default zero; unlike configure, it did not replace the UI selection.
            guard let value = spinnerValue(for: parameterKey), value != 0 else { return nil }
            return value
        }
        if binding.map { UserFirmwareRoleAccess.isRoleSelector(parameterID: $0.parameterID, dataCode: $0.dataCode) } == true { return Int(getDeviceRoleUseCase.invoke().wireValue) }
        return value
    }

    func currentSelectedIndex() -> Int? {
        if roleParameterKey != nil { return Int(getDeviceRoleUseCase.invoke().wireValue) }
        if let parameterKey = spinnerParameterKey {
            guard let value = spinnerValue(for: parameterKey), value >= 0 else { return nil }
            return value
        }
        guard let binding else { return nil }
        if UserFirmwareRoleAccess.isRoleSelector(parameterID: binding.parameterID, dataCode: binding.dataCode) { return Int(getDeviceRoleUseCase.invoke().wireValue) }
        return getSettingsParameterUseCase.spinnerValue(
            deviceAddress: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        )?.intValue
    }

    private func spinnerValue(for parameterKey: String) -> Int? {
        (getSpinnerSettingsUseCase.invoke(parameterKeys: [parameterKey])
            .values[parameterKey] as? NSNumber)?.intValue
    }

    private var spinnerParameterKey: String? {
        let code = WidgetV3Support.widgetCode(from: widget)
        guard code == WidgetV3Support.WidgetCode.spinboxV3 ||
                code == WidgetV3Support.WidgetCode.comboboxV3 else { return nil }
        let keys = V3ParameterKeys.shared
        return WidgetV3Support.parameterKey(for: binding, among: [
            keys.P_KEY_HAND_CONTROL_MODE,
            keys.P_KEY_GESTURE_CHANGE_MODE,
            keys.P_KEY_EMG_CONTROL_MODE,
            keys.P_KEY_LEFT_RIGHT_HAND,
            keys.P_KEY_SETTINGS_PROFILE
        ])
    }

    private var roleParameterKey: String? { Self.roleParameterKey(widget: widget, binding: binding) }

    private static func roleParameterKey(widget: Widget, binding: WidgetV3BindingInfo?) -> String? {
        let code = WidgetV3Support.widgetCode(from: widget)
        guard code == WidgetV3Support.WidgetCode.spinboxV3 || code == WidgetV3Support.WidgetCode.comboboxV3 else { return nil }
        return WidgetV3Support.parameterKey(for: binding, among: [V3ParameterKeys.shared.P_KEY_DEVICE_ROLE])
    }

    private static func role(for index: Int) -> V3DeviceRole? {
        switch index {
        case 0: return .prosthetist
        case 1: return .serviceEngineer
        case 2: return .user
        default: return nil
        }
    }

    // Cached widgets can identify a role selector by labels even without its canonical binding.
    private var isDeviceRoleSelector: Bool {
        if let binding, UserFirmwareRoleAccess.isRoleSelector(parameterID: binding.parameterID, dataCode: binding.dataCode) {
            return true
        }
        guard items.count >= 2 else { return false }
        func normalized(_ value: String) -> String {
            value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        }
        let prosthetistNames = [SharedLocalizedText.text(SharedRes.strings().prosthetist), "Prosthetist", "Протезист"].map(normalized)
        let engineerNames = [SharedLocalizedText.text(SharedRes.strings().service_engineer), "Service engineer", "Сервисный инженер"].map(normalized)
        return prosthetistNames.contains(normalized(items[0])) && engineerNames.contains(normalized(items[1]))
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: SpinnerListItemViewModelV3, rhs: SpinnerListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }
}
