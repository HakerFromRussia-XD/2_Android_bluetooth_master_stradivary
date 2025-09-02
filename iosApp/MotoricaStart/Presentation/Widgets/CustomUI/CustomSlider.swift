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
                    .offset(x: CGFloat((value - range.lowerBound) / (range.upperBound - range.lowerBound)) * (geometry.size.width / 2) - geometry.size.width / 2)
                    .frame(width: CGFloat((value - range.lowerBound) / (range.upperBound - range.lowerBound)) * geometry.size.width, height: trackHeight)

                // Обводка
                RoundedRectangle(cornerRadius: cornerRadius)
                    .strokeBorder(borderColor, lineWidth: borderWidth)
                    .frame(height: trackHeight)

                // Ползунок
                Circle()
                    .fill(Color.white)
                    .shadow(radius: 2)
                    .frame(width: trackHeight, height: trackHeight)//это размеры пипки за которую тянем
                    .offset(x: (CGFloat((value - range.lowerBound) / (range.upperBound - range.lowerBound)) * geometry.size.width - geometry.size.width/2))//чтобы пипка двигалась под пальцем
                    .gesture(
                        DragGesture()
                            .onChanged { gesture in
                                let availableWidth = (geometry.size.width-trackHeight/2)
                                let normalizedX = Float(CGFloat((gesture.location.x-trackHeight/2)/(availableWidth/2))+1)/2 // Нормализуем значение от 0 до 1 (от левого до правого края)
                                value = max(range.lowerBound, min(normalizedX * (range.upperBound - range.lowerBound) + range.lowerBound, range.upperBound))
                            }
                    )
            }
            .padding(.top, 4)
        }
        .frame(height: trackHeight)
    }
}
