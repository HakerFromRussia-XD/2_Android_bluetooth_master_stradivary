import UIKit
import SwiftUI
import Combine
import shared
import Foundation

final class ToggleSliderViewCellV3: UITableViewCell {
    static let reuseIdentifier = String(describing: ToggleSliderViewCellV3.self)

    private var viewModel: ToggleSliderListItemViewModelV3?
    private var provider: ToggleSliderProviderV3?
    private var toggleCancellable: AnyCancellable?
    private var job: Kotlinx_coroutines_coreJob?
    private var isProgrammaticUpdate = false

    override func prepareForReuse() {
        super.prepareForReuse()
        toggleCancellable?.cancel()
        toggleCancellable = nil
        job?.cancel(cause: nil)
        job = nil
        provider = nil
        viewModel = nil
        contentConfiguration = nil
        isProgrammaticUpdate = false
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: ToggleSliderListItemViewModelV3) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        let initial = viewModel.currentValue() ?? (enabled: false, progress: viewModel.minProgress)
        let provider = ToggleSliderProviderV3(
            title: viewModel.title,
            unitLabel: viewModel.unitLabel,
            minValue: Float(viewModel.minProgress),
            maxValue: Float(viewModel.maxProgress),
            step: 1,
            increment: max(viewModel.increment, 0.0001),
            isEnabled: initial.enabled,
            progress: Float(initial.progress)
        )
        self.provider = provider

        toggleCancellable?.cancel()
        toggleCancellable = provider.$isEnabled
            .dropFirst()
            .removeDuplicates()
            .sink { [weak self] isEnabled in
                guard let self else { return }
                if self.isProgrammaticUpdate {
                    self.isProgrammaticUpdate = false
                    return
                }
                let progress = Int(self.provider?.progress ?? Float(self.viewModel?.minProgress ?? 0))
                self.viewModel?.sendValue(enabled: isEnabled, progress: progress)
            }

        var configuration = UIHostingConfiguration {
            ToggleSliderRowViewV3(
                provider: provider,
                onSliderEditingEnded: { [weak self] finalValue in
                    guard let self else { return }
                    self.viewModel?.sendValue(
                        enabled: self.provider?.isEnabled ?? false,
                        progress: Int(finalValue.rounded())
                    )
                },
                onToggleTap: { [weak self] in
                    guard let self else { return }
                    self.provider?.isEnabled.toggle()
                },
                onStepEditingEnded: { [weak self] finalValue in
                    guard let self else { return }
                    self.viewModel?.sendValue(
                        enabled: self.provider?.isEnabled ?? false,
                        progress: Int(finalValue.rounded())
                    )
                }
        )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration

        job?.cancel(cause: nil)
        job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self, self.viewModel?.matches(snapshot: snapshot) == true else { return }
            guard let unpacked = self.viewModel?.unpack(snapshot: snapshot) else { return }
            DispatchQueue.main.async {
                self.isProgrammaticUpdate = true
                self.provider?.isEnabled = unpacked.enabled
                self.provider?.progress = Float(unpacked.progress)
            }
        }

        viewModel.requestCurrent()
    }
}

private final class ToggleSliderProviderV3: ObservableObject {
    let title: String
    let unitLabel: String
    let minValue: Float
    let maxValue: Float
    let step: Float
    let increment: Float
    @Published var isEnabled: Bool
    @Published var progress: Float

    init(
        title: String,
        unitLabel: String,
        minValue: Float,
        maxValue: Float,
        step: Float,
        increment: Float,
        isEnabled: Bool,
        progress: Float
    ) {
        self.title = title
        self.unitLabel = unitLabel
        self.minValue = minValue
        self.maxValue = max(maxValue, minValue + 1)
        self.step = max(step, 1)
        self.increment = max(increment, 0.0001)
        self.isEnabled = isEnabled
        self.progress = min(max(progress, minValue), self.maxValue)
    }
}

private struct ToggleSliderRowViewV3: View {
    @ObservedObject var provider: ToggleSliderProviderV3
    let onSliderEditingEnded: (Float) -> Void
    let onToggleTap: () -> Void
    let onStepEditingEnded: (Float) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(provider.title)
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .foregroundColor(Color("ubi4_white"))
                    .lineLimit(1)
                    .padding(.top, 8)
                    .padding(.leading, 10)

                Spacer()

