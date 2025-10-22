//
//  SwitchListItemViewModel.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.10.2025.
//
import Foundation
import shared

struct SwitchListItemViewModel: Equatable, Hashable {
    private static let requestTracker = RequestTracker()
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension SwitchListItemViewModel {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
    }
    
    func requestSwitch() {
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }
        let data = BLECommands.shared.requestSwitcher(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )

        sendBytes(data)
        print("[request] requestSwitch deviceAddress = \(Int32(widget.deviceAddress))   parameterID = \(Int32(widget.parameterID))")
    }
    func sendSwitchState(isOn: Bool) {
        let data = BLECommands.shared.sendSwitcherCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            switchState: isOn
        )

        sendBytes(data)
    }

    func cachedSwitchValue() -> Bool? {
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: Int32(widget.deviceAddress), parameterID: Int32(widget.parameterID))

        guard parameter.firstReceiveDataFlag == false else { return nil }

        return switchValue(from: parameter)
    }

    func switchValue(from parameter: BaseParameterInfoStruct) -> Bool? {
        let data = parameter.data
        guard data.count >= 2 else { return nil }

        let prefix = data.prefix(2)
        let value = Int(prefix, radix: 16) ?? 0

        print("[request] requestSwitch value = \(value != 0)")
        return value != 0
    }

    
    private func sendBytes (_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {})
    }
    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }
    static func == (lhs: SwitchListItemViewModel, rhs: SwitchListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
    static func resetRequestCache() {
        requestTracker.reset()
    }
}

private extension SwitchListItemViewModel {
    final class RequestTracker {
        private var requestedIdentifiers: Set<String> = []
        private let lock = NSLock()

        func shouldRequest(for identifier: String) -> Bool {
            lock.lock()
            defer { lock.unlock() }

            let isNew = !requestedIdentifiers.contains(identifier)
            if isNew {
                requestedIdentifiers.insert(identifier)
            }
            return isNew
        }

        func reset() {
            lock.lock()
            requestedIdentifiers.removeAll()
            lock.unlock()
        }
    }
}
