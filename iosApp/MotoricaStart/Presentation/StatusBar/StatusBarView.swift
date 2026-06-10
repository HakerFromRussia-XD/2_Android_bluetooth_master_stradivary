//
//  StatusBarView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.01.2026.
//

import SwiftUI
import Combine
import shared
#if canImport(Lottie)
import Lottie
#endif

struct StatusBarView: View {
    enum Constants {
        static let height: CGFloat = 44
        static let iconSize: CGFloat = 22
        static let leadingButtonSize: CGFloat = 34
        static let leadingIconSize: CGFloat = 24
        static let statusIndicatorVisualSize: CGFloat = 14
        static let statusIndicatorRenderSize: CGFloat = 14
        static let batteryRingSize: CGFloat = 28
    }

    enum LeadingButton {
        case account
        case back
    }

    @ObservedObject var viewModel: StatusBarViewModel
    var leadingButton: LeadingButton = .account
    var isDisconnectActionEnabled = true
    var onAccountTap: (() -> Void)?
    var onAccountLongPress: (() -> Void)?
    var onHelpTap: (() -> Void)?
    var onBackTap: (() -> Void)?
    var onDisconnectConfirmed: (() -> Void)?
    @State private var isDisconnectDialogPresented = false
    @State private var isDisconnectDialogVisible = false
    @State private var disconnectDialogDismissWorkItem: DispatchWorkItem?
    private let dialogAnimationDuration: TimeInterval = 0.22

    var body: some View {
        HStack(spacing: 12) {
            HStack(spacing: 6) {
                leadingButtonControl

                if leadingButton == .account, onHelpTap != nil {
                    Button {
                        onHelpTap?()
                    } label: {
                        statusBarButtonBackground {
                            Ubi4QuestionIcon()
                                .stroke(
                                    Color("ubi4_white"),
                                    style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round)
                                )
                                .frame(width: Constants.leadingIconSize, height: Constants.leadingIconSize)
                        }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text(SharedRes.strings().help.desc().localized()))
                    .accessibilityIdentifier(AccessibilityIdentifier.statusBarHelpButton)
                }
            }

            Spacer()

            HStack(spacing: 8) {
                Button {
                    guard isDisconnectActionEnabled else { return }
                    guard viewModel.serialNumber != "—", !viewModel.serialNumber.isEmpty else { return }
                    presentDisconnectDialog()
                } label: {
                    Text(viewModel.serialNumber)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Color("ubi4_white"))
                        .lineLimit(1)
                    }
                .buttonStyle(.plain)
                .accessibilityIdentifier(AccessibilityIdentifier.statusBarDeviceNameButton)
                .accessibilityLabel(Text(SharedLocalizedText.text(SharedRes.strings().device_serial_number)))
                .accessibilityValue(Text(viewModel.serialNumber))

                ConnectionStatusIndicatorView(isConnected: viewModel.isConnected)
                    .frame(width: Constants.statusIndicatorRenderSize, height: Constants.statusIndicatorRenderSize)
                    .scaleEffect(0.03)
                    .frame(width: Constants.statusIndicatorVisualSize, height: Constants.statusIndicatorVisualSize)
                    .accessibilityIdentifier(AccessibilityIdentifier.statusBarConnectionIndicator)
                    .accessibilityLabel(Text(viewModel.isConnected ? SharedLocalizedText.text(SharedRes.strings().connection_established) : SharedLocalizedText.text(SharedRes.strings().connection_lost)))
            }

            Spacer()

