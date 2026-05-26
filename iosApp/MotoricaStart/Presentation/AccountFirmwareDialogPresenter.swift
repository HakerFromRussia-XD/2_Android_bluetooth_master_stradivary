import UIKit

final class AccountFirmwareDialogPresenter {
    private weak var presentingViewController: UIViewController?
    private var currentDialog: UIViewController?

    private let backColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
    private let grayColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
    private let borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444)
    private let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
    private let actionColor = UIColor.accountColor("ubi4_yes_system_blue", fallback: 0x4A91FF)
    private let activeColor = UIColor.accountColor("ubi4_active", fallback: 0xC6F158)

    init(presentingViewController: UIViewController) {
        self.presentingViewController = presentingViewController
    }

    func showFirmwareFiles(
        files: [AccountFirmwareFile],
        onSelect: @escaping (AccountFirmwareFile) -> Void,
        onDelete: @escaping (AccountFirmwareFile) -> Void
    ) {
        let dialog = FirmwareFilesDialogViewController(
            files: files,
            colors: colors,
            onSelect: { [weak self] file in
                self?.dismissCurrent(animated: false)
                onSelect(file)
            },
            onDelete: onDelete
        )
        present(dialog)
    }

    func showConfirmSendFirmwareFile(
        onConfirm: @escaping () -> Void
    ) {
        let dialog = FirmwareConfirmDialogViewController(
            title: NSLocalizedString("Are you sure you want to update software version?", comment: ""),
            colors: colors,
            onConfirm: { [weak self] in
                self?.dismissCurrent(animated: false)
                onConfirm()
            },
            onCancel: { [weak self] in
                self?.dismissCurrent(animated: true)
            }
        )
        present(dialog)
    }

    func showProgress() -> FirmwareProgressDialogViewController {
        let dialog = FirmwareProgressDialogViewController(colors: colors)
        present(dialog)
        return dialog
    }

    func showWarning(title: String, message: String) {
        let dialog = FirmwareWarningDialogViewController(
            title: title,
            message: message,
            colors: colors,
            onClose: { [weak self] in self?.dismissCurrent(animated: true) }
        )
        present(dialog)
    }

    func dismissCurrent(animated: Bool) {
        currentDialog?.dismiss(animated: animated)
        currentDialog = nil
    }

    private var colors: FirmwareDialogColors {
        FirmwareDialogColors(
            back: backColor,
            gray: grayColor,
            border: borderColor,
            text: textColor,
            action: actionColor,
            active: activeColor
        )
    }

    private func present(_ dialog: UIViewController) {
        dismissCurrent(animated: false)
        currentDialog = dialog
        dialog.modalPresentationStyle = .overFullScreen
        dialog.modalTransitionStyle = .crossDissolve
        presentingViewController?.present(dialog, animated: true)
    }
}

struct FirmwareDialogColors {
    let back: UIColor
    let gray: UIColor
    let border: UIColor
    let text: UIColor
    let action: UIColor
    let active: UIColor
}

class FirmwareBaseDialogViewController: UIViewController {
    let card = UIView()
    let colors: FirmwareDialogColors

    init(colors: FirmwareDialogColors) {
        self.colors = colors
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.black.withAlphaComponent(0.45)
        card.translatesAutoresizingMaskIntoConstraints = false
        card.backgroundColor = colors.gray
        card.layer.cornerRadius = 16
        card.layer.masksToBounds = true
        view.addSubview(card)
        NSLayoutConstraint.activate([
            card.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            card.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            card.widthAnchor.constraint(equalToConstant: 270)
        ])
    }

    func addDivider(below view: UIView) -> UIView {
        let divider = UIView()
        divider.backgroundColor = colors.border
        divider.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(divider)
        NSLayoutConstraint.activate([
            divider.topAnchor.constraint(equalTo: view.bottomAnchor),
            divider.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider.heightAnchor.constraint(equalToConstant: 1)
        ])
        return divider
    }

