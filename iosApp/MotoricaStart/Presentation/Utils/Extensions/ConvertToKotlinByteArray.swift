//
//  ConvertToKotlinByteArray.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 12.08.2025.
//
import shared

extension KotlinByteArray {
    convenience init(_ bytes: [UInt8]) {
        self.init(size: Int32(bytes.count))
        bytes.enumerated().forEach { set(index: Int32($0), value: Int8(bitPattern: $1)) }
    }
    var hex: String {
        (0..<Int(size)).map { String(format:"%02X", UInt8(bitPattern: get(index: Int32($0)))) }.joined(separator:" ")
    }
}
