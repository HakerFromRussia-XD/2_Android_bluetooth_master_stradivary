//
//  WidgetsListViewModelActions.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.07.2025.
//


import Foundation
import Combine

struct WidgetsListViewModelActions {
    /// Note: if you would need to edit widget inside Details screen and update this Widgets List screen with updated widget then you would need this closure:
    /// showWidgetDetails: (Widget, @escaping (_ updated: Widget) -> Void) -> Void
    let showWidgetDetails: (Widget) -> Void
    let showWidgetQueriesSuggestions: (@escaping (_ didSelect: WidgetQuery) -> Void) -> Void
    let closeWidgetQueriesSuggestions: () -> Void
}
