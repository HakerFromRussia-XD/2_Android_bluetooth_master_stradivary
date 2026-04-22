import XCTest
import shared

final class GestureSettingsV3DistributionTests: XCTestCase {
    private let gestureKeys: [String] = [
        "gestureId",
        "openPosition1", "openPosition2", "openPosition3", "openPosition4", "openPosition5", "openPosition6",
        "closePosition1", "closePosition2", "closePosition3", "closePosition4", "closePosition5", "closePosition6",
        "openToCloseTimeShift1", "openToCloseTimeShift2", "openToCloseTimeShift3", "openToCloseTimeShift4", "openToCloseTimeShift5", "openToCloseTimeShift6",
        "closeToOpenTimeShift1", "closeToOpenTimeShift2", "closeToOpenTimeShift3", "closeToOpenTimeShift4", "closeToOpenTimeShift5", "closeToOpenTimeShift6"
    ]

    private func decodeGestureV3(from values: [String: Int]) -> Gesture? {
        let hex = gestureKeys
            .map { key -> String in
                let value = max(0, min(255, values[key] ?? 0))
                return String(format: "%02X", value)
            }
            .joined()
        return SerializationObjects.shared.decodeGesture(raw: "\"\(hex)\"")
    }

    // Та же раскладка, что в AAPLOpenGLRendererV3.updateGestureSettings
    private func distributeToFingerStages(_ gesture: Gesture) -> [String: Int] {
        return [
            "openStage1": Int(gesture.openPosition4),
            "openStage2": Int(gesture.openPosition3),
            "openStage3": Int(gesture.openPosition2),
            "openStage4": Int(gesture.openPosition1),
            "openStage5": Int(gesture.openPosition5),
            "openStage6": Int(gesture.openPosition6),
            "closeStage1": Int(gesture.closePosition4),
            "closeStage2": Int(gesture.closePosition3),
            "closeStage3": Int(gesture.closePosition2),
            "closeStage4": Int(gesture.closePosition1),
            "closeStage5": Int(gesture.closePosition5),
            "closeStage6": Int(gesture.closePosition6)
        ]
    }

    func testGesture7_shouldDecodeAndDistributeToV3FingerStages() {
        let values: [String: Int] = [
            "gestureId": 70,
            "openPosition1": 0,
            "openPosition2": 97,
            "openPosition3": 100,
            "openPosition4": 0,
            "openPosition5": 0,
            "openPosition6": 0,
            "closePosition1": 100,
            "closePosition2": 100,
            "closePosition3": 100,
            "closePosition4": 100,
            "closePosition5": 100,
            "closePosition6": 0,
            "openToCloseTimeShift1": 0,
            "openToCloseTimeShift2": 0,
            "openToCloseTimeShift3": 0,
            "openToCloseTimeShift4": 0,
            "openToCloseTimeShift5": 0,
            "openToCloseTimeShift6": 0,
            "closeToOpenTimeShift1": 0,
            "closeToOpenTimeShift2": 0,
            "closeToOpenTimeShift3": 0,
            "closeToOpenTimeShift4": 0,
            "closeToOpenTimeShift5": 0,
            "closeToOpenTimeShift6": 0
        ]

        guard let decoded = decodeGestureV3(from: values) else {
            XCTFail("GestureSettingsV3 decoding returned nil")
            return
        }

        XCTAssertEqual(Int(decoded.gestureId), 70)
        XCTAssertEqual(Int(decoded.openPosition1), 0)
        XCTAssertEqual(Int(decoded.openPosition2), 97)
        XCTAssertEqual(Int(decoded.openPosition3), 100)
        XCTAssertEqual(Int(decoded.openPosition4), 0)
        XCTAssertEqual(Int(decoded.openPosition5), 0)
        XCTAssertEqual(Int(decoded.openPosition6), 0)
        XCTAssertEqual(Int(decoded.closePosition1), 100)
        XCTAssertEqual(Int(decoded.closePosition2), 100)
        XCTAssertEqual(Int(decoded.closePosition3), 100)
        XCTAssertEqual(Int(decoded.closePosition4), 100)
        XCTAssertEqual(Int(decoded.closePosition5), 100)
        XCTAssertEqual(Int(decoded.closePosition6), 0)
        XCTAssertEqual(Int(decoded.openToCloseTimeShift1), 0)
        XCTAssertEqual(Int(decoded.openToCloseTimeShift2), 0)
        XCTAssertEqual(Int(decoded.openToCloseTimeShift3), 0)
        XCTAssertEqual(Int(decoded.openToCloseTimeShift4), 0)
        XCTAssertEqual(Int(decoded.openToCloseTimeShift5), 0)
        XCTAssertEqual(Int(decoded.openToCloseTimeShift6), 0)
        XCTAssertEqual(Int(decoded.closeToOpenTimeShift1), 0)
        XCTAssertEqual(Int(decoded.closeToOpenTimeShift2), 0)
        XCTAssertEqual(Int(decoded.closeToOpenTimeShift3), 0)
        XCTAssertEqual(Int(decoded.closeToOpenTimeShift4), 0)
        XCTAssertEqual(Int(decoded.closeToOpenTimeShift5), 0)
        XCTAssertEqual(Int(decoded.closeToOpenTimeShift6), 0)

        let stages = distributeToFingerStages(decoded)
        XCTAssertEqual(stages["openStage1"], 0)   // openPosition4
        XCTAssertEqual(stages["openStage2"], 100) // openPosition3
        XCTAssertEqual(stages["openStage3"], 97)  // openPosition2
        XCTAssertEqual(stages["openStage4"], 0)   // openPosition1
        XCTAssertEqual(stages["openStage5"], 0)   // openPosition5
        XCTAssertEqual(stages["openStage6"], 0)   // openPosition6

        XCTAssertEqual(stages["closeStage1"], 100) // closePosition4
        XCTAssertEqual(stages["closeStage2"], 100) // closePosition3
        XCTAssertEqual(stages["closeStage3"], 100) // closePosition2
        XCTAssertEqual(stages["closeStage4"], 100) // closePosition1
        XCTAssertEqual(stages["closeStage5"], 100) // closePosition5
        XCTAssertEqual(stages["closeStage6"], 0)   // closePosition6
    }
}
