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
            onSelect: { file in
                onSelect(file)
            },
            onDelete: onDelete
        )
        present(dialog)
    }

    func showConfirmSendFirmwareFile(
        onConfirm: @escaping () -> Void
    ) {
        if let filesDialog = currentDialog as? FirmwareFilesDialogViewController {
            filesDialog.showConfirm(
                title: NSLocalizedString("Are you sure you want to update your software version?", comment: ""),
                onConfirm: { [weak self] in
                    self?.dismissCurrent(animated: true) {
                        DispatchQueue.main.async {
                            onConfirm()
                        }
                    }
                },
                onCancel: { [weak self] in
                    self?.dismissCurrent(animated: true)
                }
            )
            return
        }

        let dialog = FirmwareConfirmDialogViewController(
            title: NSLocalizedString("Are you sure you want to update your software version?", comment: ""),
            colors: colors,
            onConfirm: { [weak self] in
                self?.dismissCurrent(animated: true) {
                    DispatchQueue.main.async {
                        onConfirm()
                    }
                }
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

    func dismissCurrent(animated: Bool, completion: (() -> Void)? = nil) {
        guard let currentDialog = self.currentDialog else {
            completion?()
            return
        }
        self.currentDialog = nil
        if animated, let firmwareDialog = currentDialog as? FirmwareBaseDialogViewController {
            firmwareDialog.animateOut {
                currentDialog.dismiss(animated: false) {
                    completion?()
                }
            }
        } else {
            currentDialog.dismiss(animated: false) {
                completion?()
            }
        }
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
        dismissCurrent(animated: false) { [weak self] in
            guard let self else { return }
            DispatchQueue.main.async {
                self.currentDialog = dialog
                dialog.modalPresentationStyle = .overFullScreen
                self.presentingViewController?.present(dialog, animated: false) {
                    (dialog as? FirmwareBaseDialogViewController)?.animateIn()
                }
            }
        }
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
    private enum Animation {
        static let duration: TimeInterval = 0.3
    }

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
        view.backgroundColor = UIColor.black.withAlphaComponent(0.65)
        view.alpha = 0
        card.translatesAutoresizingMaskIntoConstraints = false
        card.backgroundColor = colors.gray
        card.layer.cornerRadius = 14
        card.layer.borderWidth = 0
        card.layer.borderColor = colors.border.cgColor
        card.layer.masksToBounds = true
        view.addSubview(card)
        NSLayoutConstraint.activate([
            card.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            card.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            card.widthAnchor.constraint(equalToConstant: 270)
        ])
    }

    func animateIn() {
        view.alpha = 0
        UIView.animate(
            withDuration: Animation.duration,
            delay: 0,
            options: [.curveEaseInOut, .beginFromCurrentState]
        ) {
            self.view.alpha = 1
        }
    }

    func animateOut(completion: @escaping () -> Void) {
        UIView.animate(
            withDuration: Animation.duration,
            delay: 0,
            options: [.curveEaseInOut, .beginFromCurrentState]
        ) {
            self.view.alpha = 0
        } completion: { _ in
            completion()
        }
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

private final class FirmwareFilesDialogViewController: FirmwareBaseDialogViewController, UITableViewDataSource, UITableViewDelegate {
    private var files: [AccountFirmwareFile]
    private let onSelect: (AccountFirmwareFile) -> Void
    private let onDelete: (AccountFirmwareFile) -> Void
    private let tableView = UITableView(frame: .zero, style: .plain)
    private let cellReuseIdentifier = "FirmwareFileCell"

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

        tableView.backgroundColor = .clear
        tableView.separatorColor = colors.border
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 10, bottom: 0, right: 10)
        tableView.rowHeight = 44
        tableView.tableFooterView = UIView()
        tableView.allowsSelection = true
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(FirmwareFileCell.self, forCellReuseIdentifier: cellReuseIdentifier)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        listContainer.addSubview(tableView)
        updateEmptyState()

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

            tableView.topAnchor.constraint(equalTo: listContainer.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: listContainer.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: listContainer.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: listContainer.bottomAnchor),

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

    func showConfirm(title: String, onConfirm: @escaping () -> Void, onCancel: @escaping () -> Void) {
        UIView.transition(
            with: card,
            duration: 0.3,
            options: [.transitionCrossDissolve, .beginFromCurrentState]
        ) {
            self.configureConfirmContent(title: title, onConfirm: onConfirm, onCancel: onCancel)
        }
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        files.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = (tableView.dequeueReusableCell(withIdentifier: cellReuseIdentifier) as? FirmwareFileCell)
            ?? FirmwareFileCell(style: .default, reuseIdentifier: cellReuseIdentifier)
        let file = files[indexPath.row]

        cell.configure(file: file, colors: colors) { [weak self] in
            self?.select(file)
        }

        if file.isDeletable {
            let delete = UIButton(type: .system)
            delete.setImage(UIImage(systemName: "trash"), for: .normal)
            delete.tintColor = colors.text
            delete.frame = CGRect(x: 0, y: 0, width: 44, height: 44)
            delete.addAction(UIAction { [weak self] _ in
                guard let self else { return }
                self.onDelete(file)
                self.files.removeAll { $0.url == file.url }
                self.updateEmptyState()
                tableView.reloadData()
            }, for: .touchUpInside)
            cell.accessoryView = delete
        } else {
            cell.accessoryView = nil
        }

        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        select(files[indexPath.row])
    }

    private func select(_ file: AccountFirmwareFile) {
        view.accessibilityValue = "selected=\(file.name)"
        tableView.isUserInteractionEnabled = false
        onSelect(file)
    }

    private func updateEmptyState() {
        if files.isEmpty {
            let empty = UILabel()
            empty.text = NSLocalizedString("No firmware files", comment: "")
            empty.textColor = colors.text.withAlphaComponent(0.65)
            empty.font = .systemFont(ofSize: 12, weight: .semibold)
            empty.textAlignment = .center
            tableView.backgroundView = empty
            return
        }
        tableView.backgroundView = nil
    }

    private func configureConfirmContent(title: String, onConfirm: @escaping () -> Void, onCancel: @escaping () -> Void) {
        card.subviews.forEach { $0.removeFromSuperview() }
        card.backgroundColor = colors.gray
        card.layer.cornerRadius = 16
        view.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmDialog
        card.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmDialog

        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.textColor = colors.text
        titleLabel.font = .systemFont(ofSize: 17, weight: .bold)
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 3
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(titleLabel)

        let divider1 = UIView()
        divider1.backgroundColor = colors.border
        divider1.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(divider1)

        let ok = makeActionButton(
            title: NSLocalizedString("OK", comment: ""),
            font: .systemFont(ofSize: 18, weight: .bold),
            action: onConfirm
        )
        let cancel = makeActionButton(
            title: NSLocalizedString("Cancel", comment: ""),
            font: .systemFont(ofSize: 18, weight: .regular),
            action: onCancel
        )
        ok.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmOkButton
        cancel.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmCancelButton
        card.addSubview(ok)

        let divider2 = UIView()
        divider2.backgroundColor = colors.border
        divider2.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(divider2)
        card.addSubview(cancel)

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            titleLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            divider1.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 16),
            divider1.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider1.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider1.heightAnchor.constraint(equalToConstant: 1),

            ok.topAnchor.constraint(equalTo: divider1.bottomAnchor),
            ok.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            ok.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            ok.heightAnchor.constraint(equalToConstant: 44),

            divider2.topAnchor.constraint(equalTo: ok.bottomAnchor),
            divider2.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider2.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider2.heightAnchor.constraint(equalToConstant: 1),

            cancel.topAnchor.constraint(equalTo: divider2.bottomAnchor),
            cancel.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            cancel.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            cancel.heightAnchor.constraint(equalToConstant: 52),
            cancel.bottomAnchor.constraint(equalTo: card.bottomAnchor)
        ])
    }
}

