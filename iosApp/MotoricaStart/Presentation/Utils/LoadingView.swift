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
    
    private static func getKeyWindow() -> UIWindow? {
        if #available(iOS 15.0, *) {
            // Для iOS 15 и выше ищем ключевое окно среди всех сцен, а не только активной,
            // так как во время старта подключения сцена может еще не перейти в foregroundActive.
            let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            if let window = scenes
                .flatMap({ $0.windows })
                .first(where: { $0.isKeyWindow }) {
                return window
            }
        } else {
            // Для iOS 14 и ниже используем старый способ
            return UIApplication.shared.windows.first(where: { $0.isKeyWindow })
        }
        return nil
    }
}
