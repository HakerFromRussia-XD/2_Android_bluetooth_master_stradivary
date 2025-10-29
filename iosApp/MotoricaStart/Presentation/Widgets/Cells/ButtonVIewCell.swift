import SwiftUI


import SwiftUI

struct ButtonVIewCell: View {
    let title: String
    var action: () -> Void

    // стили
    var height: CGFloat = 48
    var cornerRadius: CGFloat = 12

   
    var fill = Color("ubi4_gray")
    var stroke = Color("ubi4_gray_border")
    var textColor: Color = .white

    @State private var isPressed = false

    var body: some View {
           Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .light))
                .foregroundStyle(textColor)
                .frame(maxWidth: .infinity, minHeight: height, maxHeight: height)
                .contentShape(RoundedRectangle(cornerRadius: cornerRadius))
        }
        .buttonStyle(.plain)
        .background(
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(fill)
                .overlay(
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .stroke(stroke, lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1) // ~elevation=3dp
        )
        .overlay( // «ripple» подсветка при нажатии
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color.white.opacity(isPressed ? 0.12 : 0))
        )
        .scaleEffect(isPressed ? 0.98 : 1)
        .animation(.easeOut(duration: 0.12), value: isPressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
    }
}

#Preview {
    ZStack {
        Color("ubi4_back").ignoresSafeArea()
        ButtonVIewCell(title: "Gesture №1") { print("tap") }
            .padding(.horizontal, 16) // marginStart/End = 16dp
            .padding(.vertical, 4)    // marginTop/Bottom = 4dp
    }
    .preferredColorScheme(.dark)
}

