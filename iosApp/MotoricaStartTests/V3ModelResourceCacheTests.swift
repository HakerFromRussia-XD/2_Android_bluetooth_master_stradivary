import XCTest
import ObjectiveC.runtime

final class V3ModelResourceCacheTests: XCTestCase {
    private typealias SimpleValidator = @convention(c) (
        AnyClass,
        Selector,
        NSData,
        UnsafeMutableRawPointer?
    ) -> Bool

    private typealias DeformationValidator = @convention(c) (
        AnyClass,
        Selector,
        NSData,
        Int,
        Bool,
        UnsafeMutableRawPointer?
    ) -> Bool

    private func cacheClass() throws -> AnyClass {
        guard let cacheClass = NSClassFromString("V3ModelResourceCache") else {
            throw XCTSkip("V3ModelResourceCache is unavailable in the test host")
        }
        return cacheClass
    }

    private func validate(_ data: Data, selectorName: String) throws -> Bool {
        let cacheClass = try cacheClass()
        let selector = NSSelectorFromString(selectorName)
        guard let method = class_getClassMethod(cacheClass, selector) else {
            XCTFail("Missing V3 cache validator \(selectorName)")
            return false
        }
        let function = unsafeBitCast(method_getImplementation(method), to: SimpleValidator.self)
        return function(cacheClass, selector, data as NSData, nil)
    }

    private func validateDeformation(
        _ data: Data,
        vertexCount: Int = 3,
        requiresCenterline: Bool
    ) throws -> Bool {
        let cacheClass = try cacheClass()
        let selector = NSSelectorFromString(
            "validateDeformationData:expectedVertexCount:requiresCenterline:error:"
        )
        guard let method = class_getClassMethod(cacheClass, selector) else {
            XCTFail("Missing V3 deformation validator")
            return false
        }
        let function = unsafeBitCast(method_getImplementation(method), to: DeformationValidator.self)
        return function(cacheClass, selector, data as NSData, vertexCount, requiresCenterline, nil)
    }

    private func sharedCacheObject() throws -> AnyObject {
        let cacheClass = try cacheClass()
        let selector = NSSelectorFromString("sharedCache")
        guard let method = class_getClassMethod(cacheClass, selector) else {
            throw XCTSkip("V3 cache singleton is unavailable")
        }
        typealias Function = @convention(c) (AnyClass, Selector) -> AnyObject
        let function = unsafeBitCast(method_getImplementation(method), to: Function.self)
        return function(cacheClass, selector)
    }

    private func callVoid(_ object: AnyObject, selectorName: String) {
        let selector = NSSelectorFromString(selectorName)
        guard let method = class_getInstanceMethod(type(of: object), selector) else {
            XCTFail("Missing V3 cache method \(selectorName)")
            return
        }
        typealias Function = @convention(c) (AnyObject, Selector) -> Void
        let function = unsafeBitCast(method_getImplementation(method), to: Function.self)
        function(object, selector)
    }

    private func preload(_ object: AnyObject, completion: @escaping (Bool, NSError?) -> Void) {
        let selector = NSSelectorFromString("preloadWithCompletion:")
        guard let method = class_getInstanceMethod(type(of: object), selector) else {
            XCTFail("Missing V3 preload method")
            completion(false, nil)
            return
        }
        typealias Completion = @convention(block) (Bool, NSError?) -> Void
        typealias Function = @convention(c) (AnyObject, Selector, Completion) -> Void
        let function = unsafeBitCast(method_getImplementation(method), to: Function.self)
        let block: Completion = completion
        function(object, selector, block)
    }

    private func resolvedGroups(_ object: AnyObject) -> [String: [String]] {
        let selector = NSSelectorFromString("resolvedGroupPartIDsForTesting")
        guard let method = class_getInstanceMethod(type(of: object), selector) else {
            XCTFail("Missing V3 group snapshot method")
            return [:]
        }
        typealias Function = @convention(c) (AnyObject, Selector) -> NSDictionary
        let function = unsafeBitCast(method_getImplementation(method), to: Function.self)
        return function(object, selector) as? [String: [String]] ?? [:]
    }

