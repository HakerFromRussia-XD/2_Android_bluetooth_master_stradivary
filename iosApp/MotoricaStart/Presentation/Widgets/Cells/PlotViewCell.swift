import UIKit
import DGCharts
import Combine

final class PlotViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: PlotViewCell.self)
    static let height = CGFloat(130)
    private var viewModel: PlotListItemViewModel!
    @IBOutlet weak var lineChartView: LineChartView!
    
    // charts
    let values = (0..<1).map { (i) -> ChartDataEntry in
        let val = Double(arc4random_uniform(UInt32(1))+3)
        return ChartDataEntry(x: Double(i), y: val)
    }
    var firstInit: Bool = true
    var count: Int = 0
    
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
//        if lineChartView?.data == nil {
//            lineChartView?.data = LineChartData()
//        }
        initChart()
    }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: PlotListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        
        lineChartView.data = viewModel.chartData
    }
    
    
    // MARK: - работа с графиком
    func addEntry (sens1: Int, sens2: Int) {
        let data: ChartData = (self.lineChartView?.data!)!

        var set1 : LineChartDataSet
        var set2 : LineChartDataSet
        
        if (firstInit) {
            set1 = createSet1(values: values)
            set2 = createSet2(values: values)
            data.append(set1)
            data.append(set2)
            firstInit = false
        } else {
            set1 = (data[1] as? LineChartDataSet)!
            set2 = (data[2] as? LineChartDataSet)!
        }
        if (set1.count >= 300) {
            zaglushka(bool1: (set1.removeFirst()))
            zaglushka(bool1: (set2.removeFirst()))
        }
        
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens1)), toDataSet: 1)
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens2)), toDataSet: 2)
        
        data.notifyDataChanged()
        self.lineChartView?.notifyDataSetChanged()
        self.lineChartView?.setVisibleXRangeMaximum(300)
        self.lineChartView?.moveViewToX(Double(set2.count - 300))
        self.count += 1
    }
    func initChart() {
        self.lineChartView?.noDataText = "Нет данных"
        self.lineChartView?.data = LineChartData()
        var data = self.lineChartView?.data
        let set1 = LineChartDataSet(entries: [], label: "")
        data = LineChartData(dataSet: set1)
        var data2 = self.lineChartView?.data
        let set2 = LineChartDataSet(entries: [], label: "")
        data2 = LineChartData(dataSet: set2)
        self.lineChartView?.data = data
        self.lineChartView?.data = data2
        
        self.lineChartView?.isExclusiveTouch = false
        self.lineChartView?.isMultipleTouchEnabled = false
        self.lineChartView?.dragEnabled = false
        self.lineChartView?.dragDecelerationEnabled = false
        self.lineChartView?.setScaleEnabled(false)
        self.lineChartView?.drawGridBackgroundEnabled = false
        self.lineChartView?.pinchZoomEnabled = false
        self.lineChartView?.backgroundColor = UIColor(named: "transparent") ?? .clear

        self.lineChartView?.legend.enabled = false
        self.lineChartView?.animate(yAxisDuration: 0.7)
        
        let x: XAxis = self.lineChartView!.xAxis
        x.labelTextColor = UIColor(named: "transparent") ?? .clear
        x.drawGridLinesEnabled = false
        x.axisMaximum = 4000000
        x.avoidFirstLastClippingEnabled = true
        
        let y: YAxis = self.lineChartView!.leftAxis
        y.axisMaximum = 255
        y.axisMinimum = 0
        y.labelTextColor = UIColor(named: "transparent") ?? .clear
        y.drawGridLinesEnabled = true
        y.drawAxisLineEnabled = false
        y.gridColor = UIColor(named: "transparent") ?? .clear
        self.lineChartView?.rightAxis.axisLineColor = UIColor(named: "transparent") ?? .clear
        self.lineChartView?.rightAxis.labelTextColor = UIColor(named: "transparent") ?? .clear
    }
    func createSet1(values: [ChartDataEntry]) -> LineChartDataSet {
        let set1 = LineChartDataSet(entries: [], label: "")
        set1.axisDependency = YAxis.AxisDependency.left
        set1.lineWidth = 2
        set1.setColor(UIColor(named: "lineColor_open")!)
        set1.mode = LineChartDataSet.Mode.linear
        set1.drawCirclesEnabled = false
        set1.drawValuesEnabled = false
        
        return set1
    }
    func createSet2(values: [ChartDataEntry]) -> LineChartDataSet {
        let set2 = LineChartDataSet(entries: [], label: "")
        set2.axisDependency = YAxis.AxisDependency.left
        set2.lineWidth = 2
        set2.setColor(UIColor(named: "lineColor_close")!)
        set2.mode = LineChartDataSet.Mode.linear
        set2.drawCirclesEnabled = false
        set2.drawValuesEnabled = false
        
        return set2
    }
    private func zaglushka(bool1: Bool) {    }
}
