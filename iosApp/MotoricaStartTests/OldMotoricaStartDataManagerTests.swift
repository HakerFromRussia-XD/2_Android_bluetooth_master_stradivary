@testable import OldMotoricaStart
import XCTest

final class OldMotoricaStartDataManagerTests: XCTestCase {
    private var documentsDirectory: URL!

    override func setUpWithError() throws {
        try super.setUpWithError()
        documentsDirectory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: documentsDirectory, withIntermediateDirectories: true)
        DataManager.setDocumentDirectoryOverrideForTesting(documentsDirectory)
        DataManager.resetCacheForTesting()
    }

    override func tearDownWithError() throws {
        DataManager.resetCacheForTesting()
        DataManager.setDocumentDirectoryOverrideForTesting(nil)
        if let documentsDirectory {
            try? FileManager.default.removeItem(at: documentsDirectory)
        }
        documentsDirectory = nil
        try super.tearDownWithError()
    }

    func testSaveLoadRoundTripForSaveObjectString() {
        let object = SaveObjectString(key: "DEVICE_MAC", value: "83A2C8C4-9007-417F-242E-99C27E2BFD80")

        DataManager.save(object, with: object.key)
        let loaded = DataManager.load(object.key, with: SaveObjectString.self)

        XCTAssertEqual(loaded.key, object.key)
        XCTAssertEqual(loaded.value, object.value)
    }

    func testSaveOverwritesExistingKey() {
        DataManager.save(SaveObjectString(key: "SENS_NUM", value: "1"), with: "SENS_NUM")
        DataManager.save(SaveObjectString(key: "SENS_NUM", value: "2"), with: "SENS_NUM")

        let loaded = DataManager.load("SENS_NUM", with: SaveObjectString.self)

        XCTAssertEqual(loaded.key, "SENS_NUM")
        XCTAssertEqual(loaded.value, "2")
    }

    func testDeleteRemovesFileAndCachedValue() {
        DataManager.save(SaveObjectString(key: "DRIVER_NUM_STRING", value: "2.37"), with: "DRIVER_NUM_STRING")
        XCTAssertEqual(valuesByKey(DataManager.loadAll(SaveObjectString.self))["DRIVER_NUM_STRING"], "2.37")

        DataManager.delete("DRIVER_NUM_STRING")

        XCTAssertFalse(FileManager.default.fileExists(atPath: documentsDirectory.appendingPathComponent("DRIVER_NUM_STRING").path))
        XCTAssertNil(valuesByKey(DataManager.loadAll(SaveObjectString.self))["DRIVER_NUM_STRING"])
    }

    func testLoadAllReturnsValidLegacyJsonFilesForSystemAndUuidKeys() {
        let uuidKey = "83A2C8C4-9007-417F-242E-99C27E2BFD80"
        writeLegacyObject(key: "DEVICE_MAC", value: uuidKey)
        writeLegacyObject(key: "DRIVER_NUM_STRING", value: "2.37")
        writeLegacyObject(key: "SENS_NUM", value: "14")
        writeLegacyObject(key: uuidKey, value: "INDY")

        let values = valuesByKey(DataManager.loadAll(SaveObjectString.self))

        XCTAssertEqual(values["DEVICE_MAC"], uuidKey)
        XCTAssertEqual(values["DRIVER_NUM_STRING"], "2.37")
        XCTAssertEqual(values["SENS_NUM"], "14")
        XCTAssertEqual(values[uuidKey], "INDY")
    }

    func testLoadAllRegressionSkipsEmptyBrokenForeignFilesAndDirectories() throws {
        // Before DataManager hardening this red scenario reached fatalError via loadAll -> load.
        writeLegacyObject(key: "DEVICE_MAC", value: "F2B2C176-16CD-F595-25AC-28F99EFDC0BD")
        try Data().write(to: documentsDirectory.appendingPathComponent("ubi4.db.lck"))
        try Data("{".utf8).write(to: documentsDirectory.appendingPathComponent("BROKEN_JSON"))
        try Data("not sqlite json".utf8).write(to: documentsDirectory.appendingPathComponent("ubi4.db"))
        try Data("wal".utf8).write(to: documentsDirectory.appendingPathComponent("ubi4.db-wal"))
        try Data("shm".utf8).write(to: documentsDirectory.appendingPathComponent("ubi4.db-shm"))
        try FileManager.default.createDirectory(
            at: documentsDirectory.appendingPathComponent("Firmware", isDirectory: true),
            withIntermediateDirectories: true
        )

        let values = valuesByKey(DataManager.loadAll(SaveObjectString.self))

        XCTAssertEqual(values, ["DEVICE_MAC": "F2B2C176-16CD-F595-25AC-28F99EFDC0BD"])
    }

    func testLoadAllRegressionHandlesLargeLegacyJsonCatalog() {
        // The old INDY freeze reproduced when repeated BLE updates scanned a large Documents catalog.
        for index in 0..<250 {
            writeLegacyObject(key: "LEGACY_KEY_\(index)", value: "\(index)")
        }

        let values = valuesByKey(DataManager.loadAll(SaveObjectString.self))

        XCTAssertEqual(values.count, 250)
        XCTAssertEqual(values["LEGACY_KEY_0"], "0")
        XCTAssertEqual(values["LEGACY_KEY_249"], "249")
    }

    func testSaveAndDeleteKeepLoadAllCacheInSync() {
        DataManager.save(SaveObjectString(key: "DEVICE_MAC", value: "old"), with: "DEVICE_MAC")
        XCTAssertEqual(valuesByKey(DataManager.loadAll(SaveObjectString.self))["DEVICE_MAC"], "old")

        DataManager.save(SaveObjectString(key: "DEVICE_MAC", value: "new"), with: "DEVICE_MAC")
        DataManager.save(SaveObjectString(key: "SENS_NUM", value: "11"), with: "SENS_NUM")
        DataManager.delete("DEVICE_MAC")

        let values = valuesByKey(DataManager.loadAll(SaveObjectString.self))
        XCTAssertNil(values["DEVICE_MAC"])
        XCTAssertEqual(values["SENS_NUM"], "11")
    }

    private func writeLegacyObject(key: String, value: String) {
        let object = SaveObjectString(key: key, value: value)
        let data = try! JSONEncoder().encode(object)
        try! data.write(to: documentsDirectory.appendingPathComponent(key, isDirectory: false))
    }

    private func valuesByKey(_ objects: [SaveObjectString]) -> [String: String] {
        Dictionary(uniqueKeysWithValues: objects.map { ($0.key, $0.value) })
    }
}

