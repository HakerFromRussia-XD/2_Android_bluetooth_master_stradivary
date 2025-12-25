//
//  BluetoothStorageKeys.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 24.12.2025.
//

import Foundation

enum BluetoothStorageKeys {
    static let devices = TypedStorageKey<[BLEDevice]>(rawValue: "BLEDevices")
    static let selectedFilterIndex = TypedStorageKey<Int>(rawValue: "selectedFilterIndex")
}