                Text(formattedValueText)
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .foregroundColor(Color("ubi4_white"))
                    .padding(.top, 8)
                    .padding(.trailing, 10)

                if !provider.unitLabel.isEmpty {
                    Text(provider.unitLabel)
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                        .padding(.top, 8)
                        .padding(.trailing, 10)
                }
            }

            HStack(spacing: 8) {
                Button(action: {
                    onToggleTap()
                }) {
                    Image(systemName: "power")
                        .font(.system(size: 20, weight: .light))
                        .foregroundColor(provider.isEnabled ? Color("ubi4_active") : Color("ubi4_gray_border"))
                        .frame(width: 48, height: 30)
                }
                .buttonStyle(.plain)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color("ubi4_gray"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                        )
                )
                .padding(.leading, 8)

                ToggleStepButtonV3(
                    label: "-",
                    isEnabled: provider.isEnabled,
                    action: decrement
                )

                CustomSlider(
                    value: Binding(
                        get: { provider.progress },
                        set: { provider.progress = $0.rounded() }
                    ),
                    range: provider.minValue...provider.maxValue,
                    trackHeight: 30,
                    cornerRadius: 10,
                    borderWidth: 1,
                    activeColor: provider.isEnabled ? Color("ubi4_active") : Color("ubi4_gray_border"),
                    inactiveColor: Color("ubi4_gray"),
                    borderColor: Color("ubi4_gray_border"),
                    isEnabled: provider.isEnabled,
                    showsThumb: provider.isEnabled,
                    editingDidEnd: { value in
                        onSliderEditingEnded(value)
                    }
                )
                .padding(.leading, 16)
                .padding(.trailing, 16)
                .padding(.bottom, 8)

                ToggleStepButtonV3(
                    label: "+",
                    isEnabled: provider.isEnabled,
                    action: increment
                )
                    .padding(.trailing, 8)
            }
            .padding(.bottom, 4)
        }
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color("ubi4_gray"))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
    }

    private func decrement() {
        guard provider.isEnabled else { return }
        let next = max(provider.minValue, provider.progress - provider.step)
        guard next != provider.progress else { return }
        provider.progress = next
        onStepEditingEnded(next)
    }

    private func increment() {
        guard provider.isEnabled else { return }
        let next = min(provider.maxValue, provider.progress + provider.step)
        guard next != provider.progress else { return }
        provider.progress = next
        onStepEditingEnded(next)
    }

    private var formattedValueText: String {
        let rawValue = provider.progress.rounded()
        let multiplied = rawValue * provider.increment

        if provider.increment >= 1.0 {
            return "\(Int(multiplied))"
        }

        let divisor = Int((1.0 / provider.increment).rounded())
        let pattern = (divisor == 2 || divisor == 5 || divisor == 10) ? "%.1f" : "%.2f"
        return String(format: pattern, locale: Locale(identifier: "en_US_POSIX"), multiplied)
    }
}

private struct ToggleStepButtonV3: View {
    let label: String
    let isEnabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.custom("SFProDisplay-Light", size: 14))
                .foregroundColor(isEnabled ? Color("ubi4_white") : Color("ubi4_gray_border"))
                .frame(width: 48, height: 30)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color("ubi4_gray"))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
        )
    }
}

#if DEBUG
import SwiftUI

struct ToggleSliderRowViewV3_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            ToggleSliderRowViewV3(
                provider: ToggleSliderProviderV3(
                    title: "Чувствительность датчика открытия",
                    unitLabel: "",
                    minValue: 0,
                    maxValue: 100,
                    step: 1,
                    increment: 1,
                    isEnabled: true,
                    progress: 75
                ),
                onSliderEditingEnded: { _ in },
                onToggleTap: {},
                onStepEditingEnded: { _ in }
            )
            .previewDisplayName("Enabled")

            ToggleSliderRowViewV3(
                provider: ToggleSliderProviderV3(
                    title: "Чувствительность датчика открытия",
                    unitLabel: "",
                    minValue: 0,
                    maxValue: 100,
                    step: 1,
                    increment: 0.1,
                    isEnabled: false,
                    progress: 40
                ),
                onSliderEditingEnded: { _ in },
                onToggleTap: {},
                onStepEditingEnded: { _ in }
            )
            .previewDisplayName("Disabled")
        }
        .padding()
        .background(Color("ubi4_back"))
        .previewLayout(.sizeThatFits)
        .preferredColorScheme(.dark)
    }
}
#endif
