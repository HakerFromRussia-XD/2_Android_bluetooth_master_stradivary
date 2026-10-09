import Foundation
import shared
import UIKit

enum V3ServiceFirmwareAction {
    case pickerRequested(deviceAddress: Int32, localFiles: [AccountFirmwareFile])
    case fileSelected(AccountFirmwareFile)
    case fileDeleted(AccountFirmwareFile)
    case cancelRequested
}

enum V3ServiceFirmwareUiState {
    case idle
    case loading
    case files([AccountFirmwareFile])
    case selected(AccountFirmwareFile)
    case failed(String, [AccountFirmwareFile])
}

final class V3ServiceFirmwareViewModel {
    private let loadCatalog: LoadServiceFirmwareCatalogUseCaseV3
    private let filesForBoard: GetServiceFirmwareForBoardUseCaseV3
    private let downloadFiles: DownloadServiceFirmwareFilesUseCaseV3
    private var requestTask: Task<Void, Never>?
    private var pickerFiles: [AccountFirmwareFile] = []
    let uiState = Observable<V3ServiceFirmwareUiState>(.idle)

    init(loadCatalog: LoadServiceFirmwareCatalogUseCaseV3,
         filesForBoard: GetServiceFirmwareForBoardUseCaseV3,
         downloadFiles: DownloadServiceFirmwareFilesUseCaseV3) {
        self.loadCatalog = loadCatalog
        self.filesForBoard = filesForBoard
        self.downloadFiles = downloadFiles
    }

    func onAction(_ action: V3ServiceFirmwareAction) {
        requestTask?.cancel()
        switch action {
        case .pickerRequested(let address, let localFiles):
            pickerFiles = localFiles
            uiState.value = .loading
            requestTask = Task { @MainActor [weak self, loadCatalog, filesForBoard] in
                do {
                    let catalog = try await loadCatalog.invoke()
                    try Task.checkCancellation()
                    let remoteFiles = filesForBoard.invoke(catalog: catalog, deviceAddress: address)
                    var names = Set(localFiles.map { $0.name.lowercased() })
                    let files = (localFiles + remoteFiles.compactMap { remote -> AccountFirmwareFile? in
                        guard names.insert(remote.name.lowercased()).inserted else { return nil }
                        return AccountFirmwareFile(remoteFile: remote)
                    }).sorted { $0.name.lowercased() < $1.name.lowercased() }
                    self?.pickerFiles = files
                    self?.uiState.value = .files(files)
                } catch {
                    guard !Task.isCancelled else { return }
                    self?.uiState.value = .failed(error.localizedDescription, localFiles)
                }
            }
        case .fileSelected(let file):
            switch file.source {
            case .local:
                uiState.value = .selected(file)
            case .remote(let remote):
                uiState.value = .loading
                let fallback = pickerFiles
                requestTask = Task { @MainActor [weak self, downloadFiles] in
                    do {
                        let downloaded = try await downloadFiles.invoke(files: [remote])
                        try Task.checkCancellation()
                        guard let local = downloaded.first else { throw CocoaError(.fileNoSuchFile) }
                        self?.uiState.value = .selected(AccountFirmwareFile(
                            name: local.name, url: URL(fileURLWithPath: local.path), isDeletable: false
                        ))
                    } catch {
                        guard !Task.isCancelled else { return }
                        self?.uiState.value = .failed(error.localizedDescription, fallback)
                    }
                }
            }
        case .fileDeleted(let file):
            pickerFiles.removeAll { $0.url == file.url }
            uiState.value = .files(pickerFiles)
        case .cancelRequested:
            requestTask = nil
            uiState.value = .idle
        }
    }

    deinit {
        requestTask?.cancel()
    }
}

final class AccountFirmwareUpdateController {
    private weak var presentingViewController: UIViewController?
    private lazy var dialogPresenter = AccountFirmwareDialogPresenter(
        presentingViewController: requirePresentingViewController()
    )
    private var updateJob: Kotlinx_coroutines_coreJob?
    private var stalledProgressWorkItem: DispatchWorkItem?
    private let viewModelV3: V3ServiceFirmwareViewModel?
    private var pickerBoard: AccountBridgeBoard?
    private var onUpdateFinished: (() -> Void)?

