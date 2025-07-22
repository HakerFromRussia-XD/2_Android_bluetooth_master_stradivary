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
    @Binding var value: Double
    let range: ClosedRange<Double>
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
                                let newValue = min(max(range.lowerBound, Double(gesture.location.x / geometry.size.width) * (range.upperBound - range.lowerBound) + range.lowerBound), range.upperBound)
                                value = newValue
                            }
                    )
            }
            .padding(.horizontal, trackHeight / 2)
        }
        .frame(height: trackHeight)
    }
}



