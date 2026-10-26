import Foundation
import shared

enum V3AutoLoginAction {
    case currentStateRequested
    case stateChanged(Bool)
}

struct V3AutoLoginUiState {
    let isEnabled: Bool?
}

final class V3AutoLoginViewModel {
    private let getAutoLoginEnabledUseCase: GetAutoLoginEnabledUseCaseV3
    private let setAutoLoginEnabledUseCase: SetAutoLoginEnabledUseCaseV3
    private(set) var uiState = V3AutoLoginUiState(isEnabled: nil)

    init(get: GetAutoLoginEnabledUseCaseV3, set: SetAutoLoginEnabledUseCaseV3) {
        getAutoLoginEnabledUseCase = get
        setAutoLoginEnabledUseCase = set
    }

    func onAction(_ action: V3AutoLoginAction) {
        switch action {
        case .currentStateRequested:
            uiState = V3AutoLoginUiState(isEnabled: getAutoLoginEnabledUseCase.invoke())
        case .stateChanged(let isEnabled):
            setAutoLoginEnabledUseCase.invoke(enabled: isEnabled)
            uiState = V3AutoLoginUiState(isEnabled: isEnabled)
        }
    }
}

struct SwitcherListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
    private let autoLoginViewModel: V3AutoLoginViewModel
    private let observeSwitcherResponsesUseCase: ObserveSwitcherResponsesUseCaseV3
    private let getSwitcherValueUseCase: GetSwitcherValueUseCaseV3
    private let requestSwitcherValueUseCase: RequestSwitcherValueUseCaseV3
    private let setSwitcherValueUseCase: SetSwitcherValueUseCaseV3
    let title: String
    let widget: Widget
    let binding: WidgetV3BindingInfo?
}

extension SwitcherListItemViewModelV3 {
    init(
        widget: Widget,
        observeSwitcherResponsesUseCase: ObserveSwitcherResponsesUseCaseV3,
        getSwitcherValueUseCase: GetSwitcherValueUseCaseV3,
        requestSwitcherValueUseCase: RequestSwitcherValueUseCaseV3,
        setSwitcherValueUseCase: SetSwitcherValueUseCaseV3,
        getAutoLoginEnabledUseCase: GetAutoLoginEnabledUseCaseV3,
        setAutoLoginEnabledUseCase: SetAutoLoginEnabledUseCaseV3
    ) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.observeSwitcherResponsesUseCase = observeSwitcherResponsesUseCase
        self.getSwitcherValueUseCase = getSwitcherValueUseCase
        self.requestSwitcherValueUseCase = requestSwitcherValueUseCase
        self.setSwitcherValueUseCase = setSwitcherValueUseCase
        self.autoLoginViewModel = V3AutoLoginViewModel(
            get: getAutoLoginEnabledUseCase,
            set: setAutoLoginEnabledUseCase
        )
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
        requestSwitcherValueUseCase.invoke(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        )
    }

    func sendState(_ isOn: Bool) {
        if isMobileSmartConnectionSetting {
            autoLoginViewModel.onAction(.stateChanged(isOn))
            print("[SWITCH][mobile-v3] set smart connection enabled=\(isOn)")
            return
        }
        guard let binding else { return }
        setSwitcherValueUseCase.invoke(
            deviceAddress: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            value: isOn
        )
    }

    func observeStateResponses(onResponse: @escaping (Bool) -> Void) -> Kotlinx_coroutines_coreJob? {
        guard !isMobileSmartConnectionSetting, let binding else { return nil }
        return observeSwitcherResponsesUseCase.invoke(
            deviceAddress: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) { value in
            if let isOn = value?.boolValue { onResponse(isOn) }
        }
    }

    func currentState() -> Bool? {
        if isMobileSmartConnectionSetting {
            autoLoginViewModel.onAction(.currentStateRequested)
            return autoLoginViewModel.uiState.isEnabled
        }
        guard let binding else { return nil }
        return getSwitcherValueUseCase.invoke(
            deviceAddress: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        )?.boolValue
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: SwitcherListItemViewModelV3, rhs: SwitcherListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }
}
