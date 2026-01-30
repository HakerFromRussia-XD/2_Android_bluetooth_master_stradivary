//
//  StatusBarView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.01.2026.
//

import SwiftUI

struct StatusBarView: View {
    enum Constants {
        static let height: CGFloat = 44
        static let iconSize: CGFloat = 22
        static let statusDotSize: CGFloat = 10
        static let batteryRingSize: CGFloat = 28
    }

    @ObservedObject var viewModel: StatusBarViewModel

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle")
                .font(.system(size: Constants.iconSize, weight: .regular))
                .foregroundColor(Color("ubi4_white"))
                .accessibilityLabel(Text("Вход в личный кабинет"))

            Spacer()

            HStack(spacing: 8) {
                Image(viewModel.isConnected ? "connect_status" : "disconnect_status")
                    .resizable()
                    .frame(width: Constants.statusDotSize, height: Constants.statusDotSize)
                    .accessibilityLabel(Text(viewModel.isConnected ? "Соединение установлено" : "Соединение потеряно"))

                Text(viewModel.serialNumber)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Color("ubi4_white"))
                    .lineLimit(1)
                    .accessibilityLabel(Text("Серийный номер устройства"))
            }

            Spacer()

            BatteryRingView(level: viewModel.batteryLevel)
                .frame(width: Constants.batteryRingSize, height: Constants.batteryRingSize)
                .accessibilityLabel(Text("Уровень заряда"))
        }
        .padding(.horizontal, 16)
        .frame(height: Constants.height)
        .background(Color("ubi4_back"))
    }
}

private struct BatteryRingView: View {
    private let normalizedLevel: Double

    init(level: Double) {
        normalizedLevel = min(max(level, 0.0), 1.0)
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color("ubi4_gray_border"), lineWidth: 3)

            Circle()
                .trim(from: 0, to: normalizedLevel)
                .stroke(
                    Color("ubi4_yes_system_blue"),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            Text("\(Int(normalizedLevel * 100))%")
                .font(.system(size: 9, weight: .semibold))
                .foregroundColor(Color("ubi4_white"))
        }
    }
}
