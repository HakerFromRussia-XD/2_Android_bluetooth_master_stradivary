import XCTest
import shared
import ObjectiveC.runtime

final class GestureSettingsV3DistributionTests: XCTestCase {
    private enum AndroidV3GestureStates {
        static let open = 0
        static let close = 1
        static let openDelay = 128
        static let closeDelay = 129
        static let save = 255
    }

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

    // Android V3 parity helpers (UBI4GripperScreenWithEncodersActivityV3 + RendererV3)
    private func androidValidationRange(_ value: Int) -> Int {
        max(0, min(100, value))
    }

    private func androidRangeConversion(input: Int, range: Int, offset: Int) -> Int {
        let validated = androidValidationRange(input)
        var result = Float(validated) / 100.0 * Float(range)
        result = Float(range) - result
        result += Float(offset)
        return Int(result)
    }

    private func androidInverseRangeConversion(input: Int, range: Int, offset: Int) -> Int {
        var result = Float(input) / Float(range) * 100.0
        result = Float(range) - result
        result += Float(offset)
        return Int(result)
    }

    private func androidRawStageForThumbFlex(transfer: Int) -> Int {
        let emitted = 100 - Int(Float(transfer + 60) / 90.0 * 100.0)
        let angleFromRenderer = 88 - emitted
        let stateValue = Int(Float(angleFromRenderer) / 100.0 * 91.0) - 49
        return androidValidationRange(androidInverseRangeConversion(input: stateValue, range: 85, offset: -53))
    }

    private func androidRawStageForThumbRotation(transfer: Int) -> Int {
        let emitted = 100 - Int(Float(transfer) / 90.0 * 100.0)
        let angleFromRenderer = 98 - emitted
        let stateValue = Int(Float(angleFromRenderer) / 100.0 * 90.0)
        return androidValidationRange(androidInverseRangeConversion(input: stateValue, range: 85, offset: 15))
    }

    private func callRendererV3IntClassMethod(selectorName: String, argument: Int) -> Int {
        guard let rendererClass = NSClassFromString("AAPLOpenGLRendererV3") else {
            XCTFail("AAPLOpenGLRendererV3 class not found in runtime")
            return .min
        }
        let selector = NSSelectorFromString(selectorName)
        guard let method = class_getClassMethod(rendererClass, selector) else {
            XCTFail("Class method \(selectorName) not found on AAPLOpenGLRendererV3")
            return .min
        }
        typealias MethodType = @convention(c) (AnyClass, Selector, Int) -> Int
        let implementation = method_getImplementation(method)
        let function = unsafeBitCast(implementation, to: MethodType.self)
        return function(rendererClass, selector, argument)
    }

    private func callRendererV3BoolClassMethod(selectorName: String, argument: Bool) -> Int {
        guard let rendererClass = NSClassFromString("AAPLOpenGLRendererV3") else {
            XCTFail("AAPLOpenGLRendererV3 class not found in runtime")
            return .min
        }
        let selector = NSSelectorFromString(selectorName)
        guard let method = class_getClassMethod(rendererClass, selector) else {
            XCTFail("Class method \(selectorName) not found on AAPLOpenGLRendererV3")
            return .min
        }
        typealias MethodType = @convention(c) (AnyClass, Selector, Bool) -> Int32
        let implementation = method_getImplementation(method)
        let function = unsafeBitCast(implementation, to: MethodType.self)
        return Int(function(rendererClass, selector, argument))
    }

    private func callRendererV3NoArgClassMethod(selectorName: String) -> Int {
        guard let rendererClass = NSClassFromString("AAPLOpenGLRendererV3") else {
            XCTFail("AAPLOpenGLRendererV3 class not found in runtime")
            return .min
        }
        let selector = NSSelectorFromString(selectorName)
        guard let method = class_getClassMethod(rendererClass, selector) else {
            XCTFail("Class method \(selectorName) not found on AAPLOpenGLRendererV3")
            return .min
        }
        typealias MethodType = @convention(c) (AnyClass, Selector) -> Int32
        let implementation = method_getImplementation(method)
        let function = unsafeBitCast(implementation, to: MethodType.self)
        return Int(function(rendererClass, selector))
    }

    private func packetBytes(from byteArray: KotlinByteArray) -> [UInt8] {
        (0..<Int(byteArray.size)).map { index in
            UInt8(bitPattern: byteArray.get(index: Int32(index)))
        }
    }

