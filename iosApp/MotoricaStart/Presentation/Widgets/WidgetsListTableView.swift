//
//  WidgetsListTableView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.09.2025.
//
import UIKit

/// Custom table view that allows scrolling to cancel touches on buttons.
final class WidgetsListTableView: UITableView {
    func configure() {
        delaysContentTouches = false
        canCancelContentTouches = true
        
        // У UITableView внутри есть скрытый UIScrollView (UITableViewWrapperView);
        // на нём тоже нужно отключить задержку
        (subviews.first as? UIScrollView)?.delaysContentTouches = false

        // Чтобы пан‑жест начинал распознаваться сразу
        panGestureRecognizer.delaysTouchesBegan = false
    }

    override func touchesShouldCancel(in view: UIView) -> Bool {
        // если касание началось на UIControl (UIButton и т.п.),
        // то при вертикальном движении отменяем его и передаём жест прокрутки
        if view is UIControl { return true }
        return super.touchesShouldCancel(in: view)
    }
}