            BatteryRingView(level: viewModel.batteryLevel)
                .frame(width: Constants.batteryRingSize, height: Constants.batteryRingSize)
                .accessibilityLabel(Text(SharedLocalizedText.text(SharedRes.strings().charge_level)))
        }
        .padding(.horizontal, 16)
        .frame(height: Constants.height)
        .background(Color("ubi4_back"))
        .fullScreenCover(isPresented: $isDisconnectDialogPresented) {
            StatusBarDisconnectDialogOverlay(
                isVisible: $isDisconnectDialogVisible,
                title: SharedRes.strings().disconnection_from_the_device.desc().localized(),
                message: SharedRes.strings().are_you_sure_you_want_to_disconnect_from_your_device_and_go_to_the_scan_screen.desc().localized(),
                confirmTitle: SharedRes.strings().ok.desc().localized(),
                cancelTitle: SharedRes.strings().cancel.desc().localized(),
                onConfirm: {
                    confirmDisconnectImmediately()
                },
                onCancel: {
                    dismissDisconnectDialog()
                }
            )
            .interactiveDismissDisabled()
            .background(ClearFullScreenBackgroundView())
        }
    }

    @ViewBuilder
    private var leadingButtonImage: some View {
        switch leadingButton {
        case .account:
            Ubi4AccountIcon()
                .stroke(
                    Color("ubi4_white"),
                    style: StrokeStyle(lineWidth: 1.4, lineCap: .round, lineJoin: .round)
                )
                .frame(width: Constants.leadingIconSize, height: Constants.leadingIconSize)
        case .back:
            Image("ic_arrow_left")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color("ubi4_white"))
                .frame(width: 22, height: 22)
        }
    }

    private func statusBarButtonBackground<Icon: View>(@ViewBuilder icon: () -> Icon) -> some View {
        ZStack {
            Circle()
                .fill(Color("ubi4_gray"))
                .overlay(
                    Circle()
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
            icon()
        }
        .frame(width: Constants.leadingButtonSize, height: Constants.leadingButtonSize)
    }

    @ViewBuilder
    private var leadingButtonControl: some View {
        switch leadingButton {
        case .account:
            statusBarButtonBackground {
                leadingButtonImage
            }
            .contentShape(Circle())
            .onTapGesture {
                onAccountTap?()
            }
            .onLongPressGesture(minimumDuration: 0.8) {
                onAccountLongPress?()
            }
            .accessibilityAddTraits(.isButton)
            .accessibilityLabel(Text(SharedLocalizedText.text(SharedRes.strings().account)))
            .accessibilityIdentifier(AccessibilityIdentifier.statusBarAccountButton)
        case .back:
            Button {
                onBackTap?()
            } label: {
                statusBarButtonBackground {
                    leadingButtonImage
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(SharedLocalizedText.text(SharedRes.strings().back)))
            .accessibilityIdentifier("AccessibilityIdentifierStatusBarBackButton")
        }
    }

    private func presentDisconnectDialog() {
        disconnectDialogDismissWorkItem?.cancel()
        isDisconnectDialogVisible = false
        setDisconnectDialogPresentedWithoutSystemAnimation {
            isDisconnectDialogPresented = true
        }
        DispatchQueue.main.async {
            withAnimation(.easeInOut(duration: dialogAnimationDuration)) {
                isDisconnectDialogVisible = true
            }
        }
    }

    private func dismissDisconnectDialog(onDismissed: (() -> Void)? = nil) {
        guard isDisconnectDialogPresented else { return }
        withAnimation(.easeInOut(duration: dialogAnimationDuration)) {
            isDisconnectDialogVisible = false
        }
        disconnectDialogDismissWorkItem?.cancel()
        let workItem = DispatchWorkItem {
            setDisconnectDialogPresentedWithoutSystemAnimation {
                isDisconnectDialogPresented = false
            }
            disconnectDialogDismissWorkItem = nil
            onDismissed?()
        }
        disconnectDialogDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + dialogAnimationDuration,
            execute: workItem
        )
    }

    private func confirmDisconnectImmediately() {
        disconnectDialogDismissWorkItem?.cancel()
        disconnectDialogDismissWorkItem = nil
        onDisconnectConfirmed?()
        isDisconnectDialogVisible = false
        setDisconnectDialogPresentedWithoutSystemAnimation {
            isDisconnectDialogPresented = false
        }
    }

    private func setDisconnectDialogPresentedWithoutSystemAnimation(_ updates: () -> Void) {
        var transaction = Transaction(animation: nil)
        transaction.disablesAnimations = true
        withTransaction(transaction) {
            updates()
        }
    }
}

