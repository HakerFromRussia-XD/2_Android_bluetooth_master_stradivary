import UIKit
import SwiftUI
import shared

extension Notification.Name {
    static let v3DeviceNameDidUpdate = Notification.Name("V3DeviceNameDidUpdate")
}

final class TextInputViewCellV3: UITableViewCell {
    static let reuseIdentifier = String(describing: TextInputViewCellV3.self)

    private let keyValueStorage: KeyValueStorage = UserDefaultsKeyValueStorage()
    private var viewModel: TextInputListItemViewModelV3?

    override func prepareForReuse() {
        super.prepareForReuse()
        viewModel = nil
        contentConfiguration = nil
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: TextInputListItemViewModelV3) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        var configuration = UIHostingConfiguration {
            TextInputRowViewV3(
                placeholder: viewModel.placeholder,
                buttonTitle: viewModel.buttonTitle,
                initialText: prefilledText(),
                trimToLimit: { [weak self] text in
                    self?.viewModel?.trimToByteLimit(text) ?? text
                },
                onLimitReached: { [weak self] in
                    self?.showToast("Лимит символов исчерпан")
                },
                onRequestPrefill: { [weak self] in
                    self?.prefilledText() ?? ""
                },
                onSend: { [weak self] text in
                    self?.handleSend(text)
                }
            )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration
    }

    private func prefilledText() -> String {
        let storedName = (try? keyValueStorage.load(for: BluetoothStorageKeys.selectedDeviceNameStorageKey)) ?? ""
        return viewModel?.prefillDisplayName(storedFullName: storedName) ?? ""
    }

    private func handleSend(_ input: String) {
        let normalizedInput = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedInput.isEmpty else {
            showToast("Введите текст")
            return
        }

        guard let fullName = viewModel?.sendInput(normalizedInput) else {
            showToast("Ошибка отправки")
            return
        }

        try? keyValueStorage.save(fullName, for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
        let displayName = DeviceNameBridgeV3.shared.displayName(deviceName: fullName)
        NotificationCenter.default.post(name: .v3DeviceNameDidUpdate, object: displayName)
        showToast("Имя установлено")
    }

    private func showToast(_ message: String) {
        let hostView = window ?? contentView
        let toast = UILabel()
        toast.text = message
        toast.textAlignment = .center
        toast.font = UIFont.systemFont(ofSize: 14)
        toast.textColor = .white
        toast.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        toast.layer.cornerRadius = 10
        toast.clipsToBounds = true
        toast.alpha = 0

        let padding: CGFloat = 24
        let textWidth = toast.intrinsicContentSize.width + padding
        let maxWidth = hostView.bounds.width - 32
        let width = min(textWidth, maxWidth)
        let height: CGFloat = 38
        let y = hostView.bounds.height - 120
        toast.frame = CGRect(
            x: (hostView.bounds.width - width) / 2,
            y: y,
            width: width,
            height: height
        )

        hostView.addSubview(toast)
        UIView.animate(withDuration: 0.2, animations: {
            toast.alpha = 1
        }) { _ in
            UIView.animate(withDuration: 0.25, delay: 1.2, options: .curveEaseOut, animations: {
                toast.alpha = 0
            }) { _ in
                toast.removeFromSuperview()
            }
        }
    }
}

private struct TextInputRowViewV3: View {
    let placeholder: String
    let buttonTitle: String
    let initialText: String
    let trimToLimit: (String) -> String
    let onLimitReached: () -> Void
    let onRequestPrefill: () -> String
    let onSend: (String) -> Void

    @State private var inputText: String = ""
    @State private var didApplyInitial = false

    var body: some View {
        HStack(spacing: 8) {
            VStack(spacing: 2) {
                TextField(
                    "",
                    text: $inputText,
                    prompt: Text(placeholder).foregroundColor(Color("ubi4_deactivate_text"))
                )
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(Color("ubi4_white"))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .padding(.leading, 10)
                .padding(.trailing, 4)
                .onTapGesture {
                    inputText = onRequestPrefill()
                }
                .onChange(of: inputText) { newValue in
                    let trimmed = trimToLimit(newValue)
                    if trimmed != newValue {
                        inputText = trimmed
                        onLimitReached()
                    }
                }

                Rectangle()
                    .fill(Color("ubi4_deactivate_text"))
                    .frame(height: 1)
                    .padding(.horizontal, 10)
            }
            .frame(maxWidth: .infinity)

            Button(action: {
                onSend(inputText)
            }) {
                Text(buttonTitle)
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(Color("ubi4_white"))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.plain)
            .frame(width: 130, height: 40)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color("ubi4_gray"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                    )
            )
        }
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity, minHeight: 56, maxHeight: 56)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_back"))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
        .onAppear {
            guard !didApplyInitial else { return }
            didApplyInitial = true
            inputText = initialText
        }
    }
}
