import UIKit
import SwiftUI
import shared

final class SpinnerViewCellV3: UITableViewCell {
    static let reuseIdentifier = String(describing: SpinnerViewCellV3.self)
    private enum RoleAccess {
        static let prosthetistIndex = 0
        static let serviceEngineerIndex = 1
        static let deviceRoleParameterID = 0x0F
        static let deviceRoleDataCode = 0
        static let pin = "1234"
    }
    private var viewModel: SpinnerListItemViewModelV3?
    private var provider: SpinnerProviderV3?
    private var job: Kotlinx_coroutines_coreJob?
    private var isDropdownExpanded = false
    private var dropdownFrameInCell: CGRect = .zero
    private var dropdownItemFramesInCell: [Int: CGRect] = [:]
    private let estimatedDropdownItemHeight: CGFloat = 50
    private let dropdownVerticalGap: CGFloat = 8

    override func prepareForReuse() {
        super.prepareForReuse()
        job?.cancel(cause: nil)
        job = nil
        provider = nil
        viewModel = nil
        isDropdownExpanded = false
        dropdownFrameInCell = .zero
        dropdownItemFramesInCell = [:]
        layer.zPosition = 0
        contentConfiguration = nil
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: SpinnerListItemViewModelV3) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        clipsToBounds = false
        contentView.clipsToBounds = false
        layer.masksToBounds = false
        contentView.layer.masksToBounds = false

        let currentSelectedIndex = viewModel.currentSelectedIndex()
        let selected = currentSelectedIndex ?? viewModel.initialSelectedIndex
        if let currentSelectedIndex {
            viewModel.applyHandSideDeviceSnapshot(currentSelectedIndex)
        }
        let provider = SpinnerProviderV3(
            title: viewModel.title,
            items: viewModel.items,
            selectedIndex: selected
        )
        provider.isExpanded = false
        self.provider = provider
        isDropdownExpanded = false
        dropdownFrameInCell = .zero
        dropdownItemFramesInCell = [:]
        layer.zPosition = 0

        var configuration = UIHostingConfiguration {
            SpinnerRowViewV3(
                provider: provider,
                onSelect: { [weak self] index in
                    self?.handleSelection(index)
                },
                onExpandedChanged: { [weak self] isExpanded in
                    self?.updateDropdownState(isExpanded: isExpanded)
                },
                shouldOpenDropdownAbove: { [weak self] in
                    self?.shouldOpenDropdownAbove() ?? false
                },
                onDropdownFrameChanged: { [weak self] frame in
                    self?.dropdownFrameInCell = frame
                },
                onDropdownItemFramesChanged: { [weak self] frames in
                    self?.dropdownItemFramesInCell = frames
                }
            )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration

        job?.cancel(cause: nil)
        job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self, self.viewModel?.matches(snapshot: snapshot) == true else { return }
            guard let index = self.viewModel?.selectedIndex(from: snapshot) else { return }
            DispatchQueue.main.async {
                self.provider?.selectedIndex = index
                self.viewModel?.applyHandSideDeviceSnapshot(index)
            }
        }

