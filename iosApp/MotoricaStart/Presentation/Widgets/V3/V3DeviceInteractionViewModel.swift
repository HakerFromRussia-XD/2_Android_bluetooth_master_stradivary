import Foundation
import shared

enum V3DeviceInteractionAction {
    case connectionChanged(isReady: Bool)
    case synchronizationChanged(completed: Bool, inProgress: Bool)
    case progressReceived
    case synchronizationFinished
}

struct V3DeviceInteractionUiState: Equatable {
    let isEnabled: Bool
}

/// Owns command availability for the connection, independently of the selected tab.
final class V3DeviceInteractionViewModel {
    let uiState = Observable(V3DeviceInteractionUiState(isEnabled: false))

    private let updateInteraction: UpdateDeviceInteractionUseCaseV3
    private let observeConnection: (@escaping (V3DeviceInteractionAction) -> Void) -> [Kotlinx_coroutines_coreJob]
    private var jobs: [Kotlinx_coroutines_coreJob] = []
    private var isReady = false
    private var nativeSynchronizationCompleted = false
    private var hasReceivedProgress = false
    private var synchronizationFinished = false

    init(
        updateInteraction: UpdateDeviceInteractionUseCaseV3,
        observeConnection: @escaping (@escaping (V3DeviceInteractionAction) -> Void) -> [Kotlinx_coroutines_coreJob]
    ) {
        self.updateInteraction = updateInteraction
        self.observeConnection = observeConnection
    }

    func startObserving() {
        guard jobs.isEmpty else { return }
        onAction(.connectionChanged(isReady: false))
        jobs = observeConnection { [weak self] action in self?.onAction(action) }
    }

    func onAction(_ action: V3DeviceInteractionAction) {
        switch action {
        case .connectionChanged(let ready):
            isReady = ready
            if !ready {
                hasReceivedProgress = false
                synchronizationFinished = false
            }
        case .synchronizationChanged(let completed, let inProgress):
            nativeSynchronizationCompleted = completed && !inProgress
            if !completed { synchronizationFinished = false }
        case .progressReceived:
            guard isReady else { return }
            hasReceivedProgress = true
        case .synchronizationFinished:
            synchronizationFinished = isReady
        }
        let enabled = updateInteraction.invoke(
            enabled: isReady && nativeSynchronizationCompleted && hasReceivedProgress && synchronizationFinished
        )
        let state = V3DeviceInteractionUiState(isEnabled: enabled)
        if uiState.value != state { uiState.value = state }
    }

    deinit {
        jobs.forEach { $0.cancel(cause: nil) }
    }
}
