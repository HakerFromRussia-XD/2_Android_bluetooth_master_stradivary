import UIKit
import SwiftUI
import Combine
import shared

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
                onDisableTap: { [weak self] in
                    self?.provider?.isEnabled = false
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
    @Published var isEnabled: Bool
    @Published var progress: Float

    init(
        title: String,
        unitLabel: String,
        minValue: Float,
        maxValue: Float,
        isEnabled: Bool,
        progress: Float
    ) {
        self.title = title
        self.unitLabel = unitLabel
        self.minValue = minValue
        self.maxValue = max(maxValue, minValue + 1)
        self.isEnabled = isEnabled
        self.progress = min(max(progress, minValue), self.maxValue)
    }
}

private struct ToggleSliderRowViewV3: View {
    @ObservedObject var provider: ToggleSliderProviderV3
    let onSliderEditingEnded: (Float) -> Void
    let onDisableTap: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text(provider.title)
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .foregroundColor(Color("ubi4_white"))
                    .lineLimit(2)

                Spacer(minLength: 8)

                Text("\(Int(provider.progress.rounded()))")
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .foregroundColor(Color("ubi4_white"))

                if !provider.unitLabel.isEmpty {
                    Text(provider.unitLabel)
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)

            HStack(spacing: 8) {
                CustomSlider(
                    value: Binding(
                        get: { provider.progress },
                        set: { provider.progress = $0 }
                    ),
                    range: provider.minValue...provider.maxValue,
                    trackHeight: 30,
                    cornerRadius: 10,
                    borderWidth: 1,
                    activeColor: Color("ubi4_active"),
                    inactiveColor: Color("ubi4_gray"),
                    borderColor: Color("ubi4_gray_border"),
                    editingDidEnd: { value in
                        provider.isEnabled = true
                        onSliderEditingEnded(value)
                    }
                )
                .opacity(provider.isEnabled ? 1 : 0.55)

                Button(action: {
                    onDisableTap()
                }) {
                    Text("Выкл")
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(Color("ubi4_white"))
                        .frame(width: 72, height: 30)
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(provider.isEnabled ? Color("ubi4_gray") : Color("ubi4_active"))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                                )
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 10)
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
}
