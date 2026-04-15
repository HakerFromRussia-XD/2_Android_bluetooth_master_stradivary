import UIKit
import shared

final class PlotViewCellV3: PlotViewCell {
    private var viewModelV3: PlotListItemViewModelV3?
    private var thresholdV3Job: Kotlinx_coroutines_coreJob?
    private var pendingThresholds: (open: Int, close: Int)?
    private var pendingThresholdSentAt: CFTimeInterval = 0
    private let staleThresholdGraceInterval: CFTimeInterval = 0.5

    override func prepareForReuse() {
        thresholdV3Job?.cancel(cause: nil)
        thresholdV3Job = nil
        viewModelV3 = nil
        pendingThresholds = nil
        pendingThresholdSentAt = 0
        super.prepareForReuse()
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: PlotListItemViewModelV3) {
        self.viewModelV3 = viewModel
        print("[V3-PLOT][CELL] configure title=\(viewModel.title)")

        configureCommon(
            parameterInfoSet: viewModel.parameterInfoSet,
            requestThresholds: { viewModel.requestThresholds() },
            sendThresholds: { [weak self] open, close in
                self?.registerPendingThresholds(open: open, close: close)
                viewModel.sendThresholds(openThreshold: open, closeThreshold: close)
            },
            cachedThresholds: { viewModel.cachedThresholds() }
        )

        thresholdV3Job?.cancel(cause: nil)
        thresholdV3Job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self else { return }
            guard viewModel.matchesThresholdSnapshot(snapshot) else { return }
            guard let thresholds = viewModel.thresholds(from: snapshot) else { return }

            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                guard self.shouldApplyIncomingThresholds(open: thresholds.open, close: thresholds.close) else {
                    return
                }
                self.applyThresholds(
                    open: thresholds.open,
                    close: thresholds.close,
                    animated: true
                )
            }
        }
    }
}

private extension PlotViewCellV3 {
    func registerPendingThresholds(open: Int, close: Int) {
        pendingThresholds = (open, close)
        pendingThresholdSentAt = CACurrentMediaTime()
    }

    func shouldApplyIncomingThresholds(open: Int, close: Int) -> Bool {
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
}
