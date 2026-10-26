import Foundation
import shared

extension Notification.Name {
    static let v3HandSideDidChange = Notification.Name("V3HandSideDidChange")
}

@objcMembers
final class V3HandSideProvider: NSObject {
    static let shared = AppDIContainer.makeHandSideProviderV3()

    static let deviceAddress: Int32 = 1
    static let parameterID: Int32 = 0x10
    static let dataCode: Int32 = 0x0E

    private var updatesJob: Kotlinx_coroutines_coreJob?
    private var started = false
    private var lastPublishedSide: Int?
    private let getHandSide: GetHandSideUseCaseV3
    private let observeHandSide: ObserveHandSideResponsesUseCaseV3
    private let storeHandSide: StoreHandSideUseCaseV3

    var currentSide: Int {
        if V3ModelTestSceneConfiguration.isActive {
            return 1
        }
        return currentStoreSide() ?? -1
    }

    @nonobjc init(getHandSide: GetHandSideUseCaseV3,
                  observeHandSide: ObserveHandSideResponsesUseCaseV3,
                  storeHandSide: StoreHandSideUseCaseV3) {
        self.getHandSide = getHandSide
        self.observeHandSide = observeHandSide
        self.storeHandSide = storeHandSide
        super.init()
    }

    deinit {
        updatesJob?.cancel(cause: nil)
    }

    func startObserving() {
        if V3ModelTestSceneConfiguration.isActive {
            return
        }
        guard !started else {
            publishCurrentStoreSide()
            return
        }
        started = true
        updatesJob = observeHandSide.invoke { [weak self] value in
            self?.consume(Int(truncating: value))
        }
        publishCurrentStoreSide()
    }

    func applyDeviceValue(_ side: Int) {
        guard side == 0 || side == 1 else { return }
        NSLog("[V3HandSide] source=deviceSnapshot side=%@",
              side == 0 ? "left" : "right")
        publish(side)
    }

    func applyWidgetValue(_ side: Int) {
        guard side == 0 || side == 1 else { return }
        NSLog("[V3HandSide] source=widgetStore side=%@",
              side == 0 ? "left" : "right")
        storeHandSide.invoke(side: Int32(side))
        publish(side)
    }

#if DEBUG
    // Retain the existing native snapshot-test selector; production responses are mapped in shared data.
    private static func side(from snapshot: ParameterSnapshotV3Bridge) -> Int? {
        guard snapshot.addressDevice == deviceAddress,
              snapshot.parameterID == parameterID,
              snapshot.dataCode == dataCode,
              let raw = V3SnapshotParser.intField(
                from: snapshot.serializedValue,
                field: "spinnerValue"
              ) else {
            return nil
        }
        return raw == 0 || raw == 1 ? raw : nil
    }

    @objc(sideValueForTestingFromSnapshot:)
    static func sideValueForTesting(from snapshot: ParameterSnapshotV3Bridge) -> NSNumber? {
        side(from: snapshot).map(NSNumber.init(value:))
    }
#endif

    private func consume(_ side: Int) {
        DispatchQueue.main.async { [weak self] in
            self?.publish(side)
        }
    }

    private func publishCurrentStoreSide() {
        guard let side = currentStoreSide() else {
            NSLog("[V3HandSide] source=parameterStore state=missing")
            return
        }
        NSLog("[V3HandSide] source=parameterStore side=%@",
              side == 0 ? "left" : "right")
        publish(side)
    }

    private func publish(_ side: Int) {
        let changed = lastPublishedSide != side
        lastPublishedSide = side
        NSLog("[V3HandSide] source=parameterStore applied side=%@ changed=%d",
              side == 0 ? "left" : "right", changed)
        guard changed else { return }
        notifySideChanged()
    }

    private func notifySideChanged() {
        NotificationCenter.default.post(
            name: .v3HandSideDidChange,
            object: self,
            userInfo: ["side": currentSide]
        )
    }

    private func currentStoreSide() -> Int? {
        getHandSide.invoke()?.intValue
    }

}