    init(presentingViewController: UIViewController, viewModelV3: V3ServiceFirmwareViewModel? = nil) {
        self.presentingViewController = presentingViewController
        self.viewModelV3 = viewModelV3
        viewModelV3?.uiState.observe(on: self) { [weak self] state in
            self?.renderFirmwareState(state)
        }
    }

    deinit {
        updateJob?.cancel(cause: nil)
        stalledProgressWorkItem?.cancel()
    }

    func availableFirmwareFiles() -> [AccountFirmwareFile] {
        var files: [AccountFirmwareFile] = []
        let bundleFiles = Bundle.main.urls(forResourcesWithExtension: "zip", subdirectory: nil) ?? []
        files += bundleFiles.map {
            AccountFirmwareFile(name: $0.lastPathComponent, url: $0, isDeletable: false)
        }

        FirmwareDocumentsDirectory.prepareSharedFolder()
        files += FirmwareDocumentsDirectory.firmwareFiles()

        return files
            .reduce(into: [String: AccountFirmwareFile]()) { partial, file in
                let key = file.name.lowercased()
                if file.isDeletable || partial[key] == nil {
                    partial[key] = file
                }
            }
            .values
            .sorted { $0.name.lowercased() < $1.name.lowercased() }
    }

    func availableFirmwareFileNames() -> [String] {
        availableFirmwareFiles().map(\.name)
    }

    func showFirmwarePicker(for board: AccountBridgeBoard, onFinished: @escaping () -> Void) {
        if let viewModelV3 {
            pickerBoard = board
            onUpdateFinished = onFinished
            viewModelV3.onAction(.pickerRequested(deviceAddress: board.deviceAddress, localFiles: availableFirmwareFiles()))
            return
        }
        showFirmwareFiles(availableFirmwareFiles(), board: board, onFinished: onFinished)
    }

    func cancelCatalogRequest() {
        viewModelV3?.onAction(.cancelRequested)
        pickerBoard = nil
        onUpdateFinished = nil
    }

    private func renderFirmwareState(_ state: V3ServiceFirmwareUiState) {
        guard let board = pickerBoard, let onFinished = onUpdateFinished else { return }
        switch state {
        case .idle:
            break
        case .loading:
            presentingViewController?.showToast(SharedRes.strings().firmware_is_being_downloaded.desc().localized())
        case .files(let files):
            showFirmwareFiles(files, board: board, onFinished: onFinished)
        case .selected(let file):
            confirmAndRunUpdate(board: board, file: file, onFinished: onFinished)
        case .failed(let message, let files):
            showFirmwareFiles(files, board: board, onFinished: onFinished)
            presentingViewController?.showToast(message)
        }
    }

    private func showFirmwareFiles(_ files: [AccountFirmwareFile], board: AccountBridgeBoard,
                                   onFinished: @escaping () -> Void) {
        if viewModelV3 != nil, dialogPresenter.updateFirmwareFiles(files) { return }
        dialogPresenter.showFirmwareFiles(
            files: files,
            onSelect: { [weak self] file in
                guard let self else { return }
                if let viewModelV3 = self.viewModelV3 {
                    viewModelV3.onAction(.fileSelected(file))
                } else {
                    self.confirmAndRunUpdate(board: board, file: file, onFinished: onFinished)
                }
            },
            onDelete: { [weak self] file in
                guard file.isDeletable, let url = file.url else { return }
                try? FileManager.default.removeItem(at: url)
                self?.viewModelV3?.onAction(.fileDeleted(file))
            },
            onCancel: { [weak self] in self?.cancelCatalogRequest() }
        )
    }

    private func confirmAndRunUpdate(
        board: AccountBridgeBoard,
        file: AccountFirmwareFile,
        onFinished: @escaping () -> Void
    ) {
        dialogPresenter.showConfirmSendFirmwareFile { [weak self] in
            self?.runUpdate(board: board, file: file, onFinished: onFinished)
        }
    }