    private func deformationDiagnostics(_ object: AnyObject) -> [String: [String: NSNumber]] {
        let selector = NSSelectorFromString("deformationDiagnosticsForTesting")
        guard let method = class_getInstanceMethod(type(of: object), selector) else {
            XCTFail("Missing V3 deformation diagnostics method")
            return [:]
        }
        typealias Function = @convention(c) (AnyObject, Selector) -> NSDictionary
        let function = unsafeBitCast(method_getImplementation(method), to: Function.self)
        return function(object, selector) as? [String: [String: NSNumber]] ?? [:]
    }

    private func appendInt32(_ value: Int32, to data: inout Data) {
        var littleEndian = UInt32(bitPattern: value).littleEndian
        Swift.withUnsafeBytes(of: &littleEndian) { data.append(contentsOf: $0) }
    }

    private func appendUInt32(_ value: UInt32, to data: inout Data) {
        var littleEndian = value.littleEndian
        Swift.withUnsafeBytes(of: &littleEndian) { data.append(contentsOf: $0) }
    }

    private func appendFloat(_ value: Float, to data: inout Data) {
        appendUInt32(value.bitPattern, to: &data)
    }

    private func appendVertex(_ position: (Float, Float, Float), nanNormal: Bool, to data: inout Data) {
        let values: [Float] = [
            position.0, position.1, position.2,
            nanNormal ? .nan : 0, 0, 1,
            1, 1, 1, 1,
            0, 0,
            1, 0, 0,
            0, 1, 0
        ]
        values.forEach { appendFloat($0, to: &data) }
    }

    private func appendGeometry(
        badIndex: Bool = false,
        nanNormal: Bool = false,
        to data: inout Data
    ) {
        appendVertex((0, 0, 0), nanNormal: nanNormal, to: &data)
        appendVertex((1, 0, 0), nanNormal: false, to: &data)
        appendVertex((0, 1, 0), nanNormal: false, to: &data)
        appendUInt32(0, to: &data)
        appendUInt32(1, to: &data)
        appendUInt32(badIndex ? 3 : 2, to: &data)
    }

    private func makeV3MB(badIndex: Bool = false, nanNormal: Bool = false) -> Data {
        var data = Data("V3MB".utf8)
        [1, 18, 3, 3, 0, 0, 0, 0, 0, 0].forEach { appendInt32(Int32($0), to: &data) }
        appendGeometry(badIndex: badIndex, nanNormal: nanNormal, to: &data)
        return data
    }

    private func makeV3PB() -> Data {
        let partName = Data("part_1".utf8)
        var data = Data("V3PB".utf8)
        [1, 18, 1, 3, 3, 0, 0, 0, 0, 0, 0].forEach { appendInt32(Int32($0), to: &data) }
        [partName.count, 3, 3, 1, 1, 3].forEach { appendInt32(Int32($0), to: &data) }
        data.append(partName)
        appendGeometry(to: &data)
        return data
    }

    private func makeV3DF(version: Int32, invalidWeight: Bool = false) -> Data {
        var data = Data("V3DF".utf8)
        [version, 3, 6].forEach { appendInt32($0, to: &data) }
        for vertex in 0..<3 {
            appendFloat(invalidWeight && vertex == 0 ? 1.2 : 1, to: &data)
            appendFloat(invalidWeight && vertex == 0 ? -0.2 : 0, to: &data)
            for _ in 0..<4 { appendFloat(0, to: &data) }
        }
        if version >= 2 {
            for _ in 0..<3 { appendInt32(0, to: &data) }
        }
        if version == 3 {
            appendInt32(5, to: &data)
            for node in 0..<5 {
                appendFloat(Float(node), to: &data)
                appendFloat(0, to: &data)
                appendFloat(0, to: &data)
            }
        }
        return data
    }

    func testBinaryFormats_acceptValidV3PBAndV3MB() throws {
        XCTAssertTrue(try validate(makeV3PB(), selectorName: "validatePartsBundleData:error:"))
        XCTAssertTrue(try validate(makeV3MB(), selectorName: "validateModelPartData:error:"))
    }

    func testBinaryFormats_rejectTruncationTrailingBytesBadIndicesAndNaN() throws {
        var trailing = makeV3MB()
        trailing.append(0)
        XCTAssertFalse(try validate(Data(makeV3PB().dropLast()), selectorName: "validatePartsBundleData:error:"))
        XCTAssertFalse(try validate(trailing, selectorName: "validateModelPartData:error:"))
        XCTAssertFalse(try validate(makeV3MB(badIndex: true), selectorName: "validateModelPartData:error:"))
        XCTAssertFalse(try validate(makeV3MB(nanNormal: true), selectorName: "validateModelPartData:error:"))
    }

