import UIKit
import SwiftUI
import Combine
import shared

final class CommandViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing:CommandViewCell.self)
    private let textInputWidgetCode = 0x1A
    private var viewModel: CommandListItemViewModel!
    private let mainQueue: DispatchQueueType = DispatchQueue.main
    private var numberCancellable: AnyCancellable?
    static let height = CGFloat(56)
    
    // Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    private var cancellable: AnyCancellable?
    
    override func awakeFromNib() { super.awakeFromNib() }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: CommandListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        let baseStruct = WidgetMetadataExtractor.extractBaseStruct(from: viewModel.widget.widget?.value)
        let widgetCode = Int(baseStruct?.widgetCode ?? -1)
        let (inputTitle, buttonTitle) = splitTextInputTitle(viewModel.title)

        var configuration: UIHostingConfiguration<AnyView>
        if widgetCode == textInputWidgetCode {
            configuration = UIHostingConfiguration {
                AnyView(
                    TextInputCommandWidgetView(
                        placeholder: inputTitle,
                        buttonTitle: buttonTitle
                    ) { [weak self] text in
                        let normalized = text.trimmingCharacters(in: .whitespacesAndNewlines)
                        self?.showToast(normalized.isEmpty ? "Введите текст" : normalized)
                    }
                )
            }
        } else {
            configuration = UIHostingConfiguration {
                AnyView(
                    CustomButton(
                        title: viewModel.title,
                        onPress: {
                            viewModel.didPressDown()
                        },
                        onRelease: {
                            viewModel.didRelease()
                        }
                    )
                )
            }
        }

        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration
        numberCancellable?.cancel()
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        contentConfiguration = nil
    }
    
    private func updateUI(_ ref: ParameterRef, viewModel: CommandListItemViewModel) {
        print("[BLE-COMMUNICATION] in updateUI")
        print("[BLE-COMMUNICATION] in updateUI viewModel.deviceAddress = \(viewModel.widget.deviceAddress)")
        print("[BLE-COMMUNICATION] in updateUI viewModel.parameterID = \(viewModel.widget.parameterID)")
        guard ref.addressDevice == viewModel.widget.deviceAddress,
              ref.parameterID   == viewModel.widget.parameterID else { return }
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        print("[BLE-COMMUNICATION] in updateUI for ref = \(ref)")
        
        
        viewModel.widget.commandUnified?.clickCommand

    }

    private func splitTextInputTitle(_ rawTitle: String) -> (String, String) {
        let parts = rawTitle
            .split(separator: "%", omittingEmptySubsequences: false)
            .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }

        let placeholder = parts.first?.isEmpty == false ? parts.first! : "Введите текст"
        let buttonTitle = parts.count > 1 && !parts[1].isEmpty ? parts[1] : "Отправить"
        return (placeholder, buttonTitle)
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

private struct TextInputCommandWidgetView: View {
    let placeholder: String
    let buttonTitle: String
    let onSend: (String) -> Void

    @State private var inputText: String = ""

    var body: some View {
        HStack(spacing: 8) {
            TextField("", text: $inputText, prompt: Text(placeholder).foregroundColor(.white.opacity(0.6)))
                .font(.system(size: 12, weight: .light))
                .foregroundColor(.white)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .padding(.leading, 10)
                .padding(.trailing, 4)

            Button(action: {
                onSend(inputText)
            }) {
                Text(buttonTitle)
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(RoundedRectangle(cornerRadius: 10))
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
        .frame(maxWidth: .infinity, minHeight: 48, maxHeight: 48)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_back"))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
    }
}
