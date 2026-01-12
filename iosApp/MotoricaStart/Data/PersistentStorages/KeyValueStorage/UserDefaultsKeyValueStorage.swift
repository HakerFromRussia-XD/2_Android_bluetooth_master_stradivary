//
//  UserDefaultsKeyValueStorage.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 24.12.2025.
//

import Foundation

final class UserDefaultsKeyValueStorage: KeyValueStorage {

    private let userDefaults: UserDefaults
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    private struct StoredValueContainer: Codable {
        let typeName: String
        let payload: Data
    }

    init(
        userDefaults: UserDefaults = .standard,
        encoder: JSONEncoder = JSONEncoder(),
        decoder: JSONDecoder = JSONDecoder()
    ) {
        self.userDefaults = userDefaults
        self.encoder = encoder
        self.decoder = decoder
    }

    func save<Value: Codable>(_ value: Value, for key: TypedStorageKey<Value>) throws {
        print("Вызвана функция SAVE  value =", value.self, "  key =", key.rawValue)
        do {
            let payload = try encoder.encode(value)
            let container = StoredValueContainer(typeName: key.typeName, payload: payload)
            let data = try encoder.encode(container)
            userDefaults.set(data, forKey: key.rawValue)
        } catch {
            throw KeyValueStorageError.encodingFailed(error)
        }
    }

    func load<Value: Codable>(for key: TypedStorageKey<Value>) throws -> Value? {
        guard let data = userDefaults.data(forKey: key.rawValue) else { return nil }
        do {
            let container = try decoder.decode(StoredValueContainer.self, from: data)
            guard container.typeName == key.typeName else {
                throw KeyValueStorageError.typeMismatch(
                    expected: key.typeName,
                    actual: container.typeName
                )
            }
            return try decoder.decode(Value.self, from: container.payload)
        } catch let error as KeyValueStorageError {
            throw error
        } catch {
            throw KeyValueStorageError.decodingFailed(error)
        }
    }

    func removeValue<Value>(for key: TypedStorageKey<Value>) {
        userDefaults.removeObject(forKey: key.rawValue)
    }
}
