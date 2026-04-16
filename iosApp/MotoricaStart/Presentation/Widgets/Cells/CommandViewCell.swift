import UIKit
import SwiftUI
import Combine
import shared

final class CommandViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: CommandViewCell.self)
    static let height = CGFloat(56)

    private var viewModel: CommandListItemViewModel?
    private var viewModelV3: CommandListItemViewModelV3?
    private var cancellable: AnyCancellable?

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: CommandListItemViewModel) {
        self.viewModel = viewModel
        self.viewModelV3 = nil
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        contentConfiguration = UIHostingConfiguration {
            CustomButton(
                title: viewModel.title,
                onPress: {
                    viewModel.didPressDown()
                },
                onRelease: {
                    viewModel.didRelease()
                }
            )
        }
        .margins(.vertical, 4)
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: CommandListItemViewModelV3) {
        self.viewModel = nil
        self.viewModelV3 = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        contentConfiguration = UIHostingConfiguration {
            MultiCommandButtonsWidgetView(
                titles: viewModel.visibleButtonTitles,
                onPress: { index in
                    viewModel.didPressDown(at: index)
                },
                onRelease: { index in
                    viewModel.didRelease(at: index)
                }
            )
        }
        .margins(.vertical, 4)
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        viewModel = nil
        viewModelV3 = nil
        cancellable?.cancel()
        cancellable = nil
        contentConfiguration = nil
    }
}

private struct MultiCommandButtonsWidgetView: View {
    let titles: [String]
    let onPress: (Int) -> Void
    let onRelease: (Int) -> Void

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Array(titles.enumerated()), id: \.offset) { index, title in
                CustomButton(
                    title: title,
                    onPress: {
                        onPress(index)
                    },
                    onRelease: {
                        onRelease(index)
                    }
                )
            }
        }
    }
}
