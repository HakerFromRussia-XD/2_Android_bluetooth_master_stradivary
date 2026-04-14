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
    @State private var isMenuPresented = false

    private enum Layout {
        static let rowHeight: CGFloat = 42
        static let dropdownWidth: CGFloat = 220
    }

    var body: some View {
        HStack(spacing: 8) {
            Text(provider.title)
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(.white)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                isMenuPresented = true
            } label: {
                HStack(spacing: 6) {
                    Text(provider.selectedTitle)
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .center)

                    Image(systemName: isMenuPresented ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12, weight: .regular))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                }
                .frame(height: 40)
                .padding(.horizontal, 12)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color("ubi4_gray"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                        )
                )
            }
            .buttonStyle(.plain)
            .frame(width: Layout.dropdownWidth)
            .popover(isPresented: $isMenuPresented, attachmentAnchor: .rect(.bounds), arrowEdge: .top) {
                spinnerPopup
                    .modifier(CompactPopoverModifier())
            }
        }
        .padding(.horizontal, 8)
        .frame(height: 54)
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

    @ViewBuilder
    private var spinnerPopup: some View {
        VStack(spacing: 0) {
            ForEach(Array(provider.items.enumerated()), id: \.offset) { index, item in
                Button {
                    onSelect(index)
                    isMenuPresented = false
                } label: {
                    Text(item)
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(Color("ubi4_white"))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .frame(height: Layout.rowHeight)

                if index < provider.items.count - 1 {
                    Rectangle()
                        .fill(Color("ubi4_gray_border"))
                        .frame(height: 1)
                }
            }
        }
        .frame(width: Layout.dropdownWidth)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(Color("ubi4_gray"))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.35), radius: 8, x: 0, y: 4)
        )
        .padding(.vertical, 6)
        .padding(.horizontal, 4)
    }
}

private struct CompactPopoverModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.4, *) {
            content.presentationCompactAdaptation(.popover)
        } else {
            content
        }
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
                    selectedIndex: 4
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
