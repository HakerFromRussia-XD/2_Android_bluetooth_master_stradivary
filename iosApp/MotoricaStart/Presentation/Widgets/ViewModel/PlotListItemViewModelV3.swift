import Foundation
import shared

struct PlotListItemViewModelV3: Equatable, Hashable {
    private static let requestTracker = RequestTracker()
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let parameterInfoSet: Set<ParameterInfoData>
    private let thresholdBinding: WidgetV3BindingInfo?
}

extension PlotListItemViewModelV3 {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        let parameterInfoSet = ParameterInfoData.makeSet(
            from: widget.plotUnified?.baseParameterWidgetStruct?.parameterInfoSet
        )
        self.parameterInfoSet = parameterInfoSet
        self.thresholdBinding = Self.selectThresholdBinding(
            from: WidgetV3Support.bindings(from: widget),
            fallback: parameterInfoSet
        )

        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let thresholdBinding {
            self.identifier = "\(widgetPosition)-\(thresholdBinding.deviceAddress)-\(thresholdBinding.parameterID)-\(thresholdBinding.dataCode)-plot-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-plot-v3"
        }
    }

    func matchesThresholdSnapshot(_ snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard snapshot.codecId == "THRESHOLDS" else { return false }

        if let target = resolveThresholdTarget() {
            return snapshot.addressDevice == Int32(target.deviceAddress)
                && snapshot.parameterID == Int32(target.parameterID)
                && Self.canonicalThresholdDataCode(for: Int(snapshot.dataCode))
                    == Self.canonicalThresholdDataCode(for: target.dataCode)
        }

        return parameterInfoSet.contains {
            $0.deviceAddress == snapshot.addressDevice
                && $0.parameterID == snapshot.parameterID
        }
    }

    func thresholds(from snapshot: ParameterSnapshotV3Bridge) -> (open: Int, close: Int)? {
        guard
            let deviceOpen = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openThreshold"),
            let deviceClose = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeThreshold")
        else {
            return nil
        }
        // V3 payload returns thresholds in hardware order opposite to current UI semantics.
        return (open: deviceClose, close: deviceOpen)
    }

    func cachedThresholds() -> (open: Int, close: Int)? {
        guard let target = resolveThresholdTarget() else {
            print("[V3-PLOT][VM] cachedThresholds skipped: threshold target is unresolved")
            return nil
        }

        for dataCode in Self.thresholdDataCodeCandidates(for: target.dataCode) {
            if let snapshot = WidgetStateBridgeV3.shared.getCurrent(
                addressDevice: Int32(target.deviceAddress),
                parameterID: Int32(target.parameterID),
                dataCode: Int32(dataCode)
            ) {
                return thresholds(from: snapshot)
            }
        }

        return nil
    }

    func requestThresholds() {
        guard let target = resolveThresholdTarget() else {
            print("[V3-PLOT][VM] requestThresholds skipped: threshold target is unresolved")
            return
        }
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }
        let readDataCode = Self.canonicalThresholdDataCode(for: target.dataCode)
        guard let data = WidgetCommandBridgeV3.shared.buildReadRequest(
            parameterID: Int32(target.parameterID),
            dataCode: Int32(readDataCode)
        ) else {
            print("[V3-PLOT][VM] requestThresholds failed: buildReadRequest returned nil, target=\(target)")
            return
        }

        print("[V3-PLOT][VM] requestThresholds target=\(target) bytes=\(data.hexString)")
        sendBytes(data)
    }

    func sendThresholds(openThreshold: Int, closeThreshold: Int) {
        let normalizedUiOpen = min(max(openThreshold, 0), 255)
        let normalizedUiClose = min(max(closeThreshold, 0), 255)
        // Keep UI semantics aligned with legacy PlotViewCell: swap before sending to device.
        let deviceOpen = normalizedUiClose
        let deviceClose = normalizedUiOpen
        let data = WidgetCommandBridgeV3.shared.buildSendThresholds(
            openThreshold: Int32(deviceOpen),
            closeThreshold: Int32(deviceClose)
        )
        print(
            "[V3-PLOT][VM] sendThresholds uiOpen=\(openThreshold)->\(normalizedUiOpen) uiClose=\(closeThreshold)->\(normalizedUiClose) deviceOpen=\(deviceOpen) deviceClose=\(deviceClose) bytes=\(data.hexString)"
        )
        sendBytes(data)
    }

    private func sendBytes(_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        print(
            "[V3-PLOT][VM] sendBytes command=\(gatt.SERIALPORTCHAR_UUID) type=\(gatt.WRITE) bytes=\(data.hexString)"
        )
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.SERIALPORTCHAR_UUID,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: PlotListItemViewModelV3, rhs: PlotListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier
            && lhs.title == rhs.title
    }

    static func resetRequestCache() {
        requestTracker.reset()
    }

    private func resolveThresholdTarget() -> WidgetV3BindingInfo? {
        if let thresholdBinding {
            return WidgetV3BindingInfo(
                parameterID: thresholdBinding.parameterID,
                dataCode: Self.canonicalThresholdDataCode(for: thresholdBinding.dataCode),
                deviceAddress: thresholdBinding.deviceAddress,
                dataOffset: thresholdBinding.dataOffset
            )
        }

        if let fromSet = parameterInfoSet.first(
            where: {
                $0.parameterID == Int32(ParameterCode.prosthesisModuleControlV3)
                    && Self.isThresholdDataCode(Int($0.dataCode))
            }
        ) {
            return WidgetV3BindingInfo(
                parameterID: Int(fromSet.parameterID),
                dataCode: Self.canonicalThresholdDataCode(for: Int(fromSet.dataCode)),
                deviceAddress: Int(fromSet.deviceAddress),
                dataOffset: Int(fromSet.dataOffset)
            )
        }

        if widget.deviceAddress > 0 {
            return WidgetV3BindingInfo(
                parameterID: ParameterCode.prosthesisModuleControlV3,
                dataCode: ParameterCode.thresholdSetV3,
                deviceAddress: Int(widget.deviceAddress),
                dataOffset: 0
            )
        }

        return nil
    }
}

