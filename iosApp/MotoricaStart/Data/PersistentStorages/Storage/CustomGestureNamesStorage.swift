import Foundation
import shared

final class V3CustomGestureNamesRepositoryImpl: NSObject, V3CustomGestureNamesRepository {
    static let shared = V3CustomGestureNamesRepositoryImpl()
    private let keyValueStorage: KeyValueStorage

    init(storage: KeyValueStorage = UserDefaultsKeyValueStorage()) {
        keyValueStorage = storage
        super.init()
    }

    func getCustomGestureNames() -> V3CustomGestureNames {
        V3CustomGestureNames(names: loadNames(), collectionNames: [])
    }

    func renameGesture(index: Int32, name: String) -> [String] {
        updateName(name, at: Int(index))
    }

    func getDeviceName() -> String {
        do {
            return try keyValueStorage.load(for: BluetoothStorageKeys.selectedDeviceNameStorageKey) ?? ""
        } catch {
            print("[Storage] failed to load selected device name: \(error)")
            return ""
        }
    }

    func loadNames() -> [String] {
        let defaults = Self.defaultNames()
        let key = TypedStorageKey<[String]>(
            rawValue: BluetoothStorageKeys.customGestureNameStorageKey.rawValue + getDeviceName()
        )
        do {
            if var stored = try keyValueStorage.load(for: key) {
                if stored.count != defaults.count {
                    stored = Self.mergedNames(stored, defaults: defaults)
                    try? keyValueStorage.save(stored, for: key)
                }
                print("loadNames 3")
                return stored
            }
        } catch {
            print("loadNames 1")
        }

        try? keyValueStorage.save(defaults, for: key)
        print("loadNames 2")
        return defaults
    }

    @discardableResult
    func updateName(_ name: String, at index: Int) -> [String] {
        let key = TypedStorageKey<[String]>(
            rawValue: BluetoothStorageKeys.customGestureNameStorageKey.rawValue + getDeviceName()
        )
        var names = loadNames()
        guard names.indices.contains(index) else { return names }
        names[index] = name
        print("Вызвана функция updateName names = \(names)")
        try? keyValueStorage.save(names, for: key)
        return names
    }

    private static func mergedNames(_ stored: [String], defaults: [String]) -> [String] {
        if stored.count >= defaults.count {
            return Array(stored.prefix(defaults.count))
        }
        return stored + Array(defaults[stored.count...])
    }

    private static func defaultNames() -> [String] {
        [
            SharedRes.strings().gesture_1_btn,
            SharedRes.strings().gesture_2_btn,
            SharedRes.strings().gesture_3_btn,
            SharedRes.strings().gesture_4_btn,
            SharedRes.strings().gesture_5_btn,
            SharedRes.strings().gesture_6_btn,
            SharedRes.strings().gesture_7_btn,
            SharedRes.strings().gesture_8_btn,
            SharedRes.strings().gesture_9_btn,
            SharedRes.strings().gesture_10_btn,
            SharedRes.strings().gesture_11_btn,
            SharedRes.strings().gesture_12_btn,
            SharedRes.strings().gesture_13_btn,
            SharedRes.strings().gesture_14_btn
        ].map { $0.desc().localized() }
    }
}
