import Foundation
import shared

struct CommandListItemViewModel: Equatable, Hashable {
    private static let textInputWidgetCode = 0x1A
    private static let buttonsV3WidgetCode = 0x12

    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    private let widgetCode: Int
    private let orderedButtonBindings: [WidgetV3BindingInfo]
    private let parsedButtonTitles: [String]
}

extension CommandListItemViewModel {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
        self.widgetCode = WidgetV3Support.widgetCode(from: widget)

        self.orderedButtonBindings = WidgetV3Support.bindings(from: widget)
            .sorted { $0.dataOffset < $1.dataOffset }
            .prefix(3)
            .map { $0 }

        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        let bindingSignature = orderedButtonBindings
            .map { "\($0.deviceAddress)-\($0.parameterID)-\($0.dataCode)-\($0.dataOffset)" }
            .joined(separator: "_")
        self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-\(bindingSignature)-command"

        self.parsedButtonTitles = Self.parseButtonTitles(from: self.title)
    }

    var isTextInputWidget: Bool {
        widgetCode == Self.textInputWidgetCode
    }

    var isV3ButtonsWidget: Bool {
        widgetCode == Self.buttonsV3WidgetCode
    }

    var visibleButtonTitles: [String] {
        if isV3ButtonsWidget {
            return orderedButtonBindings
                .enumerated()
                .map { index, _ in
                    let title = parsedButtonTitles[safe: index]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                    return title.isEmpty ? "Button \(index + 1)" : title
                }
        }

        return [title]
    }

    func didPressDown(at index: Int = 0) {
        if isV3ButtonsWidget {
            sendV3ButtonCommand(at: index, isPressDown: true)
            return
        }

        let command = widget.commandUnified?.pressedCommand ?? 0
        print("[BLE_COMMAND] \(command) send")

        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: command
        )

        sendBytes(data)
    }

    func didRelease(at index: Int = 0) {
        if isV3ButtonsWidget {
            sendV3ButtonCommand(at: index, isPressDown: false)
            return
        }

        let commandUnified = widget.commandUnified
        let command = ((commandUnified?.clickCommand == 0) ? commandUnified?.releasedCommand : commandUnified?.clickCommand) ?? 0

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

    private func sendV3ButtonCommand(at index: Int, isPressDown: Bool) {
        guard orderedButtonBindings.indices.contains(index) else { return }
        let binding = orderedButtonBindings[index]
        let subcommand = isPressDown ? binding.dataCode : 0

        let data = WidgetCommandBridgeV3.shared.buildSendSubcommand(
            subcommand: Int32(subcommand),
            parameter: 0
        )

        sendBytes(data)
    }

    private static func parseButtonTitles(from rawTitle: String) -> [String] {
        rawTitle
            .split(separator: "%", omittingEmptySubsequences: false)
            .map { String($0) }
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

private extension Array {
    subscript(safe index: Int) -> Element? {
        guard indices.contains(index) else { return nil }
        return self[index]
    }
}
