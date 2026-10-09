import Foundation
import shared

enum PlotListItemActionV3 {
    case samplesReceived([KotlinInt])
    case graphTick(ticks: Int)
    case graphStateRestored(PlotListItemViewModelV3.GraphState)
    case thresholdsRequested
    case currentThresholdsRequested
    case thresholdsReceived(V3PlotThresholds?)
    case thresholdsCommitted(open: Int, close: Int)
}

struct PlotListItemUiStateV3 {
    let thresholds: (open: Int, close: Int)?
    let graph: PlotListItemViewModelV3.GraphState
}

final class PlotListItemViewModelV3: Equatable, Hashable {
    typealias ThresholdConfiguration = (
        parameters: Set<ParameterInfoData>,
        binding: WidgetV3BindingInfo?,
        target: WidgetV3BindingInfo?
    )
    // The Cell restores this value when it is rebound, preserving its existing graph continuity.
    struct GraphState {
        var samples: (first: Int, second: Int) = (0, 255)
        var current: (first: Double, second: Double) = (0, 0)
        var start: (first: Double, second: Double) = (0, 0)
        var target: (first: Double, second: Double) = (0, 0)
        var previous: (first: Int, second: Int) = (0, 0)
        var tick = 0

        var frame: (first: Int, second: Int) {
            (Int(current.first.rounded()), Int(current.second.rounded()))
        }

        mutating func advance(ticks: Int) {
            let new1 = Double(samples.first)
            let new2 = Double(samples.second)
            if new1 != target.first || new2 != target.second {
                start = current
                target = (new1, new2)
                tick = 0
            }
            let ticks = max(1, ticks)
            let progress = min(1.0, Double(tick + 1) / Double(ticks))
            current.first = start.first + (target.first - start.first) * progress
            current.second = start.second + (target.second - start.second) * progress
            if tick < ticks - 1 {
                tick += 1
            } else {
                previous = (Int(target.first), Int(target.second))
            }
        }
    }

    private static let requestTracker = RequestTracker()
    private let identifier: String
    let title: String
    let widget: Widget
    let parameterInfoSet: Set<ParameterInfoData>
    private let thresholdTarget: WidgetV3BindingInfo?
    private let getPlotSettingsUseCase: GetPlotSettingsUseCaseV3
    private let requestPlotThresholdsUseCase: RequestPlotThresholdsUseCaseV3
    private let observePlotThresholdsUseCase: ObservePlotThresholdsUseCaseV3
    private let editPlotThresholdUseCase: EditPlotThresholdUseCaseV3
    private let setPlotThresholdsUseCase: SetPlotThresholdsUseCaseV3
    private let observePlotSamplesUseCase: ObservePlotSamplesUseCaseV3
    private(set) var uiState = PlotListItemUiStateV3(thresholds: nil, graph: GraphState())

    init(
        widget: Widget,
        thresholdConfiguration: ThresholdConfiguration,
        getPlotSettingsUseCase: GetPlotSettingsUseCaseV3,
        requestPlotThresholdsUseCase: RequestPlotThresholdsUseCaseV3,
        observePlotThresholdsUseCase: ObservePlotThresholdsUseCaseV3,
        editPlotThresholdUseCase: EditPlotThresholdUseCaseV3,
        setPlotThresholdsUseCase: SetPlotThresholdsUseCaseV3,
        observePlotSamplesUseCase: ObservePlotSamplesUseCaseV3
    ) {
        self.title = widget.title ?? ""
        self.widget = widget
        self.parameterInfoSet = thresholdConfiguration.parameters
        self.thresholdTarget = thresholdConfiguration.target
        self.getPlotSettingsUseCase = getPlotSettingsUseCase
        self.requestPlotThresholdsUseCase = requestPlotThresholdsUseCase
        self.observePlotThresholdsUseCase = observePlotThresholdsUseCase
        self.editPlotThresholdUseCase = editPlotThresholdUseCase
        self.setPlotThresholdsUseCase = setPlotThresholdsUseCase
        self.observePlotSamplesUseCase = observePlotSamplesUseCase
        let thresholdBinding = thresholdConfiguration.binding

        let widgetPosition = WidgetMetadataExtractor
            .extractBaseStruct(from: widget.widget?.value)?
            .widgetPosition ?? -1
        if let thresholdBinding {
            self.identifier = "\(widgetPosition)-\(thresholdBinding.deviceAddress)-\(thresholdBinding.parameterID)-\(thresholdBinding.dataCode)-plot-v3"
        } else {
            self.identifier = "\(widgetPosition)-\(widget.deviceAddress)-\(widget.parameterID)-plot-v3"
        }
    }

