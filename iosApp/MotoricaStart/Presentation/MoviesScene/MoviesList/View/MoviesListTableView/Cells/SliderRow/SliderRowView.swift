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
                Text("\(Int(provider.value_1))") // Преобразуем value в строку и отображаем
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.top, 8)
                    .padding(.trailing, 10) // отступ справа
            }
            HStack {
                StepButton(label: "-", action: decrement_1)
                        .padding(.leading, 8)
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
                .padding(.leading, 16)
                .padding(.trailing, 16)
                .padding(.bottom, 8)
                StepButton(label: "+", action: increment_1)
                       .padding(.trailing, 8)
            }
            HStack {
                Text(provider.title_2)
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.top, 16) // отступ сверху
                    .padding(.leading, 10) // отступ слева
                Spacer()
                Text("\(Int(provider.value_2))") // Преобразуем value в строку и отображаем
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .padding(.top, 16)
                    .padding(.trailing, 10) // отступ справа
            }
            HStack {
                StepButton(label: "-", action: decrement_2)
                        .padding(.leading, 8)
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
                .padding(.leading, 16)
                .padding(.trailing, 16)
                .padding(.bottom, 8)
                StepButton(label: "+", action: increment_2)
                       .padding(.trailing, 8)
            }
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
    
    private func decrement_1() {provider.value_1 = provider.value_1 - 1}
    private func increment_1() {provider.value_1 = provider.value_1 + 1}
    private func decrement_2() {provider.value_2 = provider.value_2 - 1}
    private func increment_2() {provider.value_2 = provider.value_2 + 1}
}


struct StepButton: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.custom("SFProDisplay-Light", size: 14))
                .foregroundColor(Color("ubi4_white"))
                .frame(width: 48, height: 30)
        }
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color("ubi4_gray"))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
        )
    }
}
