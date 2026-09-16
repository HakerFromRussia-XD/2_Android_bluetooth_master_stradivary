import UIKit
import Network
import shared

// Shared with the role selector so technical modes never start automatic updates.
enum UserFirmwareRoleAccess {
    static let key = "UBI4_ROLE_SELECTED_V3"
    static var selectedRole: Int {
        guard let role = UserDefaults.standard.object(forKey: key) as? Int,
              (0...2).contains(role) else { return 2 }
        return role
    }
    static func isRoleSelector(parameterID: Int, dataCode: Int) -> Bool {
        parameterID == 1 && dataCode == 15
    }
}

final class UserFirmwareUpdatePresenter: NSObject, UserFirmwareHost {
    private weak var owner: UIViewController?
    private var updates: UserFirmwareUpdates!
    private var observation: Kotlinx_coroutines_coreJob?
    private var observers: [NSObjectProtocol] = []
    private let network = NWPathMonitor()
    private var dialog: UserFirmwareProgressViewController?
    private var lastState = UserFirmwareUiState(phase: "idle", boardNumber: 0, boardCount: 0, progress: 0, detail: "")

    init(owner: UIViewController) {
        self.owner = owner
        super.init()
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let directory = base.appendingPathComponent("user_firmware", isDirectory: true)
        do { try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true) }
        catch { return }
        updates = UserFirmwareUpdates(directory: directory.path, host: self)
        observation = updates.observe { [weak self] state in
            DispatchQueue.main.async { self?.render(state) }
        }
        observers.append(NotificationCenter.default.addObserver(forName: UserDefaults.didChangeNotification, object: nil, queue: .main) { [weak self] _ in self?.refreshRole() })
        observers.append(NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { [weak self] _ in self?.foreground() })
        network.pathUpdateHandler = { [weak self] path in
            if path.status == .satisfied { DispatchQueue.main.async { self?.updates?.environmentChanged() } }
        }
        network.start(queue: DispatchQueue(label: "user-firmware-network"))
        refreshRole()
    }
    func foreground() { refreshRole(); updates?.environmentChanged(); render(lastState) }
    private func refreshRole() { updates?.setUserRole(enabled: UserFirmwareRoleAccess.selectedRole == 2) }
    private func render(_ state: UserFirmwareUiState) {
        let completedNow = state.phase == "complete" && lastState.phase != "complete"
        lastState = state
        if completedNow { BLEComponents.shared.bleManager.restartV3Synchronization() }
        UIApplication.shared.isIdleTimerDisabled = state.blocksInteraction
        guard state.blocksInteraction else {
            dialog?.dismiss(animated: false)
            dialog = nil
            return
        }
        if let dialog { dialog.render(state); return }
        guard let owner, owner.viewIfLoaded?.window != nil else { return }
        var presenter = owner
        while let presented = presenter.presentedViewController { presenter = presented }
        let progress = UserFirmwareProgressViewController()
        progress.onInstall = { [weak self] in self?.updates?.start() }
        progress.onOK = { [weak self] in self?.updates?.acknowledge() }
        progress.modalPresentationStyle = .overFullScreen
        progress.isModalInPresentation = true
        dialog = progress
        presenter.present(progress, animated: false) { progress.render(state) }
    }
    func read(path: String, callback: @escaping (UserFirmwareArchive?) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async {
            let archive = try? FirmwareArchiveReader.readArchive(at: URL(fileURLWithPath: path))
            DispatchQueue.main.async {
                callback(archive.map { UserFirmwareArchive(descriptorText: $0.descriptorText, payload: $0.payload.userFirmwareBytes()) })
            }
        }
    }
    func prepareTransfer(callback: @escaping (KotlinBoolean) -> Void) { callback(KotlinBoolean(bool: true)) }
    deinit {
        observation?.cancel(cause: nil)
        updates?.close()
        network.cancel()
        observers.forEach(NotificationCenter.default.removeObserver)
        UIApplication.shared.isIdleTimerDisabled = false
    }
}