private struct Ubi4AccountIcon: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let scale = min(rect.width, rect.height) / 24
        let origin = CGPoint(
            x: rect.midX - 12 * scale,
            y: rect.midY - 12 * scale
        )
        func point(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
            CGPoint(x: origin.x + x * scale, y: origin.y + y * scale)
        }

        path.move(to: point(4, 21))
        path.addLine(to: point(4, 18.5))
        path.addCurve(to: point(9.5, 13), control1: point(4, 15.462), control2: point(6.462, 13))
        path.addLine(to: point(14.5, 13))
        path.addCurve(to: point(20, 18.5), control1: point(17.538, 13), control2: point(20, 15.462))
        path.addLine(to: point(20, 21))

        path.move(to: point(8, 21))
        path.addLine(to: point(8, 18))
        path.move(to: point(16, 21))
        path.addLine(to: point(16, 18))

        path.move(to: point(16, 6.5))
        path.addCurve(to: point(12, 10.5), control1: point(16, 8.709), control2: point(14.209, 10.5))
        path.addCurve(to: point(8, 6.5), control1: point(9.791, 10.5), control2: point(8, 8.709))
        path.addCurve(to: point(12, 2.5), control1: point(8, 4.291), control2: point(9.791, 2.5))
        path.addCurve(to: point(16, 6.5), control1: point(14.209, 2.5), control2: point(16, 4.291))
        return path
    }
}

private struct Ubi4QuestionIcon: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let scale = min(rect.width, rect.height) / 24
        let origin = CGPoint(
            x: rect.midX - 12 * scale,
            y: rect.midY - 12 * scale
        )
        func point(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
            CGPoint(x: origin.x + x * scale, y: origin.y + y * scale)
        }

        path.move(to: point(12, 19))
        path.addLine(to: point(12.01, 19))
        path.move(to: point(8.217, 7.697))
        path.addCurve(to: point(12, 5), control1: point(8.758, 6.128), control2: point(10.247, 5))
        path.addCurve(to: point(16, 9), control1: point(14.209, 5), control2: point(16, 6.791))
        path.addCurve(to: point(13.558, 12.685), control1: point(16, 10.656), control2: point(14.993, 12.078))
        path.addCurve(to: point(12.317, 13.277), control1: point(12.817, 12.999), control2: point(12.447, 13.156))
        path.addCurve(to: point(12.061, 13.663), control1: point(12.163, 13.421), control2: point(12.134, 13.465))
        path.addCurve(to: point(12, 14.6), control1: point(12, 13.83), control2: point(12, 14.087))
        path.addLine(to: point(12, 16))
        return path
    }
}

private struct StatusBarDisconnectDialogOverlay: View {
    @Binding var isVisible: Bool
    let title: String
    let message: String
    let confirmTitle: String
    let cancelTitle: String
    let onConfirm: () -> Void
    let onCancel: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()

            StatusBarDisconnectDialog(
                title: title,
                message: message,
                confirmTitle: confirmTitle,
                cancelTitle: cancelTitle,
                onConfirm: onConfirm,
                onCancel: onCancel
            )
            .padding(.horizontal, 8)
        }
        .opacity(isVisible ? 1 : 0)
    }
}

private struct ClearFullScreenBackgroundView: UIViewRepresentable {
    final class ClearBackgroundHostView: UIView {
        override func didMoveToWindow() {
            super.didMoveToWindow()
            applyClearBackgrounds()
            DispatchQueue.main.async { [weak self] in
                self?.applyClearBackgrounds()
            }
        }

        private func applyClearBackgrounds() {
            backgroundColor = .clear
            isOpaque = false

            var current: UIView? = self
            while let view = current {
                view.backgroundColor = .clear
                view.isOpaque = false
                current = view.superview
            }
        }
    }

    func makeUIView(context: Context) -> UIView {
        ClearBackgroundHostView()
    }

    func updateUIView(_ uiView: UIView, context: Context) { }
}

