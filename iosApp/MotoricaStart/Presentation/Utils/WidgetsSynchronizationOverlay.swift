//
//  WidgetsSynchronizationOverlay.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 14.10.2025.
//

import UIKit
#if canImport(Lottie)
import Lottie
#endif

final class WidgetsSynchronizationOverlay {
    static let shared = WidgetsSynchronizationOverlay()

    private var overlayView: UIView?
    #if canImport(Lottie)
    private var animationView: AnimationView?
    #else
    private var activityIndicator: UIActivityIndicatorView?
    #endif

    private init() { }

    func show() {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            if let overlayView {
                self.resumeAnimationIfNeeded()
                return
            }

            guard let window = self.keyWindow else { return }

            let overlay = UIView(frame: window.bounds)
            overlay.translatesAutoresizingMaskIntoConstraints = false
            overlay.backgroundColor = UIColor.black.withAlphaComponent(0.35)
            window.addSubview(overlay)

            NSLayoutConstraint.activate([
                overlay.leadingAnchor.constraint(equalTo: window.leadingAnchor),
                overlay.trailingAnchor.constraint(equalTo: window.trailingAnchor),
                overlay.topAnchor.constraint(equalTo: window.topAnchor),
                overlay.bottomAnchor.constraint(equalTo: window.bottomAnchor)
            ])

            let container = UIView()
            container.translatesAutoresizingMaskIntoConstraints = false
            overlay.addSubview(container)

            NSLayoutConstraint.activate([
                container.centerXAnchor.constraint(equalTo: overlay.centerXAnchor),
                container.centerYAnchor.constraint(equalTo: overlay.centerYAnchor)
            ])

            #if canImport(Lottie)
            let animationView = AnimationView(name: "synhronization")
            animationView.translatesAutoresizingMaskIntoConstraints = false
            animationView.loopMode = .loop
            animationView.contentMode = .scaleAspectFit
            animationView.backgroundBehavior = .pauseAndRestore
            container.addSubview(animationView)

            NSLayoutConstraint.activate([
                animationView.widthAnchor.constraint(equalToConstant: 200),
                animationView.heightAnchor.constraint(equalTo: animationView.widthAnchor)
            ])

            animationView.play()
            self.animationView = animationView
            #else
            let indicator = UIActivityIndicatorView(style: .large)
            indicator.translatesAutoresizingMaskIntoConstraints = false
            indicator.startAnimating()
            container.addSubview(indicator)

            NSLayoutConstraint.activate([
                indicator.widthAnchor.constraint(equalToConstant: 60),
                indicator.heightAnchor.constraint(equalTo: indicator.widthAnchor)
            ])

            self.activityIndicator = indicator
            #endif

            self.overlayView = overlay

            NotificationCenter.default.addObserver(
                self,
                selector: #selector(self.handleOrientationChange),
                name: UIDevice.orientationDidChangeNotification,
                object: nil
            )
        }
    }

    func hide() {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            NotificationCenter.default.removeObserver(
                self,
                name: UIDevice.orientationDidChangeNotification,
                object: nil
            )

            #if canImport(Lottie)
            self.animationView?.stop()
            self.animationView?.removeFromSuperview()
            self.animationView = nil
            #else
            self.activityIndicator?.stopAnimating()
            self.activityIndicator?.removeFromSuperview()
            self.activityIndicator = nil
            #endif

            self.overlayView?.removeFromSuperview()
            self.overlayView = nil
        }
    }

    @objc private func handleOrientationChange() {
        DispatchQueue.main.async { [weak self] in
            guard let self, let overlayView, let superview = overlayView.superview else { return }
            overlayView.frame = superview.bounds
            superview.layoutIfNeeded()
        }
    }

    private func resumeAnimationIfNeeded() {
        #if canImport(Lottie)
        animationView?.play()
        #else
        activityIndicator?.startAnimating()
        #endif
    }

    private var keyWindow: UIWindow? {
        if #available(iOS 15.0, *) {
            return UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first { $0.activationState == .foregroundActive }?
                .windows
                .first { $0.isKeyWindow }
        } else {
            return UIApplication.shared.windows.first { $0.isKeyWindow }
        }
    }
}
