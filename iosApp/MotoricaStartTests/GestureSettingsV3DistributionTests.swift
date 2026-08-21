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

    func testOpenGLController_routesOnlyV3ModeToV3Renderer() {
        XCTAssertEqual(
            rendererClassName(forV3Mode: true),
            "AAPLOpenGLRendererV3"
        )
        XCTAssertEqual(
            rendererClassName(forV3Mode: false),
            "AAPLOpenGLRenderer"
        )
    }

    private func rendererClassName(forV3Mode useV3Mode: Bool) -> String? {
        guard let controllerClass = NSClassFromString("AAPLOpenGLViewControllerV3") else {
            XCTFail("AAPLOpenGLViewControllerV3 class not found in runtime")
            return nil
        }
        let selector = NSSelectorFromString("rendererClassForV3Mode:")
        guard let method = class_getClassMethod(controllerClass, selector) else {
            XCTFail("Renderer routing method not found on AAPLOpenGLViewControllerV3")
            return nil
        }
        typealias MethodType = @convention(c) (AnyClass, Selector, Bool) -> AnyClass
        let function = unsafeBitCast(method_getImplementation(method), to: MethodType.self)
        return NSStringFromClass(function(controllerClass, selector, useV3Mode))
    }

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

    private func callRendererV3MatrixSnapshots(handSide: Int, positions: [NSNumber]) -> [String: [Double]] {
        guard let rendererClass = NSClassFromString("AAPLOpenGLRendererV3") else {
            XCTFail("AAPLOpenGLRendererV3 class not found in runtime")
            return [:]
        }
        let selector = NSSelectorFromString("matrixSnapshotsForTestingWithHandSide:positions:")
        guard let method = class_getClassMethod(rendererClass, selector) else {
            XCTFail("Matrix snapshot helper not found on AAPLOpenGLRendererV3")
            return [:]
        }
        typealias MethodType = @convention(c) (AnyClass, Selector, Int, NSArray) -> NSDictionary
        let function = unsafeBitCast(method_getImplementation(method), to: MethodType.self)
        let raw = function(rendererClass, selector, handSide, positions as NSArray)
        var result: [String: [Double]] = [:]
        for case let key as String in raw.allKeys {
            guard let values = raw[key] as? [NSNumber] else { continue }
            result[key] = values.map(\.doubleValue)
        }
        return result
    }

    private func callRendererV3Transition(start: [NSNumber], target: [NSNumber], delays: [NSNumber], elapsed: Double) -> [Double] {
        guard let rendererClass = NSClassFromString("AAPLOpenGLRendererV3") else {
            XCTFail("AAPLOpenGLRendererV3 class not found in runtime")
            return []
        }
        let selector = NSSelectorFromString("transitionPositionsForTestingFrom:target:delays:elapsed:")
        guard let method = class_getClassMethod(rendererClass, selector) else {
            XCTFail("Transition helper not found on AAPLOpenGLRendererV3")
            return []
        }
        typealias MethodType = @convention(c) (AnyClass, Selector, NSArray, NSArray, NSArray, Double) -> NSArray
        let function = unsafeBitCast(method_getImplementation(method), to: MethodType.self)
        return (function(rendererClass, selector, start as NSArray, target as NSArray, delays as NSArray, elapsed) as? [NSNumber])?
            .map(\.doubleValue) ?? []
    }

    private func gestureKeyClipState(at milliseconds: Double) -> NSDictionary {
        guard let rendererClass = NSClassFromString("AAPLOpenGLRendererV3") else {
            XCTFail("AAPLOpenGLRendererV3 class not found in runtime")
            return [:]
        }
        let selector = NSSelectorFromString("gestureKeyClipStateForTestingAtMilliseconds:")
        guard let method = class_getClassMethod(rendererClass, selector) else {
            XCTFail("Gesture Key clip sampler not found")
            return [:]
        }
        typealias MethodType = @convention(c) (AnyClass, Selector, Double) -> NSDictionary
        let function = unsafeBitCast(method_getImplementation(method), to: MethodType.self)
        return function(rendererClass, selector, milliseconds)
    }

    private func cupGripClipState(at milliseconds: Double) -> NSDictionary {
        guard let rendererClass = NSClassFromString("AAPLOpenGLRendererV3") else {
            XCTFail("AAPLOpenGLRendererV3 class not found in runtime")
            return [:]
        }
        let selector = NSSelectorFromString("cupGripClipStateForTestingAtMilliseconds:")
        guard let method = class_getClassMethod(rendererClass, selector) else {
            XCTFail("Cup Grip clip sampler not found")
            return [:]
        }
        typealias MethodType = @convention(c) (AnyClass, Selector, Double) -> NSDictionary
        let function = unsafeBitCast(method_getImplementation(method), to: MethodType.self)
        return function(rendererClass, selector, milliseconds)
    }

    private typealias Matrix4 = [Double]

    private func identityMatrix() -> Matrix4 {
        [1, 0, 0, 0,
         0, 1, 0, 0,
         0, 0, 1, 0,
         0, 0, 0, 1]
    }

    private func multiply(_ left: Matrix4, _ right: Matrix4) -> Matrix4 {
        var result = Array(repeating: 0.0, count: 16)
        for column in 0..<4 {
            for row in 0..<4 {
                result[column * 4 + row] = (0..<4).reduce(0.0) { value, index in
                    value + left[index * 4 + row] * right[column * 4 + index]
                }
            }
        }
        return result
    }

    private func multiply(_ matrices: Matrix4...) -> Matrix4 {
        matrices.reduce(identityMatrix(), multiply)
    }

    private func translation(_ x: Double, _ y: Double, _ z: Double) -> Matrix4 {
        [1, 0, 0, 0,
         0, 1, 0, 0,
         0, 0, 1, 0,
         x, y, z, 1]
    }

    private func scale(_ x: Double, _ y: Double, _ z: Double) -> Matrix4 {
        [x, 0, 0, 0,
         0, y, 0, 0,
         0, 0, z, 0,
         0, 0, 0, 1]
    }

    private func rotation(_ degrees: Double, axisX: Double, axisY: Double, axisZ: Double) -> Matrix4 {
        let length = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        let x = axisX / length
        let y = axisY / length
        let z = axisZ / length
        let radians = degrees * .pi / 180.0
        let cosine = cos(radians)
        let sine = sin(radians)
        let inverseCosine = 1.0 - cosine
        let row0 = [
            cosine + x * x * inverseCosine,
            x * y * inverseCosine - z * sine,
            x * z * inverseCosine + y * sine,
            0.0
        ]
        let row1 = [
            y * x * inverseCosine + z * sine,
            cosine + y * y * inverseCosine,
            y * z * inverseCosine - x * sine,
            0.0
        ]
        let row2 = [
            z * x * inverseCosine - y * sine,
            z * y * inverseCosine + x * sine,
            cosine + z * z * inverseCosine,
            0.0
        ]
        return [
            row0[0], row1[0], row2[0], 0,
            row0[1], row1[1], row2[1], 0,
            row0[2], row1[2], row2[2], 0,
            0, 0, 0, 1
        ]
    }

    private func tiltedZRotation(angle: Double, tiltX: Double, tiltY: Double, mirrored: Bool) -> Matrix4 {
        let transformedTiltX = mirrored ? -tiltX : tiltX
        let transformedAngle = mirrored ? -angle : angle
        return multiply(
            rotation(transformedTiltX, axisX: 1, axisY: 0, axisZ: 0),
            rotation(tiltY, axisX: 0, axisY: 1, axisZ: 0),
            rotation(transformedAngle, axisX: 0, axisY: 0, axisZ: 1),
            rotation(-tiltY, axisX: 0, axisY: 1, axisZ: 0),
            rotation(-transformedTiltX, axisX: 1, axisY: 0, axisZ: 0)
        )
    }

    private func aroundPivot(_ transform: Matrix4, pivot: (Double, Double, Double)) -> Matrix4 {
        multiply(
            translation(pivot.0, pivot.1, pivot.2),
            transform,
            translation(-pivot.0, -pivot.1, -pivot.2)
        )
    }

    private func expectedV3Matrices(handSide: Int, positions: [Double]) -> [String: Matrix4] {
        let mirrored = handSide == 0
        let mirror = mirrored ? scale(1, -1, 1) : identityMatrix()
        let general = multiply(
            rotation(mirrored ? 95 : -95, axisX: 0, axisY: 1, axisZ: 0),
            rotation(90, axisX: 0, axisY: 0, axisZ: 1)
        )

        func digit(
            percent: Double,
            proximalStart: (Double, Double, Double),
            distalStart: (Double, Double, Double),
            linkOffset: (Double, Double, Double),
            finalOffset: (Double, Double, Double),
            proximalTilt: (Double, Double),
            distalTilt: (Double, Double)
        ) -> (Matrix4, Matrix4) {
            let first = tiltedZRotation(angle: -percent, tiltX: proximalTilt.0, tiltY: proximalTilt.1, mirrored: mirrored)
            let second = tiltedZRotation(angle: -percent, tiltX: distalTilt.0, tiltY: distalTilt.1, mirrored: mirrored)
            let proximal = multiply(
                general,
                translation(finalOffset.0, finalOffset.1, finalOffset.2),
                first,
                mirror,
                translation(proximalStart.0, proximalStart.1, proximalStart.2)
            )
            let distal = multiply(
                general,
                translation(finalOffset.0, finalOffset.1, finalOffset.2),
                first,
                translation(linkOffset.0, linkOffset.1, linkOffset.2),
                second,
                mirror,
                translation(distalStart.0, distalStart.1, distalStart.2)
            )
            return (proximal, distal)
        }

        let index = digit(
            percent: positions[3],
            proximalStart: (-10, 2, 29), distalStart: (-41, 2, 29),
            linkOffset: (31, 0, 0), finalOffset: (10, mirrored ? 2 : -2, -29),
            proximalTilt: (-4, 4), distalTilt: (-4, 4)
        )
        let middle = digit(
            percent: positions[2],
            proximalStart: (-12, 0, -11), distalStart: (-46.5, 0, -11),
            linkOffset: (34.5, 0, 0), finalOffset: (12, 0, 11),
            proximalTilt: (0, -1), distalTilt: (0, -1)
        )
        let ring = digit(
            percent: positions[1],
            proximalStart: (-9, 0, 8), distalStart: (-43, 0, 8),
            linkOffset: (34, 0, 0), finalOffset: (9, 0, -8),
            proximalTilt: (7, -6), distalTilt: (6, -3)
        )
        let little = digit(
            percent: positions[0],
            proximalStart: (-6, -10, 25), distalStart: (-39, -10, 25),
            linkOffset: (33, 0, 0), finalOffset: (6, mirrored ? -10 : 10, -25),
            proximalTilt: (16, -8), distalTilt: (16, -8)
        )

        func thumbAngle(percent: Double, minimum: Double, maximum: Double) -> Double {
            (maximum - min(100, max(0, percent)) * (maximum - minimum) / 100.0).rounded()
        }

        func correctedZRotation(_ angle: Double) -> Matrix4 {
            let correction = mirrored ? -34.0 : 34.0
            return multiply(
                rotation(correction, axisX: 1, axisY: 0, axisZ: 0),
                rotation(angle, axisX: 0, axisY: 0, axisZ: -1),
                rotation(-correction, axisX: 1, axisY: 0, axisZ: 0)
            )
        }

        func thumb(includeSecondPhalanx: Bool) -> Matrix4 {
            let sideSign = mirrored ? -1.0 : 1.0
            let firstAngle = thumbAngle(percent: positions[4], minimum: -35, maximum: 49)
            let secondAngle = thumbAngle(percent: positions[5], minimum: -68, maximum: 22)
            let phalanxAngle = thumbAngle(percent: positions[4], minimum: -25, maximum: 20)
            var model = mirror
            if includeSecondPhalanx {
                let pivot = (-18.0, mirrored ? 52.430767 : -52.430767, -49.062533)
                model = multiply(aroundPivot(correctedZRotation(mirrored ? 20 : -20), pivot: pivot), model)
                model = multiply(aroundPivot(correctedZRotation(sideSign * phalanxAngle), pivot: pivot), model)
            }
            let firstPivot = (-40.648183, mirrored ? 27.336317 : -27.336317, -31.565383)
            let secondPivot = (-65.678083, mirrored ? 18.191633 : -18.191633, -28.560333)
            model = multiply(aroundPivot(correctedZRotation(sideSign * firstAngle), pivot: firstPivot), model)
            let secondRotation = multiply(
                rotation(34, axisX: 1, axisY: 0, axisZ: 0),
                rotation(sideSign * (secondAngle + 34), axisX: 1, axisY: 0, axisZ: 0),
                rotation(-34, axisX: 1, axisY: 0, axisZ: 0)
            )
            model = multiply(aroundPivot(secondRotation, pivot: secondPivot), model)
            return multiply(general, model)
        }

        return [
            "base": multiply(general, mirror),
            "indexProximal": index.0,
            "indexDistal": index.1,
            "middleProximal": middle.0,
            "middleDistal": middle.1,
            "ringProximal": ring.0,
            "ringDistal": ring.1,
            "littleProximal": little.0,
            "littleDistal": little.1,
            "thumbFirst": thumb(includeSecondPhalanx: false),
            "thumbSecond": thumb(includeSecondPhalanx: true)
        ]
    }

    private func packetBytes(from byteArray: KotlinByteArray) -> [UInt8] {
        (0..<Int(byteArray.size)).map { index in
            UInt8(bitPattern: byteArray.get(index: Int32(index)))
        }
    }

    private func handSideSnapshot(
        address: Int32 = 1,
        parameterID: Int32 = 0x10,
        dataCode: Int32 = 0x0E,
        serializedValue: String
    ) -> ParameterSnapshotV3Bridge {
        ParameterSnapshotV3Bridge(
            addressDevice: address,
            parameterID: parameterID,
            dataCode: dataCode,
            codecId: "SPINNER",
            widgetKind: "SPINNER",
            valuePath: "spinnerValue",
            serializedValue: serializedValue
        )
    }

    private func handSideFromProvider(_ snapshot: ParameterSnapshotV3Bridge) -> Int? {
        guard let providerClass = NSClassFromString("MotoricaStart.V3HandSideProvider")
                ?? NSClassFromString("V3HandSideProvider") else {
            XCTFail("V3HandSideProvider class not found in runtime")
            return nil
        }
        let selector = NSSelectorFromString("sideValueForTestingFromSnapshot:")
        guard let method = class_getClassMethod(providerClass, selector) else {
            XCTFail("V3HandSideProvider test selector not found")
            return nil
        }
        typealias Function = @convention(c) (AnyClass, Selector, AnyObject) -> NSNumber?
        let function = unsafeBitCast(method_getImplementation(method), to: Function.self)
        return function(providerClass, selector, snapshot)?.intValue
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

    func testV3Transition_usesTenMillisecondDelayFiveMillisecondsPerPercentAndCosineEasing() {
        let start = Array(repeating: NSNumber(value: 0), count: 6)
        let target = Array(repeating: NSNumber(value: 100), count: 6)
        let delays = Array(repeating: NSNumber(value: 2), count: 6)

        let beforeDelay = callRendererV3Transition(start: start, target: target, delays: delays, elapsed: 0.019)
        let halfway = callRendererV3Transition(start: start, target: target, delays: delays, elapsed: 0.270)
        let complete = callRendererV3Transition(start: start, target: target, delays: delays, elapsed: 0.520)

        XCTAssertEqual(beforeDelay.count, 6)
        XCTAssertTrue(beforeDelay.allSatisfy { abs($0) < 0.0001 })
        XCTAssertTrue(halfway.allSatisfy { abs($0 - 50.0) < 0.0001 })
        XCTAssertTrue(complete.allSatisfy { abs($0 - 100.0) < 0.0001 })

        let interrupted = callRendererV3Transition(
            start: Array(repeating: NSNumber(value: 35), count: 6),
            target: Array(repeating: NSNumber(value: 80), count: 6),
            delays: Array(repeating: NSNumber(value: 0), count: 6),
            elapsed: 0.1125
        )
        XCTAssertTrue(interrupted.allSatisfy { abs($0 - 57.5) < 0.0001 })
    }

    func testV3Matrices_matchAndroidOrderForBothHandsAtZeroHalfAndFullTravel() {
        for handSide in [0, 1] {
            for percent in [0.0, 50.0, 100.0] {
                let positions = Array(repeating: percent, count: 6)
                let actual = callRendererV3MatrixSnapshots(
                    handSide: handSide,
                    positions: positions.map(NSNumber.init(value:))
                )
                let expected = expectedV3Matrices(handSide: handSide, positions: positions)
                XCTAssertEqual(actual.keys.sorted(), expected.keys.sorted())
                for key in expected.keys.sorted() {
                    guard let actualMatrix = actual[key], let expectedMatrix = expected[key] else {
                        XCTFail("Missing matrix \(key) side=\(handSide) percent=\(percent)")
                        continue
                    }
                    XCTAssertEqual(actualMatrix.count, 16, "Unexpected matrix size for \(key)")
                    for index in 0..<min(actualMatrix.count, expectedMatrix.count) {
                        XCTAssertEqual(
                            actualMatrix[index],
                            expectedMatrix[index],
                            accuracy: 0.0002,
                            "Matrix mismatch key=\(key) element=\(index) side=\(handSide) percent=\(percent)"
                        )
                    }
                }
            }
        }
    }

    func testV3HandSideProvider_mapsOnlyTheRegisteredSpinnerSnapshot() {
        XCTAssertEqual(
            handSideFromProvider(handSideSnapshot(serializedValue: "{\"spinnerValue\":0}")),
            0
        )
        XCTAssertEqual(
            handSideFromProvider(handSideSnapshot(serializedValue: "{\"spinnerValue\":1}")),
            1
        )
        XCTAssertNil(
            handSideFromProvider(handSideSnapshot(address: 2, serializedValue: "{\"spinnerValue\":0}"))
        )
        XCTAssertNil(
            handSideFromProvider(handSideSnapshot(parameterID: 0x11, serializedValue: "{\"spinnerValue\":0}"))
        )
        XCTAssertNil(
            handSideFromProvider(handSideSnapshot(dataCode: 0x0F, serializedValue: "{\"spinnerValue\":0}"))
        )
        XCTAssertNil(
            handSideFromProvider(handSideSnapshot(serializedValue: "{}"))
        )
    }

    func testV3HandSideReadRequest_mapsSetCodeToGetCode() {
        guard let packet = WidgetCommandBridgeV3.shared.buildReadRequest(
            parameterID: 0x10,
            dataCode: 0x0E
        ) else {
            XCTFail("Hand-side read request was not built")
            return
        }
        let bytes = packetBytes(from: packet)
        XCTAssertGreaterThanOrEqual(bytes.count, 4)
        XCTAssertEqual(bytes[1], 0x10)
        XCTAssertEqual(bytes[2], 0x0F)
    }

    func testGestureKeyClip_closesHoldsAndReturnsAtConstantSpeed() {
        func thumb(_ time: Double) -> Double {
            let state = gestureKeyClipState(at: time)
            return (state["fingers"] as? [NSNumber])?[4].doubleValue ?? .nan
        }
        func keyX(_ time: Double) -> Double {
            let state = gestureKeyClipState(at: time)
            return (state["object"] as? [NSNumber])?[0].doubleValue ?? .nan
        }

        XCTAssertEqual(thumb(0), -35, accuracy: 0.001)
        XCTAssertEqual(thumb(150), -14, accuracy: 0.001)
        XCTAssertEqual(thumb(300), 7, accuracy: 0.001)
        XCTAssertEqual(thumb(450), 7, accuracy: 0.001)
        XCTAssertEqual(thumb(600), 7, accuracy: 0.001)
        XCTAssertEqual(thumb(750), -14, accuracy: 0.001)
        XCTAssertEqual(thumb(900), -35, accuracy: 0.001)
        XCTAssertEqual(keyX(0), keyX(900), accuracy: 0.001)
        XCTAssertEqual((gestureKeyClipState(at: 899)["complete"] as? NSNumber)?.boolValue, false)
        XCTAssertEqual((gestureKeyClipState(at: 900)["complete"] as? NSNumber)?.boolValue, true)
    }

    func testCupGripClip_opensHoldsAndReturnsWithLittleFingerFixed() {
        func fingers(_ time: Double) -> [Double] {
            ((cupGripClipState(at: time)["fingers"] as? [NSNumber]) ?? []).map(\.doubleValue)
        }

        XCTAssertEqual(fingers(0), [100, 60, 58, 55, 50, 100])
        XCTAssertEqual(fingers(150), [100, 30, 29, 27.5, 25, 100])
        XCTAssertEqual(fingers(300), [100, 0, 0, 0, 0, 100])
        XCTAssertEqual(fingers(450), [100, 0, 0, 0, 0, 100])
        XCTAssertEqual(fingers(600), [100, 0, 0, 0, 0, 100])
        XCTAssertEqual(fingers(750), [100, 30, 29, 27.5, 25, 100])
        XCTAssertEqual(fingers(900), [100, 60, 58, 55, 50, 100])
        XCTAssertEqual((cupGripClipState(at: 899)["complete"] as? NSNumber)?.boolValue, false)
        XCTAssertEqual((cupGripClipState(at: 900)["complete"] as? NSNumber)?.boolValue, true)
    }
}