        viewModel.requestCurrent()
    }

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        if super.point(inside: point, with: event) { return true }
        return isDropdownExpanded
    }

    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        if isDropdownExpanded {
            return self
        }
        return super.hitTest(point, with: event)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else {
            super.touchesEnded(touches, with: event)
            return
        }

        let location = touch.location(in: self)
        guard handleDropdownTap(at: location) else {
            super.touchesEnded(touches, with: event)
            return
        }
    }

    private func handleDropdownTap(at location: CGPoint) -> Bool {
        guard isDropdownExpanded, let provider else { return false }

        let itemCount = provider.items.count
        guard itemCount > 0 else {
            provider.isExpanded = false
            return true
        }

        let dropdownFrame = effectiveDropdownFrame(itemCount: itemCount)
        if !dropdownFrame.insetBy(dx: -8, dy: -8).contains(location) {
            provider.isExpanded = false
            return true
        }

        provider.isExpanded = false
        if let selectedIndex = SpinnerDropdownTapResolver.selectedIndex(
            at: location,
            dropdownFrame: dropdownFrame,
            itemFrames: dropdownItemFramesInCell,
            itemCount: itemCount
        ), provider.selectedIndex != selectedIndex {
            handleSelection(selectedIndex)
        }
        return true
    }

    private func handleSelection(_ index: Int) {
        guard let provider, let viewModel else { return }
        guard provider.items.indices.contains(index) else { return }
        guard isProtectedRoleIndex(index, viewModel: viewModel) else {
            provider.selectedIndex = index
            viewModel.sendSelectedIndex(index)
            return
        }

        let previousIndex = provider.selectedIndex
        presentRolePinDialog(
            onSuccess: { [weak self] in
                self?.provider?.selectedIndex = index
                self?.viewModel?.sendSelectedIndex(index)
            },
            onCancelOrFail: { [weak self] in
                self?.provider?.selectedIndex = previousIndex
            }
        )
    }

    private func isProtectedRoleIndex(_ index: Int, viewModel: SpinnerListItemViewModelV3) -> Bool {
        guard index == RoleAccess.prosthetistIndex || index == RoleAccess.serviceEngineerIndex else {
            return false
        }
        return isDeviceRoleSelector(viewModel)
    }

    private func isDeviceRoleSelector(_ viewModel: SpinnerListItemViewModelV3) -> Bool {
        if let binding = viewModel.binding,
           binding.parameterID == RoleAccess.deviceRoleParameterID,
           binding.dataCode == RoleAccess.deviceRoleDataCode {
            return true
        }

        guard viewModel.items.count >= 2 else { return false }
        let normalizedItems = viewModel.items.map(normalizedRoleName)
        let prosthetistNames = [
            SharedLocalizedText.text(SharedRes.strings().prosthetist),
            "Prosthetist",
            "Протезист"
        ].map(normalizedRoleName)
        let serviceEngineerNames = [
            SharedLocalizedText.text(SharedRes.strings().service_engineer),
            "Service engineer",
            "Сервисный инженер"
        ].map(normalizedRoleName)

        return prosthetistNames.contains(normalizedItems[RoleAccess.prosthetistIndex]) &&
            serviceEngineerNames.contains(normalizedItems[RoleAccess.serviceEngineerIndex])
    }

    private func normalizedRoleName(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    private func presentRolePinDialog(
        onSuccess: @escaping () -> Void,
        onCancelOrFail: @escaping () -> Void
    ) {
        guard let viewController = alertPresenter() else {
            NSLog("[RoleAccess] Failed to find presenter for role PIN dialog")
            onCancelOrFail()
            return
        }
        let dialog = RolePinDialogViewController(
            title: SharedLocalizedText.text(SharedRes.strings().enter_settings_pin),
            cancelTitle: SharedLocalizedText.text(SharedRes.strings().cancel),
            expectedPin: RoleAccess.pin,
            onSuccess: onSuccess,
            onCancelOrFail: onCancelOrFail
        )
        DispatchQueue.main.async {
            viewController.present(dialog, animated: false)
        }
    }

    private func effectiveDropdownFrame(itemCount: Int) -> CGRect {
        if dropdownFrameInCell != .zero {
            return dropdownFrameInCell
        }

        // Keep visual geometry unchanged, but provide a fallback hit region when SwiftUI
        // frame preferences are not ready at the exact tap moment.
        let estimatedX = max(0, bounds.width - 8 - 220)
        let estimatedHeight = estimatedDropdownHeight(itemCount: itemCount)
        let estimatedY: CGFloat = provider?.opensAbove == true ? -estimatedHeight : 60
        return CGRect(x: estimatedX, y: estimatedY, width: 220, height: estimatedHeight)
    }

    private func updateDropdownState(isExpanded: Bool) {
        isDropdownExpanded = isExpanded
        if isExpanded {
            layer.zPosition = 1000
            hostingTableView?.bringSubviewToFront(self)
        } else {
            layer.zPosition = 0
            dropdownFrameInCell = .zero
            dropdownItemFramesInCell = [:]
        }
    }

    private func shouldOpenDropdownAbove() -> Bool {
        guard let provider, provider.items.isEmpty == false else { return false }
        guard let window else { return false }

        let dropdownHeight = estimatedDropdownHeight(itemCount: provider.items.count)
        let cellFrame = convert(bounds, to: window)
        let visibleBounds = dropdownVisibleBounds(in: window)
        let availableBelow = visibleBounds.maxY - cellFrame.maxY - dropdownVerticalGap
        let availableAbove = cellFrame.minY - visibleBounds.minY - dropdownVerticalGap

        return availableBelow < dropdownHeight && availableAbove > availableBelow
    }

    private func estimatedDropdownHeight(itemCount: Int) -> CGFloat {
        CGFloat(itemCount) * estimatedDropdownItemHeight + CGFloat(max(0, itemCount - 1))
    }

    private func dropdownVisibleBounds(in window: UIWindow) -> CGRect {
        var visibleBounds = window.bounds.inset(by: window.safeAreaInsets)

        if let tableView = hostingTableView {
            let tableFrame = tableView.convert(tableView.bounds, to: window)
            visibleBounds = visibleBounds.intersection(tableFrame)
        }

        if let tabBar = nearestTabBar(), tabBar.isHidden == false, tabBar.alpha > 0.01 {
            let tabBarFrame = tabBar.convert(tabBar.bounds, to: window)
            let maxY = min(visibleBounds.maxY, tabBarFrame.minY)
            visibleBounds.size.height = max(0, maxY - visibleBounds.minY)
        }

        return visibleBounds.isNull ? window.bounds.inset(by: window.safeAreaInsets) : visibleBounds
    }

    private func nearestTabBar() -> UITabBar? {
        var responder: UIResponder? = self
        while let current = responder {
            if let tabBarController = current as? UITabBarController {
                return tabBarController.tabBar
            }
            responder = current.next
        }
        return nil
    }

    private var hostingTableView: UITableView? {
        var currentView: UIView? = superview
        while let view = currentView {
            if let tableView = view as? UITableView { return tableView }
            currentView = view.superview
        }
        return nil
    }

    private func nearestViewController() -> UIViewController? {
        var responder: UIResponder? = self
        while let current = responder {
            if let viewController = current as? UIViewController {
                return viewController
            }
            responder = current.next
        }
        return nil
    }

    private func alertPresenter() -> UIViewController? {
        if let viewController = nearestViewController() {
            return topViewController(from: viewController)
        }

        let rootViewController = window?.rootViewController ?? UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
        return topViewController(from: rootViewController)
    }

    private func topViewController(from root: UIViewController?) -> UIViewController? {
        if let presented = root?.presentedViewController {
            return topViewController(from: presented)
        }
        if let navigation = root as? UINavigationController {
            return topViewController(from: navigation.visibleViewController)
        }
        if let tabBar = root as? UITabBarController {
            return topViewController(from: tabBar.selectedViewController)
        }
        return root
    }
}

