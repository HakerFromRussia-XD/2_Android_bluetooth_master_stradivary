//
//  BLEDevice.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import Foundation

public struct BLEDevice: Identifiable, Hashable {
    public let id: UUID
    public let name: String
    public let uuid: UUID
    public let rssi: Int
}
