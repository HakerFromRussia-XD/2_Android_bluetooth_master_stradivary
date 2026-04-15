// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation
import shared

struct PlotListItemViewModel: Equatable, Hashable {
    private static let requestTracker = RequestTracker()
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let parameterInfoSet: Set<ParameterInfoData>
}

extension PlotListItemViewModel {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.parameterInfoSet = ParameterInfoData.makeSet(
            from: widget.plotUnified?.baseParameterWidgetStruct?.parameterInfoSet
        )

        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-plot"
    }

    func contains(ref: ParameterRef) -> Bool {
        parameterInfoSet.contains {
            $0.deviceAddress == ref.addressDevice &&
            $0.parameterID == ref.parameterID
        }
    }

    func cachedThresholds() -> (open: Int, close: Int)? {
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: Int32(widget.deviceAddress), parameterID: Int32(widget.parameterID))
        guard !parameter.data.isEmpty else { return nil }
        let decoded = SerializationObjects.shared.decodePlotThresholds(raw: "\"\(parameter.data)\"")
        return (open: Int(decoded.threshold1), close: Int(decoded.threshold2))
    }

    func requestThresholds() {
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }

        let data = BLECommands.shared.requestThresholds(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )

        sendBytes(data)
    }

    func sendThresholds(openThreshold: Int, closeThreshold: Int) {
        let data = BLECommands.shared.sendThresholdsCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            thresholds: [
                KotlinInt(int: Int32(openThreshold)),
                KotlinInt(int: Int32(closeThreshold))
            ]
        )

        sendBytes(data)
    }

    private func sendBytes(_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
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