private final class RolePinDialogViewController: UIViewController, UITextFieldDelegate {
    private enum Layout {
        static let width: CGFloat = 350
        static let cornerRadius: CGFloat = 16
        static let contentInset: CGFloat = 16
        static let dividerInset: CGFloat = 10
        static let verticalSpacing: CGFloat = 16
        static let inputHeight: CGFloat = 48
        static let inputCornerRadius: CGFloat = 10
        static let dividerHeight: CGFloat = 1
    }

    private let dialogTitle: String
    private let cancelTitle: String
    private let expectedPin: String
    private let onSuccess: () -> Void
    private let onCancelOrFail: () -> Void
    private let input = UITextField()
    private var completed = false

    init(
        title: String,
        cancelTitle: String,
        expectedPin: String,
        onSuccess: @escaping () -> Void,
        onCancelOrFail: @escaping () -> Void
    ) {
        self.dialogTitle = title
        self.cancelTitle = cancelTitle
        self.expectedPin = expectedPin
        self.onSuccess = onSuccess
        self.onCancelOrFail = onCancelOrFail
        super.init(nibName: nil, bundle: nil)
        modalPresentationStyle = .overFullScreen
        modalTransitionStyle = .crossDissolve
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        buildView()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        input.becomeFirstResponder()
    }

