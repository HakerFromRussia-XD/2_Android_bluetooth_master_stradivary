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
    let editingDidEnd: ((Float) -> Void)
    @State private var isDragging: Bool = false
    @State private var displayedValue: Float
    @State private var isAnimationLocked: Bool = false
    @State private var pendingValue: Float?
    private let animationDuration: Double = 3.0

    init(
        value: Binding<Float>,
        range: ClosedRange<Float>,
        trackHeight: CGFloat,
        cornerRadius: CGFloat,
        borderWidth: CGFloat,
        activeColor: Color,
        inactiveColor: Color,
        borderColor: Color,
        editingDidEnd: @escaping ((Float) -> Void)
    ) {
        self._value = value
        self.range = range
        self.trackHeight = trackHeight
        self.cornerRadius = cornerRadius
        self.borderWidth = borderWidth
        self.activeColor = activeColor
        self.inactiveColor = inactiveColor
        self.borderColor = borderColor
        self.editingDidEnd = editingDidEnd
        _displayedValue = State(initialValue: value.wrappedValue)
    }

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
                    .offset(x: CGFloat((displayedValue - range.lowerBound) / (range.upperBound - range.lowerBound)) * (geometry.size.width / 2) - geometry.size.width / 2)
                    .frame(width: CGFloat((displayedValue - range.lowerBound) / (range.upperBound - range.lowerBound)) * geometry.size.width, height: trackHeight)
                
                // Обводка
                RoundedRectangle(cornerRadius: cornerRadius)
                    .strokeBorder(borderColor, lineWidth: borderWidth)
                    .frame(height: trackHeight)
                
                // Ползунок
                Circle()
                    .fill(Color.white)
                    .shadow(radius: 2)
                    .frame(width: trackHeight, height: trackHeight)//это размеры пипки за которую тянем
                    .offset(x: (CGFloat((displayedValue - range.lowerBound) / (range.upperBound - range.lowerBound)) * geometry.size.width - geometry.size.width/2))//чтобы пипка двигалась под пальцем
                    .gesture(
                        DragGesture()
                            .onChanged { gesture in
                                isDragging = true
                                let availableWidth = (geometry.size.width-trackHeight/2)
                                let normalizedX = Float(CGFloat((gesture.location.x-trackHeight/2)/(availableWidth/2))+1)/2 // Нормализуем значение от 0 до 1 (от левого до правого края)
                                let newValue = max(range.lowerBound, min(normalizedX * (range.upperBound - range.lowerBound) + range.lowerBound, range.upperBound))
                                displayedValue = newValue
                                value = newValue
                            }
                            .onEnded { _ in
                                isDragging = false
                                displayedValue = value
                                editingDidEnd(value)
                            }
                    )
            }
            .padding(.top, 4)
            .onChange(of: value) { newValue in
                guard !isDragging else { return }
                handleProgrammaticUpdate(newValue)
            }
            .frame(height: trackHeight)
        }
    }
        
    private func handleProgrammaticUpdate(_ newValue: Float) {
        if isAnimationLocked {
            pendingValue = newValue
            return
        }
        
        animate(to: newValue)
    }
        
    private func animate(to target: Float) {
        isAnimationLocked = true
        withAnimation(.easeInOut(duration: animationDuration)) {
            self.displayedValue = target
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + animationDuration) {
            self.isAnimationLocked = false
            if let pendingValue {
                self.pendingValue = nil
                self.animate(to: pendingValue)
            }
        }
    }
}
