import UIKit
import SwiftUI
import shared

final class SpinnerViewCellV3: UITableViewCell {
    static let reuseIdentifier = String(describing: SpinnerViewCellV3.self)

    private var viewModel: SpinnerListItemViewModelV3?
    private var provider: SpinnerProviderV3?
    private var job: Kotlinx_coroutines_coreJob?

    override func prepareForReuse() {
        super.prepareForReuse()
        job?.cancel(cause: nil)
        job = nil
        provider = nil
        viewModel = nil
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

        let selected = viewModel.currentSelectedIndex() ?? viewModel.initialSelectedIndex
        let provider = SpinnerProviderV3(
            title: viewModel.title,
            items: viewModel.items,
            selectedIndex: selected
        )
        self.provider = provider

        var configuration = UIHostingConfiguration {
            SpinnerRowViewV3(provider: provider) { [weak self] index in
                self?.provider?.selectedIndex = index
                self?.viewModel?.sendSelectedIndex(index)
            }
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration

        job?.cancel(cause: nil)
        job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self, self.viewModel?.matches(snapshot: snapshot) == true else { return }
            guard let index = self.viewModel?.selectedIndex(from: snapshot) else { return }
            DispatchQueue.main.async {
                self.provider?.selectedIndex = index
            }
        }

        viewModel.requestCurrent()
    }
}

private final class SpinnerProviderV3: ObservableObject {
    let title: String
    let items: [String]
    @Published var selectedIndex: Int

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
    @State private var isExpanded = false

    private enum Layout {
        static let rowHeight: CGFloat = 56
        static let pickerHeight: CGFloat = 40
        static let dropdownWidth: CGFloat = 220
        static let panelCornerRadius: CGFloat = 18
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
                isExpanded.toggle()
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

                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
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
            .overlay(alignment: .topTrailing) {
                if isExpanded {
                    dropdownPanel
                        .frame(width: Layout.dropdownWidth)
                        .offset(y: Layout.panelTopOffset)
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
        .zIndex(isExpanded ? 1000 : 0)
        .animation(.easeInOut(duration: 0.16), value: isExpanded)
    }

    private var dropdownPanel: some View {
        VStack(spacing: 0) {
            ForEach(Array(provider.items.enumerated()), id: \.offset) { index, item in
                Button {
                    isExpanded = false
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

#if DEBUG
import SwiftUI

struct SpinnerRowViewV3_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            SpinnerRowViewV3(
                provider: SpinnerProviderV3(
                    title: "Режим работы протеза",
                    items: [
                        "Нормальный",
                        "Спортивный",
                        "Плавное управление силой",
                        "Плавное управление скоростью",
                        "Плавное управление силой и скоростью"
                    ],
                    selectedIndex: 1
                ),
                onSelect: { _ in }
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
