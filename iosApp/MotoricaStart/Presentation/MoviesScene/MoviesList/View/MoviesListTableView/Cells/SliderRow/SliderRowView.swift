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
    let title_1: String
    let numLabel: String
    
    init( value: Float = 0, title_1: String, numLabel: String) {
        self.value = value
        self.title_1 = title_1
        self.numLabel = numLabel
    }
}

struct SliderRowView: View {
    @ObservedObject var provider: SliderRowProvider
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // HStack для выравнивания title_1 и numLabel по горизонтали
            HStack {
                Text(provider.title_1)
                    .font(.custom("YourCustomFontName", size: 18)) // Используем свой шрифт из ассетов
                    .padding(.leading, 16) // отступ слева
                Spacer() // чтобы текст title_1 был слева
                Text("\(provider.value)") // Преобразуем value в строку и отображаем
                    .font(.custom("YourCustomFontName", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.trailing, 16) // отступ справа
            }
            .padding(.top, 8) // отступ сверху
            
            // Используем CustomSlider вместо стандартного Slider
            CustomSlider(
                value: Binding(
                    get: { provider.value },
                    set: { provider.value = Float($0) }
                ),
                range: 0...100,
                trackHeight: 8,
                cornerRadius: 4,
                borderWidth: 1,
                activeColor: Color("ubi4_active"),
                inactiveColor: Color("ubi4_inactive"),
                borderColor: Color("ubi4_gray_border")
            )
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color("ubi4_gray")) // Фон для ячейки (используем цвет из ассетов)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1) // Обводка
                )
        )
//        .shadow(color: Color.black, radius: 3, x: 0, y: 2)
    }
}
