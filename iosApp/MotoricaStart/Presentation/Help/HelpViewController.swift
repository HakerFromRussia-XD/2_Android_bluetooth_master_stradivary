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
    private let activeColor = UIColor.accountColor("ubi4_active", fallback: 0xC6F158)

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
            addSectionTitle(section.title, topInset: HelpMetrics.sectionSpacing)
            addCard(makeMenuCard(items: section.items))
        }
    }

    private func renderPage(_ page: InstructionPage) {
        addSectionTitle(page.title)
        for card in page.cards {
            addContentCard(card)
        }
        if !page.relatedItems.isEmpty {
            addSectionTitle(SharedRes.strings().prostheses_use, topInset: HelpMetrics.sectionSpacing)
            addCard(makeMenuCard(items: page.relatedItems))
        }
    }

    private func addSectionTitle(_ resource: StringResource, topInset: CGFloat = 0) {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false

        let label = UILabel()
        label.text = resource.desc().localized()
        label.font = .systemFont(ofSize: 14, weight: .semibold)
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

    private func makeMenuCard(items: [InstructionMenuItem]) -> UIView {
        let card = HelpCardView(backgroundColor: cardColor, borderColor: borderColor)
        for (index, item) in items.enumerated() {
            card.addRow(HelpMenuRow(item: item, textColor: textColor, inactiveTextColor: inactiveTextColor) { [weak self] item in
                self?.handleMenuItem(item)
            })
            if index != items.indices.last {
                card.addDivider(color: borderColor)
            }
        }
        return card
    }

    private func addContentCard(_ card: InstructionCard) {
        let container = HelpCardView(backgroundColor: cardColor, borderColor: borderColor)
        let stack = UIStackView()
        stack.axis = .vertical
        stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        stack.layoutMargins = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        stack.isLayoutMarginsRelativeArrangement = true
        container.addRow(stack)

        for block in card.blocks {
            stack.addArrangedSubview(makeBlockView(block))
        }
        addCard(container)
    }

    private func makeBlockView(_ block: InstructionBlock) -> UIView {
        switch block.type {
        case .heading:
            return makeLabel(block.text?.desc().localized() ?? "", font: .systemFont(ofSize: 14, weight: .semibold), color: textColor)
        case .paragraph:
            return makeLabel(block.text?.desc().localized() ?? "", font: .systemFont(ofSize: 14, weight: .regular), color: textColor)
        case .notice:
            return makeLabel(block.text?.desc().localized() ?? "", font: .systemFont(ofSize: 14, weight: .semibold), color: activeColor)
        case .numbered:
            let stack = UIStackView()
            stack.axis = .vertical
            stack.spacing = 8
            for (index, resource) in block.items.enumerated() {
                stack.addArrangedSubview(makeLabel("\(index + 1). \(resource.desc().localized())", font: .systemFont(ofSize: 14), color: textColor))
            }
            return stack
        case .bullets:
            let stack = UIStackView()
            stack.axis = .vertical
            stack.spacing = 8
            for resource in block.items {
                stack.addArrangedSubview(makeLabel("• \(resource.desc().localized())", font: .systemFont(ofSize: 14), color: textColor))
            }
            return stack
        case .image:
            guard let image = block.image?.toUIImage() else { return UIView() }
            let imageView = UIImageView(image: image)
            imageView.contentMode = .scaleAspectFit
            imageView.clipsToBounds = true
            imageView.translatesAutoresizingMaskIntoConstraints = false
            imageView.heightAnchor.constraint(equalToConstant: CGFloat(block.imageHeight)).isActive = true
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
        let divider = UIView()
        divider.backgroundColor = color
        divider.translatesAutoresizingMaskIntoConstraints = false
        divider.heightAnchor.constraint(equalToConstant: 1).isActive = true
        stack.addArrangedSubview(divider)
    }
}

private final class HelpMenuRow: UIControl {
    private let item: InstructionMenuItem
    private let action: (InstructionMenuItem) -> Void

    init(item: InstructionMenuItem, textColor: UIColor, inactiveTextColor: UIColor, action: @escaping (InstructionMenuItem) -> Void) {
        self.item = item
        self.action = action
        super.init(frame: .zero)
        setup(textColor: textColor, inactiveTextColor: inactiveTextColor)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setup(textColor: UIColor, inactiveTextColor: UIColor) {
        heightAnchor.constraint(greaterThanOrEqualToConstant: HelpMetrics.rowHeight).isActive = true
        isEnabled = item.actionType != .disabled

        let label = UILabel()
        label.text = item.title.desc().localized()
        label.font = .systemFont(ofSize: 14, weight: .regular)
        label.textColor = isEnabled ? textColor : inactiveTextColor
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        addSubview(label)

        let chevron = UIImageView(image: UIImage(named: "ic_navigate_next")?.withRenderingMode(.alwaysTemplate))
        chevron.tintColor = isEnabled ? textColor : inactiveTextColor
        chevron.contentMode = .scaleAspectFit
        chevron.isHidden = !isEnabled
        chevron.translatesAutoresizingMaskIntoConstraints = false
        addSubview(chevron)

        addTarget(self, action: #selector(handleTap), for: .touchUpInside)

        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: topAnchor, constant: 16),
            label.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 18),
            label.trailingAnchor.constraint(lessThanOrEqualTo: chevron.leadingAnchor, constant: -12),
            label.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -16),

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
