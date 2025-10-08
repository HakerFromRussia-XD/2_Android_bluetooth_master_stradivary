import Foundation
import shared

struct SliderListItemViewModel: Equatable, Hashable {
    private let identifier: String
    let title: String
    let title_2: String
    var paramCount: Int
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension SliderListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false, bleManager: BleManagerKmm) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.title_2 = widget.title_2 ?? ""
        self.paramCount = widget.sliderUnified?.baseParameterWidgetStruct?.parameterInfoSet.count ?? 0
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
        
//        sendBytes(data)
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
