import Foundation
import shared
import UIKit

final class AccountFirmwareUpdateController {
    private weak var presentingViewController: UIViewController?
    private lazy var dialogPresenter = AccountFirmwareDialogPresenter(
        presentingViewController: requirePresentingViewController()
    )
    private var updateJob: Kotlinx_coroutines_coreJob?
    private var stalledProgressWorkItem: DispatchWorkItem?
    private let catalog = AccountFirmwareCatalogBridge()
    private var catalogJob: Kotlinx_coroutines_coreJob?
    private var catalogRequestID: UUID?
    private var catalogCallbacks: [() -> Void] = []
    private var downloadJob: Kotlinx_coroutines_coreJob?
    private var pickerID: UUID?

    init(presentingViewController: UIViewController) {
        self.presentingViewController = presentingViewController
    }

    deinit {
        catalogJob?.cancel(cause: nil)
        downloadJob?.cancel(cause: nil)
        updateJob?.cancel(cause: nil)
        stalledProgressWorkItem?.cancel()
    }

    func refreshCatalog(onUpdated: @escaping () -> Void) {
        catalogCallbacks.append(onUpdated)
        guard catalogRequestID == nil else { return }
        let requestID = UUID()
        catalogRequestID = requestID
        catalogJob = catalog.refresh { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, self.catalogRequestID == requestID else { return }
                self.catalogRequestID = nil
                self.catalogJob = nil
                let callbacks = self.catalogCallbacks
                self.catalogCallbacks.removeAll()
                callbacks.forEach { $0() }
            }
        }
    }

    func isUpdateAvailable(for board: AccountBridgeBoard) -> Bool {
        catalog.isUpdateAvailable(address: board.deviceAddress, version: board.version)
    }

    func showFirmwarePicker(for board: AccountBridgeBoard, onFinished: @escaping () -> Void) {
        guard UserFirmwareRoleAccess.canUpdateFirmware, pickerID == nil, updateJob == nil else { return }
        let requestID = UUID()
        pickerID = requestID
        let files = remoteFiles(for: board)
        dialogPresenter.showFirmwareFiles(
            files: files,
            onSelect: { [weak self] file in
                guard let self, let remoteFile = file.remoteFile,
                      self.pickerID == requestID, UserFirmwareRoleAccess.canUpdateFirmware else { return }
                self.downloadSelectedFile(remoteFile, board: board, pickerID: requestID, onFinished: onFinished)
            },
            onDelete: { _ in },
            onCancel: { [weak self] in
                self?.cancelSelection()
            }
        )
        if catalog.isLoaded() {
            dialogPresenter.setFirmwareFilesLoading(false, message: files.isEmpty ? FirmwareLocalizedText.noFirmwareFilesMessage : nil)
            return
        }
        dialogPresenter.setFirmwareFilesLoading(true, message: FirmwareLocalizedText.catalogLoadingMessage)
        refreshCatalog { [weak self] in
            guard let self, self.pickerID == requestID else { return }
            let files = self.remoteFiles(for: board)
            self.dialogPresenter.updateFirmwareFiles(files)
            let message = self.catalog.isLoaded()
                ? (files.isEmpty ? FirmwareLocalizedText.noFirmwareFilesMessage : nil)
                : FirmwareLocalizedText.catalogLoadFailedMessage
            self.dialogPresenter.setFirmwareFilesLoading(false, message: message)
        }
    }

    func cancelPendingDownloads() {
        catalogRequestID = nil
        catalogJob?.cancel(cause: nil)
        catalogJob = nil
        catalogCallbacks.removeAll()
        let hadPicker = pickerID != nil
        cancelSelection()
        if hadPicker {
            dialogPresenter.dismissCurrent(animated: false)
        }
    }

    private func cancelSelection() {
        pickerID = nil
        downloadJob?.cancel(cause: nil)
        downloadJob = nil
    }

    private func remoteFiles(for board: AccountBridgeBoard) -> [AccountFirmwareFile] {
        catalog.filesForBoard(address: board.deviceAddress).map { AccountFirmwareFile(remoteFile: $0) }
    }

    private func downloadSelectedFile(
        _ file: RemoteFirmwareFile,
        board: AccountBridgeBoard,
        pickerID requestID: UUID,
        onFinished: @escaping () -> Void
    ) {
        guard downloadJob == nil else { return }
        guard let cacheURL = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else {
            dialogPresenter.setFirmwareFilesLoading(false, message: FirmwareLocalizedText.archiveDownloadFailedMessage)
            return
        }
        dialogPresenter.setFirmwareFilesLoading(
            true,
            message: "\(FirmwareLocalizedText.archiveDownloadingMessage)\n\(file.name)"
        )
        // Only the explicitly selected catalog entry is downloaded.
        downloadJob = catalog.download(file: file, cachePath: cacheURL.path) { [weak self] path in
            DispatchQueue.main.async {
                guard let self, self.pickerID == requestID,
                      UserFirmwareRoleAccess.canUpdateFirmware else { return }
                self.downloadJob = nil
                guard let path else {
                    self.dialogPresenter.setFirmwareFilesLoading(
                        false,
                        message: "\(file.name)\n\(FirmwareLocalizedText.archiveDownloadFailedMessage)"
                    )
                    return
                }
                let downloadedFile = AccountFirmwareFile(name: file.name, url: URL(fileURLWithPath: path), isDeletable: false)
                self.dialogPresenter.showConfirmSendFirmwareFile(
                    onConfirm: { [weak self] in
                        guard let self, self.pickerID == requestID,
                              UserFirmwareRoleAccess.canUpdateFirmware else { return }
                        self.pickerID = nil
                        self.runUpdate(board: board, file: downloadedFile, onFinished: onFinished)
                    },
                    onCancel: { [weak self] in self?.cancelSelection() }
                )
            }
        }
    }

    private func runUpdate(
        board: AccountBridgeBoard,
        file: AccountFirmwareFile,
        onFinished: @escaping () -> Void
    ) {
        guard let fileURL = file.url else { return }
        do {
            let archive = try FirmwareArchiveReader.readArchive(at: fileURL)
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

                    self.updateJob = nil
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
