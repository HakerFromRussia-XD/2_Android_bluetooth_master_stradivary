import SwiftUI
import UIKit
import shared

private enum HelpMetrics {
    static let sideInset: CGFloat = 16
    static let rowHeight: CGFloat = 56
    static let cardRadius: CGFloat = 12
    static let cardTop: CGFloat = 16
    static let sectionSpacing: CGFloat = 22
}

private enum HelpFont {
    static func interSemiBold(_ size: CGFloat) -> UIFont {
        named(["Inter-SemiBold", "font_inter_semi_bold", "Inter"], size: size, weight: .semibold)
    }

    static func openSansRegular(_ size: CGFloat) -> UIFont {
        named(["OpenSans-Regular", "OpenSans", "font_open_sans_regular"], size: size, weight: .regular)
    }

    static func openSansSemiBold(_ size: CGFloat) -> UIFont {
        named(["OpenSans-SemiBold", "OpenSansRoman-SemiBold", "font_open_sans_semi_bold"], size: size, weight: .semibold)
    }

    private static func named(_ names: [String], size: CGFloat, weight: UIFont.Weight) -> UIFont {
        for name in names {
            if let font = UIFont(name: name, size: size) {
                return font
            }
        }
        return .systemFont(ofSize: size, weight: weight)
    }
}

final class HelpViewController: UIViewController {
    private let pageId: String?
    private let scrollView = UIScrollView()
    private let contentStack = UIStackView()
    private var statusBarHostingController: UIHostingController<StatusBarView>?

    private let backgroundColor = UIColor.accountColor("ubi4_back", fallback: 0x2A2A2A)
    private let cardColor = UIColor.accountColor("ubi4_gray", fallback: 0x373737)
    private let borderColor = UIColor.accountColor("ubi4_gray_border", fallback: 0x444444)
    private let textColor = UIColor.accountColor("ubi4_white", fallback: 0xFCFCFC)
    private let inactiveTextColor = UIColor.accountColor("ubi4_deactivate_text", fallback: 0x838383)
    private let linkColor = UIColor.accountColor("ubi4_yes_system_blue", fallback: 0x43A7FF)

