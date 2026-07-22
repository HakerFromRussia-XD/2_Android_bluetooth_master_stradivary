import UIKit
import DGCharts
import Combine
import shared

final class PlotViewCellV3: UITableViewCell {
    static let reuseIdentifier = "PlotViewCell"
    static let height = CGFloat(330)
    private var viewModelV3: PlotListItemViewModelV3?
    private var viewModelLegacy: PlotListItemViewModel?
    private var widgetPlotInfo: WidgetPlotInfo?
    private var sendThresholdsHandler: ((Int, Int) -> Void)?

    // charts
    var firstInit: Bool = true
    let values = (0..<1).map { (i) -> ChartDataEntry in
        let val = Double(arc4random_uniform(UInt32(1))+3)
        return ChartDataEntry(x: Double(i), y: val)
    }
    var reseve_sensor_1_data: Int = 0
    var reseve_sensor_2_data: Int = 255
    var old_reseve_sensor_1_data: Int = 0
    var old_reseve_sensor_2_data: Int = 0
    
    // Сколько точек (тиков) нужно, чтобы дойти от old -> target
    var timerTiks: Int = 3 // зачем: задаёт длительность плавного перехода в тиках
    
    // Текущее "рисуемое" значение (переходное)
    private var current_sensor_1: Double = 0 // зачем: хранит промежуточное значение для прямой
    private var current_sensor_2: Double = 0 // зачем: хранит промежуточное значение для прямой

    // Старт и цель перехода
    private var start_sensor_1: Double = 0   // зачем: фиксируем откуда начинаем линию
    private var start_sensor_2: Double = 0   // зачем: фиксируем откуда начинаем линию
    private var target_sensor_1: Double = 0  // зачем: фиксируем куда ведём линию
    private var target_sensor_2: Double = 0  // зачем: фиксируем куда ведём линию

    // Счётчик тиков внутри текущего перехода
    private var rampTick: Int = 0            // зачем: понимаем на каком шаге (0...timerTiks)
    
    var count: Int = 0
    
    @IBOutlet private weak var backgroundPlot : UIView!
    @IBOutlet private weak var lineChartView: LineChartView!
    
    @IBOutlet private weak var allCHRl: UIView!
    @IBOutlet private weak var limitCH1: UIView!
    @IBOutlet private weak var limitCH2: UIView!
    @IBOutlet private weak var openCHV: UIView!
    @IBOutlet private weak var closeCHV: UIView!
    @IBOutlet private weak var openThresholdTitleLabel: UILabel!
    @IBOutlet private weak var closeThresholdTitleLabel: UILabel!
    @IBOutlet private weak var openThresholdTv: UILabel!
    @IBOutlet private weak var closeThresholdTv: UILabel!
    private var timer: Timer?
    
    private var openPanGesture: UIPanGestureRecognizer?
    private var closePanGesture: UIPanGestureRecognizer?
    private var openThreshold: Int = 0
    private var closeThreshold: Int = 0
    private var plotDateEntryJob: Kotlinx_coroutines_coreJob?
    private var thresholdLegacyJob: Kotlinx_coroutines_coreJob?
    private var thresholdV3Job: Kotlinx_coroutines_coreJob?
    private var needsThresholdLayout: Bool = false
    private var pendingThresholds: (open: Int, close: Int)?
    private var pendingThresholdSentAt: CFTimeInterval = 0
    private let staleThresholdGraceInterval: CFTimeInterval = 0.5
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    override func awakeFromNib() {
        super.awakeFromNib()
        initChart()
        applyLocalizedStaticTexts()
        setupGestureRecognizers()
        startTimer()
        backgroundPlot.layer.borderColor = UIColor(named: "ubi4_gray_border")?.cgColor
    }

    private func applyLocalizedStaticTexts() {
        setLocalizedText(
            SharedLocalizedText.text(SharedRes.strings().open_threshold),
            on: openThresholdTitleLabel
        )
        setLocalizedText(
            SharedLocalizedText.text(SharedRes.strings().close_threshold),
            on: closeThresholdTitleLabel
        )
    }