private extension PlotListItemViewModelV3 {
    enum ParameterCode {
        static let prosthesisModuleControlV3 = 0x0F
        static let thresholdSetV3 = 0x2F
        static let thresholdGetV3 = 0x30
        static let thresholdLegacyV2 = 0x1A
    }

    static func selectThresholdBinding(
        from bindings: [WidgetV3BindingInfo],
        fallback parameterInfoSet: Set<ParameterInfoData>
    ) -> WidgetV3BindingInfo? {
        if let binding = bindings.first(
            where: {
                $0.parameterID == ParameterCode.prosthesisModuleControlV3
                    && isThresholdDataCode($0.dataCode)
            }
        ) {
            return binding
        }

        if let binding = bindings.first(where: { isThresholdDataCode($0.dataCode) }) {
            return binding
        }

        guard let fromSet = parameterInfoSet.first(
            where: {
                $0.parameterID == Int32(ParameterCode.prosthesisModuleControlV3)
                    && isThresholdDataCode(Int($0.dataCode))
            }
        ) else {
            return nil
        }

        return WidgetV3BindingInfo(
            parameterID: Int(fromSet.parameterID),
            dataCode: Int(fromSet.dataCode),
            deviceAddress: Int(fromSet.deviceAddress),
            dataOffset: Int(fromSet.dataOffset)
        )
    }

    static func isThresholdDataCode(_ dataCode: Int) -> Bool {
        switch dataCode {
        case ParameterCode.thresholdSetV3, ParameterCode.thresholdGetV3, ParameterCode.thresholdLegacyV2:
            return true
        default:
            return false
        }
    }

    static func canonicalThresholdDataCode(for dataCode: Int) -> Int {
        switch dataCode {
        case ParameterCode.thresholdSetV3, ParameterCode.thresholdGetV3, ParameterCode.thresholdLegacyV2:
            return ParameterCode.thresholdSetV3
        default:
            return dataCode
        }
    }

    static func thresholdDataCodeCandidates(for dataCode: Int) -> [Int] {
        let canonical = canonicalThresholdDataCode(for: dataCode)
        return [canonical, ParameterCode.thresholdGetV3, ParameterCode.thresholdLegacyV2]
            .reduce(into: [Int]()) { acc, code in
                if !acc.contains(code) {
                    acc.append(code)
                }
            }
    }

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
