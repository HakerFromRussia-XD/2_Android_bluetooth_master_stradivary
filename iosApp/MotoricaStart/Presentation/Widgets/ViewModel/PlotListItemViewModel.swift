// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation
import DGCharts
import UIKit

struct PlotListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    let id: Widget.Identifier
    let title: String
    let deviceAddress: Int
    let parameterID: Int
    let chartData: LineChartData
}

extension PlotListItemViewModel {
    init(id: Widget.Identifier, widget: Widget, chartData: LineChartData = LineChartData(), showSecondSlider: Bool = false) {
        self.id = id
        self.title = widget.title ?? ""
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
        self.chartData = chartData
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
    
    static func == (lhs: PlotListItemViewModel, rhs: PlotListItemViewModel) -> Bool {
        lhs.id == rhs.id
    }
}
