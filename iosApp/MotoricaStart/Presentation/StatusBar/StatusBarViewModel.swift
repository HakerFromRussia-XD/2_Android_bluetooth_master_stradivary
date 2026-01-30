//
//  StatusBarViewModel.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.01.2026.
//

import Foundation
import SwiftUI

final class StatusBarViewModel: ObservableObject {
    @Published var serialNumber: String
    @Published var batteryLevel: Double
    @Published var isConnected: Bool

    init(serialNumber: String = "—", batteryLevel: Double = 0.0, isConnected: Bool = false) {
        self.serialNumber = serialNumber
        self.batteryLevel = batteryLevel
        self.isConnected = isConnected
    }

    func update(serialNumber: String? = nil, batteryLevel: Double? = nil, isConnected: Bool? = nil) {
        if let serialNumber {
            self.serialNumber = serialNumber
        }
        if let batteryLevel {
            self.batteryLevel = batteryLevel
        }
        if let isConnected {
            self.isConnected = isConnected
        }
    }
}
