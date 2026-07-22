import UIKit
import QuartzCore
import shared

@objcMembers
final class V3ModelTestSceneConfiguration: NSObject {
    static var isActive = false
    private static let temporaryOpenV3ModelFirstScreen = false

    static func shouldOpen(
        arguments: [String] = ProcessInfo.processInfo.arguments,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> Bool {
#if DEBUG
        if arguments.contains("-v3-model-test-scene") {
            return true
        }
        if arguments.contains("-disable-v3-model-test-scene") ||
            arguments.contains(where: { $0.hasPrefix("-ui-test-") }) ||
            environment["XCTestConfigurationFilePath"] != nil {
            return false
        }
        return temporaryOpenV3ModelFirstScreen
#else
        return false
#endif
    }
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    let appDIContainer = AppDIContainer()
    private var appFlowCoordinator: AppFlowCoordinator?
    var window: UIWindow?
    private weak var statusBarOverlayView: UIView?
    private var v3TestSceneStartedAt: CFTimeInterval?
    private var v3FirstFrameObserver: NSObjectProtocol?
    
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        NSLog("[startup] didFinishLaunching begin")
        if V3ModelTestSceneConfiguration.shouldOpen() {
            startV3ModelTestScene()
            return true
        }

        let navigationController = StatusBarNavigationController()
        let ubi4BackgroundColor = UIColor(named: "ubi4_back") ?? UIColor.black
        navigationController.view.backgroundColor = ubi4BackgroundColor
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.backgroundColor = ubi4BackgroundColor
        window?.rootViewController = navigationController
        window?.makeKeyAndVisible()
        updateStatusBarOverlay(backgroundColor: ubi4BackgroundColor)
        DispatchQueue.main.async { [weak self] in
            guard let self, let backgroundColor = self.window?.backgroundColor else { return }
            self.updateStatusBarOverlay(backgroundColor: backgroundColor)
        }
        AppAppearance.setupAppearance()
        appFlowCoordinator = AppFlowCoordinator(
            navigationController: navigationController,
            appDIContainer: appDIContainer
        )
        appFlowCoordinator?.start()
        NSLog("[startup] appFlowCoordinator started")

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            NSLog("[startup] shared bootstrap begin")
            SharedBootstrapper.shared.initialize()
            FirmwareDocumentsDirectory.prepareSharedFolder()
            SmartConnectionSettingsStore().resetScanAutoConnectionDeactivationForLaunch()
            BleLogSettings.syncSharedStore()
            NSLog("[startup] shared bootstrap finished")

            DispatchQueue.main.async {
                guard let self else { return }
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
                    NSLog("[startup] legacy bridges start")
                    LegacyBleCommandBridge.startIfNeeded()
                    LegacyBleCommandProbe.startIfNeeded {
                        self?.window
                    }
                }
            }
        }
        return true
    }

    private func startV3ModelTestScene() {
        let startedAt = CACurrentMediaTime()
        v3TestSceneStartedAt = startedAt
        V3ModelTestSceneConfiguration.isActive = true
        AppAppearance.setupAppearance()

        let backgroundColor = UIColor(named: "ubi4_back") ?? UIColor.black
        let loadingController = UIViewController()
        loadingController.view.backgroundColor = backgroundColor
        loadingController.view.accessibilityIdentifier = "AccessibilityIdentifierV3ModelTestLoading"

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.backgroundColor = backgroundColor
        window?.rootViewController = loadingController
        window?.makeKeyAndVisible()

        v3FirstFrameObserver = NotificationCenter.default.addObserver(
            forName: .V3ModelFirstFramePresented,
            object: V3ModelResourceCache.shared(),
            queue: .main
        ) { [weak self] _ in
            guard let self, let startedAt = self.v3TestSceneStartedAt else { return }
            let totalMs = (CACurrentMediaTime() - startedAt) * 1_000.0
            let metrics = V3ModelResourceCache.shared().latestMetrics
            NSLog(
                "[V3TestMetrics] firstPresentedFrame launchToFirstFrameMs=%.3f preloadMs=%.3f cpuDecodeMs=%.3f deformationMs=%.3f shaderMs=%.3f textureMs=%.3f gpuBuffersMs=%.3f astc=%d",
                totalMs,
                metrics["totalMs"]?.doubleValue ?? -1.0,
                metrics["cpuDecodeMs"]?.doubleValue ?? -1.0,
                metrics["deformationPreparationMs"]?.doubleValue ?? -1.0,
                metrics["shaderCompilationMs"]?.doubleValue ?? -1.0,
                metrics["textureUploadMs"]?.doubleValue ?? -1.0,
                metrics["gpuBuffersMs"]?.doubleValue ?? -1.0,
                metrics["astc"]?.boolValue == true
            )
            self.v3TestSceneStartedAt = nil
            if let observer = self.v3FirstFrameObserver {
                NotificationCenter.default.removeObserver(observer)
                self.v3FirstFrameObserver = nil
            }
        }

        let cache = V3ModelResourceCache.shared()
        cache.mark3DOpenRequested()
        NSLog("[V3TestMetrics] launch begin")
        cache.preload { [weak self] ready, error in
            guard let self else { return }
            let elapsedMs = (CACurrentMediaTime() - startedAt) * 1_000.0
            NSLog(
                "[V3TestMetrics] preloadCallback launchToPreloadReadyMs=%.3f ready=%d error=%@",
                elapsedMs,
                ready,
                error?.localizedDescription ?? "none"
            )
            guard ready else {
                self.showV3ModelTestFailure(error)
                return
            }

            let storyboard = UIStoryboard(name: "WidgetsListViewController", bundle: .main)
            guard let controller = storyboard.instantiateViewController(
                withIdentifier: "VCG_3D"
            ) as? AAPLOpenGLViewControllerV3 else {
                self.showV3ModelTestFailure(nil)
                return
            }
            controller.useV3Mode = true
            controller.modelTestMode = true
            controller.gestureNumber = 0
            self.window?.rootViewController = controller
            NSLog(
                "[V3TestMetrics] controllerInstalled launchToControllerMs=%.3f",
                (CACurrentMediaTime() - startedAt) * 1_000.0
            )
        }
    }

    private func showV3ModelTestFailure(_ error: Error?) {
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.numberOfLines = 0
        label.textAlignment = .center
        label.textColor = .white
        label.text = "Не удалось загрузить V3 3D модель\n\(error?.localizedDescription ?? "Неизвестная ошибка")"
        window?.rootViewController?.view.addSubview(label)
        if let rootView = window?.rootViewController?.view {
            NSLayoutConstraint.activate([
                label.leadingAnchor.constraint(equalTo: rootView.leadingAnchor, constant: 24),
                label.trailingAnchor.constraint(equalTo: rootView.trailingAnchor, constant: -24),
                label.centerYAnchor.constraint(equalTo: rootView.centerYAnchor)
            ])
        }
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        CoreDataStorage.shared.saveContext()
    }

    func applicationWillTerminate(_ application: UIApplication) {
        GameControlBroadcaster.shared.stop()
    }

    func updateStatusBarOverlay(backgroundColor: UIColor) {
        guard let window else { return }
        window.backgroundColor = backgroundColor

        // On Dynamic Island devices statusBarFrame can be shorter than safeAreaInsets.top.
        // We fill the whole top safe area to avoid a visible seam.
        let statusBarHeight = window.windowScene?.statusBarManager?.statusBarFrame.height ?? 0
        let overlayHeight = max(window.safeAreaInsets.top, statusBarHeight)
        guard overlayHeight > 0 else { return }

        let overlay: UIView
        if let existingOverlay = statusBarOverlayView {
            overlay = existingOverlay
        } else {
            overlay = UIView()
            overlay.isUserInteractionEnabled = false
            overlay.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
            window.addSubview(overlay)
            statusBarOverlayView = overlay
        }

        overlay.frame = CGRect(x: 0, y: 0, width: window.bounds.width, height: overlayHeight)
        overlay.backgroundColor = backgroundColor
    }
}
