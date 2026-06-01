import UIKit
import OldMotoricaStart

final class OldMotoricaStartLauncherAdapter {
    private let launcher = OldMotoricaStartLauncher()

    func makeRootViewController(for device: BLEDevice) -> UIViewController {
        LegacyDocumentsCompatibility.prepareDocumentsForLegacyFlow()
        let rootViewController = launcher.makeRootViewController(
            connectionHint: .init(
                deviceName: device.name,
                deviceUUID: device.uuid.uuidString
            )
        )
        rootViewController.view.accessibilityIdentifier = AccessibilityIdentifier.oldMotoricaStartRoot
        return rootViewController
    }
}

enum LegacyDocumentsCompatibility {
    private static let firmwareFolderName = "Firmware"
    private static let stashFolderName = "OldMotoricaStartHiddenDocuments"

    static func prepareDocumentsForLegacyFlow() {
        stashDocumentItemIfNeeded(named: firmwareFolderName)
    }

    static func restoreNewAppDocumentsIfNeeded() {
        restoreDocumentItemIfNeeded(named: firmwareFolderName)
    }

    private static func stashDocumentItemIfNeeded(named itemName: String) {
        guard let sourceURL = documentsURL?.appendingPathComponent(itemName),
              let stashRootURL,
              FileManager.default.fileExists(atPath: sourceURL.path) else {
            return
        }

        let destinationURL = stashRootURL.appendingPathComponent(itemName)
        do {
            try FileManager.default.createDirectory(at: stashRootURL, withIntermediateDirectories: true)
            try moveOrMergeItem(from: sourceURL, to: destinationURL)
        } catch {
            print("LegacyDocumentsCompatibility stash failed: \(error)")
        }
    }

    private static func restoreDocumentItemIfNeeded(named itemName: String) {
        guard let sourceURL = stashRootURL?.appendingPathComponent(itemName),
              let destinationURL = documentsURL?.appendingPathComponent(itemName),
              FileManager.default.fileExists(atPath: sourceURL.path) else {
            return
        }

        do {
            try moveOrMergeItem(from: sourceURL, to: destinationURL)
        } catch {
            print("LegacyDocumentsCompatibility restore failed: \(error)")
        }
    }

    private static func moveOrMergeItem(from sourceURL: URL, to destinationURL: URL) throws {
        let fileManager = FileManager.default
        var sourceIsDirectory: ObjCBool = false
        guard fileManager.fileExists(atPath: sourceURL.path, isDirectory: &sourceIsDirectory) else { return }

        if !fileManager.fileExists(atPath: destinationURL.path) {
            try fileManager.moveItem(at: sourceURL, to: destinationURL)
            return
        }

        guard sourceIsDirectory.boolValue else {
            let uniqueDestinationURL = nonCollidingURL(for: destinationURL)
            try fileManager.moveItem(at: sourceURL, to: uniqueDestinationURL)
            return
        }

        try fileManager.createDirectory(at: destinationURL, withIntermediateDirectories: true)
        let contents = try fileManager.contentsOfDirectory(
            at: sourceURL,
            includingPropertiesForKeys: [.isDirectoryKey],
            options: []
        )

        for childURL in contents {
            try moveOrMergeItem(
                from: childURL,
                to: destinationURL.appendingPathComponent(childURL.lastPathComponent)
            )
        }

        try fileManager.removeItem(at: sourceURL)
    }

    private static func nonCollidingURL(for originalURL: URL) -> URL {
        let fileManager = FileManager.default
        guard fileManager.fileExists(atPath: originalURL.path) else { return originalURL }

        let directoryURL = originalURL.deletingLastPathComponent()
        let baseName = originalURL.deletingPathExtension().lastPathComponent
        let fileExtension = originalURL.pathExtension

        for index in 1...999 {
            let candidateName = fileExtension.isEmpty
                ? "\(baseName)-legacy-\(index)"
                : "\(baseName)-legacy-\(index).\(fileExtension)"
            let candidateURL = directoryURL.appendingPathComponent(candidateName)
            if !fileManager.fileExists(atPath: candidateURL.path) {
                return candidateURL
            }
        }

        let fallbackName = fileExtension.isEmpty
            ? "\(baseName)-legacy-\(UUID().uuidString)"
            : "\(baseName)-legacy-\(UUID().uuidString).\(fileExtension)"
        return directoryURL.appendingPathComponent(fallbackName)
    }

    private static var documentsURL: URL? {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
    }

    private static var stashRootURL: URL? {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first?
            .appendingPathComponent(stashFolderName, isDirectory: true)
    }
}