    func makeActionButton(title: String, font: UIFont, action: @escaping () -> Void) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.setTitleColor(colors.action, for: .normal)
        button.titleLabel?.font = font
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }
}

private final class FirmwareFilesDialogViewController: FirmwareBaseDialogViewController {
    private var files: [AccountFirmwareFile]
    private let onSelect: (AccountFirmwareFile) -> Void
    private let onDelete: (AccountFirmwareFile) -> Void
    private let stack = UIStackView()

    init(
        files: [AccountFirmwareFile],
        colors: FirmwareDialogColors,
        onSelect: @escaping (AccountFirmwareFile) -> Void,
        onDelete: @escaping (AccountFirmwareFile) -> Void
    ) {
        self.files = files
        self.onSelect = onSelect
        self.onDelete = onDelete
        super.init(colors: colors)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        card.backgroundColor = colors.back
        view.accessibilityIdentifier = AccessibilityIdentifier.firmwareFilesDialog
        card.accessibilityIdentifier = AccessibilityIdentifier.firmwareFilesDialog

        let title = UILabel()
        title.text = NSLocalizedString("Select firmware file", comment: "")
        title.textColor = colors.text
        title.font = .systemFont(ofSize: 18, weight: .bold)
        title.textAlignment = .center
        title.numberOfLines = 2
        title.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(title)

        let listContainer = UIView()
        listContainer.backgroundColor = colors.gray
        listContainer.layer.cornerRadius = 12
        listContainer.layer.borderWidth = 1
        listContainer.layer.borderColor = colors.border.cgColor
        listContainer.layer.masksToBounds = true
        listContainer.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(listContainer)

        stack.axis = .vertical
        stack.spacing = 0
        stack.translatesAutoresizingMaskIntoConstraints = false
        listContainer.addSubview(stack)
        rebuildRows()

        let divider = UIView()
        divider.backgroundColor = colors.border
        divider.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(divider)
        let cancel = makeActionButton(
            title: NSLocalizedString("Cancel", comment: ""),
            font: .systemFont(ofSize: 17, weight: .regular)
        ) { [weak self] in
            self?.dismiss(animated: true)
        }
        card.addSubview(cancel)

        NSLayoutConstraint.activate([
            title.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            title.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            title.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            listContainer.topAnchor.constraint(equalTo: title.bottomAnchor, constant: 16),
            listContainer.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            listContainer.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            listContainer.heightAnchor.constraint(equalToConstant: 352),

            stack.topAnchor.constraint(equalTo: listContainer.topAnchor),
            stack.leadingAnchor.constraint(equalTo: listContainer.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: listContainer.trailingAnchor),
            stack.bottomAnchor.constraint(lessThanOrEqualTo: listContainer.bottomAnchor),

            divider.topAnchor.constraint(equalTo: listContainer.bottomAnchor, constant: 16),
            divider.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider.heightAnchor.constraint(equalToConstant: 1),

            cancel.topAnchor.constraint(equalTo: divider.bottomAnchor),
            cancel.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            cancel.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            cancel.heightAnchor.constraint(equalToConstant: 52),
            cancel.bottomAnchor.constraint(equalTo: card.bottomAnchor)
        ])
    }

    private func rebuildRows() {
        stack.arrangedSubviews.forEach {
            stack.removeArrangedSubview($0)
            $0.removeFromSuperview()
        }

        if files.isEmpty {
            let empty = UILabel()
            empty.text = NSLocalizedString("No firmware files", comment: "")
            empty.textColor = colors.text.withAlphaComponent(0.65)
            empty.font = .systemFont(ofSize: 12, weight: .semibold)
            empty.textAlignment = .center
            empty.heightAnchor.constraint(equalToConstant: 44).isActive = true
            stack.addArrangedSubview(empty)
            return
        }

        files.forEach { file in
            let row = FirmwareFileRow(colors: colors, file: file) { [weak self] in
                self?.onSelect(file)
            } onDelete: { [weak self] in
                guard let self else { return }
                self.onDelete(file)
                self.files.removeAll { $0.url == file.url }
                self.rebuildRows()
            }
            stack.addArrangedSubview(row)
        }
    }
}

