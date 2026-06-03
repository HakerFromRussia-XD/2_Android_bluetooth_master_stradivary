import UIKit
import ObjectiveC
import OldMotoricaStart

final class OldMotoricaStartLauncherAdapter {
    private let launcher = OldMotoricaStartLauncher()

    func makeRootViewController(for device: BLEDevice) -> UIViewController {
        LegacyDocumentsCompatibility.prepareDocumentsForLegacyFlow()
        LegacyAccessibilityMarkerBridge.installIfNeeded()
        let rootViewController = launcher.makeRootViewController(
            connectionHint: .init(
                deviceName: device.name,
                deviceUUID: device.uuid.uuidString
            )
        )
        rootViewController.view.accessibilityIdentifier = AccessibilityIdentifier.oldMotoricaStartRoot
        LegacyAccessibilityMarkerBridge.applyMarkers(to: rootViewController.view)
        return rootViewController
    }
}

private enum LegacyAccessibilityMarkerBridge {
    private static var isInstalled = false

    static func installIfNeeded() {
        guard !isInstalled,
              let originalMethod = class_getInstanceMethod(
                UIViewController.self,
                #selector(UIViewController.viewDidAppear(_:))
              ),
              let swizzledMethod = class_getInstanceMethod(
                UIViewController.self,
                #selector(UIViewController.motoricaLegacy_viewDidAppear(_:))
              ) else {
            return
        }

        method_exchangeImplementations(originalMethod, swizzledMethod)
        isInstalled = true
    }

    static func applyMarkersIfNeeded(for viewController: UIViewController) {
        guard Bundle(for: type(of: viewController)) == Bundle(for: OldMotoricaStartLauncher.self) else {
            return
        }

        applyMarkers(to: viewController.view)
    }

    static func applyMarkers(to view: UIView) {
        if (view.accessibilityIdentifier ?? "").isEmpty,
           let restorationIdentifier = view.restorationIdentifier,
           !restorationIdentifier.isEmpty {
            view.accessibilityIdentifier = restorationIdentifier
        }

        view.subviews.forEach(applyMarkers)
    }
}

private extension UIViewController {
    @objc dynamic func motoricaLegacy_viewDidAppear(_ animated: Bool) {
        motoricaLegacy_viewDidAppear(animated)
        LegacyAccessibilityMarkerBridge.applyMarkersIfNeeded(for: self)
    }
}

enum LegacyDocumentsCompatibility {
    private static let stashFolderName = "OldMotoricaStartHiddenDocuments"

    static func prepareDocumentsForLegacyFlow() {
        stashNonLegacyDocumentItemsIfNeeded()
    }

    static func restoreNewAppDocumentsIfNeeded() {
        restoreAllStashedDocumentItemsIfNeeded()
    }

    private static func stashNonLegacyDocumentItemsIfNeeded() {
        guard let documentsURL, let stashRootURL else {
            return
        }

        do {
            try FileManager.default.createDirectory(at: stashRootURL, withIntermediateDirectories: true)
            let contents = try FileManager.default.contentsOfDirectory(
                at: documentsURL,
                includingPropertiesForKeys: [.isDirectoryKey],
                options: []
            )

            for sourceURL in contents where !isLegacySaveObjectStringFile(sourceURL) {
                let destinationURL = stashRootURL.appendingPathComponent(sourceURL.lastPathComponent)
                try moveOrMergeItem(from: sourceURL, to: destinationURL)
            }
        } catch {
            print("LegacyDocumentsCompatibility stash failed: \(error)")
        }
    }

    private static func restoreAllStashedDocumentItemsIfNeeded() {
        guard let documentsURL, let stashRootURL,
              FileManager.default.fileExists(atPath: stashRootURL.path) else {
            return
        }

        do {
            try FileManager.default.createDirectory(at: documentsURL, withIntermediateDirectories: true)
            let contents = try FileManager.default.contentsOfDirectory(
                at: stashRootURL,
                includingPropertiesForKeys: [.isDirectoryKey],
                options: []
            )

            for sourceURL in contents {
                let destinationURL = documentsURL.appendingPathComponent(sourceURL.lastPathComponent)
                try moveOrMergeItem(from: sourceURL, to: destinationURL)
            }
        } catch {
            print("LegacyDocumentsCompatibility restore failed: \(error)")
        }
    }

    private static func isLegacySaveObjectStringFile(_ url: URL) -> Bool {
        var isDirectory: ObjCBool = false
        guard FileManager.default.fileExists(atPath: url.path, isDirectory: &isDirectory),
              !isDirectory.boolValue,
              let data = FileManager.default.contents(atPath: url.path) else {
            return false
        }

        return (try? JSONDecoder().decode(LegacySaveObjectStringProbe.self, from: data)) != nil
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

private struct LegacySaveObjectStringProbe: Decodable {
    let key: String
    let value: String
}
