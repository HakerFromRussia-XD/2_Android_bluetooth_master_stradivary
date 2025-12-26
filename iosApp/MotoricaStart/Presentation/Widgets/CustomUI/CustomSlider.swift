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
    @State private var isAnimatingValue: Bool = false
    @State private var pendingValues: [Float] = []
    

    private let animationDuration: Double = 0.3//3.0//

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
                            self.isDragging = true
                            self.isAnimatingValue = false
                            self.pendingValues.removeAll()
                            let availableWidth = (geometry.size.width-trackHeight/2)
                            let normalizedX = Float(CGFloat((gesture.location.x-trackHeight/2)/(availableWidth/2))+1)/2 // Нормализуем значение от 0 до 1 (от левого до правого края)
                            let clampedValue = max(range.lowerBound, min(normalizedX * (range.upperBound - range.lowerBound) + range.lowerBound, range.upperBound))
                            self.value = clampedValue
                            self.displayedValue = clampedValue
                        }
                        .onEnded { _ in
                            self.isDragging = false
                            self.editingDidEnd(self.value)
                        }
                )
            }
            .padding(.top, 4)
            .onAppear {
                self.displayedValue = clamp(self.value)
            }
            .onChange(of: value) { newValue in
                guard !self.isDragging else { return }
                self.animate(to: newValue)
            }
        }
        .frame(height: trackHeight)
    }
    
    private func clamp(_ newValue: Float) -> Float {
        max(range.lowerBound, min(newValue, range.upperBound))
    }

    private func animate(to newValue: Float) {
        let clampedValue = clamp(newValue)
        guard clampedValue != self.displayedValue else { return }
        self.pendingValues.append(clampedValue)
        startNextAnimationIfNeeded()
    }

    private func startNextAnimationIfNeeded() {
        guard !self.isAnimatingValue, !self.pendingValues.isEmpty else { return }
        let nextValue = self.pendingValues.removeFirst()
        self.isAnimatingValue = true
        withAnimation(.easeInOut(duration: animationDuration)) {
            self.displayedValue = nextValue
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + animationDuration) {
            if self.isAnimatingValue {
                self.isAnimatingValue = false
                self.startNextAnimationIfNeeded()
            }
        }
    }
}
