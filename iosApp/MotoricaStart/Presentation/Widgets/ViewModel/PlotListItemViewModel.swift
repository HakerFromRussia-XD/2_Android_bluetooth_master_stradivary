// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation
import DGCharts
import UIKit
import shared

struct PlotListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    private static let requestTracker = RequestTracker()
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension PlotListItemViewModel {
    init(widget: Widget,
         bleManager: BleManagerKmm
    ) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
    }
    func requestThresholds() {
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }
        let data = BLECommands.shared.requestThresholds(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )
        
        sendBytes(data)
        print("[request] requestThresholds")
    }
    func sendThresholds(openThreshold: Int, closeThreshold: Int) {
        print("sendThresholds openThreshold=\(openThreshold)   closeThreshold=\(closeThreshold)")
        let data = BLECommands.shared.sendThresholdsCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            thresholds: [
                KotlinInt(integerLiteral: openThreshold),
                0,
                KotlinInt(integerLiteral: closeThreshold),
                0
            ]
        )

        sendBytes(data)
    }
    private func sendBytes (_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        print("sendBytesKmm  iOS  отправляем данные: \(data.hexString)  из PlotListItemViewModel")
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
    static func == (lhs: PlotListItemViewModel, rhs: PlotListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
    
    static func resetRequestCache() {
        requestTracker.reset()
    }
}

private extension PlotListItemViewModel {
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