private final class FirmwareFileCell: UITableViewCell {
    private var onSelect: (() -> Void)?
    private let titleButton = UIButton(type: .system)

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
        contentView.backgroundColor = .clear
        selectionStyle = .none

        titleButton.contentHorizontalAlignment = .leading
        titleButton.titleLabel?.lineBreakMode = .byTruncatingMiddle
        titleButton.addAction(UIAction { [weak self] _ in
            self?.onSelect?()
        }, for: .touchUpInside)
        titleButton.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(titleButton)

        NSLayoutConstraint.activate([
            titleButton.topAnchor.constraint(equalTo: contentView.topAnchor),
            titleButton.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 10),
            titleButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -10),
            titleButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func configure(file: AccountFirmwareFile, colors: FirmwareDialogColors, onSelect: @escaping () -> Void) {
        self.onSelect = onSelect
        let identifier = "\(AccessibilityIdentifier.firmwareFileRowPrefix).\(accountAccessibilityKey(file.name))"
        titleButton.setTitle(file.name, for: .normal)
        titleButton.setTitleColor(colors.text, for: .normal)
        titleButton.titleLabel?.font = .systemFont(ofSize: 12, weight: .bold)
        titleButton.accessibilityIdentifier = identifier
        titleButton.accessibilityLabel = file.name
        titleButton.accessibilityValue = "file=\(file.name);deletable=\(file.isDeletable)"
        titleButton.accessibilityTraits.insert(.button)
        isAccessibilityElement = false
        accessibilityIdentifier = identifier
        accessibilityLabel = file.name
        accessibilityValue = "file=\(file.name);deletable=\(file.isDeletable)"
    }

    override func accessibilityActivate() -> Bool {
        onSelect?()
        return true
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
        card.layer.cornerRadius = 16
        view.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmDialog
        card.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmDialog

        let title = UILabel()
        title.text = dialogTitle
        title.textColor = colors.text
        title.font = .systemFont(ofSize: 17, weight: .bold)
        title.textAlignment = .center
        title.numberOfLines = 3
        title.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(title)

        let divider1 = UIView()
        divider1.backgroundColor = colors.border
        divider1.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(divider1)
        let ok = makeActionButton(title: NSLocalizedString("OK", comment: ""), font: .systemFont(ofSize: 18, weight: .bold), action: onConfirm)
        let cancel = makeActionButton(title: NSLocalizedString("Cancel", comment: ""), font: .systemFont(ofSize: 18, weight: .regular), action: onCancel)
        ok.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmOkButton
        cancel.accessibilityIdentifier = AccessibilityIdentifier.firmwareConfirmCancelButton
        card.addSubview(ok)
        let divider2 = UIView()
        divider2.backgroundColor = colors.border
        divider2.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(divider2)
        card.addSubview(cancel)

        NSLayoutConstraint.activate([
            title.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            title.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            title.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            divider1.topAnchor.constraint(equalTo: title.bottomAnchor, constant: 16),
            divider1.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider1.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider1.heightAnchor.constraint(equalToConstant: 1),

            ok.topAnchor.constraint(equalTo: divider1.bottomAnchor),
            ok.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            ok.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            ok.heightAnchor.constraint(equalToConstant: 44),

            divider2.topAnchor.constraint(equalTo: ok.bottomAnchor),
            divider2.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider2.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider2.heightAnchor.constraint(equalToConstant: 1),

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
        title.text = NSLocalizedString("File is being downloaded", comment: "")
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