    private func buildView() {
        view.backgroundColor = UIColor.black.withAlphaComponent(0.32)

        let card = UIView()
        card.translatesAutoresizingMaskIntoConstraints = false
        card.backgroundColor = UIColor(named: "ubi4_gray") ?? UIColor(white: 0x37 / 255, alpha: 1)
        card.layer.cornerRadius = Layout.cornerRadius
        card.layer.shadowColor = UIColor.black.cgColor
        card.layer.shadowOpacity = 0.3
        card.layer.shadowRadius = 8
        card.layer.shadowOffset = CGSize(width: 0, height: 4)
        view.addSubview(card)

        let titleLabel = UILabel()
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.text = dialogTitle
        titleLabel.textColor = .white
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 2
        titleLabel.font = boldDisplayFont(size: 17)
        card.addSubview(titleLabel)

        input.translatesAutoresizingMaskIntoConstraints = false
        input.delegate = self
        input.keyboardType = .numberPad
        input.textContentType = .oneTimeCode
        input.isSecureTextEntry = false
        input.textAlignment = .center
        input.textColor = .white
        input.tintColor = UIColor(named: "ubi4_yes_system_blue") ?? UIColor(red: 10 / 255, green: 132 / 255, blue: 1, alpha: 1)
        input.font = UIFont(name: "SFProDisplay-Light", size: 17) ?? .systemFont(ofSize: 17, weight: .light)
        input.backgroundColor = UIColor(named: "ubi4_back") ?? UIColor(white: 0x2A / 255, alpha: 1)
        input.layer.cornerRadius = Layout.inputCornerRadius
        input.layer.borderWidth = 1
        input.layer.borderColor = (UIColor(named: "ubi4_gray_border") ?? UIColor(white: 0x44 / 255, alpha: 1)).cgColor
        input.accessibilityIdentifier = "RolePinTextField"
        input.addTarget(self, action: #selector(inputChanged), for: .editingChanged)
        card.addSubview(input)

        let divider = UIView()
        divider.translatesAutoresizingMaskIntoConstraints = false
        divider.backgroundColor = UIColor(named: "ubi4_gray_border") ?? UIColor(white: 0x44 / 255, alpha: 1)
        card.addSubview(divider)

        let cancelButton = UIButton(type: .system)
        cancelButton.translatesAutoresizingMaskIntoConstraints = false
        cancelButton.setTitle(cancelTitle, for: .normal)
        cancelButton.setTitleColor(UIColor(named: "ubi4_yes_system_blue") ?? UIColor(red: 10 / 255, green: 132 / 255, blue: 1, alpha: 1), for: .normal)
        cancelButton.titleLabel?.font = UIFont(name: "SFProText-Bold", size: 18) ?? .systemFont(ofSize: 18, weight: .bold)
        cancelButton.addTarget(self, action: #selector(cancelTapped), for: .touchUpInside)
        card.addSubview(cancelButton)

        NSLayoutConstraint.activate([
            card.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            card.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            card.widthAnchor.constraint(equalToConstant: Layout.width).withPriority(.defaultHigh),
            card.leadingAnchor.constraint(greaterThanOrEqualTo: view.safeAreaLayoutGuide.leadingAnchor, constant: Layout.contentInset),
            card.trailingAnchor.constraint(lessThanOrEqualTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -Layout.contentInset),

            titleLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: Layout.contentInset),
            titleLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: Layout.contentInset),
            titleLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -Layout.contentInset),

            input.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: Layout.verticalSpacing),
            input.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: Layout.contentInset),
            input.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -Layout.contentInset),
            input.heightAnchor.constraint(equalToConstant: Layout.inputHeight),

            divider.topAnchor.constraint(equalTo: input.bottomAnchor, constant: Layout.verticalSpacing),
            divider.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: Layout.dividerInset),
            divider.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -Layout.dividerInset),
            divider.heightAnchor.constraint(equalToConstant: Layout.dividerHeight),

            cancelButton.topAnchor.constraint(equalTo: divider.bottomAnchor),
            cancelButton.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            cancelButton.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            cancelButton.heightAnchor.constraint(equalToConstant: 54),
            cancelButton.bottomAnchor.constraint(equalTo: card.bottomAnchor)
        ])
    }

    private func boldDisplayFont(size: CGFloat) -> UIFont {
        let base = UIFont(name: "SFProDisplay-Light", size: size) ?? .systemFont(ofSize: size, weight: .light)
        guard let descriptor = base.fontDescriptor.withSymbolicTraits(.traitBold) else {
            return .systemFont(ofSize: size, weight: .bold)
        }
        return UIFont(descriptor: descriptor, size: size)
    }

    @objc private func inputChanged() {
        let digits = (input.text ?? "").filter(\.isNumber)
        let limited = String(digits.prefix(expectedPin.count))
        if input.text != limited {
            input.text = limited
        }
        guard limited.count == expectedPin.count else { return }
        finish(success: limited == expectedPin)
    }

    @objc private func cancelTapped() {
        finish(success: false)
    }

    private func finish(success: Bool) {
        guard !completed else { return }
        completed = true
        input.resignFirstResponder()
        dismiss(animated: false) { [onSuccess, onCancelOrFail] in
            success ? onSuccess() : onCancelOrFail()
        }
    }
}

