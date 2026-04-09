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

    var body: some View {
        HStack(spacing: 8) {
            Text(provider.title)
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(.white)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)

            Menu {
                ForEach(Array(provider.items.enumerated()), id: \.offset) { index, item in
                    Button(item) {
                        onSelect(index)
                    }
                }
            } label: {
                HStack(spacing: 6) {
                    Text(provider.selectedTitle)
                        .font(.custom("SFProDisplay-Light", size: 12))
                        .foregroundColor(.white)
                        .lineLimit(1)
                    Image(systemName: "chevron.down")
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
}