    init(pageId: String? = nil) {
        self.pageId = pageId
        super.init(nibName: nil, bundle: nil)
        hidesBottomBarWhenPushed = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupView()
        renderContent()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    private func setupView() {
        view.backgroundColor = backgroundColor
        setupTopBar()
        setupScrollView()
    }

    private func setupTopBar() {
        let statusBar = UIHostingController(
            rootView: StatusBarView(
                viewModel: WidgetsTabContainerViewController.sharedStatusBarViewModel,
                leadingButton: .back,
                onBackTap: { [weak self] in
                    self?.navigationController?.popViewController(animated: true)
                },
                onDisconnectConfirmed: { [weak self] in
                    StatusBarDisconnectCoordinator.disconnectAndShowScan(from: self)
                }
            )
        )
        statusBarHostingController = statusBar
        addChild(statusBar)
        statusBar.view.translatesAutoresizingMaskIntoConstraints = false
        statusBar.view.backgroundColor = .clear
        view.addSubview(statusBar.view)

        NSLayoutConstraint.activate([
            statusBar.view.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            statusBar.view.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            statusBar.view.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            statusBar.view.heightAnchor.constraint(equalToConstant: StatusBarView.Constants.height)
        ])
        statusBar.didMove(toParent: self)
    }

    private func setupScrollView() {
        guard let statusBarView = statusBarHostingController?.view else { return }
        scrollView.backgroundColor = backgroundColor
        scrollView.alwaysBounceVertical = true
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scrollView)

        contentStack.axis = .vertical
        contentStack.spacing = 0
        contentStack.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(contentStack)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: statusBarView.bottomAnchor, constant: 8),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            contentStack.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            contentStack.leadingAnchor.constraint(equalTo: scrollView.frameLayoutGuide.leadingAnchor),
            contentStack.trailingAnchor.constraint(equalTo: scrollView.frameLayoutGuide.trailingAnchor),
            contentStack.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor, constant: -20)
        ])
    }

    private func renderContent() {
        contentStack.arrangedSubviews.forEach {
            contentStack.removeArrangedSubview($0)
            $0.removeFromSuperview()
        }

        if let pageId, let page = InstructionBridge.shared.page(id: pageId) {
            renderPage(page)
        } else {
            renderIndex()
        }
    }

    private func renderIndex() {
        addSectionTitle(SharedRes.strings().help)
        for section in InstructionBridge.shared.indexSections() {
            addSectionTitle(
                section.title,
                topInset: HelpMetrics.sectionSpacing,
                font: section.id == "contact_us" ? HelpFont.openSansSemiBold(14) : HelpFont.interSemiBold(14)
            )
            if section.id == "contact_us" {
                addContactSection(items: section.items)
            } else {
                addCard(makeMenuCard(items: section.items))
            }
        }
    }

    private func renderPage(_ page: InstructionPage) {
        addSectionTitle(page.title)
        for card in page.cards {
            addContentCard(card)
        }
        if !page.relatedItems.isEmpty, let relatedTitle = page.relatedTitle {
            addSectionTitle(relatedTitle, topInset: HelpMetrics.sectionSpacing, font: HelpFont.openSansSemiBold(14))
            addCard(makeMenuCard(items: page.relatedItems, currentId: page.id))
        }
    }

    private func addSectionTitle(_ resource: StringResource, topInset: CGFloat = 0, font: UIFont = HelpFont.interSemiBold(14)) {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false

        let label = UILabel()
        label.text = resource.desc().localized()
        label.font = font
        label.textColor = textColor
        label.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(label)

        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: container.topAnchor, constant: topInset),
            label.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: HelpMetrics.sideInset),
            label.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -HelpMetrics.sideInset),
            label.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        contentStack.addArrangedSubview(container)
    }

    private func addCard(_ card: UIView) {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(card)
        NSLayoutConstraint.activate([
            card.topAnchor.constraint(equalTo: container.topAnchor, constant: HelpMetrics.cardTop),
            card.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: HelpMetrics.sideInset),
            card.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -HelpMetrics.sideInset),
            card.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        contentStack.addArrangedSubview(container)
    }

    private func makeMenuCard(items: [InstructionMenuItem], currentId: String? = nil) -> UIView {
        let card = HelpCardView(backgroundColor: cardColor, borderColor: borderColor)
        for (index, item) in items.enumerated() {
            card.addRow(HelpMenuRow(item: item, isCurrent: item.id == currentId, textColor: textColor) { [weak self] item in
                self?.handleMenuItem(item)
            })
            if index != items.indices.last {
                card.addDivider(color: borderColor)
            }
        }
        return card
    }

    private func addContactSection(items: [InstructionMenuItem]) {
        if let phoneItem = items.first(where: { $0.actionType == .phone }) {
            addCard(makeContactCard(item: phoneItem))
        }

        let socialItems = items.filter { $0.actionType == .url }
        guard !socialItems.isEmpty else { return }

        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false

        let stack = UIStackView()
        stack.axis = .horizontal
        stack.alignment = .center
        stack.distribution = .fill
        stack.spacing = 20
        stack.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(stack)

        for item in socialItems {
            let button = makeSocialButton(item: item)
            stack.addArrangedSubview(button)
        }

        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: container.topAnchor, constant: 22),
            stack.centerXAnchor.constraint(equalTo: container.centerXAnchor),
            stack.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -28)
        ])
        contentStack.addArrangedSubview(container)
    }

    private func makeContactCard(item: InstructionMenuItem) -> UIView {
        let card = HelpCardView(backgroundColor: cardColor, borderColor: borderColor)
        card.addRow(
            HelpContactRow(
                title: item.title.desc().localized(),
                subtitle: SharedRes.strings().instruction_phone_display.desc().localized(),
                icon: SharedRes.images().ic_phone_call.toUIImage(),
                titleColor: inactiveTextColor,
                subtitleColor: linkColor,
                iconTintColor: textColor
            ) { [weak self] in
                self?.handleMenuItem(item)
            }
        )
        return card
    }

    private func makeSocialButton(item: InstructionMenuItem) -> UIButton {
        let button = UIButton(type: .custom)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setImage(socialImage(for: item), for: .normal)
        button.imageView?.contentMode = .scaleAspectFit
        button.addAction(
            UIAction { [weak self] _ in
                self?.handleMenuItem(item)
            },
            for: .touchUpInside
        )
        NSLayoutConstraint.activate([
            button.widthAnchor.constraint(equalToConstant: 32),
            button.heightAnchor.constraint(equalToConstant: 32)
        ])
        return button
    }

    private func socialImage(for item: InstructionMenuItem) -> UIImage? {
        switch item.id {
        case "vk":
            return SharedRes.images().ic_vkontakte.toUIImage()
        case "telegram":
            return SharedRes.images().ic_telegramm.toUIImage()
        default:
            return nil
        }
    }

    private func addContentCard(_ card: InstructionCard) {
        let container = HelpCardView(backgroundColor: cardColor, borderColor: borderColor)
        let stack = UIStackView()
        stack.axis = .vertical
        stack.spacing = 0
        stack.translatesAutoresizingMaskIntoConstraints = false
        stack.layoutMargins = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        stack.isLayoutMarginsRelativeArrangement = true
        container.addRow(stack)

        for block in card.blocks {
            stack.addArrangedSubview(makeBlockContainer(block))
        }
        addCard(container)
    }

    private func makeBlockContainer(_ block: InstructionBlock) -> UIView {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        let view = makeBlockView(block)
        view.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(view)
        NSLayoutConstraint.activate([
            view.topAnchor.constraint(equalTo: container.topAnchor, constant: CGFloat(block.topMargin)),
            view.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            view.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            view.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        return container
    }

    private func makeBlockView(_ block: InstructionBlock) -> UIView {
        switch block.type {
        case .heading:
            return makeLabel(block.text?.desc().localized() ?? "", font: HelpFont.interSemiBold(14), color: textColor)
        case .paragraph:
            return makeLabel(block.text?.desc().localized() ?? "", font: HelpFont.openSansRegular(14), color: textColor)
        case .emphasis:
            return makeLabel(block.text?.desc().localized() ?? "", font: HelpFont.openSansSemiBold(14), color: textColor)
        case .notice:
            return makeLabel(block.text?.desc().localized() ?? "", font: HelpFont.interSemiBold(14), color: textColor)
        case .numbered:
            let stack = UIStackView()
            stack.axis = .vertical
            stack.spacing = 8
            for (index, resource) in block.items.enumerated() {
                stack.addArrangedSubview(makeNumberedRow(index: index + 1, text: resource.desc().localized()))
            }
            return stack
        case .iconText:
            return makeIconTextRow(block)
        case .image:
            guard let image = block.image?.toUIImage() else { return UIView() }
            let imageView = UIImageView(image: image)
            imageView.contentMode = .scaleAspectFit
            imageView.translatesAutoresizingMaskIntoConstraints = false
            if block.imageWidth > 0 {
                imageView.widthAnchor.constraint(equalToConstant: CGFloat(block.imageWidth)).isActive = true
            }
            if block.imageHeight > 0 {
                imageView.heightAnchor.constraint(equalToConstant: CGFloat(block.imageHeight)).isActive = true
            } else if image.size.width > 0 {
                imageView.heightAnchor.constraint(equalTo: imageView.widthAnchor, multiplier: image.size.height / image.size.width).isActive = true
            }
            return imageView
        default:
            return UIView()
        }
    }

    private func makeLabel(_ text: String, font: UIFont, color: UIColor) -> UILabel {
        let label = UILabel()
        label.text = text
        label.font = font
        label.textColor = color
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        return label
    }

    private func makeNumberedRow(index: Int, text: String) -> UIView {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.alignment = .top
        stack.spacing = 0

        let number = makeLabel("\(index).  ", font: HelpFont.openSansRegular(14), color: textColor)
        number.setContentHuggingPriority(.required, for: .horizontal)
        let label = makeLabel(text, font: HelpFont.openSansRegular(14), color: textColor)
        stack.addArrangedSubview(number)
        stack.addArrangedSubview(label)
        return stack
    }

    private func makeIconTextRow(_ block: InstructionBlock) -> UIView {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.alignment = .center
        stack.spacing = 12

        if let image = block.image?.toUIImage() {
            let imageView = UIImageView(image: image)
            imageView.contentMode = .scaleAspectFit
            imageView.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                imageView.widthAnchor.constraint(equalToConstant: CGFloat(block.imageWidth)),
                imageView.heightAnchor.constraint(equalToConstant: CGFloat(block.imageHeight))
            ])
            stack.addArrangedSubview(imageView)
        }

        stack.addArrangedSubview(makeLabel(block.text?.desc().localized() ?? "", font: HelpFont.openSansRegular(14), color: textColor))
        return stack
    }

    private func handleMenuItem(_ item: InstructionMenuItem) {
        switch item.actionType {
        case .page:
            navigationController?.pushViewController(HelpViewController(pageId: item.target), animated: true)
        case .phone:
            guard let url = URL(string: "tel:\(item.target)") else { return }
            UIApplication.shared.open(url)
        case .url:
            guard let url = URL(string: item.target) else { return }
            UIApplication.shared.open(url)
        case .disabled:
            break
        default:
            break
        }
    }
}