    private func runUpdate(
        board: AccountBridgeBoard,
        file: AccountFirmwareFile,
        onFinished: @escaping () -> Void
    ) {
        guard let url = file.url else { return }
        do {
            let archive = try FirmwareArchiveReader.readArchive(at: url)
            let progressDialog = dialogPresenter.showProgress()
            scheduleStalledProgressWarning()

            updateJob?.cancel(cause: nil)
            updateJob = FirmwareUpdateBridge.shared.runV3FirmwareUpdate(
                deviceAddress: board.deviceAddress,
                fileName: archive.fileName,
                descriptorText: archive.descriptorText,
                payload: archive.payload.kotlinByteArray()
            ) { [weak self, weak progressDialog] event in
                DispatchQueue.main.async {
                    guard let self else { return }
                    if event.kind == "progress" {
                        self.scheduleStalledProgressWarning()
                        progressDialog?.update(progress: Int(event.progress))
                        return
                    }

                    self.stalledProgressWorkItem?.cancel()
                    self.dialogPresenter.dismissCurrent(animated: true) {
                        if event.isSuccess {
                            self.presentingViewController?.showToast(FirmwareLocalizedText.updateInstalledMessage)
                            AccountBridge.shared.refreshBoardsAfterFirmwareUpdate(
                                deviceAddress: board.deviceAddress,
                                previousVersion: board.version
                            ) { _ in
                                DispatchQueue.main.async {
                                    onFinished()
                                }
                            }
                        } else {
                            self.dialogPresenter.showWarning(
                                title: FirmwareLocalizedText.loadingErrorTitle,
                                message: FirmwareLocalizedText.bridgeErrorMessage(event.message)
                            )
                        }
                    }
                }
            }
        } catch {
            dialogPresenter.showWarning(
                title: FirmwareLocalizedText.loadingErrorTitle,
                message: error.localizedDescription
            )
        }
    }

    private func scheduleStalledProgressWarning() {
        stalledProgressWorkItem?.cancel()
        let workItem = DispatchWorkItem { [weak self] in
            self?.dialogPresenter.showWarning(
                title: FirmwareLocalizedText.loadingErrorTitle,
                message: FirmwareLocalizedText.firmwareDownloadFailedMessage
            )
        }
        stalledProgressWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + 30, execute: workItem)
    }

    private func requirePresentingViewController() -> UIViewController {
        guard let presentingViewController else {
            fatalError("AccountFirmwareUpdateController presentingViewController is nil")
        }
        return presentingViewController
    }
}

enum FirmwareDocumentsDirectory {
    private static let firmwareFolderName = "Firmware"

    static func prepareSharedFolder() {
        LegacyDocumentsCompatibility.restoreNewAppDocumentsIfNeeded()
        guard let firmwareFolder = firmwareFolderURL else { return }
        try? FileManager.default.createDirectory(
            at: firmwareFolder,
            withIntermediateDirectories: true
        )
    }

    static func firmwareFiles() -> [AccountFirmwareFile] {
        guard let documentsURL else { return [] }
        let resourceKeys: [URLResourceKey] = [.isRegularFileKey, .isDirectoryKey]
        guard let enumerator = FileManager.default.enumerator(
            at: documentsURL,
            includingPropertiesForKeys: resourceKeys,
            options: [.skipsHiddenFiles]
        ) else {
            return []
        }

        return enumerator
            .compactMap { $0 as? URL }
            .filter { $0.pathExtension.lowercased() == "zip" }
            .map { AccountFirmwareFile(name: $0.lastPathComponent, url: $0, isDeletable: true) }
    }

    private static var documentsURL: URL? {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
    }

    private static var firmwareFolderURL: URL? {
        documentsURL?.appendingPathComponent(firmwareFolderName, isDirectory: true)
    }
}

private extension Data {
    func kotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (index, byte) in enumerated() {
            result.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return result
    }
}