    func testDeformationVersions_validateWeightsAndRequiredCenterline() throws {
        let version2 = makeV3DF(version: 2)
        let version3 = makeV3DF(version: 3)
        XCTAssertTrue(try validateDeformation(version2, requiresCenterline: false))
        XCTAssertFalse(try validateDeformation(version2, requiresCenterline: true))
        XCTAssertTrue(try validateDeformation(version3, requiresCenterline: true))
        XCTAssertFalse(try validateDeformation(makeV3DF(version: 3, invalidWeight: true), requiresCenterline: true))

        var trailing = version3
        trailing.append(0)
        XCTAssertFalse(try validateDeformation(trailing, requiresCenterline: true))
    }

    func testBundledManifest_resolvesExpectedProductionGroups() throws {
        let cache = try sharedCacheObject()
        callVoid(cache, selectorName: "resetForTesting")
        let ready = expectation(description: "V3 model preload")
        var preloadError: NSError?
        preload(cache) { success, error in
            XCTAssertTrue(success, error?.localizedDescription ?? "V3 preload failed")
            preloadError = error
            ready.fulfill()
        }
        wait(for: [ready], timeout: 30)
        XCTAssertNil(preloadError)

        let groups = resolvedGroups(cache)
        XCTAssertEqual(groups["index_upper_white_plastic"], ["part_10"])
        XCTAssertEqual(Set(groups["deformable_rubber"] ?? []), Set([
            "gofra_1_deformable",
            "gofra_2_deformable"
        ]))
        XCTAssertEqual(Set(groups["selection_surface"] ?? []), Set([
            "palm_1_part_24",
            "palm_1_part_25",
            "rubber_1_part_23"
        ]))
        XCTAssertEqual(groups["all"]?.count, 35)
    }

    func testProductionBellows_deformWithoutNaNOrAnchorGaps() throws {
        let cache = try sharedCacheObject()
        callVoid(cache, selectorName: "resetForTesting")
        let ready = expectation(description: "V3 model preload for deformation")
        preload(cache) { success, error in
            XCTAssertTrue(success, error?.localizedDescription ?? "V3 preload failed")
            ready.fulfill()
        }
        wait(for: [ready], timeout: 30)

        let diagnostics = deformationDiagnostics(cache)
        XCTAssertEqual(Set(diagnostics.keys), Set(["gofra_1_deformable", "gofra_2_deformable"]))
        for (partID, values) in diagnostics {
            let vertexCount = values["vertexCount"]?.intValue ?? 0
            XCTAssertEqual(values["valid"]?.boolValue, true, "Invalid deformation runtime for \(partID)")
            XCTAssertGreaterThan(vertexCount, 0, "No deformable vertices for \(partID)")
            XCTAssertEqual(values["finiteVertexCount"]?.intValue, vertexCount, "Non-finite output for \(partID)")
            XCTAssertLessThan(values["identityMaxPositionError"]?.doubleValue ?? .infinity, 0.001, "Identity drift in \(partID)")
            XCTAssertGreaterThan(values["bottomAnchorCount"]?.intValue ?? 0, 0, "Missing palm anchors in \(partID)")
            XCTAssertGreaterThan(values["topAnchorCount"]?.intValue ?? 0, 0, "Missing finger anchors in \(partID)")
            XCTAssertLessThan(values["bottomAnchorMaxError"]?.doubleValue ?? .infinity, 0.001, "Palm anchor gap in \(partID)")
            XCTAssertLessThan(values["topAnchorMaxError"]?.doubleValue ?? .infinity, 0.001, "Finger anchor gap in \(partID)")
            XCTAssertGreaterThan(values["minimumNormalLength"]?.doubleValue ?? 0, 0.5, "Invalid normals in \(partID)")
            XCTAssertLessThan(
                values["maximumCoincidentPositionSeparation"]?.doubleValue ?? .infinity,
                0.001,
                "Coincident seam vertices separate in \(partID): \(values)"
            )
        }
    }
}
