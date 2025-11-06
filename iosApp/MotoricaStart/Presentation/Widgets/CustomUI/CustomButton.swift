//
//  ButtonViewCell.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.09.2025.
//

import SwiftUI



struct CustomButton: View {
    let title: String
    
    /// Вызывается при нажатии (палец вниз)
    var onPress: (() -> Void)? = nil
    /// Вызывается при отпускании (палец вверх)
    var onRelease: (() -> Void)? = nil

    // стили
    var height: CGFloat = 48
    var cornerRadius: CGFloat = 12

   
    var fill = Color("ubi4_gray")
    var stroke = Color("ubi4_gray_border")
    var textColor: Color = .white

    @State private var isPressed = false
    @State private var didScroll = false

    // эмпирический порог, когда считаем, что уже пошёл скролл
    private let scrollThreshold: CGFloat = 6
    
    var body: some View {
        Text(title)
            .font(.system(size: 12, weight: .light))
            .foregroundStyle(textColor)
            .frame(maxWidth: .infinity, minHeight: height, maxHeight: height)
            .contentShape(RoundedRectangle(cornerRadius: cornerRadius))
        .buttonStyle(.plain)
        .background(
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(fill)
                .overlay(
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .stroke(stroke, lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color.white.opacity(isPressed ? 0.12 : 0))
        )
        .scaleEffect(isPressed ? 0.98 : 1)
        .animation(.easeOut(duration: 0.12), value: isPressed)
        .onLongPressGesture(minimumDuration: 0, maximumDistance: .infinity,
            pressing: { pressing in
                if pressing {
                    if !isPressed {
                        isPressed = true
                        onPress?()
                    }
                } else {
                    // палец ушёл/отпущен — даём release ТОЛЬКО если не было скролла
                    if isPressed, !didScroll {
                        onRelease?()
                    }
                    isPressed = false
                }
            }, perform: { })
        .simultaneousGesture(
            DragGesture(minimumDistance: 1)
                .onChanged { value in
                    if abs(value.translation.width) > scrollThreshold ||
                       abs(value.translation.height) > scrollThreshold {
                        didScroll = true
                    }
                }
                .onEnded { _ in
                    didScroll = false
                }
        )
    }
}
