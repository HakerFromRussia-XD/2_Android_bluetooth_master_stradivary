//
//  SwitchRowView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 13.10.2025.
//

import Combine

final class SwitchProvider: ObservableObject {
    @Published var isOn: Bool
    let title: String

    init(isOn: Bool = false, title: String) {
        self.isOn = isOn
        self.title = title
    }
}