private extension NSLayoutConstraint {
    func withPriority(_ priority: UILayoutPriority) -> NSLayoutConstraint {
        self.priority = priority
        return self
    }
}

private final class SpinnerProviderV3: ObservableObject {
    let title: String
    let items: [String]
    @Published var selectedIndex: Int
    @Published var isExpanded: Bool = false
    @Published var opensAbove: Bool = false

    init(title: String, items: [String], selectedIndex: Int) {
        self.title = title
        self.items = items
        self.selectedIndex = selectedIndex
    }

    var selectedTitle: String {
        guard items.indices.contains(selectedIndex) else { return "" }
        return items[selectedIndex]
    }
}

private struct SpinnerRowViewV3: View {
    @ObservedObject var provider: SpinnerProviderV3
    let onSelect: (Int) -> Void
    let onExpandedChanged: (Bool) -> Void
    let shouldOpenDropdownAbove: () -> Bool
    let onDropdownFrameChanged: (CGRect) -> Void
    let onDropdownItemFramesChanged: ([Int: CGRect]) -> Void

    private enum Layout {
        static let rowHeight: CGFloat = 56
        static let pickerHeight: CGFloat = 40
        static let dropdownWidth: CGFloat = 220
        static let panelCornerRadius: CGFloat = 18
        static let coordinateSpaceName = "SpinnerRowV3"
        static let panelTopOffset: CGFloat = pickerHeight + 8
        static let rowCornerRadius: CGFloat = 12
        static let buttonCornerRadius: CGFloat = 10
        static let itemVerticalPadding: CGFloat = 18
        static let panelShadowRadius: CGFloat = 12
    }

    private enum Palette {
        static let rowBackground = Color("ubi4_back")
        static let rowBorder = Color("ubi4_gray_border")
        static let buttonBackground = Color("ubi4_gray")
        static let buttonBorder = Color("ubi4_gray_border")
        static let textPrimary = Color("ubi4_white")
        static let textSecondary = Color("ubi4_deactivate_text")
    }

