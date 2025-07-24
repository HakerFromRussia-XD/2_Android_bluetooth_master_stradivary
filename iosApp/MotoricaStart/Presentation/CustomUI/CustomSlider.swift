//
//  CustomSlider.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 09.06.2025.
//
//import UIKit
//
//class CustomSlider: UISlider {
//    
//    override func awakeFromNib() {
//        super.awakeFromNib()
//        
//        // Скрыть кружок
//        self.setThumbImage(nil, for: .normal)
//        
//        // Настройка минимального трека с нужной высотой и скругленными углами
//        let trackHeight: CGFloat = 30 // Высота трека (dp)
//        let cornerRadius: CGFloat = 10 // Радиус скругления углов (dp)
//        
//        // Цвет трека
//        let activeColor = UIColor.green // Можно заменить на любой цвет из ассетов
//        let inactiveColor = UIColor.lightGray // Также можно использовать цвет из ассетов
//
////        let minTrackImage = UIImage(color: activeColor, size: CGSize(width: 1, height: trackHeight), cornerRadius: cornerRadius)
////        let maxTrackImage = UIImage(color: inactiveColor, size: CGSize(width: 1, height: trackHeight), cornerRadius: cornerRadius)
//
//        // Установим кастомные изображения для трека
//        self.setMinimumTrackImage(minTrackImage, for: .normal)
//        self.setMaximumTrackImage(maxTrackImage, for: .normal)
//    }
//}

import SwiftUI

struct CustomSlider: View {
    @Binding var value: Float
    let range: ClosedRange<Float>
    let trackHeight: CGFloat
    let cornerRadius: CGFloat
    let borderWidth: CGFloat
    let activeColor: Color
    let inactiveColor: Color
    let borderColor: Color

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Фон трека
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(inactiveColor)
                    .frame(height: trackHeight)

                // Заполненная часть трека
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(activeColor)
                    .frame(width: CGFloat((value - range.lowerBound) / (range.upperBound - range.lowerBound)) * geometry.size.width, height: trackHeight)

                // Обводка
                RoundedRectangle(cornerRadius: cornerRadius)
                    .strokeBorder(borderColor, lineWidth: borderWidth)
                    .frame(height: trackHeight)

                // Ползунок
                Circle()
                    .fill(Color.white)
                    .shadow(radius: 2)
                    .frame(width: trackHeight, height: trackHeight)
                    .offset(x: CGFloat((value - range.lowerBound) / (range.upperBound - range.lowerBound)) * geometry.size.width - trackHeight / 2)
                    .gesture(
                        DragGesture()
                            .onChanged { gesture in
                                let relativeX = gesture.location.x / geometry.size.width // Нормализуем значение от 0 до 1 (от левого до правого края)
                                // Преобразуем нормализованное значение в значение слайдера
                                let newValue = Float(relativeX) * Float(range.upperBound - range.lowerBound) + Float(range.lowerBound)
                                // Ограничиваем значение слайдера в пределах диапазона
                                value = min(max(newValue, Float(range.lowerBound)), Float(range.upperBound))
                                print("========================================[test_slider]")
                                print("[test_slider] relativeX = \(relativeX)")
                                print("[test_slider] gesture.location.x = \(gesture.location.x)")
                                print("[test_slider] geometry.size.width = \(geometry.size.width)")
                                print("[test_slider] newValue = \(newValue)")
                                print("[test_slider] range.upperBound = \(range.upperBound)")
                                print("[test_slider] range.lowerBound = \(range.lowerBound)")
                                print("[test_slider] value = \(value)")
                            }
                    )
            }
            .padding(.top, 4)
            .padding(.leading, trackHeight / 2 + 4)
            .padding(.trailing, trackHeight / 2 + 4)
        }
        .frame(height: trackHeight)
    }
}
