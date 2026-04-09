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
    let parameterInfoSet: Set<ParameterInfoData>
    private let thresholdBinding: WidgetV3BindingInfo?
}

extension PlotListItemViewModel {
    init(widget: Widget,
         bleManager: BleManagerKmm
    ) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.parameterInfoSet = ParameterInfoData.makeSet(
            from: widget.plotUnified?.baseParameterWidgetStruct?.parameterInfoSet
        )
        self.thresholdBinding = WidgetV3Support.bindings(from: widget)
            .first(where: { $0.dataCode == 26 }) // PDCE_OPEN_CLOSE_THRESHOLD
        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let thresholdBinding {
            self.identifier = "\(widgetPosition)-\(thresholdBinding.deviceAddress)-\(thresholdBinding.parameterID)-\(thresholdBinding.dataCode)-plot"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-plot"
        }
    }

    func contains(ref: ParameterRef) -> Bool {
        parameterInfoSet.contains {
            $0.deviceAddress == ref.addressDevice &&
            $0.parameterID == ref.parameterID
        }
    }

    func matchesThresholdSnapshot(_ snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard snapshot.codecId == "THRESHOLDS" else { return false }
        return parameterInfoSet.contains {
            $0.deviceAddress == snapshot.addressDevice &&
            $0.parameterID == snapshot.parameterID
        }
    }

    func thresholds(from snapshot: ParameterSnapshotV3Bridge) -> (open: Int, close: Int)? {
        guard
            let open = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openThreshold"),
            let close = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeThreshold")
        else {
            return nil
        }
        return (open, close)
    }

    func cachedThresholds() -> (open: Int, close: Int)? {
        if let thresholdBinding,
           let snapshot = WidgetStateBridgeV3.shared.getCurrent(
                addressDevice: Int32(thresholdBinding.deviceAddress),
                parameterID: Int32(thresholdBinding.parameterID),
                dataCode: Int32(thresholdBinding.dataCode)
           ),
           let decoded = thresholds(from: snapshot) {
            return decoded
        }

        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: Int32(widget.deviceAddress), parameterID: Int32(widget.parameterID))
        guard !parameter.data.isEmpty else { return nil }
        let decoded = SerializationObjects.shared.decodePlotThresholds(raw: "\"\(parameter.data)\"")
        return (open: Int(decoded.threshold1), close: Int(decoded.threshold2))
    }

    func requestThresholds() {
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }
        let data: KotlinByteArray
        if let thresholdBinding {
            guard let mapped = WidgetCommandBridgeV3.shared.buildReadRequest(
                parameterID: Int32(thresholdBinding.parameterID),
                dataCode: Int32(thresholdBinding.dataCode)
            ) else { return }
            data = mapped
        } else {
            let targetAddress = Int32(widget.deviceAddress)
            let targetParameterID = Int32(widget.parameterID)
            data = BLECommands.shared.requestThresholds(
                addressDevice: targetAddress,
                parameterID: targetParameterID
            )
        }
        
        sendBytes(data)
        print("[request] requestThresholds")
    }
    func sendThresholds(openThreshold: Int, closeThreshold: Int) {
        print("sendThresholds openThreshold=\(openThreshold)   closeThreshold=\(closeThreshold)")
        let data = WidgetCommandBridgeV3.shared.buildSendThresholds(
            openThreshold: Int32(openThreshold),
            closeThreshold: Int32(closeThreshold)
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
