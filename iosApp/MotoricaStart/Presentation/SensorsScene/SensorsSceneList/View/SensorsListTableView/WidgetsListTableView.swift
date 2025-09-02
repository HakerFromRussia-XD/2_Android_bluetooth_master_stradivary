//
//  WidgetsListTableView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.09.2025.
//
import UIKit

/// Custom table view that allows scrolling to cancel touches on buttons.
final class WidgetsListTableView: UITableView {
    override func touchesShouldCancel(in view: UIView) -> Bool {
        if view is UIButton {
            return true
        }
        return super.touchesShouldCancel(in: view)
    }
}
