//
//  SliderRowView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.10.2025.
//
import SwiftUI
import Combine

struct SliderRowView: View {
    @ObservedObject var provider: SliderProvider
    let onFirstSliderEditingEnded: ((Float) -> Void)?
    let onSecondSliderEditingEnded: ((Float) -> Void)?

    init(
        provider: SliderProvider,
        onFirstSliderEditingEnded: ((Float) -> Void)? = nil,
        onSecondSliderEditingEnded: ((Float) -> Void)? = nil
    ) {
        self._provider = ObservedObject(wrappedValue: provider)
        self.onFirstSliderEditingEnded = onFirstSliderEditingEnded
        self.onSecondSliderEditingEnded = onSecondSliderEditingEnded

        // если не установлены на стороне стека, то устанавливаем по дефолту
        if (provider.maxProgress == provider.minProgress ) {
            provider.minProgress = 0
            provider.maxProgress = 100
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(provider.title_1)
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .foregroundColor(Color("ubi4_white"))
                    .padding(.top, 8) // отступ сверху
                    .padding(.leading, 10) // отступ слева
                Spacer()
                Text("\(Int(provider.value_1))") // Преобразуем value в строку и отображаем
                    .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                    .foregroundColor(Color("ubi4_white"))
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
                    range: provider.minProgress...provider.maxProgress,
                    trackHeight: 30,
                    cornerRadius: 10,
                    borderWidth: 1,
                    activeColor: Color("ubi4_active"),
                    inactiveColor: Color("ubi4_gray"),
                    borderColor: Color("ubi4_gray_border"),
                    editingDidEnd: { value in
                        onFirstSliderEditingEnded?(value)
                    }
                )
                .padding(.leading, 16)
                .padding(.trailing, 16)
                .padding(.bottom, 8)
                StepButton(label: "+", action: increment_1)
                       .padding(.trailing, 8)
            }
            .padding(.bottom, 4)
            if provider.isSecondSliderShow {
                HStack {
                    Text(provider.title_2)
                        .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                        .foregroundColor(Color("ubi4_white"))
                        .padding(.top, 12) // отступ сверху
                        .padding(.leading, 10) // отступ слева
                    Spacer()
                    Text("\(Int(provider.value_2))") // Преобразуем value в строку и отображаем
                        .font(.custom("SFProDisplay-Light", size: 14)) // Используем свой шрифт из ассетов
                        .foregroundColor(Color("ubi4_white"))
                        .padding(.top, 12)
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
                        range: provider.minProgress...provider.maxProgress,
                        trackHeight: 30,
                        cornerRadius: 10,
                        borderWidth: 1,
                        activeColor: Color("ubi4_active"),
                        inactiveColor: Color("ubi4_gray"),
                        borderColor: Color("ubi4_gray_border"),
                        editingDidEnd: { value in
                            onSecondSliderEditingEnded?(value)
                        }
                    )
                    .padding(.leading, 16)
                    .padding(.trailing, 16)
                    .padding(.bottom, 8)
                    StepButton(label: "+", action: increment_2)
                        .padding(.trailing, 8)
                }
                .padding(.bottom, 8)
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color("ubi4_gray")) // Фон для ячейки (используем цвет из ассетов)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1) // Обводка
                )
                .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
    }
    
    private func decrement_1() {
        if (provider.value_1 != provider.minProgress) {
            provider.value_1 = provider.value_1 - 1
            onFirstSliderEditingEnded?(provider.value_1)
        }
    }
    private func increment_1() {
        if (provider.value_1 != provider.maxProgress) {
            provider.value_1 = provider.value_1 + 1
            onFirstSliderEditingEnded?(provider.value_1)
        }
    }
    private func decrement_2() {
        if (provider.value_2 != provider.minProgress) {
            provider.value_2 = provider.value_2 - 1
            onSecondSliderEditingEnded?(provider.value_2)
        }
    }
    private func increment_2() {
        if (provider.value_2 != provider.maxProgress) {
            provider.value_2 = provider.value_2 + 1
            onSecondSliderEditingEnded?(provider.value_2)
        }
    }
}