    var body: some View {
        HStack(spacing: 8) {
            Text(provider.title)
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(Palette.textPrimary)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                if provider.isExpanded {
                    provider.isExpanded = false
                } else {
                    provider.opensAbove = shouldOpenDropdownAbove()
                    provider.isExpanded = true
                }
            } label: {
                HStack(spacing: 6) {
                    Text(provider.selectedTitle)
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(Palette.textPrimary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .layoutPriority(1)

                    Image(systemName: provider.isExpanded ? "chevron.up" : "chevron.down")
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(Palette.textSecondary)
                        .frame(width: 12)
                }
                .frame(height: Layout.pickerHeight)
                .padding(.horizontal, 12)
                .background(
                    RoundedRectangle(cornerRadius: Layout.buttonCornerRadius)
                        .fill(Palette.buttonBackground)
                        .overlay(
                            RoundedRectangle(cornerRadius: Layout.buttonCornerRadius)
                                .stroke(Palette.buttonBorder, lineWidth: 1)
                        )
                )
            }
            .buttonStyle(.plain)
            .frame(width: Layout.dropdownWidth)
            .overlay(alignment: provider.opensAbove ? .bottomTrailing : .topTrailing) {
                if provider.isExpanded {
                    dropdownPanel
                        .frame(width: Layout.dropdownWidth)
                        .padding(provider.opensAbove ? .bottom : .top, Layout.panelTopOffset)
                        .background(
                            GeometryReader { proxy in
                                Color.clear.preference(
                                    key: DropdownPanelFramePreferenceKey.self,
                                    value: proxy.frame(in: .named(Layout.coordinateSpaceName))
                                )
                            }
                        )
                        .transition(.opacity)
                        .zIndex(1000)
                }
            }
        }
        .padding(.horizontal, 8)
        .frame(height: Layout.rowHeight)
        .background(
            RoundedRectangle(cornerRadius: Layout.rowCornerRadius)
                .fill(Palette.rowBackground)
                .overlay(
                    RoundedRectangle(cornerRadius: Layout.rowCornerRadius)
                        .stroke(Palette.rowBorder, lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
        .coordinateSpace(name: Layout.coordinateSpaceName)
        .onPreferenceChange(DropdownPanelFramePreferenceKey.self) { frame in
            if provider.isExpanded {
                onDropdownFrameChanged(frame)
            }
        }
        .onPreferenceChange(DropdownItemFramesPreferenceKey.self) { frames in
            if provider.isExpanded {
                onDropdownItemFramesChanged(frames)
            }
        }
        .onChange(of: provider.isExpanded) { expanded in
            onExpandedChanged(expanded)
            if !expanded {
                onDropdownFrameChanged(.zero)
                onDropdownItemFramesChanged([:])
            }
        }
        .onAppear {
            onExpandedChanged(provider.isExpanded)
        }
        .zIndex(provider.isExpanded ? 1000 : 0)
        .animation(.easeInOut(duration: 0.16), value: provider.isExpanded)
    }

    private var dropdownPanel: some View {
        VStack(spacing: 0) {
            ForEach(Array(provider.items.enumerated()), id: \.offset) { index, item in
                Button {
                    provider.isExpanded = false
                    guard provider.selectedIndex != index else { return }
                    onSelect(index)
                } label: {
                    Text(item)
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(Palette.textPrimary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 12)
                        .frame(minHeight: 50)
                        .contentShape(Rectangle())
                        .background(
                            GeometryReader { proxy in
                                Color.clear.preference(
                                    key: DropdownItemFramesPreferenceKey.self,
                                    value: [index: proxy.frame(in: .named(Layout.coordinateSpaceName))]
                                )
                            }
                        )
                }
                .buttonStyle(.plain)
                .frame(maxWidth: .infinity)

                if index < provider.items.count - 1 {
                    Rectangle()
                        .fill(Palette.rowBorder)
                        .frame(height: 1)
                }
            }
        }
        .background(
            RoundedRectangle(cornerRadius: Layout.panelCornerRadius)
                .fill(Palette.buttonBackground)
                .overlay(
                    RoundedRectangle(cornerRadius: Layout.panelCornerRadius)
                        .stroke(Palette.buttonBorder, lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.35), radius: Layout.panelShadowRadius, x: 0, y: 8)
        )
    }
}

private struct DropdownPanelFramePreferenceKey: PreferenceKey {
    static var defaultValue: CGRect = .zero

    static func reduce(value: inout CGRect, nextValue: () -> CGRect) {
        let next = nextValue()
        if next != .zero {
            value = next
        }
    }
}

private struct DropdownItemFramesPreferenceKey: PreferenceKey {
    static var defaultValue: [Int: CGRect] = [:]

    static func reduce(value: inout [Int: CGRect], nextValue: () -> [Int: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, new in new })
    }
}

#if DEBUG
import SwiftUI

struct SpinnerRowViewV3_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            SpinnerRowViewV3(
                provider: SpinnerProviderV3(
                    title: SharedLocalizedText.text(SharedRes.strings().v3_prosthesis_operating_mode),
                    items: [
                        SharedLocalizedText.text(SharedRes.strings().v3_normal_mode),
                        SharedLocalizedText.text(SharedRes.strings().v3_sport_mode),
                        SharedLocalizedText.text(SharedRes.strings().v3_smooth_force_control),
                        SharedLocalizedText.text(SharedRes.strings().v3_smooth_speed_control),
                        SharedLocalizedText.text(SharedRes.strings().v3_smooth_force_and_speed_control)
                    ],
                    selectedIndex: 1
                ),
                onSelect: { _ in },
                onExpandedChanged: { _ in },
                shouldOpenDropdownAbove: { false },
                onDropdownFrameChanged: { _ in },
                onDropdownItemFramesChanged: { _ in }
            )
            .previewDisplayName("Last selected")

        }
        .padding()
        .background(Color("ubi4_back"))
        .previewLayout(.sizeThatFits)
        .preferredColorScheme(.dark)
    }
}
#endif
