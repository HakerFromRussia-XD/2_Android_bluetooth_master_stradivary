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
    @Published var value_1: Float
    let title_1: String
    let numLabel_1: String
    @Published var value_2: Float
    let title_2: String
    let numLabel_2: String
    
    init( value_1: Float = 0, title_1: String, numLabel_1: String, value_2: Float = 0, title_2: String, numLabel_2: String,) {
        self.value_1 = value_1
        self.title_1 = title_1
        self.numLabel_1 = numLabel_1
        self.value_2 = value_2
        self.title_2 = title_2
        self.numLabel_2 = numLabel_2
    }
}

struct SliderRowView: View {
    @ObservedObject var provider: SliderRowProvider
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(provider.title_1)
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.top, 8) // отступ сверху
                    .padding(.leading, 10) // отступ слева
                Spacer()
                Text("\(provider.value_1)") // Преобразуем value в строку и отображаем
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.top, 8)
                    .padding(.trailing, 10) // отступ справа
            }
            CustomSlider(
                value: Binding(
                    get: { provider.value_1 },
                    set: { provider.value_1 = Float($0) }
                ),
                range: 0...100,
                trackHeight: 30,
                cornerRadius: 10,
                borderWidth: 1,
                activeColor: Color("ubi4_active"),
                inactiveColor: Color("ubi4_inactive"),
                borderColor: Color("ubi4_gray_border")
            )
            
            
        
            HStack {
                Text(provider.title_2)
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.top, 16) // отступ сверху
                    .padding(.leading, 10) // отступ слева
                Spacer()
                Text("\(provider.value_2)") // Преобразуем value в строку и отображаем
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.top, 16)
                    .padding(.trailing, 10) // отступ справа
            }
            CustomSlider(
                value: Binding(
                    get: { provider.value_2 },
                    set: { provider.value_2 = Float($0) }
                ),
                range: -100...200,
                trackHeight: 30,
                cornerRadius: 10,
                borderWidth: 1,
                activeColor: Color("ubi4_active"),
                inactiveColor: Color("ubi4_inactive"),
                borderColor: Color("ubi4_gray_border")
            )
            .padding(.bottom, 8)
            
        }
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color("ubi4_gray")) // Фон для ячейки (используем цвет из ассетов)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1) // Обводка
                )
                .shadow(color: Color.black.opacity(0.24), radius: 2, x: 0, y: 2)
        )
    }
}