    private func setLocalizedText(_ text: String, on label: UILabel?) {
        guard let label else { return }
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.7
        label.numberOfLines = 1

        guard let attributedText = label.attributedText, attributedText.length > 0 else {
            label.text = text
            return
        }

        let attributes = attributedText.attributes(at: 0, effectiveRange: nil)
        label.attributedText = NSAttributedString(string: text, attributes: attributes)
    }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: PlotListItemViewModelV3) {
        self.viewModelV3 = viewModel
        self.viewModelLegacy = nil
        selectionStyle = .none
        print("[V3-PLOT][CELL] configure title=\(viewModel.title)")
        viewModel.requestThresholds()
        sendThresholdsHandler = { [weak self] open, close in
            self?.registerPendingThresholds(open: open, close: close)
            viewModel.sendThresholds(openThreshold: open, closeThreshold: close)
        }

        configureWidgetPlotInfo(parameterInfoSet: viewModel.parameterInfoSet)

        if let cached = viewModel.cachedThresholds() {
            applyThresholds(open: cached.open, close: cached.close, animated: false)
        }
        
        plotDateEntryJob?.cancel(cause: nil)
        plotDateEntryJob = WidgetStateBridge.shared.observePlotArray { [weak self] ref in
            self?.updatePlotData(ref)
        }

        thresholdLegacyJob?.cancel(cause: nil)
        thresholdLegacyJob = nil
        thresholdV3Job?.cancel(cause: nil)
        thresholdV3Job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self else { return }
            guard viewModel.matchesThresholdSnapshot(snapshot) else { return }
            guard let thresholds = viewModel.thresholds(from: snapshot) else { return }

            DispatchQueue.main.async { [weak self] in
                self?.applyIncomingThresholdSnapshot(open: thresholds.open, close: thresholds.close)
            }
        }
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: PlotListItemViewModel) {
        self.viewModelLegacy = viewModel
        self.viewModelV3 = nil
        selectionStyle = .none
        print("[LEGACY-PLOT][CELL] configure title=\(viewModel.title)")
        viewModel.requestThresholds()
        sendThresholdsHandler = { open, close in
            viewModel.sendThresholds(openThreshold: open, closeThreshold: close)
        }

        if let plotWidget = viewModel.widget.widget?.value as? AnyObject {
            let parameterInfoSet: Any?
            if let plotStruct = plotWidget as? PlotParameterWidgetEStruct {
                parameterInfoSet = plotStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet
            } else if let plotStruct = plotWidget as? PlotParameterWidgetSStruct {
                parameterInfoSet = plotStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
            } else {
                return
            }
            configureWidgetPlotInfo(parameterInfoSet: ParameterInfoData.makeSet(from: parameterInfoSet))
        }

        plotDateEntryJob?.cancel(cause: nil)
        plotDateEntryJob = WidgetStateBridge.shared.observePlotArray { [weak self] ref in
            self?.updatePlotData(ref)
        }

        thresholdV3Job?.cancel(cause: nil)
        thresholdV3Job = nil
        thresholdLegacyJob?.cancel(cause: nil)
        thresholdLegacyJob = WidgetStateBridge.shared.observeThresholdFlow { [weak self] ref in
            self?.updateThresholdDataLegacy(ref, viewModel: viewModel)
        }
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        plotDateEntryJob?.cancel(cause: nil)
        plotDateEntryJob = nil
        thresholdLegacyJob?.cancel(cause: nil)
        thresholdLegacyJob = nil
        thresholdV3Job?.cancel(cause: nil)
        thresholdV3Job = nil
        viewModelLegacy = nil
        viewModelV3 = nil
        widgetPlotInfo = nil
        sendThresholdsHandler = nil
        needsThresholdLayout = false
        pendingThresholds = nil
        pendingThresholdSentAt = 0
        startTimer()
    }
    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            stopTimer()
        } else if timer == nil {
            startTimer()
        }
    }
    override func layoutSubviews() {
        super.layoutSubviews()
        if let legacyViewModel = viewModelLegacy {
            let parameter = ParameterProvider.Companion()
                .getParameter(
                    deviceAddress: Int32(legacyViewModel.widget.deviceAddress),
                    parameterID: Int32(legacyViewModel.widget.parameterID)
                )
            if !parameter.data.isEmpty {
                let thresholds = SerializationObjects.shared.decodePlotThresholds(raw: "\"\(parameter.data)\"")
                openThreshold = Int(thresholds.threshold1)
                closeThreshold = Int(thresholds.threshold2)
                needsThresholdLayout = true
            }
        }
        applyThresholdLayout(animated: false)
    }
    
    
    // MARK: - работа с графиком
    func addEntry (sens1: Int, sens2: Int) {
        print("initChart 2    addEntry")
//        let data: ChartData = (self.lineChartView?.data!)!
        guard let lineChartView = lineChartView,
               let data = lineChartView.data else { return }

        var set1 : LineChartDataSet
        var set2 : LineChartDataSet
        var set3 : LineChartDataSet
        var set4 : LineChartDataSet
        
        if (firstInit) {
            set1 = createSet1(values: values)
            set2 = createSet2(values: values)
            set3 = createSet3(values: values)
            set4 = createSet4(values: values)
            data.append(set1)
            data.append(set2)
            data.append(set3)
            data.append(set4)
            firstInit = false
        } else {
            guard
                let existingSet1 = data[1] as? LineChartDataSet,
                let existingSet2 = data[2] as? LineChartDataSet,
                let existingSet3 = data[3] as? LineChartDataSet,
                let existingSet4 = data[4] as? LineChartDataSet
            else { return }
            set1 = existingSet1
            set2 = existingSet2
            set3 = existingSet3
            set4 = existingSet4
        }
        if (set1.count >= 600) {
            zaglushka(bool1: (set1.removeFirst()))
            zaglushka(bool1: (set2.removeFirst()))
            zaglushka(bool1: (set3.removeFirst()))
            zaglushka(bool1: (set4.removeFirst()))
        }
        
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens1)), toDataSet: 1)
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(sens2)), toDataSet: 2)
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(255)), toDataSet: 3)
        data.appendEntry(ChartDataEntry(x: Double(self.count), y: Double(0)), toDataSet: 4)
        
        data.notifyDataChanged()
        lineChartView.notifyDataSetChanged()
        lineChartView.setVisibleXRangeMaximum(600)
        lineChartView.moveViewToX(Double(set2.count - 600))
        self.count += 1
    }
    private func initChart() {
        guard let lineChartView = lineChartView else { return }
        print("initChart 2    прошли первую проверку")
        lineChartView.noDataText = SharedLocalizedText.text(SharedRes.strings().no_data)
        lineChartView.data = LineChartData()
        var data = lineChartView.data
        let set1 = LineChartDataSet(entries: [], label: "")
        data = LineChartData(dataSet: set1)
        var data2 = lineChartView.data
        let set2 = LineChartDataSet(entries: [], label: "")
        data2 = LineChartData(dataSet: set2)
        var data3 = lineChartView.data
        let set3 = LineChartDataSet(entries: [], label: "")
        data3 = LineChartData(dataSet: set3)
        var data4 = lineChartView.data
        let set4 = LineChartDataSet(entries: [], label: "")
        data4 = LineChartData(dataSet: set4)
        lineChartView.data = data
        lineChartView.data = data2
        lineChartView.data = data3
        lineChartView.data = data4
        
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
        set1.lineWidth = 3
        set1.setColor(UIColor(named: "ubi4_white")!)
        set1.mode = LineChartDataSet.Mode.cubicBezier
        set1.drawCirclesEnabled = false
        set1.drawValuesEnabled = false
        
        return set1
    }
    func createSet2(values: [ChartDataEntry]) -> LineChartDataSet {
        let set2 = LineChartDataSet(entries: [], label: "")
        set2.axisDependency = YAxis.AxisDependency.left
        set2.lineWidth = 3
        set2.setColor(UIColor(named: "ubi4_deactivate_text")!)
        set2.mode = LineChartDataSet.Mode.cubicBezier
        set2.drawCirclesEnabled = false
        set2.drawValuesEnabled = false
        
        return set2
    }
    func createSet3(values: [ChartDataEntry]) -> LineChartDataSet {
        let set3 = LineChartDataSet(entries: [], label: "")
        set3.axisDependency = YAxis.AxisDependency.left
        set3.lineWidth = 0
        set3.drawCirclesEnabled = false
        set3.drawValuesEnabled = false
        return set3
    }
    func createSet4(values: [ChartDataEntry]) -> LineChartDataSet {
        let set4 = LineChartDataSet(entries: [], label: "")
        set4.axisDependency = YAxis.AxisDependency.left
        set4.lineWidth = 0
        set4.drawCirclesEnabled = false
        set4.drawValuesEnabled = false
        return set4
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
            let value = setLimitPosition(limit_CH: limitCH2, thresholdLabel: openThresholdTv, in: allCHRl, touchY: loc.y)
            openThreshold = value
            widgetPlotInfo?.openThreshold = value
        case .ended:
            sendThresholdsFromUI()
            break
        default: break
        }
    }
    @objc private func handleCloseLongPress(_ gesture: UILongPressGestureRecognizer) {
        let loc = gesture.location(in: allCHRl)
        switch gesture.state {
        case .began, .changed:
            let value = setLimitPosition(limit_CH: limitCH1, thresholdLabel: closeThresholdTv, in: allCHRl, touchY: loc.y)
            closeThreshold = value
            widgetPlotInfo?.closeThreshold = value
        case .ended:
            sendThresholdsFromUI()
            break
        default: break
        }
    }
    @objc private func handleOpenTap(_ gesture: UITapGestureRecognizer) {
        print("gestureRecognizer   handleOpenTap")
        let loc = gesture.location(in: allCHRl)
        let value = setLimitPosition(limit_CH: limitCH2, thresholdLabel: openThresholdTv, in: allCHRl, touchY: loc.y)
        openThreshold = value
        widgetPlotInfo?.openThreshold = value
        sendThresholdsFromUI()
    }
    @objc private func handleCloseTap(_ gesture: UITapGestureRecognizer) {
        print("gestureRecognizer   handleCloseTap")
        let loc = gesture.location(in: allCHRl)
        let value = setLimitPosition(limit_CH: limitCH1, thresholdLabel: closeThresholdTv, in: allCHRl, touchY: loc.y)
        closeThreshold = value
        widgetPlotInfo?.closeThreshold = value
        sendThresholdsFromUI()
    }

    private func getIndexWidgetPlot(addressDevice: Int, parameterID: Int) -> Int {
        guard let widgetPlotInfo = widgetPlotInfo else { return -1 }
        for (index, widgetPlot) in widgetPlotInfo.addressDeviceSet.enumerated() {
            if widgetPlot.deviceAddress == addressDevice && widgetPlot.parameterID == parameterID {
                return index
            }
        }
        return -1
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
    private func setLimitPosition(
        limit_CH: UIView,
        thresholdLabel: UILabel,
        in container: UIView,
        thresholdValue: Int,
        animated: Bool = false
    ) {
        print("updateThreshold    setLimitPosition")
        let topOffset = CGFloat(12)
        let bottomOffset = CGFloat(10)

        let clampedValue = max(0, min(thresholdValue, 255))

        container.layoutIfNeeded()
        let total = container.bounds.height
        guard total > 0 else { return }

        let minY = topOffset
        let maxY = total - bottomOffset
        let avail = total - topOffset - bottomOffset
        guard avail > 0 else { return }

        let y = max(min(maxY - (CGFloat(clampedValue) / 255.0) * avail, maxY), minY)
        let newOriginY = y - limit_CH.bounds.height / 2

        let applyChanges = {
            limit_CH.frame.origin.y = newOriginY
            thresholdLabel.text = String(clampedValue)
        }

        if animated, abs(limit_CH.frame.origin.y - newOriginY) > .ulpOfOne {
            let animationsWereEnabled = UIView.areAnimationsEnabled
            if !animationsWereEnabled {
                UIView.setAnimationsEnabled(true)
            }

            UIView.animate(
                withDuration: 0.25,
                delay: 0,
                options: [.curveEaseInOut],
                animations: applyChanges
            ) { _ in
                if !animationsWereEnabled {
                    UIView.setAnimationsEnabled(false)
                }
            }
        } else {
            applyChanges()
        }
    }
    
    private func applyThresholdLayout(animated: Bool) {
        print("updateThreshold    applyThresholdLayout 1 needsThresholdLayout = \(needsThresholdLayout)  allCHRl.bounds.height = \(allCHRl.bounds.height)")
        guard
            needsThresholdLayout,
            allCHRl.bounds.height > 0
        else { return }
        print("updateThreshold    applyThresholdLayout 2 openThreshold = \(openThreshold)")
        
        setLimitPosition(
            limit_CH: limitCH2,
            thresholdLabel: openThresholdTv,
            in: allCHRl,
            thresholdValue: openThreshold,
            animated: animated
        )
        setLimitPosition(
            limit_CH: limitCH1,
            thresholdLabel: closeThresholdTv,
            in: allCHRl,
            thresholdValue: closeThreshold,
            animated: animated
        )
        needsThresholdLayout = false
    }
    private func updatePlotData(_ ref: PlotParameterRef) {
        //если в сете виджета ещё нет графиков, то getIndexWidgetPlot будет -1
        guard getIndexWidgetPlot(addressDevice: Int(ref.addressDevice), parameterID: Int(ref.parameterID)) != -1 else { return }
        let arr = ref.dataPlots as NSArray

        if arr.count > 0 {
            if let n1 = arr[0] as? NSNumber {
                reseve_sensor_1_data = n1.intValue
            } else if let k1 = arr[0] as? KotlinInt {
                reseve_sensor_1_data = Int(k1.intValue)
            }
            widgetPlotInfo?.dataSens1 = reseve_sensor_1_data
        }

        if arr.count > 1 {
            if let n2 = arr[1] as? NSNumber {
                reseve_sensor_2_data = n2.intValue
            } else if let k2 = arr[1] as? KotlinInt {
                reseve_sensor_2_data = Int(k2.intValue)
            }
            widgetPlotInfo?.dataSens2 = reseve_sensor_2_data
        }
    }

    private func updateThresholdDataLegacy(_ ref: ParameterRef, viewModel: PlotListItemViewModel) {
        guard ref.addressDevice == viewModel.widget.deviceAddress,
              ref.parameterID == viewModel.widget.parameterID else { return }

        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        guard !parameter.data.isEmpty else { return }
        let thresholds = SerializationObjects.shared.decodePlotThresholds(raw: "\"\(parameter.data)\"")

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.openThreshold = Int(thresholds.threshold1)
            self.closeThreshold = Int(thresholds.threshold2)
            self.needsThresholdLayout = true
            self.applyThresholdLayout(animated: true)
        }
    }

    private func applyThresholds(open: Int, close: Int, animated: Bool) {
        openThreshold = open
        closeThreshold = close
        needsThresholdLayout = true
        applyThresholdLayout(animated: animated)
    }

    private func applyIncomingThresholdSnapshot(open: Int, close: Int) {
        guard shouldApplyIncomingThresholds(open: open, close: close) else { return }
        applyThresholds(open: open, close: close, animated: true)
    }

    private func sendThresholdsFromUI() {
        if viewModelV3 != nil {
            registerPendingThresholds(open: openThreshold, close: closeThreshold)
        }
        sendThresholdsHandler?(openThreshold, closeThreshold)
    }

    private func configureWidgetPlotInfo(parameterInfoSet: Set<ParameterInfoData>) {
        widgetPlotInfo = WidgetPlotInfo(
            addressDeviceSet: parameterInfoSet,
            openThreshold: openThreshold,
            closeThreshold: closeThreshold,
            threshold3: 0,
            threshold4: 0,
            threshold5: 0,
            threshold6: 0,
            limitCH1: limitCH1,
            limitCH2: limitCH2,
            closeThresholdLabel: closeThresholdTv,
            openThresholdLabel: openThresholdTv,
            allCHRl: allCHRl,
            dataSens1: reseve_sensor_1_data,
            dataSens2: reseve_sensor_2_data,
            dataSens3: 0,
            dataSens4: 0,
            dataSens5: 0,
            dataSens6: 0
        )
    }

    private func registerPendingThresholds(open: Int, close: Int) {
        pendingThresholds = (open, close)
        pendingThresholdSentAt = CACurrentMediaTime()
    }

    private func shouldApplyIncomingThresholds(open: Int, close: Int) -> Bool {
        guard let pending = pendingThresholds else { return true }

        if pending.open == open && pending.close == close {
            pendingThresholds = nil
            return true
        }

        let elapsed = CACurrentMediaTime() - pendingThresholdSentAt
        if elapsed < staleThresholdGraceInterval {
            print(
                "[V3-PLOT][CELL] ignore stale threshold snapshot open=\(open) close=\(close) pendingOpen=\(pending.open) pendingClose=\(pending.close) elapsed=\(elapsed)"
            )
            return false
        }

        pendingThresholds = nil
        return true
    }

    private func startTimer() {
        stopTimer()
        let t = Timer(timeInterval: 0.01, repeats: true) { [weak self] _ in
            guard let self else { return }
//            self.addEntry(sens1: self.reseve_sensor_1_data, sens2: self.reseve_sensor_2_data)
            
            // 1) Считываем "сырые" цели (куда хотим прийти)
            let new1 = Double(self.reseve_sensor_1_data) // зачем: цель для перехода (sens1)
            let new2 = Double(self.reseve_sensor_2_data) // зачем: цель для перехода (sens2)

            // 2) Если цель изменилась — начинаем новый ramp
            //    (важно: старт берём от ТЕКУЩЕГО промежуточного значения, чтобы линия не "ломалась")
            if new1 != self.target_sensor_1 || new2 != self.target_sensor_2 { // зачем: реагируем на новую цель
                self.start_sensor_1 = self.current_sensor_1 // зачем: новая линия начинается от текущей точки
                self.start_sensor_2 = self.current_sensor_2 // зачем: новая линия начинается от текущей точки

                self.target_sensor_1 = new1 // зачем: фиксируем новую цель
                self.target_sensor_2 = new2 // зачем: фиксируем новую цель

                self.rampTick = 0           // зачем: стартуем переход заново на timerTiks тиков
            }

            // 3) Двигаемся по прямой start -> target за timerTiks тиков
            let ticks = max(1, self.timerTiks) // зачем: защита от 0
            let progress = min(1.0, Double(self.rampTick + 1) / Double(ticks)) // зачем: 0..1

            self.current_sensor_1 = self.start_sensor_1 + (self.target_sensor_1 - self.start_sensor_1) * progress
            self.current_sensor_2 = self.start_sensor_2 + (self.target_sensor_2 - self.start_sensor_2) * progress
            // зачем: это и есть точки на прямой между old и new

            // 4) Рисуем промежуточные значения (получится прямая из timerTiks точек)
            self.addEntry(
                sens1: Int(self.current_sensor_1.rounded()), // зачем: addEntry ждёт Int — даём ближайшее
                sens2: Int(self.current_sensor_2.rounded())  // зачем: addEntry ждёт Int — даём ближайшее
            )

            // 5) Переходим к следующему тику, а когда дошли — фиксируем old = target
            if self.rampTick < ticks - 1 {
                self.rampTick += 1 // зачем: двигаемся по линии
            } else {
                self.old_reseve_sensor_1_data = Int(self.target_sensor_1) // зачем: old становится равен new ПОСЛЕ timerTiks тиков
                self.old_reseve_sensor_2_data = Int(self.target_sensor_2) // зачем: old становится равен new ПОСЛЕ timerTiks тиков
                // rampTick можно не трогать — он и так в конце
            }
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
