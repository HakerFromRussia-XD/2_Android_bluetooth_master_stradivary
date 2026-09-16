@testable import MotoricaStart
import shared
import XCTest

final class GlobalFingerPositionV3BridgeTests: XCTestCase {
    func testBuildsThumbAndIndexMiddlePackets() throws {
        let bridge = WidgetCommandBridgeV3.shared

        let thumbRead = try XCTUnwrap(
            bridge.buildReadRequest(parameterID: 0x0F, dataCode: 0x44)
        )
        let indexMiddleRead = try XCTUnwrap(
            bridge.buildReadRequest(parameterID: 0x0F, dataCode: 0x46)
        )
        let thumbSet = try XCTUnwrap(
            bridge.buildSetInt(
                parameterID: 0x0F,
                dataCode: 0x44,
                deviceAddress: 1,
                dataOffset: 0,
                value: 37
            )
        )
        let indexMiddleSet = try XCTUnwrap(
            bridge.buildSetInt(
                parameterID: 0x0F,
                dataCode: 0x46,
                deviceAddress: 1,
                dataOffset: 0,
                value: 100
            )
        )

        XCTAssertEqual(bytes(thumbRead), [0x00, 0x0F, 0x45, 0x00, 0x3B])
        XCTAssertEqual(bytes(indexMiddleRead), [0x00, 0x0F, 0x47, 0x00, 0xAA])
        XCTAssertEqual(bytes(thumbSet), [0x00, 0x0F, 0x44, 0x25, 0xE3])
        XCTAssertEqual(bytes(indexMiddleSet), [0x00, 0x0F, 0x46, 0x64, 0x6A])
    }

    private func bytes(_ value: KotlinByteArray) -> [UInt8] {
        (0..<Int(value.size)).map { index in
            UInt8(bitPattern: value.get(index: Int32(index)))
        }
    }
}

final class ObservableTests: XCTestCase {
    private final class ObserverToken {}

    func testObserve_immediatelyEmitsCurrentValue() {
        let observable = Observable<Int>(7)
        let token = ObserverToken()
        var received = [Int]()

        observable.observe(on: token) { received.append($0) }

        XCTAssertEqual(received, [7])
    }

    func testValueChange_notifiesObserver() {
        let observable = Observable<String>("initial")
        let token = ObserverToken()
        var received = [String]()

        observable.observe(on: token) { received.append($0) }
        observable.value = "updated"

        XCTAssertEqual(received, ["initial", "updated"])
    }

    func testRemoveObserver_stopsFurtherUpdates() {
        let observable = Observable<Int>(1)
        let token = ObserverToken()
        var received = [Int]()

        observable.observe(on: token) { received.append($0) }
        observable.remove(observer: token)
        observable.value = 2

        XCTAssertEqual(received, [1])
    }
}
