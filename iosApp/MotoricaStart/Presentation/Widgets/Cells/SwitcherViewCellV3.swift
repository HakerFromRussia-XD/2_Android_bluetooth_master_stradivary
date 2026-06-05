import UIKit
import SwiftUI
import Combine
import shared

final class SwitcherViewCellV3: UITableViewCell {
    static let reuseIdentifier = String(describing: SwitcherViewCellV3.self)

    private var viewModel: SwitcherListItemViewModelV3?
    private var provider: SwitchProvider?
    private var cancellable: AnyCancellable?
    private var job: Kotlinx_coroutines_coreJob?
    private var smartConnectionObserver: NSObjectProtocol?
    private var isProgrammaticUpdate = false

    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        if let smartConnectionObserver {
            NotificationCenter.default.removeObserver(smartConnectionObserver)
        }
        smartConnectionObserver = nil
        job?.cancel(cause: nil)
        job = nil
        provider = nil
        viewModel = nil
        contentConfiguration = nil
        isProgrammaticUpdate = false
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: SwitcherListItemViewModelV3) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        let provider = SwitchProvider(
            isOn: viewModel.currentState() ?? false,
            title: viewModel.title
        )
        self.provider = provider

        cancellable?.cancel()
        cancellable = provider.$isOn
            .dropFirst()
            .removeDuplicates()
            .sink { [weak self] isOn in
                self?.handleSwitchChange(isOn: isOn)
            }

        var configuration = UIHostingConfiguration {
            SwitchRowView(provider: provider)
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration

        if viewModel.isMobileSmartConnectionSetting {
            installSmartConnectionObserver()
            viewModel.requestCurrent()
            return
        }

        job?.cancel(cause: nil)
        job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self, self.viewModel?.matches(snapshot: snapshot) == true else { return }
            guard let isOn = self.viewModel?.switchState(from: snapshot) else { return }

            DispatchQueue.main.async {
                guard self.provider?.isOn != isOn else { return }
                self.isProgrammaticUpdate = true
                self.provider?.isOn = isOn
                self.isProgrammaticUpdate = false
            }
        }

        viewModel.requestCurrent()
    }

    private func handleSwitchChange(isOn: Bool) {
        if isProgrammaticUpdate {
            isProgrammaticUpdate = false
            return
        }
        viewModel?.sendState(isOn)
    }

    private func installSmartConnectionObserver() {
        if let smartConnectionObserver {
            NotificationCenter.default.removeObserver(smartConnectionObserver)
        }
        smartConnectionObserver = NotificationCenter.default.addObserver(
            forName: .smartConnectionSettingsDidChange,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let self else { return }
            let isOn = notification.userInfo?["isEnabled"] as? Bool
                ?? self.viewModel?.currentState()
                ?? true
            guard self.provider?.isOn != isOn else { return }
            self.isProgrammaticUpdate = true
            self.provider?.isOn = isOn
            self.isProgrammaticUpdate = false
        }
    }
}
