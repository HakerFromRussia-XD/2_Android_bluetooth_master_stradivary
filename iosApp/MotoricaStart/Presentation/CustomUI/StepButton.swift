import SwiftUI

struct StepButton: View {
    let label: String
    let action: () -> Void

    // Совместимость со старым API: StepButton(title: "…") { … }
    init(title: String, action: @escaping () -> Void) {
        self.label = title
        self.action = action
    }

    init(label: String, action: @escaping () -> Void) {
        self.label = label
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.custom("SFProDisplay-Light", size: 14)) // убедись, что шрифт добавлен и имя совпадает с PostScript Name
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
        .buttonStyle(.plain)
    }
}

#Preview {
    StepButton(label: "+") { print("Button tapped") }
        .previewLayout(.sizeThatFits)
        .padding()
        .background(Color.ubi4Back)
}
