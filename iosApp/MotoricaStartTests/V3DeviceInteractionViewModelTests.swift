@testable import MotoricaStart
import Foundation
import XCTest
import shared

final class V3DeviceInteractionViewModelTests: XCTestCase {
    func testReadyConnectionAloneDoesNotEnableInteraction() {
        let (viewModel, repository) = makeViewModel()

        viewModel.onAction(.connectionChanged(isReady: true))

        XCTAssertFalse(viewModel.uiState.value.isEnabled)
        XCTAssertEqual(repository.requestedValues.last, false)

        viewModel.onAction(.synchronizationChanged(completed: true, inProgress: false))

        XCTAssertFalse(viewModel.uiState.value.isEnabled)
        XCTAssertEqual(repository.requestedValues.last, false)
    }

    func testInteractionEnablesOnlyAfterAllSynchronizationSignals() {
        let (viewModel, repository) = makeViewModel()

        viewModel.onAction(.connectionChanged(isReady: true))
        viewModel.onAction(.synchronizationChanged(completed: true, inProgress: false))
        viewModel.onAction(.progressReceived)

        XCTAssertFalse(viewModel.uiState.value.isEnabled)

        viewModel.onAction(.synchronizationFinished)

        XCTAssertTrue(viewModel.uiState.value.isEnabled)
        XCTAssertEqual(repository.requestedValues.last, true)
    }

    func testSynchronizationCompletionBeforeProgressIsPreserved() {
        let (viewModel, repository) = makeViewModel()

        viewModel.onAction(.connectionChanged(isReady: true))
        viewModel.onAction(.synchronizationChanged(completed: true, inProgress: false))
        viewModel.onAction(.synchronizationFinished)

        XCTAssertFalse(viewModel.uiState.value.isEnabled)

        viewModel.onAction(.progressReceived)

        XCTAssertTrue(viewModel.uiState.value.isEnabled)
        XCTAssertEqual(repository.requestedValues.last, true)
    }

    func testNativeAcceptanceCanArriveBeforeOrAfterSharedSynchronization() {
        let nativeAcceptanceInProgress = V3DeviceInteractionAction.synchronizationChanged(completed: true, inProgress: true)
        let nativeAcceptance = V3DeviceInteractionAction.synchronizationChanged(completed: true, inProgress: false)
        let orders: [[V3DeviceInteractionAction]] = [
            [nativeAcceptanceInProgress, nativeAcceptance, .progressReceived, .synchronizationFinished],
            [.progressReceived, .synchronizationFinished, nativeAcceptanceInProgress, nativeAcceptance]
        ]

        for actions in orders {
            let (viewModel, repository) = makeViewModel()
            viewModel.onAction(.connectionChanged(isReady: true))

            for (index, action) in actions.enumerated() {
                viewModel.onAction(action)
                XCTAssertEqual(viewModel.uiState.value.isEnabled, index == actions.count - 1)
            }

            XCTAssertEqual(repository.requestedValues.last, true)
        }
    }

    func testReconnectRequiresFreshProgressAndCompletionDespiteCachedNativeAcceptance() {
        let (viewModel, repository) = makeViewModel()
        viewModel.onAction(.connectionChanged(isReady: true))
        viewModel.onAction(.synchronizationChanged(completed: true, inProgress: false))
        viewModel.onAction(.progressReceived)
        viewModel.onAction(.synchronizationFinished)
        XCTAssertTrue(viewModel.uiState.value.isEnabled)

        viewModel.onAction(.connectionChanged(isReady: false))
        XCTAssertFalse(viewModel.uiState.value.isEnabled)

        viewModel.onAction(.connectionChanged(isReady: true))
        XCTAssertFalse(viewModel.uiState.value.isEnabled)

        viewModel.onAction(.progressReceived)
        XCTAssertFalse(viewModel.uiState.value.isEnabled)

        viewModel.onAction(.synchronizationFinished)
        XCTAssertTrue(viewModel.uiState.value.isEnabled)
        XCTAssertEqual(repository.requestedValues.last, true)
    }

    func testIncompleteNativeSynchronizationRequiresAcceptanceAndFreshCompletion() {
        for inProgress in [true, false] {
            let (viewModel, repository) = makeViewModel()
            viewModel.onAction(.connectionChanged(isReady: true))
            viewModel.onAction(.synchronizationChanged(completed: true, inProgress: false))
            viewModel.onAction(.progressReceived)
            viewModel.onAction(.synchronizationFinished)
            XCTAssertTrue(viewModel.uiState.value.isEnabled)

            viewModel.onAction(.synchronizationChanged(completed: false, inProgress: inProgress))
            XCTAssertFalse(viewModel.uiState.value.isEnabled)

            viewModel.onAction(.synchronizationChanged(completed: true, inProgress: false))
            XCTAssertFalse(viewModel.uiState.value.isEnabled)

            viewModel.onAction(.synchronizationFinished)
            XCTAssertTrue(viewModel.uiState.value.isEnabled)
            XCTAssertEqual(repository.requestedValues.last, true)
        }
    }

    private func makeViewModel() -> (V3DeviceInteractionViewModel, DeviceInteractionRepository) {
        let repository = DeviceInteractionRepository()
        let viewModel = V3DeviceInteractionViewModel(
            updateInteraction: UpdateDeviceInteractionUseCaseV3(repository: repository),
            observeConnection: { _ in [] }
        )
        return (viewModel, repository)
    }
}

private final class DeviceInteractionRepository: NSObject, V3DeviceInteractionRepository {
    private(set) var requestedValues: [Bool] = []

    func setInteractionEnabled(enabled: Bool) -> Bool {
        requestedValues.append(enabled)
        return enabled
    }
}
