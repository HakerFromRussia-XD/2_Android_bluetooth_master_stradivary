//
//  CustomSwitcher.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.09.2025.
//
import SwiftUI

struct CustomSwitcher: View {
    let title: String
    @Binding var isOn: Bool

    var showsIndicator: Bool = false
    var indicatorColor: Color = .red
    var cornerRadius: CGFloat = 16
    var innerHeight: CGFloat = 54

    var fill = Color("ubi4_gray")
    var stroke = Color("ubi4_gray_border")
    var textColor: Color = .white

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundStyle(textColor)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)

            if showsIndicator {
                Circle()
                    .fill(indicatorColor)
                    .frame(width: 10, height: 10)
            }

            Toggle("", isOn: $isOn)
                .labelsHidden()
                .toggleStyle(UbiSwitchStyle())
                .contentShape(Rectangle())
        }
        .frame(height: innerHeight)
        .padding(.horizontal, 8)
        .background(
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(fill)
                .overlay(
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .stroke(stroke, lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
        )
        .padding(.horizontal, 0)
        .padding(.vertical, 0)
        .background(Color("ubi4_back"))
    }
}

struct UbiSwitchStyle: ToggleStyle {
    @Environment(\.isEnabled) private var isEnabled

    var width: CGFloat = 52
    var height: CGFloat = 32
    var thumb: CGFloat = 28

    var onTrack = Color("ubi4_active")
    var offTrack = Color("ubi4_gray")

    var thumbOn = Color("ubi4_white")
    var thumbOff = Color("ubi4_deactivate_text")

    var border = Color("ubi4_gray_border")

    func makeBody(configuration: Configuration) -> some View {
        let currentThumb: Color = {
            if !isEnabled { return thumbOff }
            return configuration.isOn ? thumbOn : thumbOff
        }()

        let offsetX = (width / 2 - thumb / 2 - 2)

        return Button {
            withAnimation(.spring(response: 0.22, dampingFraction: 0.85)) {
                configuration.isOn.toggle()
            }
        } label: {
            ZStack {
                Capsule()
                    .fill(configuration.isOn ? onTrack : offTrack)
                    .overlay(Capsule().stroke(border, lineWidth: 1))
                    .frame(width: width, height: height)

                Circle()
                    .fill(currentThumb)
                    .shadow(radius: 0.5, y: 0.5)
                    .frame(width: thumb, height: thumb)
                    .offset(x: configuration.isOn ? offsetX : -offsetX)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityValue(configuration.isOn ? "On" : "Off")
    }
}


