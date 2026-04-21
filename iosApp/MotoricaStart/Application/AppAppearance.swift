import Foundation
import UIKit

final class AppAppearance {
    
    static func setupAppearance() {
        let statusBarBackgroundColor = UIColor(named: "ubi4_back") ?? UIColor(red: 37/255.0, green: 37/255.0, blue: 37.0/255.0, alpha: 1.0)
        if #available(iOS 15, *) {
            let navBarAppearance = UINavigationBarAppearance()
            navBarAppearance.configureWithOpaqueBackground()
            navBarAppearance.titleTextAttributes = [.foregroundColor: UIColor.white]
            navBarAppearance.backgroundColor = statusBarBackgroundColor
            UINavigationBar.appearance().standardAppearance = navBarAppearance
            UINavigationBar.appearance().scrollEdgeAppearance = navBarAppearance

            let tabBarAppearance = UITabBarAppearance()
            tabBarAppearance.configureWithOpaqueBackground()
            tabBarAppearance.backgroundColor = statusBarBackgroundColor
            UITabBar.appearance().standardAppearance = tabBarAppearance
            UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
        } else {
            UINavigationBar.appearance().barTintColor = statusBarBackgroundColor
            UINavigationBar.appearance().tintColor = .white
            UINavigationBar.appearance().titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.white]
            UITabBar.appearance().barTintColor = statusBarBackgroundColor
            UITabBar.appearance().isTranslucent = false
        }
    }
}

extension UINavigationController {
    @objc override open var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }

    @objc override open var childForStatusBarStyle: UIViewController? {
        nil
    }
}
