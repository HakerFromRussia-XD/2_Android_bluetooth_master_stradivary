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
    private static var isObservingOrientationChanges = false
    private static let animationName = "sinchronization"

    static func show(state: State) {
        DispatchQueue.main.async {
            guard let window = getKeyWindow() else { return }
            currentState = state
            
            if containerView == nil {
                let container = LoadingContainerView(frame: window.bounds, animationName: animationName)
                containerView = container
                window.addSubview(container)
                container.frame = window.bounds
                startObservingOrientationChanges()
            }
            
            guard let container = containerView else { return }
            
            if container.superview == nil {
                window.addSubview(container)
            } else {
                window.bringSubviewToFront(container)
            }
            
            container.frame = window.bounds
            container.alpha = 1
            container.apply(state: state)
            container.startAnimation()
        }
    }
        
    static func hide() {
        DispatchQueue.main.async {
            guard let container = containerView else { return }
            container.stopAnimation()
            UIView.animate(withDuration: 0.3, animations: {
                container.alpha = 0
            }, completion: { _ in
                container.removeFromSuperview()
                container.alpha = 1
                containerView = nil
                currentState = nil
                stopObservingOrientationChanges()
            })
        }
    }

    @objc static func update() {
        DispatchQueue.main.async {
            guard let container = containerView, let window = getKeyWindow() else { return }
            container.frame = window.bounds
            if let state = currentState {
                container.apply(state: state)
            }
        }
    }
    
    private static func startObservingOrientationChanges() {
        guard !isObservingOrientationChanges else { return }
        NotificationCenter.default.addObserver(self, selector: #selector(update), name: UIDevice.orientationDidChangeNotification, object: nil)
        isObservingOrientationChanges = true
    }

    private static func stopObservingOrientationChanges() {
        guard isObservingOrientationChanges else { return }
        NotificationCenter.default.removeObserver(self, name: UIDevice.orientationDidChangeNotification, object: nil)
        isObservingOrientationChanges = false
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