    func onAction(_ action: PlotListItemActionV3) {
        switch action {
        case .samplesReceived(let samples):
            var graph = uiState.graph
            if let first = samples.first { graph.samples.first = Int(first.intValue) }
            if samples.count > 1 { graph.samples.second = Int(samples[1].intValue) }
            uiState = PlotListItemUiStateV3(thresholds: uiState.thresholds, graph: graph)
        case .graphTick(let ticks):
            var graph = uiState.graph
            graph.advance(ticks: ticks)
            uiState = PlotListItemUiStateV3(thresholds: uiState.thresholds, graph: graph)
        case .graphStateRestored(let graph):
            uiState = PlotListItemUiStateV3(thresholds: uiState.thresholds, graph: graph)
        case .thresholdsRequested:
            requestThresholds()
        case .currentThresholdsRequested:
            onAction(.thresholdsReceived(getPlotSettingsUseCase.invoke().thresholds))
        case .thresholdsReceived(let thresholds):
            uiState = PlotListItemUiStateV3(thresholds: thresholds.map {
                // The existing iOS labels use the opposite order to the device payload.
                (open: Int($0.close), close: Int($0.open))
            }, graph: uiState.graph)
        case .thresholdsCommitted(let open, let close):
            // Keep the raw UI draft; only the command values are clamped.
            uiState = PlotListItemUiStateV3(thresholds: (open: open, close: close), graph: uiState.graph)
            let normalizedOpen = editPlotThresholdUseCase.invoke(
                current: V3PlotThresholds(open: 0, close: 0), threshold: .open, value: Int32(clamping: open)
            )
            let normalized = editPlotThresholdUseCase.invoke(
                current: normalizedOpen, threshold: .close, value: Int32(clamping: close)
            )
            setPlotThresholdsUseCase.invoke(thresholds: V3PlotThresholds(
                open: normalized.close, close: normalized.open
            ))
        }
    }

    // The Cell owns the returned subscription, just as it owns its timer and legacy subscription.
    func observeSamples(onSamples: @escaping (GraphState, Int) -> Void) -> Kotlinx_coroutines_coreJob {
        observePlotSamplesUseCase.observe { [self] samples in
            onAction(.samplesReceived(samples))
            onSamples(uiState.graph, samples.count)
        }
    }

    func observeThresholds(
        onThresholds: @escaping ((open: Int, close: Int)) -> Void
    ) -> Kotlinx_coroutines_coreJob {
        observePlotThresholdsUseCase.observe { [self] thresholds in
            onAction(.thresholdsReceived(thresholds))
            if let thresholds = uiState.thresholds { onThresholds(thresholds) }
        }
    }

    private func requestThresholds() {
        guard let target = thresholdTarget else {
            print("[V3-PLOT][VM] requestThresholds skipped: threshold target is unresolved")
            return
        }
        guard Self.requestTracker.shouldRequest(for: identifier) else { return }
        requestPlotThresholdsUseCase.invoke(
            parameterID: Int32(target.parameterID),
            dataCode: Int32(Self.canonicalThresholdDataCode(for: target.dataCode))
        )
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: PlotListItemViewModelV3, rhs: PlotListItemViewModelV3) -> Bool {
        lhs.identifier == rhs.identifier
            && lhs.title == rhs.title
    }

    static func resetRequestCache() {
        requestTracker.reset()
    }

    static func thresholdDataCodeCandidates(for dataCode: Int) -> [Int] {
        let canonical = canonicalThresholdDataCode(for: dataCode)
        return [canonical, ParameterCode.thresholdGetV3, ParameterCode.thresholdLegacyV2]
            .reduce(into: [Int]()) { acc, code in
                if !acc.contains(code) {
                    acc.append(code)
                }
            }
    }

