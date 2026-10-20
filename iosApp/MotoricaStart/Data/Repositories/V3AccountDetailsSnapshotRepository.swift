import Foundation
import shared

final class V3AccountDetailsSnapshotRepository: NSObject, V3AccountDetailsReader {
    private let details: [V3AccountDetail: String]

    init(profile: AccountBridgeProfile) {
        details = [
            .managerName: profile.managerName,
            .managerPhone: profile.managerPhone,
            .model: profile.prosthesisModel,
            .size: profile.prosthesisSize,
            .side: profile.handSide,
            .status: profile.prosthesisStatus,
            .transferDate: profile.dateOfReceipt,
            .rotator: profile.rotatorType,
            .touchscreenFingers: profile.touchscreenFingerPads,
            .accumulator: profile.batteryType
        ]
        super.init()
    }

    func getDetail(detail: V3AccountDetail) -> String {
        details[detail] ?? ""
    }
}
