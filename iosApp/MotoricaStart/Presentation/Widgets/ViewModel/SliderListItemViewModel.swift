import Foundation
import shared

struct SliderListItemViewModel: Equatable, Hashable {
    private static let requestTracker = RequestTracker()
    private let identifier: String
    let title: String
    let title_2: String
    let parameterInfoSet: Set<ParameterInfoData>
    let showSecondSlider: Bool
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension SliderListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false, bleManager: BleManagerKmm) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.parameterInfoSet = ParameterInfoData.makeSet(
            from: widget.sliderUnified?.baseParameterWidgetStruct?.parameterInfoSet
        )
        self.showSecondSlider = showSecondSlider || parameterInfoSet.count > 1
        self.widget = widget
        self.bleManager = bleManager
    }

    func contains(ref: ParameterRef) -> Bool {
        if parameterInfoSet.isEmpty {
            return ref.addressDevice == widget.deviceAddress && ref.parameterID == widget.parameterID
        }
        return parameterInfoSet.contains {
            $0.deviceAddress == ref.addressDevice && $0.parameterID == ref.parameterID
        }
    }

    func requestSlider() {
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }
        let data = BLECommands.shared.requestSlider(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )
        sendBytes(data)
    }

    func sendSliderProgress(progress: [KotlinInt]) {
        let data = BLECommands.shared.sendSliderCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            progress: progress
        )
        sendBytes(data)
    }

    func cachedSliderValues() -> [Float]? {
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
        let valuesCount = showSecondSlider ? 2 : 1

        for _ in 0..<valuesCount {
            guard hex.distance(from: currentIndex, to: hex.endIndex) >= chunkLength else { break }
            let nextIndex = hex.index(currentIndex, offsetBy: chunkLength)
            let slice = String(hex[currentIndex..<nextIndex])
            let value = Int(slice, radix: 16) ?? 0
            values.append(value)
            currentIndex = nextIndex
        }

        if showSecondSlider { return values }
        return values.first.map { [$0] }
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

    static func == (lhs: SliderListItemViewModel, rhs: SliderListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }

    static func resetRequestCache() {
        requestTracker.reset()
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

struct SliderListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
    private let emgGainsKey: String
    let title: String
    let title_2: String
    let widget: Widget
    let bleManager: BleManagerKmm
    let binding: WidgetV3BindingInfo?
    let minProgress: Int
    let maxProgress: Int
}

extension SliderListItemViewModelV3 {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.binding = WidgetV3Support.primaryBinding(from: widget)

        let rawMin = Int(widget.sliderUnified?.minProgress ?? 0)
        let rawMax = Int(widget.sliderUnified?.maxProgress ?? 100)
        if rawMax > rawMin {
            self.minProgress = rawMin
            self.maxProgress = rawMax
        } else {
            // Некоторые V3-виджеты приходят с диапазоном 0...0, тогда блокируется отправка (clamp -> 0).
            // Для таких случаев используем безопасный рабочий диапазон.
            self.minProgress = 0
            self.maxProgress = 100
            print(
                "[V3-SLIDER][VM] normalizeRange fallback applied rawMin=\(rawMin) rawMax=\(rawMax) -> 0...100"
            )
        }

        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let binding {
            self.identifier = "\(widgetPosition)-\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-\(binding.dataOffset)-slider-v3"
            self.emgGainsKey = "\(binding.deviceAddress)-\(binding.parameterID)-\(binding.dataCode)-emg-gains"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-slider-v3"
            self.emgGainsKey = "\(widget.deviceAddress)-\(widget.parameterID)-emg-gains"
        }
    }

    func requestCurrent() {
        guard let binding else {
            print("[V3-SLIDER][VM] requestCurrent skipped: binding is nil")
            return
        }
        guard let data = WidgetCommandBridgeV3.shared.buildReadRequest(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        ) else {
            print("[V3-SLIDER][VM] requestCurrent failed: buildReadRequest returned nil, binding=\(binding)")
            return
        }
        print("[V3-SLIDER][VM] requestCurrent binding=\(binding) bytes=\(data.hexString)")
        sendBytes(data)
    }

    func sendSliderValue(_ value: Int) {
        guard let binding else {
            print("[V3-SLIDER][VM] sendSliderValue skipped: binding is nil, value=\(value)")
            return
        }
        let clampedValue = min(max(value, minProgress), maxProgress)
        print("[V3-SLIDER][VM] sendSliderValue raw=\(value) clamped=\(clampedValue) binding=\(binding)")
        let currentEmgGains = resolveCurrentEmgGains()
        let isEmgGainBinding =
            binding.parameterID == ParameterCode.emgMasterControlV3 &&
            binding.dataCode == ParameterCode.emgGainSetV3
        if isEmgGainBinding && currentEmgGains == nil {
            print("[V3-SLIDER][VM] sendSliderValue skipped: EMG_GAINS current pair is unknown, requesting current first")
            requestCurrent()
            return
        }
        if currentEmgGains != nil || isEmgGainBinding {
            let fallbackOpen = currentEmgGains?.open ?? clampedValue
            let fallbackClose = currentEmgGains?.close ?? clampedValue
            let openGain = binding.dataOffset == 0 ? clampedValue : fallbackOpen
            let closeGain = binding.dataOffset == 1 ? clampedValue : fallbackClose
            EmgGainsCache.store(key: emgGainsKey, open: openGain, close: closeGain)
            let data = WidgetCommandBridgeV3.shared.buildSendEmgGains(
                openGain: Int32(openGain),
                closeGain: Int32(closeGain)
            )
            print(
                "[V3-SLIDER][VM] sendSliderValue EMG_GAINS AndroidParity open=\(openGain) close=\(closeGain) bytes=\(data.hexString)"
            )
            sendBytes(data)
            return
        }
        if let snapshot = currentSnapshot() {
            print("[V3-SLIDER][VM] sendSliderValue codec=\(snapshot.codecId) -> buildSetInt path")
        } else {
            print("[V3-SLIDER][VM] sendSliderValue snapshot is nil -> buildSetInt path")
        }
        guard let data = WidgetCommandBridgeV3.shared.buildSetInt(
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode),
            deviceAddress: Int32(binding.deviceAddress),
            dataOffset: Int32(binding.dataOffset),
            value: Int32(clampedValue)
        ) else {
            print("[V3-SLIDER][VM] sendSliderValue failed: buildSetInt returned nil, binding=\(binding)")
            return
        }
        print("[V3-SLIDER][VM] sendSliderValue encoded bytes=\(data.hexString)")
        sendBytes(data)
    }

    func matches(snapshot: ParameterSnapshotV3Bridge) -> Bool {
        guard let binding else { return false }
        return snapshot.addressDevice == Int32(binding.deviceAddress)
            && snapshot.parameterID == Int32(binding.parameterID)
            && snapshot.dataCode == Int32(binding.dataCode)
    }

    func sliderValue(from snapshot: ParameterSnapshotV3Bridge) -> Int? {
        if snapshot.codecId == "EMG_GAINS" {
            guard
                let openGain = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openGain"),
                let closeGain = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeGain")
            else {
                return nil
            }
            EmgGainsCache.store(key: emgGainsKey, open: openGain, close: closeGain)
            return (binding?.dataOffset == 1) ? closeGain : openGain
        }

        if snapshot.codecId == "SLIDER" {
            return V3SnapshotParser.intField(from: snapshot.serializedValue, field: "sliderValue")
        }

        return nil
    }

    func currentSliderValue() -> Int? {
        guard let snapshot = currentSnapshot() else { return nil }
        return sliderValue(from: snapshot)
    }

    private func currentSnapshot() -> ParameterSnapshotV3Bridge? {
        guard let binding else { return nil }
        return WidgetStateBridgeV3.shared.getCurrent(
            addressDevice: Int32(binding.deviceAddress),
            parameterID: Int32(binding.parameterID),
            dataCode: Int32(binding.dataCode)
        )
    }

    private func resolveCurrentEmgGains() -> (open: Int, close: Int)? {
        // Важный порядок: сначала локальный кэш, чтобы не перетира́ть только что отправленную
        // пару старым snapshot'ом, который мог прийти с задержкой.
        if let cached = EmgGainsCache.read(key: emgGainsKey) {
            return cached
        }

        if let snapshot = currentSnapshot(),
           snapshot.codecId == "EMG_GAINS",
           let openGain = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openGain"),
           let closeGain = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeGain") {
            EmgGainsCache.store(key: emgGainsKey, open: openGain, close: closeGain)
            return (openGain, closeGain)
        }
        return nil
    }

    private func sendBytes(_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        print("[V3-SLIDER][VM] sendBytes command=\(gatt.SERIALPORTCHAR_UUID) type=\(gatt.WRITE) bytes=\(data.hexString)")
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

    static func == (lhs: SliderListItemViewModelV3, rhs: SliderListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
    }
}

private extension SliderListItemViewModelV3 {
    enum ParameterCode {
        static let emgMasterControlV3 = 0x12
        static let emgGainSetV3 = 0x01
    }

    final class EmgGainsCache {
        private static var values: [String: (open: Int, close: Int)] = [:]
        private static let lock = NSLock()

        static func store(key: String, open: Int, close: Int) {
            lock.lock()
            values[key] = (open, close)
            lock.unlock()
        }

        static func read(key: String) -> (open: Int, close: Int)? {
            lock.lock()
            defer { lock.unlock() }
            return values[key]
        }
    }
}

extension KotlinByteArray {
    var hexString: String {
        var s = String()
        s.reserveCapacity(Int(self.size) * 2)
        for i in 0..<Int(self.size) {
            let b = UInt8(bitPattern: self.get(index: Int32(i)))
            s.append(String(format: "%02x", b))
        }
        return s
    }
}