    static func thresholdConfiguration(for widget: Widget) -> ThresholdConfiguration {
        let parameters = ParameterInfoData.makeSet(
            from: widget.plotUnified?.baseParameterWidgetStruct?.parameterInfoSet
        )
        let binding = selectThresholdBinding(from: WidgetV3Support.bindings(from: widget), fallback: parameters)
        return (parameters, binding, resolveThresholdTarget(widget: widget, binding: binding, parameters: parameters))
    }

    private static func resolveThresholdTarget(
        widget: Widget, binding: WidgetV3BindingInfo?, parameters: Set<ParameterInfoData>
    ) -> WidgetV3BindingInfo? {
        if let thresholdBinding = binding {
            return WidgetV3BindingInfo(
                parameterID: thresholdBinding.parameterID,
                dataCode: Self.canonicalThresholdDataCode(for: thresholdBinding.dataCode),
                deviceAddress: thresholdBinding.deviceAddress,
                dataOffset: thresholdBinding.dataOffset
            )
        }

        if let fromSet = parameters.first(
            where: {
                $0.parameterID == Int32(ParameterCode.prosthesisModuleControlV3)
                    && Self.isThresholdDataCode(Int($0.dataCode))
            }
        ) {
            return WidgetV3BindingInfo(
                parameterID: Int(fromSet.parameterID),
                dataCode: Self.canonicalThresholdDataCode(for: Int(fromSet.dataCode)),
                deviceAddress: Int(fromSet.deviceAddress),
                dataOffset: Int(fromSet.dataOffset)
            )
        }

        if widget.deviceAddress > 0 {
            return WidgetV3BindingInfo(
                parameterID: ParameterCode.prosthesisModuleControlV3,
                dataCode: ParameterCode.thresholdSetV3,
                deviceAddress: Int(widget.deviceAddress),
                dataOffset: 0
            )
        }

        return nil
    }
}

private extension PlotListItemViewModelV3 {
    enum ParameterCode {
        static let prosthesisModuleControlV3 = 0x0F
        static let thresholdSetV3 = 0x2F
        static let thresholdGetV3 = 0x30
        static let thresholdLegacyV2 = 0x1A
    }

    static func selectThresholdBinding(
        from bindings: [WidgetV3BindingInfo],
        fallback parameterInfoSet: Set<ParameterInfoData>
    ) -> WidgetV3BindingInfo? {
        if let binding = bindings.first(
            where: {
                $0.parameterID == ParameterCode.prosthesisModuleControlV3
                    && isThresholdDataCode($0.dataCode)
            }
        ) {
            return binding
        }

        if let binding = bindings.first(where: { isThresholdDataCode($0.dataCode) }) {
            return binding
        }

        guard let fromSet = parameterInfoSet.first(
            where: {
                $0.parameterID == Int32(ParameterCode.prosthesisModuleControlV3)
                    && isThresholdDataCode(Int($0.dataCode))
            }
        ) else {
            return nil
        }

        return WidgetV3BindingInfo(
            parameterID: Int(fromSet.parameterID),
            dataCode: Int(fromSet.dataCode),
            deviceAddress: Int(fromSet.deviceAddress),
            dataOffset: Int(fromSet.dataOffset)
        )
    }

    static func isThresholdDataCode(_ dataCode: Int) -> Bool {
        switch dataCode {
        case ParameterCode.thresholdSetV3, ParameterCode.thresholdGetV3, ParameterCode.thresholdLegacyV2:
            return true
        default:
            return false
        }
    }

    static func canonicalThresholdDataCode(for dataCode: Int) -> Int {
        switch dataCode {
        case ParameterCode.thresholdSetV3, ParameterCode.thresholdGetV3, ParameterCode.thresholdLegacyV2:
            return ParameterCode.thresholdSetV3
        default:
            return dataCode
        }
    }


    final class RequestTracker {
        private var requestedIdentifiers: Set<String> = []
        private let lock = NSLock()

        func shouldRequest(for identifier: String) -> Bool {
            lock.lock()
            defer { lock.unlock() }

            let isNew = !requestedIdentifiers.contains(identifier)
            if isNew {
                requestedIdentifiers.insert(identifier)
            }
            return isNew
        }

        func reset() {
            lock.lock()
            requestedIdentifiers.removeAll()
            lock.unlock()
        }
    }
}
