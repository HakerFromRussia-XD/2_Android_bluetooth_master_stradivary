//
//  BluetoothScanDeviceNameFormatter.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 01.06.2026.
//
import Foundation
import shared

enum BluetoothScanDeviceNameFormatter {
    static func displayName(_ deviceName: String?) -> String {
        let normalized = deviceName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !normalized.isEmpty else { return "" }

        let newDeviceName = DeviceNameBridgeV3.shared.displayName(deviceName: normalized)
        if newDeviceName != normalized {
            return newDeviceName
        }

        return legacyDisplayName(deviceName: normalized)
    }

    private static func legacyDisplayName(deviceName: String) -> String {
        guard deviceName.contains("FEST-X"), deviceName.count > 10, !deviceName.contains(" ") else {
            return deviceName
        }

        let prefixStart = deviceName.index(deviceName.startIndex, offsetBy: 6)
        let prefixEnd = deviceName.index(prefixStart, offsetBy: 4)
        let namePrefix = String(deviceName[prefixStart..<prefixEnd])
        let nameCode = String(deviceName[prefixEnd..<deviceName.endIndex])

        switch namePrefix {
        case "FTFS":
            return "FEST-F-\(nameCode)"
        case "FTHS":
            return "FEST-H-\(nameCode)"
        case "FTFO":
            return "FEST-FO-\(nameCode)"
        case "FTHO":
            return "FEST-HO-\(nameCode)"
        case "FTEP":
            return "FEST-EP-\(nameCode)"
        case "FTEB":
            return "FEST-EB-\(nameCode)"
        default:
            return deviceName
        }
    }
}
