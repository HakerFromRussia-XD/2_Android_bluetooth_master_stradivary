//
//  StatusBarView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.01.2026.
//

import SwiftUI

final class StatusBarViewModel: ObservableObject {
    static let shared = StatusBarViewModel()

    @Published var serialNumber: String = "—"
    @Published var batteryLevel: Double = 0.0
    @Published var isConnected: Bool = false

    private init() { }
}

struct StatusBarView: View {
    static let height: CGFloat = 44

    @ObservedObject var viewModel: StatusBarViewModel

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle")
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(.white)

            Spacer(minLength: 12)

            Text(viewModel.serialNumber)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .lineLimit(1)

            Spacer(minLength: 12)

            HStack(spacing: 10) {
                BatteryLevelIndicatorView(level: viewModel.batteryLevel)

                Image(viewModel.isConnected ? "connect_status" : "disconnect_status")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 12, height: 12)
                    .accessibilityLabel(viewModel.isConnected ? "Connected" : "Disconnected")
            }
        }
        .padding(.horizontal, 16)
        .frame(height: Self.height)
        .background(Color("ubi4_dark_back"))
    }
}

private struct BatteryLevelIndicatorView: View {
    let level: Double

    private var normalizedLevel: Double {
        min(max(level, 0), 1)
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.2), lineWidth: 3)

            Circle()
                .trim(from: 0, to: normalizedLevel)
                .stroke(
                    Color("ubi4_yes_system_blue"),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            Text("\(Int(normalizedLevel * 100))%")
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(.white)
        }
        .frame(width: 28, height: 28)
    }
}

final class StatusBarHostingController: UIHostingController<StatusBarView> {
    init(viewModel: StatusBarViewModel = .shared) {
        super.init(rootView: StatusBarView(viewModel: viewModel))
        view.backgroundColor = .clear
    }

    @available(*, unavailable)
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