private final class HelpCardView: UIView {
    private let stack = UIStackView()

    init(backgroundColor: UIColor, borderColor: UIColor) {
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false
        self.backgroundColor = backgroundColor
        layer.cornerRadius = HelpMetrics.cardRadius
        layer.borderColor = borderColor.cgColor
        layer.borderWidth = 1
        layer.masksToBounds = true

        stack.axis = .vertical
        stack.spacing = 0
        stack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: topAnchor),
            stack.leadingAnchor.constraint(equalTo: leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func addRow(_ row: UIView) {
        stack.addArrangedSubview(row)
    }

    func addDivider(color: UIColor) {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        container.heightAnchor.constraint(equalToConstant: 1).isActive = true

        let divider = UIView()
        divider.backgroundColor = color
        divider.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(divider)
        NSLayoutConstraint.activate([
            divider.topAnchor.constraint(equalTo: container.topAnchor),
            divider.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 8),
            divider.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -8),
            divider.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        stack.addArrangedSubview(container)
    }
}

private final class HelpMenuRow: UIControl {
    private let item: InstructionMenuItem
    private let isCurrent: Bool
    private let action: (InstructionMenuItem) -> Void

    init(item: InstructionMenuItem, isCurrent: Bool, textColor: UIColor, action: @escaping (InstructionMenuItem) -> Void) {
        self.item = item
        self.isCurrent = isCurrent
        self.action = action
        super.init(frame: .zero)
        setup(textColor: textColor)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(textColor: UIColor) {
        heightAnchor.constraint(equalToConstant: HelpMetrics.rowHeight).isActive = true
        isEnabled = !isCurrent

        let label = UILabel()
        label.text = item.title.desc().localized()
        label.font = isCurrent ? HelpFont.openSansSemiBold(14) : HelpFont.openSansRegular(14)
        label.textColor = textColor
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        addSubview(label)

        let chevron = UIImageView(image: UIImage(named: "ic_navigate_next")?.withRenderingMode(.alwaysTemplate))
        chevron.tintColor = textColor
        chevron.contentMode = .scaleAspectFit
        chevron.isHidden = isCurrent
        chevron.translatesAutoresizingMaskIntoConstraints = false
        addSubview(chevron)

        addTarget(self, action: #selector(handleTap), for: .touchUpInside)

        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 18),
            label.trailingAnchor.constraint(lessThanOrEqualTo: chevron.leadingAnchor, constant: -12),
            label.centerYAnchor.constraint(equalTo: centerYAnchor),

            chevron.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12),
            chevron.centerYAnchor.constraint(equalTo: centerYAnchor),
            chevron.widthAnchor.constraint(equalToConstant: 24),
            chevron.heightAnchor.constraint(equalToConstant: 24)
        ])
    }

    @objc private func handleTap() {
        action(item)
    }
}

