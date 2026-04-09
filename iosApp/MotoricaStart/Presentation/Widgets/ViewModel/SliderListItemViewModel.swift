import Foundation
import shared

struct SliderListItemViewModel: Equatable, Hashable {
    private static let requestTracker = RequestTracker()
    private let identifier: String
    let title: String
    let title_2: String
    let parameterInfoSet: Set<ParameterInfoData>
    var paramCount: Int
    let widget: Widget
    let bleManager: BleManagerKmm
    private let primaryBinding: WidgetV3BindingInfo?
    private let primaryDataOffset: Int
}

extension SliderListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        let parameterInfoSet = ParameterInfoData.makeSet(
            from: widget.sliderUnified?.baseParameterWidgetStruct?.parameterInfoSet
        )
        self.parameterInfoSet = parameterInfoSet
        self.paramCount = parameterInfoSet.count
        self.widget = widget
        self.bleManager = bleManager
        let sortedBindings = WidgetV3Support.bindings(from: widget)
            .sorted { lhs, rhs in
                if lhs.dataOffset != rhs.dataOffset { return lhs.dataOffset < rhs.dataOffset }
                if lhs.parameterID != rhs.parameterID { return lhs.parameterID < rhs.parameterID }
                if lhs.dataCode != rhs.dataCode { return lhs.dataCode < rhs.dataCode }
                return lhs.deviceAddress < rhs.deviceAddress
            }
        self.primaryBinding = sortedBindings.first
        self.primaryDataOffset = sortedBindings.first?.dataOffset
            ?? parameterInfoSet.sorted(by: { $0.dataOffset < $1.dataOffset }).first?.dataOffset
            ?? 0
        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let primaryBinding {
            self.identifier = "\(widgetPosition)-\(primaryBinding.deviceAddress)-\(primaryBinding.parameterID)-\(primaryBinding.dataCode)-\(primaryDataOffset)-slider"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-slider"
        }
    }

    func contains(ref: ParameterRef) -> Bool {
        parameterInfoSet.contains {
            $0.deviceAddress == ref.addressDevice &&
            $0.parameterID == ref.parameterID
        }
    }

    func matches(snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard snapshot.codecId == "EMG_GAINS" else { return false }
        return parameterInfoSet.contains {
            $0.deviceAddress == snapshot.addressDevice &&
            $0.parameterID == snapshot.parameterID
        }
    }

    func sliderValues(from snapshot: ParameterSnapshotV3Bridge) -> [Int]? {
        guard
            let openGain = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openGain"),
            let closeGain = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeGain")
        else {
            return nil
        }

        if paramCount > 1 {
            return [openGain, closeGain]
        }

        return [gainForCurrentWidget(open: openGain, close: closeGain)]
    }

    func requestSlider() {
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }
        let data: KotlinByteArray
        if let primaryBinding {
            guard let mapped = WidgetCommandBridgeV3.shared.buildReadRequest(
                parameterID: Int32(primaryBinding.parameterID),
                dataCode: Int32(primaryBinding.dataCode)
            ) else { return }
            data = mapped
        } else {
            let targetAddress = Int32(widget.deviceAddress)
            let targetParameterID = Int32(widget.parameterID)
            data = BLECommands.shared.requestSlider(
                addressDevice: targetAddress,
                parameterID: targetParameterID
            )
        }
        
        sendBytes(data)
        print("[request] requestSlider")
    }
    func sendSliderProgress (progress: [KotlinInt]) {
        let data: KotlinByteArray
        if let primaryBinding {
            let valueIndex = primaryBinding.dataOffset == 1 ? 1 : 0
            let fallbackValue = Int(progress.first?.intValue ?? 0)
            let sliderValue = progress.indices.contains(valueIndex)
                ? Int(progress[valueIndex].intValue)
                : fallbackValue

            guard let encoded = WidgetCommandBridgeV3.shared.buildSetInt(
                parameterID: Int32(primaryBinding.parameterID),
                dataCode: Int32(primaryBinding.dataCode),
                deviceAddress: Int32(primaryBinding.deviceAddress),
                dataOffset: Int32(primaryBinding.dataOffset),
                value: Int32(sliderValue)
            ) else { return }
            data = encoded
        } else {
            let targetAddress = Int32(widget.deviceAddress)
            let targetParameterID = Int32(widget.parameterID)
            data = BLECommands.shared.sendSliderCommand(
                addressDevice: targetAddress,
                parameterID: targetParameterID,
                progress: progress
            )
        }
        
        sendBytes(data)
    }
    private func sendBytes (_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        print("sendBytesKmm  iOS  отправляем данные: \(data.hexString)  из SliderListItemViewModel")
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
    static func == (lhs: SliderListItemViewModel, rhs: SliderListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
    
    static func resetRequestCache() {
        requestTracker.reset()
    }

    func cachedSliderValues() -> [Float]? {
        if let primaryBinding,
           let snapshot = WidgetStateBridgeV3.shared.getCurrent(
                addressDevice: Int32(primaryBinding.deviceAddress),
                parameterID: Int32(primaryBinding.parameterID),
                dataCode: Int32(primaryBinding.dataCode)
           ),
           let values = sliderValues(from: snapshot) {
            return values.map(Float.init)
        }

        let parameter = ParameterProvider.Companion()
            .getParameter(
                deviceAddress: Int32(widget.deviceAddress),
                parameterID: Int32(widget.parameterID)
            )

        guard parameter.firstReceiveDataFlag == false,
              let values = sliderValues(from: parameter) else { return nil }

        return values.map(Float.init)
    }

    func sliderValues(from parameter: BaseParameterInfoStruct) -> [Int]? {
        let entries = ParameterTypeEnum.values()
        let ordinal = Int(parameter.type)
        let count = Int(entries.size)

        guard ordinal >= 0,
              ordinal < count,
              let entry = entries.get(index: Int32(ordinal)) else { return nil }

        let sizeOf = Int(entry.sizeOf)
        guard sizeOf > 0 else { return nil }

        let chunkLength = sizeOf * 2
        let hex = parameter.data
        guard hex.count >= chunkLength else { return nil }

        var values: [Int] = []
        var currentIndex = hex.startIndex
        let valuesCount = max(paramCount, primaryDataOffset + 1, 1)

        for _ in 0..<valuesCount {
            guard hex.distance(from: currentIndex, to: hex.endIndex) >= chunkLength else { break }
            let nextIndex = hex.index(currentIndex, offsetBy: chunkLength)
            let slice = String(hex[currentIndex..<nextIndex])
            let value = Int(slice, radix: 16) ?? 0
            values.append(value)
            currentIndex = nextIndex
        }

        if paramCount > 1 {
            return values
        }

        let selected = primaryDataOffset < values.count
            ? values[primaryDataOffset]
            : values.first
        return selected.map { [$0] }
    }
}

private extension SliderListItemViewModel {
    func gainForCurrentWidget(open: Int, close: Int) -> Int {
        primaryDataOffset == 1 ? close : open
    }
}

private extension SliderListItemViewModel {
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

extension KotlinByteArray {
    /// Тот же результат, что и Kotlin bytesToHexString(ByteArray)
    var hexString: String {
        var s = String()
        s.reserveCapacity(Int(self.size) * 2)
        for i in 0..<Int(self.size) {
            // ВАЖНО: UInt8(bitPattern:) убирает знак (аналог 0xFF and ...)
            let b = UInt8(bitPattern: self.get(index: Int32(i)))
            s.append(String(format: "%02x", b))
        }
        return s
    }
}
