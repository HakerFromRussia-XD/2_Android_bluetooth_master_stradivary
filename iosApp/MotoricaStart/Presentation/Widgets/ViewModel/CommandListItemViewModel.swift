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
        print("CommandListItemViewModel didPressDown pressedCommand = \(extractMetadata()?.pressedCommand)")
        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: Int32(extractMetadata()?.pressedCommand ?? 0)
        )

        sendBytes(data)
    }
    func didRelease() {
        print("CommandListItemViewModel didRelease releasedCommand = \(extractMetadata()?.releasedCommand)")
        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: Int32(extractMetadata()?.releasedCommand ?? 0)
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
    
    func extractMetadata() -> (
            addressDevice: Int,
            parameterID: Int,
            clickCommand: Int,
            pressedCommand: Int,
            releasedCommand: Int
        )? {
            guard let value = widget.widget?.value else { return nil }

            if let commandE = value as? CommandParameterWidgetEStruct {
                let base = commandE.baseParameterWidgetEStruct.baseParameterWidgetStruct

                guard let first = base.parameterInfoSet.first
                        as? ParameterInfo<KotlinInt, KotlinInt, KotlinInt, KotlinInt> else {
                    return nil
                }

                let addressDevice = Int(first.deviceAddress?.intValue ?? 0)
                let parameterID   = Int(first.parameterID?.intValue ?? 0)

                let clickCommand    = Int(commandE.clickCommand)
                let pressedCommand  = Int(commandE.pressedCommand)
                let releasedCommand = Int(commandE.releasedCommand)

                return (addressDevice, parameterID, clickCommand, pressedCommand, releasedCommand)
            }
            else if let commandS = value as? CommandParameterWidgetSStruct {
                let base = commandS.baseParameterWidgetSStruct.baseParameterWidgetStruct

                guard let first = base.parameterInfoSet.first
                        as? ParameterInfo<KotlinInt, KotlinInt, KotlinInt, KotlinInt> else {
                    return nil
                }

                let addressDevice = Int(first.deviceAddress?.intValue ?? 0)
                let parameterID   = Int(first.parameterID?.intValue ?? 0)

                let clickCommand    = Int(commandS.clickCommand)
                let pressedCommand  = Int(commandS.pressedCommand)
                let releasedCommand = Int(commandS.releasedCommand)

                return (addressDevice, parameterID, clickCommand, pressedCommand, releasedCommand)
            }

            return nil
        }
}
