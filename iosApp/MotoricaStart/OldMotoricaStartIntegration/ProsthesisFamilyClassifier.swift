import Foundation
import shared

struct ProsthesisFamilyClassifier {
    private let oldMarkers = [
        "HRSTM",
        "BLE_test_service",
        "MLT",
        "FNG",
        "FNS",
        "MLX",
        "FNX",
        "STR",
        "CBY",
        "IND",
        "HND",
        "NEMO",
        "STAND",
        "BT05",
        "FEST"
    ]

    func family(for deviceName: String) -> MergedProsthesisFamily {
        if UiInterfaceModeBridgeV3.shared.isUbiDeviceFamily(deviceName: deviceName) {
            return .newKmm
        }

        if oldMarkers.contains(where: { deviceName.localizedCaseInsensitiveContains($0) }) {
            return .oldLegacy
        }

        return .unknown
    }

    func isKnownProsthesis(deviceName: String) -> Bool {
        family(for: deviceName) != .unknown
    }
}
