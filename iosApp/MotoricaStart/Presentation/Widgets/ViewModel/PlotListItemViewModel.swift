// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation
import DGCharts
import UIKit
import shared

struct PlotListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    private let identifier: String
    let title: String
    let widget: Widget
    let bleManager: BleManagerKmm
}

extension PlotListItemViewModel {
    init(widget: Widget,
         bleManager: BleManagerKmm
    ) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.widget = widget
        self.bleManager = bleManager
    }
    func requestThresholds() {
        let data = BLECommands.shared.requestThresholds(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID)
        )
        
        sendBytes(data)
    }
    func sendThresholds(openThreshold: Int, closeThreshold: Int) {
        print("sendThresholds openThreshold=\(openThreshold)   closeThreshold=\(closeThreshold)")
        let data = BLECommands.shared.sendThresholdsCommand(
            addressDevice: Int32(widget.deviceAddress),
            parameterID: Int32(widget.parameterID),
            thresholds: [
                KotlinInt(integerLiteral: openThreshold),
                0,
                KotlinInt(integerLiteral: closeThreshold),
                0
            ]
        )

        sendBytes(data)
    }
    private func sendBytes (_ data: KotlinByteArray) {
        let gatt = SampleGattAttributes()
        print("sendBytesKmm  iOS  отправляем данные: \(data.hexString)  из PlotListItemViewModel")
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
    static func == (lhs: PlotListItemViewModel, rhs: PlotListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
}

