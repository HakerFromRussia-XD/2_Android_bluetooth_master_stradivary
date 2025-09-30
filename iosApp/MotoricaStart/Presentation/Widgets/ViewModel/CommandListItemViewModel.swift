import Foundation
import shared

struct CommandListItemViewModel: Equatable, Hashable {
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension CommandListItemViewModel {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
    }
    
    func didPressDown() {
        print("didPressDown")
        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: widget.widget?.pressedCommand
        )

        sendBytes(data)
    }
    func didRelease() {
        print("didRelease")
        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: progress
        )

        sendBytes(data)
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
    static func == (lhs: CommandListItemViewModel, rhs: CommandListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
}
