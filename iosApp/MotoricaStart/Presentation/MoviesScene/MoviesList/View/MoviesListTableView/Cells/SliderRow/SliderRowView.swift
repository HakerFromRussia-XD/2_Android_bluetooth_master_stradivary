//
//  SliderRowView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 10.06.2025.
//

import SwiftUI
import Combine

/// Observable-обёртка, чтобы обновлять значение слайдера извне.
final class SliderRowProvider: ObservableObject {
    @Published var value: Float
    let title: String
    let subtitle: String
    
    init(title: String, subtitle: String, value: Float = .zero) {
        self.title     = title
        self.subtitle  = subtitle
        self.value     = value
    }
}

struct SliderRowView: View {
    @ObservedObject var provider: SliderRowProvider
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(provider.title)      .font(.headline)
            Text(provider.subtitle)   .font(.subheadline)
            Slider(value: $provider.value,
                   in: 0...100,
                   step: 1)
                .tint(Color("ubi4_active"))
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color("ubi4_gray_border"), lineWidth: 1)
        )
    }
}