private final class FirmwareFileRow: UIControl {
    init(
        colors: FirmwareDialogColors,
        file: AccountFirmwareFile,
        onSelect: @escaping () -> Void,
        onDelete: @escaping () -> Void
    ) {
        super.init(frame: .zero)
        heightAnchor.constraint(equalToConstant: 44).isActive = true
        isAccessibilityElement = true
        accessibilityIdentifier = "\(AccessibilityIdentifier.firmwareFileRowPrefix).\(accountAccessibilityKey(file.name))"
        accessibilityLabel = file.name
        accessibilityValue = "file=\(file.name);deletable=\(file.isDeletable)"

        let title = UILabel()
        title.text = file.name
        title.textColor = colors.text
        title.font = .systemFont(ofSize: 12, weight: .bold)
        title.lineBreakMode = .byTruncatingMiddle
        title.translatesAutoresizingMaskIntoConstraints = false
        addSubview(title)

        let delete = UIButton(type: .system)
        delete.setImage(UIImage(systemName: "trash"), for: .normal)
        delete.tintColor = colors.text
        delete.isHidden = !file.isDeletable
        delete.addAction(UIAction { [weak delete] _ in
            delete?.isUserInteractionEnabled = false
            onDelete()
        }, for: .touchUpInside)
        delete.translatesAutoresizingMaskIntoConstraints = false
        addSubview(delete)

        let divider = UIView()
        divider.backgroundColor = colors.border
        divider.translatesAutoresizingMaskIntoConstraints = false
        addSubview(divider)

        addAction(UIAction { _ in onSelect() }, for: .touchUpInside)

        NSLayoutConstraint.activate([
            title.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            title.trailingAnchor.constraint(lessThanOrEqualTo: delete.leadingAnchor, constant: -10),
            title.centerYAnchor.constraint(equalTo: centerYAnchor),

            delete.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            delete.centerYAnchor.constraint(equalTo: centerYAnchor),
            delete.widthAnchor.constraint(equalToConstant: 24),
            delete.heightAnchor.constraint(equalToConstant: 24),

            divider.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10),
            divider.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10),
            divider.bottomAnchor.constraint(equalTo: bottomAnchor),
            divider.heightAnchor.constraint(equalToConstant: 1)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private final class FirmwareConfirmDialogViewController: FirmwareBaseDialogViewController {
    private let dialogTitle: String
    private let onConfirm: () -> Void
    private let onCancel: () -> Void

    init(
        title: String,
        colors: FirmwareDialogColors,
        onConfirm: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.dialogTitle = title
        self.onConfirm = onConfirm
        self.onCancel = onCancel
        super.init(colors: colors)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        let title = UILabel()
        title.text = dialogTitle
        title.textColor = colors.text
        title.font = .systemFont(ofSize: 17, weight: .bold)
        title.textAlignment = .center
        title.numberOfLines = 3
        title.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(title)

        let divider1 = addDivider(below: title)
        let ok = makeActionButton(title: NSLocalizedString("OK", comment: ""), font: .systemFont(ofSize: 18, weight: .bold), action: onConfirm)
        let divider2 = addDivider(below: ok)
        let cancel = makeActionButton(title: NSLocalizedString("Cancel", comment: ""), font: .systemFont(ofSize: 18, weight: .regular), action: onCancel)
        card.addSubview(ok)
        card.addSubview(cancel)

        NSLayoutConstraint.activate([
            title.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            title.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            title.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            title.bottomAnchor.constraint(equalTo: card.topAnchor, constant: 72),

            ok.topAnchor.constraint(equalTo: divider1.bottomAnchor),
            ok.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            ok.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            ok.heightAnchor.constraint(equalToConstant: 44),

            divider2.topAnchor.constraint(equalTo: ok.bottomAnchor),

            cancel.topAnchor.constraint(equalTo: divider2.bottomAnchor),
            cancel.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            cancel.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            cancel.heightAnchor.constraint(equalToConstant: 52),
            cancel.bottomAnchor.constraint(equalTo: card.bottomAnchor)
        ])
    }
}

final class FirmwareProgressDialogViewController: FirmwareBaseDialogViewController {
    private let progressView = UIProgressView(progressViewStyle: .default)

    override init(colors: FirmwareDialogColors) {
        super.init(colors: colors)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        card.backgroundColor = colors.back
        view.accessibilityIdentifier = AccessibilityIdentifier.firmwareProgressDialog
        card.accessibilityIdentifier = AccessibilityIdentifier.firmwareProgressDialog

        let title = UILabel()
        title.text = NSLocalizedString("Firmware is being downloaded", comment: "")
        title.textColor = colors.text
        title.font = .systemFont(ofSize: 18, weight: .bold)
        title.textAlignment = .center
        title.numberOfLines = 2
        title.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(title)

        progressView.progressTintColor = colors.active
        progressView.trackTintColor = colors.border
        progressView.isAccessibilityElement = true
        progressView.accessibilityIdentifier = AccessibilityIdentifier.firmwareProgressBar
        progressView.accessibilityValue = "progress=0"
        progressView.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(progressView)

        NSLayoutConstraint.activate([
            card.heightAnchor.constraint(equalToConstant: 120),

            title.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            title.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            title.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            progressView.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 8),
            progressView.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -8),
            progressView.centerYAnchor.constraint(equalTo: card.centerYAnchor, constant: 26)
        ])
    }

