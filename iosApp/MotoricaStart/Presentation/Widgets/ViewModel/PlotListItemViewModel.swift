// **Note**: This item view model is to display data and does not contain any domain model to prevent views accessing it

import Foundation
import DGCharts

struct PlotListItemViewModel: Equatable, Hashable { // Assistant: добавил Hashable
    let title: String
    let deviceAddress: Int
    let parameterID: Int
    let chartData: LineChartData
}

extension PlotListItemViewModel {
    init(widget: Widget, chartData: LineChartData = LineChartData(), showSecondSlider: Bool = false) {
        self.title = widget.title ?? ""
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
        self.chartData = chartData
    }
}
