//
//  BluetoothStorageKeys.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 24.12.2025.
//

import Foundation

enum BluetoothStorageKeys {
    static let devicesStorageKey = TypedStorageKey<[BLEDevice]>(rawValue: "BLEDevices")
    static let selectedFilterIndexStorageKey = TypedStorageKey<Int>(rawValue: "selectedFilterIndex")
    static let selectedDeviceNameStorageKey = TypedStorageKey<String>(rawValue: "selectedDeviceName")
    static let customGestureNameStorageKey = TypedStorageKey<[String]>(rawValue: "customGestureNames")
}
