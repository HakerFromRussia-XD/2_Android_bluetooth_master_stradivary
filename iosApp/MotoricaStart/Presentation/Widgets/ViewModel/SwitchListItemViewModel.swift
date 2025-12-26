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
    private static let valueCache = ValueCache()
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
        let cachedValue = cachedSwitchValue()
        print("[SWITCH][request] identifier=\(identifier) cachedValue=\(String(describing: cachedValue))")
        guard cachedValue == nil else {
            print("[SWITCH][request] skip request because cached value exists")
            return
        }
        guard Self.requestTracker.shouldRequest(for: identifier) else {
            print("[SWITCH][request] skip request because it was already sent")
            return
        }
        let data = BLECommands.shared.requestSwitcher(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )

        sendBytes(data)
        print("[SWITCH][request] requestSwitch deviceAddress = \(Int32(widget.deviceAddress))   parameterID = \(Int32(widget.parameterID))")
    }
    func sendSwitchState(isOn: Bool) {
        cacheSwitchValue(isOn)
        let data = BLECommands.shared.sendSwitcherCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            switchState: isOn
        )
        
        print("[request] SEND!!! requestSwitch deviceAddress = \(Int32(widget.deviceAddress))   parameterID = \(Int32(widget.parameterID))")
        sendBytes(data)
    }

    func cachedSwitchValue() -> Bool? {
        if let cached = Self.valueCache.value(for: identifier) {
            print("[SWITCH][cache] identifier=\(identifier) return cachedValue=\(cached)")
            return cached
        }
        
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: Int32(widget.deviceAddress), parameterID: Int32(widget.parameterID))
        guard parameter.firstReceiveDataFlag == false else {
            print("[SWITCH][cache] identifier=\(identifier) firstReceiveDataFlag still true, no cached data")
            return nil
        }

        let value = switchValue(from: parameter)
        if let value {
            cacheSwitchValue(value)
        } else {
            print("[SWITCH][cache] identifier=\(identifier) failed to decode parameter data")
        }
        return value
    }

    func switchValue(from parameter: BaseParameterInfoStruct) -> Bool? {
        let data = parameter.data
        guard data.count >= 2 else { return nil }

        let prefix = data.prefix(2)
        let value = Int(prefix, radix: 16) ?? 0

        print("[SWITCH][request] requestSwitch value = \(value != 0)")
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
        valueCache.reset()
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
            print("[SWITCH][tracker] identifier=\(identifier) shouldRequest=\(isNew)")
            return isNew
        }

        func reset() {
            lock.lock()
            requestedIdentifiers.removeAll()
            lock.unlock()
            print("[SWITCH][tracker] reset")
        }
    }

    final class ValueCache {
        private var values: [String: Bool] = [:]
        private let lock = NSLock()

        func value(for identifier: String) -> Bool? {
            lock.lock()
            defer { lock.unlock() }
            return values[identifier]
        }

        func setValue(_ value: Bool, for identifier: String) {
            lock.lock()
            values[identifier] = value
            lock.unlock()
            print("[SWITCH][cache] identifier=\(identifier) store value=\(value)")
        }

        func reset() {
            lock.lock()
            values.removeAll()
            lock.unlock()
            print("[SWITCH][cache] reset")
        }
    }
}

extension SwitchListItemViewModel {
    func cacheSwitchValue(_ value: Bool) {
        Self.valueCache.setValue(value, for: identifier)
    }
}