private struct StatusBarDisconnectDialog: View {
    let title: String
    let message: String
    let confirmTitle: String
    let cancelTitle: String
    let onConfirm: () -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack {
            Spacer()
            dialogContent
                .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea()
    }

    @ViewBuilder
    private var dialogContent: some View {
        VStack(spacing: 16) {
            Text(title)
                .font(.custom("SFProText-Bold", size: 18))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.horizontal, 16)

            Text(message)
                .font(.system(size: 12, weight: .regular))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)

            VStack(spacing: 0) {
                Rectangle()
                    .fill(Color("ubi4_gray_border"))
                    .frame(height: 1)

                Button(action: onConfirm) {
                    Text(confirmTitle)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color("ubi4_yes_system_blue"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier(AccessibilityIdentifier.statusBarDisconnectConfirmButton)

                Rectangle()
                    .fill(Color("ubi4_gray_border"))
                    .frame(height: 1)

                Button(action: onCancel) {
                    Text(cancelTitle)
                        .font(.system(size: 16, weight: .light))
                        .foregroundColor(Color("ubi4_yes_system_blue"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier(AccessibilityIdentifier.statusBarDisconnectCancelButton)
            }
        }
        .padding(.top, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color("ubi4_back"))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.35), radius: 12, x: 0, y: 8)
        )
    }
}

private struct ConnectionStatusIndicatorView: View {
    let isConnected: Bool

    var body: some View {
        #if canImport(Lottie)
        ConnectionStatusLottieView(isConnected: isConnected)
        #else
        Image(isConnected ? "connect_status" : "disconnect_status")
            .resizable()
            .scaledToFit()
            .accessibilityIdentifier(AccessibilityIdentifier.statusBarConnectionIndicator)
            .accessibilityValue(Text(isConnected ? "connected" : "disconnected"))
        #endif
    }
}

@objcMembers
final class StatusBarConnectionIndicatorHostView: UIView {
    private var isConnected: Bool
    private var hostingController: UIHostingController<StatusBarConnectionIndicatorBridgeView>?
    private var connectionStateCancellable: AnyCancellable?

    override init(frame: CGRect) {
        isConnected = WidgetsTabContainerViewController.sharedStatusBarViewModel.isConnected
        super.init(frame: frame)
        configure()
    }

    required init?(coder: NSCoder) {
        isConnected = WidgetsTabContainerViewController.sharedStatusBarViewModel.isConnected
        super.init(coder: coder)
        configure()
    }

    @objc(updateConnected:)
    func updateConnected(_ connected: Bool) {
        guard isConnected != connected else { return }
        isConnected = connected
        hostingController?.rootView = makeRootView(isConnected: connected)
        updateAccessibility(isConnected: connected)
    }

