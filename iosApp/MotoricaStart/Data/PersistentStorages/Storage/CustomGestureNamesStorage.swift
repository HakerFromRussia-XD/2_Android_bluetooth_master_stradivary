//
//  CustomGestureNamesStorage.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 25.12.2025.
//

import Foundation
import shared

final class CustomGestureNamesStorage {
    static let shared = CustomGestureNamesStorage()

    private let storage: KeyValueStorage
    private let storageKey = TypedStorageKey<[String]>(rawValue: "custom_gesture_names")

    init(storage: KeyValueStorage = UserDefaultsKeyValueStorage()) {
        self.storage = storage
    }

    func loadNames() -> [String] {
        let defaults = Self.defaultNames()
        do {
            if var stored = try storage.load(for: storageKey) {
                if stored.count != defaults.count {
                    stored = Self.mergedNames(stored, defaults: defaults)
                    try? storage.save(stored, for: storageKey)
                }
                print ("loadNames 3")
                return stored
            }
        } catch {
            print ("loadNames 1")
        }

        try? storage.save(defaults, for: storageKey)
        print ("loadNames 2")
        return defaults
    }

    @discardableResult
    func updateName(_ name: String, at index: Int) -> [String] {
        var names = loadNames()
        guard names.indices.contains(index) else { return names }
        names[index] = name
        try? storage.save(names, for: storageKey)
        return names
    }

    private static func mergedNames(_ stored: [String], defaults: [String]) -> [String] {
        if stored.count >= defaults.count {
            return Array(stored.prefix(defaults.count))
        }
        return stored + defaults[stored.count...]
    }

    private static func defaultNames() -> [String] {
        return [
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
