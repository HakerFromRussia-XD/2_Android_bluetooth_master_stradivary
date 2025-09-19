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
    let chartData: LineChartData
    let widget: AnyCodable?
    let bleManager: BleManagerKmm
}

extension PlotListItemViewModel {
    init(widget: Widget,
         chartData: LineChartData = LineChartData(),
         showSecondSlider: Bool = false,
         bleManager: BleManagerKmm
    ) {
        self.title = widget.title ?? ""
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
        self.chartData = chartData
        self.widget = widget.widget
        self.bleManager = bleManager
        
        var thresholds: [Int]? = nil   // [open, close, t3, t4, t5, t6]
        var thresholdsLoaded: Bool = false
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(uuid)
    }
    
    static func == (lhs: PlotListItemViewModel, rhs: PlotListItemViewModel) -> Bool {
        lhs.uuid == rhs.uuid
    }
}