private final class HelpContactRow: UIControl {
    private let action: () -> Void

    init(
        title: String,
        subtitle: String,
        icon: UIImage?,
        titleColor: UIColor,
        subtitleColor: UIColor,
        iconTintColor: UIColor,
        action: @escaping () -> Void
    ) {
        self.action = action
        super.init(frame: .zero)
        setup(
            title: title,
            subtitle: subtitle,
            icon: icon,
            titleColor: titleColor,
            subtitleColor: subtitleColor,
            iconTintColor: iconTintColor
        )
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(
        title: String,
        subtitle: String,
        icon: UIImage?,
        titleColor: UIColor,
        subtitleColor: UIColor,
        iconTintColor: UIColor
    ) {
        heightAnchor.constraint(equalToConstant: HelpMetrics.rowHeight).isActive = true

        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.font = HelpFont.openSansRegular(12)
        titleLabel.textColor = titleColor
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)

        let subtitleLabel = UILabel()
        subtitleLabel.text = subtitle
        subtitleLabel.font = HelpFont.openSansRegular(12)
        subtitleLabel.textColor = subtitleColor
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(subtitleLabel)

        let iconView = UIImageView(image: icon?.withRenderingMode(.alwaysTemplate))
        iconView.tintColor = iconTintColor
        iconView.contentMode = .scaleAspectFit
        iconView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(iconView)

        addTarget(self, action: #selector(handleTap), for: .touchUpInside)

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: iconView.leadingAnchor, constant: -12),

            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor),
            subtitleLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            subtitleLabel.trailingAnchor.constraint(lessThanOrEqualTo: iconView.leadingAnchor, constant: -12),

            iconView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12),
            iconView.centerYAnchor.constraint(equalTo: centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 32),
            iconView.heightAnchor.constraint(equalToConstant: 32)
        ])
    }

    @objc private func handleTap() {
        action()
    }
}
