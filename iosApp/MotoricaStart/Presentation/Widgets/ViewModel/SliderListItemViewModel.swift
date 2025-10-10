import Foundation
import shared

struct SliderListItemViewModel: Equatable, Hashable {
    private let identifier: String
    let title: String
    let title_2: String
    let parameterInfoSet: Set<ParameterInfoData>
    var paramCount: Int
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension SliderListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false, bleManager: BleManagerKmm) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        let parameterInfoSet = ParameterInfoData.makeSet(
            from: widget.sliderUnified?.baseParameterWidgetStruct?.parameterInfoSet
        )
        self.parameterInfoSet = parameterInfoSet
        self.paramCount = parameterInfoSet.count
        self.widget = widget
        self.bleManager = bleManager
    }
    func requestSlider() {
        let data = BLECommands.shared.requestSlider(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )
        
        sendBytes(data)
    }
    func sendSliderProgress (progress: [KotlinInt]) {
        let data = BLECommands.shared.sendSliderCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            progress: progress
        )
        
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
    
    func cachedSliderValues() -> [Float]? {
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: Int32(widget.deviceAddress), parameterID: Int32(widget.parameterID))

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
        let valuesCount = max(paramCount, 1)

        for _ in 0..<valuesCount {
            guard hex.distance(from: currentIndex, to: hex.endIndex) >= chunkLength else { break }
            let nextIndex = hex.index(currentIndex, offsetBy: chunkLength)
            let slice = String(hex[currentIndex..<nextIndex])
            let value = Int(slice, radix: 16) ?? 0
            values.append(value)
            currentIndex = nextIndex
        }

        return values
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