final class OldMotoricaStartLegacyIndyPackedStatusTests: XCTestCase {
    func testPackedStatusMirrorsReverseSensorsFlagForOldIndy() {
        let status = LegacyIndyPackedStatus(
            driverNum: 3,
            bmsNum: 4,
            sensNum: 5,
            openThreshold: 6,
            closeThreshold: 7,
            openSensOption: 8,
            closeSensOption: 9,
            shutdownCurrent: 10,
            scaleFlags: 0b00000001
        )

        XCTAssertEqual(status.sensorValues["scale_flags_and_revers_and_one_channel"], "1")
        XCTAssertEqual(status.sensorValues["set_reverse"], "1")
        XCTAssertEqual(status.sensorValues["set_one_channel"], "0")
    }

    func testPackedStatusClearsReverseSensorsWhenOldIndyFlagIsOff() {
        let status = LegacyIndyPackedStatus(
            driverNum: 3,
            bmsNum: 4,
            sensNum: 5,
            openThreshold: 6,
            closeThreshold: 7,
            openSensOption: 8,
            closeSensOption: 9,
            shutdownCurrent: 10,
            scaleFlags: 0b00000010
        )

        XCTAssertEqual(status.sensorValues["scale_flags_and_revers_and_one_channel"], "2")
        XCTAssertEqual(status.sensorValues["set_reverse"], "0")
        XCTAssertEqual(status.sensorValues["set_one_channel"], "1")
    }
}
