//
//  SwitchRowView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 13.10.2025.
//

import SwiftUI

final class SwitchProvider: ObservableObject {
    @Published var isOn: Bool
    let title: String

    init(isOn: Bool = false, title: String) {
        self.isOn = isOn
        self.title = title
    }
}

struct SwitchRowView: View {
    @ObservedObject var provider: SwitchProvider

    init(provider: SwitchProvider) {
        self._provider = ObservedObject(wrappedValue: provider)
    }

    var body: some View {
        CustomSwitcher(
            title: provider.title,
            isOn: Binding(
                get: { provider.isOn },
                set: { newValue in
                    provider.isOn = newValue
                }
            )
        )
    }
}
