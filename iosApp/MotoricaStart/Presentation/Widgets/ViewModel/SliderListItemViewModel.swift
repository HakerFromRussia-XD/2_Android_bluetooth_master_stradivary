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

enum SliderListItemActionV3 {
    case currentValueRequested
    case valueChangeCommitted(Int)
}

struct SliderListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
    private let getSliderSettingsUseCase: GetSliderSettingsUseCaseV3
    private let observeSliderResponsesUseCase: ObserveSliderResponsesUseCaseV3
    private let requestSliderValueUseCase: RequestSliderValueUseCaseV3
    private let setSliderValueUseCase: SetSliderValueUseCaseV3
    let title: String
    let title_2: String
    let widget: Widget
    let binding: WidgetV3BindingInfo?
    let minProgress: Int
    let maxProgress: Int
}

extension SliderListItemViewModelV3 {
    init(
        widget: Widget,
        getSliderSettingsUseCase: GetSliderSettingsUseCaseV3,
        observeSliderResponsesUseCase: ObserveSliderResponsesUseCaseV3,
        requestSliderValueUseCase: RequestSliderValueUseCaseV3,
        setSliderValueUseCase: SetSliderValueUseCaseV3
    ) {
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.widget = widget
        self.binding = WidgetV3Support.primaryBinding(from: widget)
        self.getSliderSettingsUseCase = getSliderSettingsUseCase
        self.observeSliderResponsesUseCase = observeSliderResponsesUseCase
        self.requestSliderValueUseCase = requestSliderValueUseCase
        self.setSliderValueUseCase = setSliderValueUseCase

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
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-slider-v3"
        }
    }

    private func requestCurrent() {
        guard let parameterKey = sliderParameterKey else { return }
        requestSliderValueUseCase.invoke(parameterKey: parameterKey)
    }

    func onAction(_ action: SliderListItemActionV3) {
        switch action {
        case .currentValueRequested:
            requestCurrent()
        case .valueChangeCommitted(let value):
            sendSliderValue(value)
        }
    }

    private func sendSliderValue(_ value: Int) {
        guard let parameterKey = sliderParameterKey else { return }
        let clampedValue = min(max(value, minProgress), maxProgress)
        setSliderValueUseCase.invoke(parameterKey: parameterKey, value: Int32(clampedValue))
    }

    func currentSliderValue() -> Int? {
        guard let parameterKey = sliderParameterKey else { return nil }
        // Existing iOS snapshots omit the default zero, so it does not replace a UI draft.
        guard let value = (getSliderSettingsUseCase.invoke(parameterKeys: [parameterKey])
            .values[parameterKey] as? NSNumber)?.intValue, value != 0 else { return nil }
        return value
    }

    func observeSliderValue(onChanged: @escaping (Int) -> Void) -> Kotlinx_coroutines_coreJob? {
        guard let parameterKey = sliderParameterKey else { return nil }
        // Keep response events, including repeated values, so a device response can reset a UI draft.
        return observeSliderResponsesUseCase.invoke(parameterKey: parameterKey) {
            if let value = currentSliderValue() { onChanged(value) }
        }
    }

    private var sliderParameterKey: String? {
        let keys = V3ParameterKeys.shared
        return WidgetV3Support.parameterKey(for: binding, among: [
            keys.P_KEY_EMG_MAX_GAIN_VALUE,
            keys.P_KEY_EMG_GAIN_OPEN_VALUE,
            keys.P_KEY_EMG_GAIN_CLOSE_VALUE,
            keys.P_KEY_SPEED_SETTINGS,
            keys.P_KEY_FORCE_SETTINGS,
            keys.P_KEY_GLOBAL_THUMB_CLOSED_POSITION,
            keys.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
        ])
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: SliderListItemViewModelV3, rhs: SliderListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier && lhs.title == rhs.title
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
