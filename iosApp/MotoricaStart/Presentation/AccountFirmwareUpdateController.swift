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

    init(presentingViewController: UIViewController) {
        self.presentingViewController = presentingViewController
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
        dialogPresenter.showFirmwareFiles(
            files: availableFirmwareFiles(),
            onSelect: { [weak self] file in
                self?.confirmAndRunUpdate(board: board, file: file, onFinished: onFinished)
            },
            onDelete: { file in
                guard file.isDeletable else { return }
                try? FileManager.default.removeItem(at: file.url)
            }
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
        do {
            let archive = try FirmwareArchiveReader.readArchive(at: file.url)
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
