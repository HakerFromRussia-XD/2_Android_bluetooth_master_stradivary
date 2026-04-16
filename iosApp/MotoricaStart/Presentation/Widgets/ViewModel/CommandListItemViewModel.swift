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
        let command = ((commandUnified?.clickCommand == 0) ? commandUnified?.releasedCommand : commandUnified?.clickCommand) ?? 0

        let data = BLECommands.shared.sendOneButtonCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            command: command
        )

        sendBytes(data)
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
    static func == (lhs: CommandListItemViewModel, rhs: CommandListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
}

struct CommandListItemViewModelV3: Equatable, Hashable {
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
    private let orderedButtonBindings: [WidgetV3BindingInfo]
    private let parsedButtonTitles: [String]
}

extension CommandListItemViewModelV3 {
    init(widget: Widget, bleManager: BleManagerKmm) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager

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
        self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-\(bindingSignature)-command-v3"

        self.parsedButtonTitles = Self.parseButtonTitles(from: self.title)
    }

    var visibleButtonTitles: [String] {
        orderedButtonBindings
            .enumerated()
            .map { index, _ in
                let title = parsedButtonTitles[safe: index]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                return title.isEmpty ? "Button \(index + 1)" : title
            }
    }

    func didPressDown(at index: Int) {
        sendButtonCommand(at: index, isPressDown: true)
    }

    func didRelease(at index: Int) {
        sendButtonCommand(at: index, isPressDown: false)
    }

    private func sendButtonCommand(at index: Int, isPressDown: Bool) {
        guard orderedButtonBindings.indices.contains(index) else { return }
        let binding = orderedButtonBindings[index]
        let subcommand = isPressDown ? binding.dataCode : 0

        let data = WidgetCommandBridgeV3.shared.buildSendSubcommand(
            subcommand: Int32(subcommand),
            parameter: 0
        )

        print("[V3-BUTTON][VM] action=\(isPressDown ? "down" : "up") index=\(index) subcommand=\(subcommand) dataCode=\(binding.dataCode)")
        sendBytes(data)
    }

    private func sendBytes(_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        print("[V3-BUTTON][VM] sendBytes command=\(gatt.SERIALPORTCHAR_UUID) type=\(gatt.WRITE) bytes=\(data.hex)")
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.SERIALPORTCHAR_UUID,
            typeCommand: gatt.WRITE,
            onChunkSent: {}
        )
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

    static func == (lhs: CommandListItemViewModelV3, rhs: CommandListItemViewModelV3) -> Bool {
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
