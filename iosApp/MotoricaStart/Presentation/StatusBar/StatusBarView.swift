//
//  StatusBarView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.01.2026.
//

import SwiftUI
#if canImport(Lottie)
import Lottie
#endif

struct StatusBarView: View {
    enum Constants {
        static let height: CGFloat = 44
        static let iconSize: CGFloat = 22
        static let statusIndicatorVisualSize: CGFloat = 14
        static let statusIndicatorRenderSize: CGFloat = 14
        static let batteryRingSize: CGFloat = 28
    }

    @ObservedObject var viewModel: StatusBarViewModel

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle")
                .font(.system(size: Constants.iconSize, weight: .regular))
                .foregroundColor(Color("ubi4_white"))
                .accessibilityLabel(Text("Вход в личный кабинет"))

            Spacer()

            HStack(spacing: 8) {
                Text(viewModel.serialNumber)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Color("ubi4_white"))
                    .lineLimit(1)
                    .accessibilityLabel(Text("Серийный номер устройства"))

                ConnectionStatusIndicatorView(isConnected: viewModel.isConnected)
                    .frame(width: Constants.statusIndicatorRenderSize, height: Constants.statusIndicatorRenderSize)
                    .scaleEffect(0.03)
                    .frame(width: Constants.statusIndicatorVisualSize, height: Constants.statusIndicatorVisualSize)
                    .accessibilityLabel(Text(viewModel.isConnected ? "Соединение установлено" : "Соединение потеряно"))
            }

            Spacer()

            BatteryRingView(level: viewModel.batteryLevel)
                .frame(width: Constants.batteryRingSize, height: Constants.batteryRingSize)
                .accessibilityLabel(Text("Уровень заряда"))
        }
        .padding(.horizontal, 16)
        .frame(height: Constants.height)
        .background(Color("ubi4_back"))
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
            
            let startProgress: AnimationProgressTime
            if let syncedStartTime {
                let elapsed = max(0, CACurrentMediaTime() - syncedStartTime)
                let duration = animation.duration
                if duration > 0 {
                    if loop {
                        startProgress = AnimationProgressTime((elapsed.truncatingRemainder(dividingBy: duration)) / duration)
                    } else {
                        startProgress = AnimationProgressTime(min(1.0, elapsed / duration))
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

    init(level: Double) {
        normalizedLevel = min(max(level, 0.0), 1.0)
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color("ubi4_gray_border"), lineWidth: 3)

            Circle()
                .trim(from: 0, to: normalizedLevel)
                .stroke(
                    Color("ubi4_yes_system_blue"),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            Text("\(Int(normalizedLevel * 100))%")
                .font(.system(size: 9, weight: .semibold))
                .foregroundColor(Color("ubi4_white"))
        }
    }
}
