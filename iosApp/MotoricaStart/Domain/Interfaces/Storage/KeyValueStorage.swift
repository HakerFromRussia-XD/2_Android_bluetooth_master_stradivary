//
//  KeyValueStorage.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 24.12.2025.
//

import Foundation

public enum KeyValueStorageError: Error {
    case missingValue
    case typeMismatch(expected: String, actual: String)
    case encodingFailed(Error)
    case decodingFailed(Error)
}

public struct TypedStorageKey<Value: Codable> {
    public let rawValue: String
    public let typeName: String

    public init(rawValue: String, typeName: String = String(describing: Value.self)) {
        self.rawValue = rawValue
        self.typeName = typeName
    }
}

public protocol KeyValueStorage {
    func save<Value: Codable>(_ value: Value, for key: TypedStorageKey<Value>) throws
    func load<Value: Codable>(for key: TypedStorageKey<Value>) throws -> Value?
    func removeValue<Value>(for key: TypedStorageKey<Value>)
}