final class UserFirmwareProgressViewController: UIViewController {
    var onInstall: (() -> Void)?
    var onOK: (() -> Void)?
    private let heading = UILabel()
    private let message = UILabel()
    private let progress = UIProgressView(progressViewStyle: .default)
    private let activity = UIActivityIndicatorView(style: .medium)
    private let button = UIButton(type: .system)
    private var complete = false

    override func viewDidLoad() {
        super.viewDidLoad()
        isModalInPresentation = true
        view.backgroundColor = UIColor.black.withAlphaComponent(0.65)
        view.accessibilityIdentifier = "user_firmware_blocking_dialog"
        let panel = UIStackView(arrangedSubviews: [heading, message, progress, activity, button])
        panel.axis = .vertical
        panel.spacing = 20
        panel.isLayoutMarginsRelativeArrangement = true
        panel.layoutMargins = UIEdgeInsets(top: 24, left: 24, bottom: 24, right: 24)
        panel.backgroundColor = .secondarySystemBackground
        panel.layer.cornerRadius = 16
        panel.translatesAutoresizingMaskIntoConstraints = false
        heading.font = .preferredFont(forTextStyle: .headline)
        message.font = .preferredFont(forTextStyle: .body)
        heading.numberOfLines = 0
        message.numberOfLines = 0
        button.addTarget(self, action: #selector(tapped), for: .touchUpInside)
        view.addSubview(panel)
        NSLayoutConstraint.activate([
            panel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            panel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            panel.widthAnchor.constraint(lessThanOrEqualToConstant: 420),
            panel.leadingAnchor.constraint(greaterThanOrEqualTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            panel.trailingAnchor.constraint(lessThanOrEqualTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            panel.widthAnchor.constraint(equalTo: view.widthAnchor, constant: -48).withPriority(.defaultHigh)
        ])
        view.accessibilityViewIsModal = true
    }
    func render(_ state: UserFirmwareUiState) {
        loadViewIfNeeded()
        let strings = SharedRes.strings()
        complete = state.phase == "complete"
        heading.text = SharedLocalizedText.text(complete ? strings.user_firmware_complete_title : strings.user_firmware_title)
        switch state.phase {
        case "offered": message.text = SharedLocalizedText.text(strings.user_firmware_offer)
        case "complete": message.text = SharedLocalizedText.text(strings.user_firmware_complete)
        case "updating":
            let format = SharedLocalizedText.text(strings.user_firmware_updating).replacingOccurrences(of: "%1$d", with: "%d").replacingOccurrences(of: "%2$d", with: "%d")
            message.text = String(format: format, state.boardNumber, state.boardCount)
        case "verifying": message.text = SharedLocalizedText.text(strings.user_firmware_verifying)
        case "waiting": message.text = SharedLocalizedText.text(strings.user_firmware_waiting)
        default: message.text = SharedLocalizedText.text(strings.user_firmware_preparing)
        }
        button.isHidden = state.phase != "offered" && !complete
        button.isEnabled = true
        button.setTitle(SharedLocalizedText.text(complete ? strings.ok : strings.user_firmware_install), for: .normal)
        progress.isHidden = state.phase != "updating"
        progress.progress = Float(state.progress) / 100
        activity.isHidden = state.phase == "offered" || complete || state.phase == "updating"
        if activity.isHidden { activity.stopAnimating() } else { activity.startAnimating() }
    }
    @objc private func tapped() { button.isEnabled = false; if complete { onOK?() } else { onInstall?() } }
}

private extension NSLayoutConstraint {
    func withPriority(_ value: UILayoutPriority) -> NSLayoutConstraint { priority = value; return self }
}

private extension Data {
    func userFirmwareBytes() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (index, byte) in enumerated() { result.set(index: Int32(index), value: Int8(bitPattern: byte)) }
        return result
    }
}
