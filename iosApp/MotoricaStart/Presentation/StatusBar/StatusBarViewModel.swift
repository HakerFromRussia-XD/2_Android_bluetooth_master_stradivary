//
//  StatusBarViewModel.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.01.2026.
//

import Foundation
import SwiftUI
import shared

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

enum V3StatusBarAction {
    case appearance
    case nameReceived(String)
    case currentConnectionRequested
    case forceConnected
    case connectionReceived(Bool)
    case batteryReceived(Int32)
}

final class V3StatusBarViewModel {
    private let output: StatusBarViewModel
    private let getDeviceInfoText: GetDeviceInfoTextUseCaseV3
    private let observeMainStatus: ObserveMainStatusUseCaseV3

    init(
        output: StatusBarViewModel,
        getDeviceInfoText: GetDeviceInfoTextUseCaseV3,
        observeMainStatus: ObserveMainStatusUseCaseV3
    ) {
        self.output = output
        self.getDeviceInfoText = getDeviceInfoText
        self.observeMainStatus = observeMainStatus
    }

    func onAction(_ action: V3StatusBarAction) {
        switch action {
        case .appearance:
            guard let name = getDeviceInfoText.invoke(field: .deviceName), !name.isEmpty else { return }
            output.update(serialNumber: name)
        case .nameReceived(let name):
            guard !name.isEmpty else { return }
            output.update(serialNumber: name)
        case .currentConnectionRequested:
            output.update(isConnected: observeMainStatus.currentConnectionReady())
        case .forceConnected:
            output.update(isConnected: true)
        case .connectionReceived(let isConnected):
            output.update(isConnected: isConnected)
        case .batteryReceived(let rawPercent):
            let percent = max(0, min(100, Int(rawPercent)))
            output.update(batteryLevel: Double(percent) / 100.0)
        }
    }

    func observeConnectionReady(_ callback: @escaping (Bool) -> Void) -> Kotlinx_coroutines_coreJob {
        observeMainStatus.observeConnectionReady { ready in
            callback(ready.boolValue)
        }
    }

    func observeBatteryPercent(_ callback: @escaping (KotlinInt) -> Void) -> Kotlinx_coroutines_coreJob {
        observeMainStatus.observeBatteryPercent(callback: callback)
    }
}
