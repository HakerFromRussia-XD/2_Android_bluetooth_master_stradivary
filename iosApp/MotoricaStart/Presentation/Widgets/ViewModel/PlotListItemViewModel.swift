// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation
import DGCharts
import UIKit
import shared

struct PlotListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    private let uuid = UUID()
    let title: String
    let deviceAddress: Int
    let parameterID: Int
    let widget: AnyCodable?
    let bleManager: BleManagerKmm
}

extension PlotListItemViewModel {
    init(widget: Widget,
         showSecondSlider: Bool = false,
         bleManager: BleManagerKmm
    ) {
        self.title = widget.title ?? ""
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
        self.widget = widget.widget
        self.bleManager = bleManager
    }
    
    func requestThresholds() {
        let data = BLECommands.shared.requestThresholds(
            addressDevice: Int32(deviceAddress),
            parameterID: Int32(parameterID)
        )
        
        sendBytes(data)
    }
    func sendThresholds(openThreshold: Int, closeThreshold: Int) {
        print("sendThresholds openThreshold=\(openThreshold)   closeThreshold=\(closeThreshold)")
        let data = BLECommands.shared.sendThresholdsCommand(
            addressDevice: Int32(deviceAddress),
            parameterID: Int32(parameterID),
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
        bleManager.sendBytesKmm(
            data: data,
            command: gatt.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: gatt.WRITE,
            onChunkSent: {})
    }
    
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(uuid)
    }
    static func == (lhs: PlotListItemViewModel, rhs: PlotListItemViewModel) -> Bool {
        lhs.uuid == rhs.uuid
    }
}
