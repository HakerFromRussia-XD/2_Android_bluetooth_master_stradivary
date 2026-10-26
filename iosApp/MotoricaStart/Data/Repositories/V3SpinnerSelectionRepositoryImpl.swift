import Foundation
import shared

final class V3SpinnerSelectionRepositoryImpl: NSObject, V3SpinnerSelectionRepository {
    private let parameters: V3SettingsParameterRepository
    private let userDefaults: UserDefaults

    init(parameters: V3SettingsParameterRepository, userDefaults: UserDefaults = .standard) {
        self.parameters = parameters
        self.userDefaults = userDefaults
        super.init()
    }

    func sendSelection(deviceAddress: Int32, parameterID: Int32, dataCode: Int32, dataOffset: Int32, index: Int32) {
        let value = Int(index)
        if UserFirmwareRoleAccess.isRoleSelector(parameterID: Int(parameterID), dataCode: Int(dataCode)) {
            userDefaults.set(value, forKey: UserFirmwareRoleAccess.key)
        }
        if parameterID == 0x10, dataCode == 0x0E {
            NSLog("[V3HandSide] source=widget selectedIndex=%d address=%d parameter=0x%02X dataCode=0x%02X",
                  value, Int(deviceAddress), Int(parameterID), Int(dataCode))
            V3HandSideProvider.shared.applyWidgetValue(value)
        }
        parameters.sendParameterValue(
            deviceAddress: deviceAddress,
            parameterID: parameterID,
            dataCode: dataCode,
            dataOffset: dataOffset,
            value: index
        )
    }

    func applyHandSideDeviceValue(index: Int32) {
        V3HandSideProvider.shared.applyDeviceValue(Int(index))
    }
}
