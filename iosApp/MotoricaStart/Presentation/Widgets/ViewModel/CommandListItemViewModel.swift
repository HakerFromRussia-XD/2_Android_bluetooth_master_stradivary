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
        let command = widget.commandUnified?.pressedCommand ?? 0
        print("[BLE_COMMAND] \(command) send")
        
        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: command
        )

        sendBytes(data)
    }
    func didRelease() {
        let commandUnified = widget.commandUnified
        var command = ((commandUnified?.clickCommand == 0) ? commandUnified?.releasedCommand : commandUnified?.clickCommand) ?? 0
        
        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: command
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
