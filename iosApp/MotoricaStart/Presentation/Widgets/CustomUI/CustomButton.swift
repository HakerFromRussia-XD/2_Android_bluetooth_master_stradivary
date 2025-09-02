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
                .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
        )
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color.white.opacity(isPressed ? 0.12 : 0))
        )
        .scaleEffect(isPressed ? 0.98 : 1)
        .animation(.easeOut(duration: 0.12), value: isPressed)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in
                    if !isPressed {
                        isPressed = true
                        onPress?()
                    }
                }
                .onEnded { _ in
                    isPressed = false
                    onRelease?()
                }
        )
    }
}
