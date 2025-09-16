import UIKit
import DGCharts
import Combine
import shared

final class PlotViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: PlotViewCell.self)
    static let height = CGFloat(330)
    private var viewModel: PlotListItemViewModel!

    // charts
    var firstInit: Bool = true
    let values = (0..<1).map { (i) -> ChartDataEntry in
        let val = Double(arc4random_uniform(UInt32(1))+3)
        return ChartDataEntry(x: Double(i), y: val)
    }
    var reseve_sensor_1_data: Int = 0
    var reseve_sensor_2_data: Int = 255
    var count: Int = 0
    
    @IBOutlet private weak var backgroundPlot : UIView!
    @IBOutlet private weak var lineChartView: LineChartView!
    
    @IBOutlet private weak var allCHRl: UIView!
    @IBOutlet private weak var limitCH1: UIView!
    @IBOutlet private weak var limitCH2: UIView!
    @IBOutlet private weak var openCHV: UIView!
    @IBOutlet private weak var closeCHV: UIView!
    @IBOutlet private weak var openThresholdTv: UILabel!
    @IBOutlet private weak var closeThresholdTv: UILabel!
    private var timer: Timer?
    
    private var openPanGesture: UIPanGestureRecognizer?
    private var closePanGesture: UIPanGestureRecognizer?
    private var openThreshold: Int = 0
    private var closeThreshold: Int = 0
    private var job: Kotlinx_coroutines_coreJob?
    
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    override func awakeFromNib() {
        super.awakeFromNib()
        initChart()
        setupGestureRecognizers()
        startTimer()
        backgroundPlot.layer.borderColor = UIColor(named: "ubi4_gray_border")?.cgColor
    }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: PlotListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        
        job?.cancel(cause: nil)
        job = WidgetStateBridge.shared.observePlotArray { [weak self] ref in
            self?.updatePlotData(ref, viewModel: viewModel)
        }
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        job?.cancel(cause: nil)
        job = nil
        startTimer()
    }
    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            stopTimer()
        }
    }
    
    // MARK: - работа с графиком
    func addEntry (sens1: Int, sens2: Int) {
        print("initChart 2    addEntry")
//        let data: ChartData = (self.lineChartView?.data!)!
        guard let lineChartView = lineChartView,
               let data = lineChartView.data else { return }

        var set1 : LineChartDataSet
        var set2 : LineChartDataSet
        
        if (firstInit) {
            set1 = createSet1(values: values)
            set2 = createSet2(values: values)
            data.append(set1)
            data.append(set2)
            firstInit = false
        } else {
            guard
                let existingSet1 = data[1] as? LineChartDataSet,
                let existingSet2 = data[2] as? LineChartDataSet
            else { return }
            set1 = existingSet1
            set2 = existingSet2
        }
        if (set1.count >= 300) {
            zaglushka(bool1: (set1.removeFirst()))
            zaglushka(bool1: (set2.removeFirst()))
        }
        
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens1)), toDataSet: 1)
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens2)), toDataSet: 2)
        
        data.notifyDataChanged()
        lineChartView.notifyDataSetChanged()
        lineChartView.setVisibleXRangeMaximum(300)
        lineChartView.moveViewToX(Double(set2.count - 300))
        self.count += 1
    }
    private func initChart() {
        guard let lineChartView = lineChartView else { return }
        print("initChart 2    прошли первую проверку")
        lineChartView.noDataText = "Нет данных"
        lineChartView.data = LineChartData()
        var data = lineChartView.data
        let set1 = LineChartDataSet(entries: [], label: "")
        data = LineChartData(dataSet: set1)
        var data2 = lineChartView.data
        let set2 = LineChartDataSet(entries: [], label: "")
        data2 = LineChartData(dataSet: set2)
        lineChartView.data = data
        lineChartView.data = data2
        
        lineChartView.highlightPerTapEnabled = false
        lineChartView.doubleTapToZoomEnabled = false
        lineChartView.isUserInteractionEnabled = false
        lineChartView.isExclusiveTouch = false
        lineChartView.isMultipleTouchEnabled = false
        lineChartView.dragEnabled = false
        lineChartView.dragDecelerationEnabled = false
        lineChartView.setScaleEnabled(false)
        lineChartView.drawGridBackgroundEnabled = false
        lineChartView.pinchZoomEnabled = false
        lineChartView.backgroundColor = UIColor(named: "transparent") ?? .clear

        lineChartView.legend.enabled = false
        lineChartView.animate(yAxisDuration: 0.7)

        let x: XAxis = lineChartView.xAxis
        x.labelTextColor = UIColor(named: "transparent") ?? .clear
        x.drawGridLinesEnabled = false
        x.axisMaximum = 4000000
        x.avoidFirstLastClippingEnabled = true
        
        let y: YAxis = lineChartView.leftAxis
        y.axisMaximum = 255
        y.axisMinimum = 0
        y.labelTextColor = UIColor(named: "transparent") ?? .clear
        y.drawGridLinesEnabled = true
        y.drawAxisLineEnabled = false
        y.gridColor = UIColor(named: "transparent") ?? .clear
        lineChartView.rightAxis.axisLineColor = UIColor(named: "transparent") ?? .clear
        lineChartView.rightAxis.labelTextColor = UIColor(named: "transparent") ?? .clear
        print("initChart 2    закончили настройку")
    }
    func createSet1(values: [ChartDataEntry]) -> LineChartDataSet {
        let set1 = LineChartDataSet(entries: [], label: "")
        set1.axisDependency = YAxis.AxisDependency.left
        set1.lineWidth = 2
        set1.setColor(UIColor(named: "ubi4_white")!)
        set1.mode = LineChartDataSet.Mode.linear
        set1.drawCirclesEnabled = false
        set1.drawValuesEnabled = false
        
        return set1
    }
    func createSet2(values: [ChartDataEntry]) -> LineChartDataSet {
        let set2 = LineChartDataSet(entries: [], label: "")
        set2.axisDependency = YAxis.AxisDependency.left
        set2.lineWidth = 2
        set2.setColor(UIColor(named: "ubi4_active")!)
        set2.mode = LineChartDataSet.Mode.linear
        set2.drawCirclesEnabled = false
        set2.drawValuesEnabled = false
        
        return set2
    }
    private func zaglushka(bool1: Bool) {    }
    
    private func setupGestureRecognizers() {
        // --- OPEN CHV ---
        let openTap = UITapGestureRecognizer(target: self, action: #selector(handleOpenTap))
        openCHV.addGestureRecognizer(openTap)

        let openLongPress = UILongPressGestureRecognizer(target: self, action: #selector(handleOpenLongPress))
        openLongPress.minimumPressDuration = 0.1
        openLongPress.allowableMovement = 20
        openLongPress.cancelsTouchesInView = true
        openCHV.addGestureRecognizer(openLongPress)

        // --- CLOSE CHV ---
        let closeTap = UITapGestureRecognizer(target: self, action: #selector(handleCloseTap))
        closeCHV.addGestureRecognizer(closeTap)

        let closeLongPress = UILongPressGestureRecognizer(target: self, action: #selector(handleCloseLongPress))
        closeLongPress.minimumPressDuration = 0.1
        closeLongPress.allowableMovement = 20
        openLongPress.cancelsTouchesInView = true
        closeCHV.addGestureRecognizer(closeLongPress)
    }
    @objc private func handleOpenLongPress(_ gesture: UILongPressGestureRecognizer) {
        let loc = gesture.location(in: allCHRl)
        switch gesture.state {
        case .began, .changed:
            openThreshold = setLimitPosition(limit_CH: limitCH2, thresholdLabel: openThresholdTv, in: allCHRl, touchY: loc.y)
        case .ended:
            // TODO: отправить openThreshold
            break
        default: break
        }
    }
    @objc private func handleCloseLongPress(_ gesture: UILongPressGestureRecognizer) {
        let loc = gesture.location(in: allCHRl)
        switch gesture.state {
        case .began, .changed:
            closeThreshold = setLimitPosition(limit_CH: limitCH1, thresholdLabel: closeThresholdTv, in: allCHRl, touchY: loc.y)
        case .ended:
            // TODO: отправить openThreshold
            break
        default: break
        }
    }
    @objc private func handleOpenTap(_ gesture: UITapGestureRecognizer) {
        print("gestureRecognizer   handleOpenTap")
        let loc = gesture.location(in: allCHRl)
        openThreshold = setLimitPosition(limit_CH: limitCH2, thresholdLabel: openThresholdTv, in: allCHRl, touchY: loc.y)
    }
    @objc private func handleCloseTap(_ gesture: UITapGestureRecognizer) {
        print("gestureRecognizer   handleCloseTap")
        let loc = gesture.location(in: allCHRl)
        closeThreshold = setLimitPosition(limit_CH: limitCH1, thresholdLabel: closeThresholdTv, in: allCHRl, touchY: loc.y)
    }

    private func setLimitPosition(limit_CH: UIView, thresholdLabel: UILabel, in container: UIView, touchY: CGFloat) -> Int {
        // Конвертируем dp в реальные точки (pt)
        let topOffset = CGFloat(12)
        let bottomOffset = CGFloat(10)
        
        let total = container.bounds.height
        let minY = topOffset
        let maxY = total - bottomOffset

        let y = min(max(touchY, minY), maxY)
        limit_CH.frame.origin.y = y - limit_CH.bounds.height / 2

        let avail = total - topOffset - bottomOffset
        let value = Int(((maxY - y) / avail) * 255.0)
        
        thresholdLabel.text = String(value)
        return value
    }
    
    
    private func updatePlotData(_ ref: PlotParameterRef, viewModel: PlotListItemViewModel) {
        print("updatePlotData    ref.addressDevice = \(String(describing: ref.addressDevice))")
        print("updatePlotData    viewModel.deviceAddress = \(String(describing: viewModel.deviceAddress))")
        print("updatePlotData    ref.parameterID = \(String(describing: ref.parameterID))")
        print("updatePlotData    viewModel.parameterID = \(String(describing: viewModel.parameterID))")
//        guard ref.addressDevice == viewModel.deviceAddress,
//              ref.parameterID == viewModel.parameterID else { return }
        let arr = ref.dataPlots as NSArray

        if arr.count > 0 {
            if let n1 = arr[0] as? NSNumber {
                reseve_sensor_1_data = n1.intValue
            } else if let k1 = arr[0] as? KotlinInt {
                reseve_sensor_1_data = Int(k1.intValue)
            }
        }

        if arr.count > 1 {
            if let n2 = arr[1] as? NSNumber {
                reseve_sensor_2_data = n2.intValue
            } else if let k2 = arr[1] as? KotlinInt {
                reseve_sensor_2_data = Int(k2.intValue)
            }
        }
    }
    private func startTimer() {
        stopTimer()
        let t = Timer(timeInterval: 0.01, repeats: true) { [weak self] _ in
            guard let self else { return }
            self.addEntry(sens1: self.reseve_sensor_1_data, sens2: self.reseve_sensor_2_data)
        }
        RunLoop.main.add(t, forMode: .common) // явная привязка
        timer = t
    }
    func stopTimer() {
        print("[Lifecycle]  stopTimer")
        timer?.invalidate()
        timer = nil
    }
}
