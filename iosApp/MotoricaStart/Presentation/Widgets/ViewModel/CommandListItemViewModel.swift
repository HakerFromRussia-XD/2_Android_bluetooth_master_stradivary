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
    
//    if (extractMetadata()?.clickCommand == 0) {}
    func didPressDown() {
        // тут нужно доставать из commandEStruct или commandSStruct pressedCommand
        let commandE = widget.commandEStruct
        let commandS = widget.commandSStruct
        let command = ((commandE?.pressedCommand) ?? (commandS?.pressedCommand)) ?? 0
        
        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: command
        )

        sendBytes(data)
    }
    func didRelease() {
        // тут нужно доставать из commandEStruct или commandSStruct pressedCommand
        let commandE = widget.commandEStruct
        let commandS = widget.commandSStruct
        let command = ((
            (commandE?.clickCommand == 0) ? commandE?.releasedCommand : commandE?.clickCommand
        ) ?? (
            (commandS?.clickCommand == 0) ? commandS?.releasedCommand : commandS?.clickCommand
        )) ?? 0
        
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