    private func makeGestureV3() -> Gesture {
        let values: [String: Int] = [
            "gestureId": 70,
            "openPosition1": 11,
            "openPosition2": 22,
            "openPosition3": 33,
            "openPosition4": 44,
            "openPosition5": 55,
            "openPosition6": 66,
            "closePosition1": 77,
            "closePosition2": 88,
            "closePosition3": 99,
            "closePosition4": 10,
            "closePosition5": 20,
            "closePosition6": 30,
            "openToCloseTimeShift1": 1,
            "openToCloseTimeShift2": 2,
            "openToCloseTimeShift3": 3,
            "openToCloseTimeShift4": 4,
            "openToCloseTimeShift5": 5,
            "openToCloseTimeShift6": 6,
            "closeToOpenTimeShift1": 7,
            "closeToOpenTimeShift2": 8,
            "closeToOpenTimeShift3": 9,
            "closeToOpenTimeShift4": 10,
            "closeToOpenTimeShift5": 11,
            "closeToOpenTimeShift6": 12
        ]
        guard let gesture = decodeGestureV3(from: values) else {
            fatalError("Failed to create test gesture")
        }
        return gesture
    }

    private func extractGestureStateByte(from packet: [UInt8]) -> Int {
        let headerSize = 5
        let gestureStateDataIndex = 26
        return Int(packet[headerSize + gestureStateDataIndex])
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

    func testThumbFlexStageEncoding_matchesAndroidV3() {
        for transfer in -60...30 {
            let androidRaw = androidRawStageForThumbFlex(transfer: transfer)
            let iosRaw = callRendererV3IntClassMethod(selectorName: "rawStageForThumbFlexTransfer:", argument: transfer)
            XCTAssertEqual(iosRaw, androidRaw, "Mismatch for transfer=\(transfer)")
        }
    }

    func testThumbRotationStageEncoding_matchesAndroidV3() {
        for transfer in 0...90 {
            let androidRaw = androidRawStageForThumbRotation(transfer: transfer)
            let iosRaw = callRendererV3IntClassMethod(selectorName: "rawStageForThumbRotationTransfer:", argument: transfer)
            XCTAssertEqual(iosRaw, androidRaw, "Mismatch for transfer=\(transfer)")
        }
    }

    func testThumbTransferDecodingFromRaw_matchesAndroidV3() {
        for raw in 0...100 {
            let androidFlexTransfer = androidRangeConversion(input: raw, range: 90, offset: -59)
            let androidRotationTransfer = androidRangeConversion(input: raw, range: 92, offset: -1)
            let iosFlexTransfer = callRendererV3IntClassMethod(selectorName: "thumbFlexTransferForRawStage:", argument: raw)
            let iosRotationTransfer = callRendererV3IntClassMethod(selectorName: "thumbRotationTransferForRawStage:", argument: raw)
            XCTAssertEqual(iosFlexTransfer, androidFlexTransfer, "Flex transfer mismatch for raw=\(raw)")
            XCTAssertEqual(iosRotationTransfer, androidRotationTransfer, "Rotation transfer mismatch for raw=\(raw)")
        }
    }

    func testV3GestureStateConstants_matchAndroid() {
        XCTAssertEqual(
            callRendererV3BoolClassMethod(selectorName: "runtimeGestureStateForClosed:", argument: false),
            AndroidV3GestureStates.open
        )
        XCTAssertEqual(
            callRendererV3BoolClassMethod(selectorName: "runtimeGestureStateForClosed:", argument: true),
            AndroidV3GestureStates.close
        )
        XCTAssertEqual(
            callRendererV3BoolClassMethod(selectorName: "transitionGestureStateForClosed:", argument: false),
            AndroidV3GestureStates.openDelay
        )
        XCTAssertEqual(
            callRendererV3BoolClassMethod(selectorName: "transitionGestureStateForClosed:", argument: true),
            AndroidV3GestureStates.closeDelay
        )
        XCTAssertEqual(
            callRendererV3NoArgClassMethod(selectorName: "saveGestureState"),
            AndroidV3GestureStates.save
        )
    }

    func testV3BLEPacket_stateByte_matchesAndroidStatesForMoveSwitchAndSave() {
        let gesture = makeGestureV3()
        let states = [
            AndroidV3GestureStates.open,
            AndroidV3GestureStates.close,
            AndroidV3GestureStates.openDelay,
            AndroidV3GestureStates.closeDelay,
            AndroidV3GestureStates.save
        ]

        for state in states {
            let model = GestureWithAddress(
                addressDevice: 1,
                parameterID: 70,
                gesture: gesture,
                gestureState: Int32(state)
            )
            let packet = packetBytes(from: BLECommandsV3.shared.sendGestureInfo(gestureWithAddress: model))
            XCTAssertEqual(packet.count, 33, "Unexpected packet size for state=\(state)")
            XCTAssertEqual(extractGestureStateByte(from: packet), state, "State byte mismatch for state=\(state)")
        }
    }
}