    private func configure() {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = .clear
        isOpaque = false
        clipsToBounds = false
        isAccessibilityElement = true

        let hostingController = UIHostingController(rootView: makeRootView(isConnected: isConnected))
        self.hostingController = hostingController
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        hostingController.view.backgroundColor = .clear
        hostingController.view.isOpaque = false
        addSubview(hostingController.view)

        NSLayoutConstraint.activate([
            hostingController.view.leadingAnchor.constraint(equalTo: leadingAnchor),
            hostingController.view.trailingAnchor.constraint(equalTo: trailingAnchor),
            hostingController.view.topAnchor.constraint(equalTo: topAnchor),
            hostingController.view.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        updateAccessibility(isConnected: isConnected)
        connectionStateCancellable = WidgetsTabContainerViewController.sharedStatusBarViewModel.$isConnected
            .receive(on: RunLoop.main)
            .sink { [weak self] isConnected in
                self?.updateConnected(isConnected)
            }
    }

    private func makeRootView(isConnected: Bool) -> StatusBarConnectionIndicatorBridgeView {
        StatusBarConnectionIndicatorBridgeView(isConnected: isConnected)
    }

    private func updateAccessibility(isConnected: Bool) {
        accessibilityIdentifier = AccessibilityIdentifier.statusBarConnectionIndicator
        accessibilityLabel = isConnected ? SharedLocalizedText.text(SharedRes.strings().connection_established) : SharedLocalizedText.text(SharedRes.strings().connection_lost)
        accessibilityValue = isConnected ? "connected" : "disconnected"
    }
}

private struct StatusBarConnectionIndicatorBridgeView: View {
    let isConnected: Bool

    var body: some View {
        let indicator = ConnectionStatusIndicatorView(isConnected: isConnected)
            .frame(
                width: StatusBarView.Constants.statusIndicatorRenderSize,
                height: StatusBarView.Constants.statusIndicatorRenderSize
            )

        #if canImport(Lottie)
        indicator
            .scaleEffect(0.03)
            .frame(
                width: StatusBarView.Constants.statusIndicatorVisualSize,
                height: StatusBarView.Constants.statusIndicatorVisualSize
            )
        #else
        indicator
            .frame(
                width: StatusBarView.Constants.statusIndicatorVisualSize,
                height: StatusBarView.Constants.statusIndicatorVisualSize
            )
        #endif
    }
}

#if canImport(Lottie)
private struct ConnectionStatusLottieView: UIViewRepresentable {
    private enum AnimationAsset: String {
        case disconnectToConnect = "disconnect_to_connect"
        case connectToDisconnect = "connect_to_disconnect"
        case reconnect = "reconnect"
    }

    private enum RuntimePhase {
        case connectedStatic
        case animated(
            asset: AnimationAsset,
            startedAt: CFTimeInterval,
            loop: Bool,
            holdLastFrame: Bool
        )
    }

    private final class RuntimeStore {
        var lastConnectionState: Bool?
        var phase: RuntimePhase = .connectedStatic
    }

    private struct CachedAnimation {
        let animation: LottieAnimation
        let imageProvider: AnimationImageProvider?
    }

    private final class AnimationCacheStore {
        var byAsset: [AnimationAsset: CachedAnimation] = [:]
    }

    private static let runtime = RuntimeStore()
    private static let animationCache = AnimationCacheStore()

    final class Coordinator {
        var requestToken = 0
        var lastRenderedSignature: String?
        weak var animationView: LottieAnimationView?
    }

    let isConnected: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> LottieAnimationView {
        let animationView = LottieAnimationView(configuration: .shared)
        animationView.contentMode = .scaleAspectFit
        animationView.clipsToBounds = true
        animationView.backgroundBehavior = .pauseAndRestore
        animationView.mainThreadRenderingEngineShouldForceDisplayUpdateOnEachFrame = true
        animationView.animationSpeed = 1
        animationView.isAccessibilityElement = true
        animationView.accessibilityIdentifier = AccessibilityIdentifier.statusBarConnectionIndicator
        context.coordinator.animationView = animationView
        return animationView
    }

    func updateUIView(_ uiView: LottieAnimationView, context: Context) {
        uiView.contentMode = .scaleAspectFit
        uiView.setNeedsLayout()
        uiView.layoutIfNeeded()
        context.coordinator.animationView = uiView
        applyState(on: uiView, coordinator: context.coordinator)
    }

    private func applyState(on view: LottieAnimationView, coordinator: Coordinator) {
        let previousState = Self.runtime.lastConnectionState
        Self.runtime.lastConnectionState = isConnected

        if previousState == nil {
            if isConnected {
                Self.runtime.phase = .connectedStatic
            } else if case .connectedStatic = Self.runtime.phase {
                Self.runtime.phase = .animated(
                    asset: .reconnect,
                    startedAt: CACurrentMediaTime(),
                    loop: true,
                    holdLastFrame: false
                )
            }
            applyRuntimeState(on: view, coordinator: coordinator)
            return
        }

        if previousState != isConnected {
            if isConnected {
                transitionToConnected(on: view, coordinator: coordinator)
            } else {
                transitionToDisconnected(on: view, coordinator: coordinator)
            }
            return
        }

        applyRuntimeState(on: view, coordinator: coordinator)
    }

    private func transitionToConnected(on view: LottieAnimationView, coordinator: Coordinator) {
        let startTime = CACurrentMediaTime()
        play(
            .disconnectToConnect,
            on: view,
            coordinator: coordinator,
            loop: false,
            holdLastFrame: true,
            syncedStartTime: startTime,
            signature: animationSignature(
                asset: .disconnectToConnect,
                startedAt: startTime,
                loop: false,
                holdLastFrame: true,
                isConnected: isConnected
            )
        )
    }

    private func transitionToDisconnected(on view: LottieAnimationView, coordinator: Coordinator) {
        let disconnectStart = CACurrentMediaTime()
        play(
            .connectToDisconnect,
            on: view,
            coordinator: coordinator,
            loop: false,
            holdLastFrame: false,
            syncedStartTime: disconnectStart,
            signature: animationSignature(
                asset: .connectToDisconnect,
                startedAt: disconnectStart,
                loop: false,
                holdLastFrame: false,
                isConnected: isConnected
            )
        ) {
            guard Self.runtime.lastConnectionState == false else { return }
            let reconnectStart = CACurrentMediaTime()
            Self.runtime.phase = .animated(
                asset: .reconnect,
                startedAt: reconnectStart,
                loop: true,
                holdLastFrame: false
            )
            guard let animationView = coordinator.animationView else { return }
            play(
                .reconnect,
                on: animationView,
                coordinator: coordinator,
                loop: true,
                holdLastFrame: false,
                syncedStartTime: reconnectStart,
                signature: animationSignature(
                    asset: .reconnect,
                    startedAt: reconnectStart,
                    loop: true,
                    holdLastFrame: false,
                    isConnected: false
                )
            )
        }
    }

    private func runtimeSignature(phase: RuntimePhase, isConnected: Bool) -> String {
        switch phase {
        case .connectedStatic:
            return "state:\(isConnected)|static:connected"
        case let .animated(asset, startedAt, loop, holdLastFrame):
            return animationSignature(
                asset: asset,
                startedAt: startedAt,
                loop: loop,
                holdLastFrame: holdLastFrame,
                isConnected: isConnected
            )
        }
    }

    private func animationSignature(
        asset: AnimationAsset,
        startedAt: CFTimeInterval,
        loop: Bool,
        holdLastFrame: Bool,
        isConnected: Bool
    ) -> String {
        "state:\(isConnected)|asset:\(asset.rawValue)|start:\(startedAt)|loop:\(loop)|hold:\(holdLastFrame)"
    }

    private func applyRuntimeState(on view: LottieAnimationView, coordinator: Coordinator) {
        let phase = Self.runtime.phase
        let signature = runtimeSignature(phase: phase, isConnected: isConnected)
        guard coordinator.lastRenderedSignature != signature else { return }

        switch phase {
        case .connectedStatic:
            if isConnected {
                showStatic(
                    .disconnectToConnect,
                    progress: 1.0,
                    on: view,
                    coordinator: coordinator,
                    signature: signature
                )
            } else {
                let reconnectStart = CACurrentMediaTime()
                Self.runtime.phase = .animated(
                    asset: .reconnect,
                    startedAt: reconnectStart,
                    loop: true,
                    holdLastFrame: false
                )
                play(
                    .reconnect,
                    on: view,
                    coordinator: coordinator,
                    loop: true,
                    holdLastFrame: false,
                    syncedStartTime: reconnectStart,
                    signature: animationSignature(
                        asset: .reconnect,
                        startedAt: reconnectStart,
                        loop: true,
                        holdLastFrame: false,
                        isConnected: false
                    )
                )
            }

        case let .animated(asset, startedAt, loop, holdLastFrame):
            if isConnected {
                if asset == .disconnectToConnect {
                    play(
                        .disconnectToConnect,
                        on: view,
                        coordinator: coordinator,
                        loop: false,
                        holdLastFrame: true,
                        syncedStartTime: startedAt,
                        signature: signature
                    )
                } else {
                    Self.runtime.phase = .connectedStatic
                    showStatic(
                        .disconnectToConnect,
                        progress: 1.0,
                        on: view,
                        coordinator: coordinator,
                        signature: runtimeSignature(phase: .connectedStatic, isConnected: true)
                    )
                }
                return
            }

            if asset == .disconnectToConnect {
                transitionToDisconnected(on: view, coordinator: coordinator)
                return
            }

            if asset == .connectToDisconnect {
                play(
                    .connectToDisconnect,
                    on: view,
                    coordinator: coordinator,
                    loop: false,
                    holdLastFrame: false,
                    syncedStartTime: startedAt,
                    signature: signature
                ) {
                    guard Self.runtime.lastConnectionState == false else { return }
                    let reconnectStart = CACurrentMediaTime()
                    Self.runtime.phase = .animated(
                        asset: .reconnect,
                        startedAt: reconnectStart,
                        loop: true,
                        holdLastFrame: false
                    )
                    guard let animationView = coordinator.animationView else { return }
                    play(
                        .reconnect,
                        on: animationView,
                        coordinator: coordinator,
                        loop: true,
                        holdLastFrame: false,
                        syncedStartTime: reconnectStart,
                        signature: animationSignature(
                            asset: .reconnect,
                            startedAt: reconnectStart,
                            loop: true,
                            holdLastFrame: false,
                            isConnected: false
                        )
                    )
                }
                return
            }

            play(
                asset,
                on: view,
                coordinator: coordinator,
                loop: loop,
                holdLastFrame: holdLastFrame,
                syncedStartTime: startedAt,
                signature: signature
            )
        }
    }

    private func play(
        _ asset: AnimationAsset,
        on view: LottieAnimationView,
        coordinator: Coordinator,
        loop: Bool,
        holdLastFrame: Bool,
        syncedStartTime: CFTimeInterval? = nil,
        signature: String,
        completion: (() -> Void)? = nil
    ) {
        coordinator.requestToken += 1
        let token = coordinator.requestToken
        let runtimeStartTime = syncedStartTime ?? CACurrentMediaTime()
        coordinator.lastRenderedSignature = signature

        Self.runtime.phase = .animated(
            asset: asset,
            startedAt: runtimeStartTime,
            loop: loop,
            holdLastFrame: holdLastFrame
        )

        let applyAnimation: (LottieAnimation, AnimationImageProvider?) -> Void = { animation, imageProvider in
            guard token == coordinator.requestToken else { return }
            view.stop()
            view.animation = animation
            view.contentMode = .scaleAspectFit
            view.loopMode = loop ? .loop : .playOnce
            view.imageProvider = imageProvider ?? BundleImageProvider(bundle: .main, searchPath: nil)
            view.accessibilityValue = "animating:\(asset.rawValue)"
            
            let startProgress: AnimationProgressTime
            if let syncedStartTime {
                let elapsed = max(0, CACurrentMediaTime() - syncedStartTime)
                let duration = animation.duration
                if duration > 0 {
                    if loop {
                        startProgress = AnimationProgressTime((elapsed.truncatingRemainder(dividingBy: duration)) / duration)
                    } else {
                        let normalizedProgress = elapsed / duration
                        // If resource loading was delayed and the one-shot animation "starts"
                        // already at the end, replay it from the beginning so the user sees it.
                        startProgress = normalizedProgress >= 1.0
                            ? 0
                            : AnimationProgressTime(min(1.0, normalizedProgress))
                    }
                } else {
                    startProgress = 0
                }
            } else {
                startProgress = 0
            }

            view.currentProgress = startProgress
            view.setNeedsLayout()
            view.layoutIfNeeded()
            view.forceDisplayUpdate()

            if !loop && startProgress >= 0.999 {
                if holdLastFrame {
                    view.pause()
                    view.currentProgress = 1
                    view.accessibilityValue = "connected"
                    Self.runtime.phase = .connectedStatic
                }
                completion?()
                return
            }

            view.play { finished in
                guard token == coordinator.requestToken else { return }
                if holdLastFrame && finished {
                    view.pause()
                    view.currentProgress = 1
                    view.accessibilityValue = "connected"
                    Self.runtime.phase = .connectedStatic
                }
                completion?()
            }
        }

        resolveAnimation(for: asset) { cachedAnimation in
            guard token == coordinator.requestToken else { return }
            if let cachedAnimation {
                applyAnimation(cachedAnimation.animation, cachedAnimation.imageProvider)
            } else {
                view.stop()
                completion?()
            }
        }
    }

    private func showStatic(
        _ asset: AnimationAsset,
        progress: AnimationProgressTime,
        on view: LottieAnimationView,
        coordinator: Coordinator,
        signature: String
    ) {
        coordinator.requestToken += 1
        let token = coordinator.requestToken
        coordinator.lastRenderedSignature = signature

        let applyAnimation: (LottieAnimation, AnimationImageProvider?) -> Void = { animation, imageProvider in
            guard token == coordinator.requestToken else { return }
            view.stop()
            view.animation = animation
            view.contentMode = .scaleAspectFit
            view.loopMode = .playOnce
            view.imageProvider = imageProvider ?? BundleImageProvider(bundle: .main, searchPath: nil)
            view.currentProgress = progress
            view.accessibilityValue = progress >= 1 ? "connected" : "disconnected"
            view.setNeedsLayout()
            view.layoutIfNeeded()
            view.forceDisplayUpdate()
        }

        resolveAnimation(for: asset) { cachedAnimation in
            guard token == coordinator.requestToken else { return }
            if let cachedAnimation {
                applyAnimation(cachedAnimation.animation, cachedAnimation.imageProvider)
            } else {
                view.stop()
            }
        }
    }

    private func resolveAnimation(
        for asset: AnimationAsset,
        completion: @escaping (CachedAnimation?) -> Void
    ) {
        if let cached = Self.animationCache.byAsset[asset] {
            completion(cached)
            return
        }

        let storeAndComplete: (LottieAnimation, AnimationImageProvider?) -> Void = { animation, imageProvider in
            let cached = CachedAnimation(animation: animation, imageProvider: imageProvider)
            Self.animationCache.byAsset[asset] = cached
            completion(cached)
        }

        if Bundle.main.url(forResource: asset.rawValue, withExtension: "lottie") != nil {
            DotLottieFile.named(asset.rawValue, bundle: .main) { result in
                DispatchQueue.main.async {
                    switch result {
                    case .success(let dotLottieFile):
                        if let animationContainer = dotLottieFile.animations.first {
                            storeAndComplete(animationContainer.animation, animationContainer.configuration.imageProvider)
                        } else if let animation = LottieAnimation.named(asset.rawValue) {
                            storeAndComplete(animation, nil)
                        } else {
                            completion(nil)
                        }
                    case .failure:
                        if let animation = LottieAnimation.named(asset.rawValue) {
                            storeAndComplete(animation, nil)
                        } else {
                            completion(nil)
                        }
                    }
                }
            }
            return
        }

        if let animation = LottieAnimation.named(asset.rawValue) {
            storeAndComplete(animation, nil)
        } else {
            completion(nil)
        }
    }
}
#endif

private struct BatteryRingView: View {
    private let normalizedLevel: Double
    private let percent: Int

    init(level: Double) {
        normalizedLevel = min(max(level, 0.0), 1.0)
        percent = Int(normalizedLevel * 100)
    }

    private var batteryColor: Color {
        switch percent {
        case ..<20:
            return Color("ubi4_no_system_red")
        case 20...40:
            return Color("ubi4_yellow")
        default:
            return Color("ubi4_active")
        }
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color("ubi4_gray_border"), lineWidth: 3)

            Circle()
                .trim(from: 0, to: normalizedLevel)
                .stroke(
                    batteryColor,
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            Text("\(percent)%")
                .font(.system(size: 9, weight: .semibold))
                .foregroundColor(batteryColor)
        }
    }
}
