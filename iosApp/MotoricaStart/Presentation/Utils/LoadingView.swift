import UIKit

final class LoadingView {
    struct State {
        let message: String
        let progress: Float

        init(message: String, progress: Float = 0) {
            self.message = message
            self.progress = progress
        }
    }
    private static var containerView: LoadingContainerView?
    private static var currentState: State?
    private static weak var containerHostView: UIView?
    private static let animationName = "sinchronization"
    private static let hideFadeDuration: TimeInterval = 0.3
    private static let fallbackState = State(
        message: NSLocalizedString("Синхронизация данных...", comment: ""),
        progress: 0
    )

    static func show(state: State = fallbackState, in hostView: UIView? = nil) {
        DispatchQueue.main.async {
            guard let hostView = resolveHostView(preferredHostView: hostView) else { return }
            currentState = state
            
            if containerView == nil {
                let container = LoadingContainerView(frame: hostView.bounds, animationName: animationName)
                containerView = container
            }
            
            guard let container = containerView else { return }
            
            if container.superview !== hostView {
                container.removeFromSuperview()
                hostView.addSubview(container)
            } else if container.superview == nil {
                hostView.addSubview(container)
            } else {
                hostView.bringSubviewToFront(container)
            }
            
            container.frame = hostView.bounds
            container.alpha = 1
            container.apply(state: state)
            container.startAnimation()
        }
    }
        
    static func hide() {
        DispatchQueue.main.async {
            guard let container = containerView else { return }
            container.stopAnimation()
            container.layer.removeAllAnimations()

            let completion: (Bool) -> Void = { _ in
                container.removeFromSuperview()
                container.alpha = 1
                containerView = nil
                currentState = nil
                containerHostView = nil
            }

            guard container.superview != nil else {
                completion(true)
                return
            }

            UIView.animate(
                withDuration: hideFadeDuration,
                delay: 0,
                options: [.beginFromCurrentState, .curveEaseInOut, .allowUserInteraction],
                animations: {
                container.alpha = 0
            },
                completion: completion
            )
        }
    }

    @objc static func update() {
        DispatchQueue.main.async {
            guard let container = containerView, let hostView = container.superview else { return }
            container.frame = hostView.bounds
            if let state = currentState {
                container.apply(state: state)
            }
        }
    }
    
    private static func resolveHostView(preferredHostView: UIView?) -> UIView? {
        if let preferredHostView {
            containerHostView = preferredHostView
            return preferredHostView
        }
        
        if let cachedHostView = containerHostView, cachedHostView.window != nil {
            return cachedHostView
        }
        
        containerHostView = nil
        return getKeyWindow()
    }
    
    private static func getKeyWindow() -> UIWindow? {
        if #available(iOS 15.0, *) {
            // Для iOS 15 и выше используем оконную сцену
            if let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene {
                return scene.windows.first(where: { $0.isKeyWindow })
            }
        } else {
            // Для iOS 14 и ниже используем старый способ
            return UIApplication.shared.windows.first(where: { $0.isKeyWindow })
        }
        return nil
    }
}
