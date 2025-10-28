//
//  GestureOpticProvider.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 24.10.2025.
//

import SwiftUI

final class GestureOpticProvider: ObservableObject {
    @Published var activeGesture: Int
    let title: String

    init(activeGesture: Int = 0, title: String) {
        self.activeGesture = activeGesture
        self.title = title
    }
}