    func update(progress: Int) {
        progressView.setProgress(Float(progress) / 100, animated: true)
        progressView.accessibilityValue = "progress=\(progress)"
    }
}

private final class FirmwareWarningDialogViewController: FirmwareBaseDialogViewController {
    private let dialogTitle: String
    private let dialogMessage: String
    private let onClose: () -> Void

    init(title: String, message: String, colors: FirmwareDialogColors, onClose: @escaping () -> Void) {
        self.dialogTitle = title
        self.dialogMessage = message
        self.onClose = onClose
        super.init(colors: colors)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        let icon = UIImageView(image: UIImage(systemName: "exclamationmark.circle"))
        icon.tintColor = colors.action
        icon.contentMode = .scaleAspectFit
        icon.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(icon)

        let title = UILabel()
        title.text = dialogTitle
        title.textColor = colors.text
        title.font = .systemFont(ofSize: 17, weight: .bold)
        title.textAlignment = .center
        title.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(title)

        let message = UILabel()
        message.text = dialogMessage
        message.textColor = colors.text
        message.font = .systemFont(ofSize: 13, weight: .semibold)
        message.textAlignment = .center
        message.numberOfLines = 0
        message.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(message)

        let divider = UIView()
        divider.backgroundColor = colors.border
        divider.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(divider)
        let ok = makeActionButton(title: NSLocalizedString("OK", comment: ""), font: .systemFont(ofSize: 18, weight: .bold), action: onClose)
        card.addSubview(ok)

        NSLayoutConstraint.activate([
            icon.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            icon.centerXAnchor.constraint(equalTo: card.centerXAnchor),
            icon.widthAnchor.constraint(equalToConstant: 50),
            icon.heightAnchor.constraint(equalToConstant: 50),

            title.topAnchor.constraint(equalTo: icon.bottomAnchor, constant: 12),
            title.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            title.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            message.topAnchor.constraint(equalTo: title.bottomAnchor, constant: 2),
            message.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            message.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            divider.topAnchor.constraint(equalTo: message.bottomAnchor, constant: 16),
            divider.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider.heightAnchor.constraint(equalToConstant: 1),

            ok.topAnchor.constraint(equalTo: divider.bottomAnchor),
            ok.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            ok.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            ok.heightAnchor.constraint(equalToConstant: 52),
            ok.bottomAnchor.constraint(equalTo: card.bottomAnchor)
        ])
    }
}
