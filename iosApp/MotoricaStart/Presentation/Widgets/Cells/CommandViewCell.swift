import UIKit
import SwiftUI
import Combine
import shared

final class CommandViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing:CommandViewCell.self)
    private var viewModel: CommandListItemViewModel!
    private let mainQueue: DispatchQueueType = DispatchQueue.main
    private var numberCancellable: AnyCancellable?
    static let height = CGFloat(56)
    
    // Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    private var cancellable: AnyCancellable?
    
    override func awakeFromNib() { super.awakeFromNib() }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: CommandListItemViewModel) {
        self.viewModel = viewModel
        @State var isOn = true
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        
        // 2. Вклеиваем SwiftUI контент
        var configuration = UIHostingConfiguration {
            CustomButton(
                title: viewModel.title,
                onPress: {
                    viewModel.didPressDown()
                },
                onRelease: {
                    viewModel.didRelease()
                }
            )
//            CustomSwitcher(title: viewModel.title, isOn: $isOn)
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration
        numberCancellable?.cancel()
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        cancellable?.cancel()
        cancellable = nil
        contentConfiguration = nil
    }
    
    private func updateUI(_ ref: ParameterRef, viewModel: CommandListItemViewModel) {
        print("[BLE-COMMUNICATION] in updateUI")
        print("[BLE-COMMUNICATION] in updateUI viewModel.deviceAddress = \(viewModel.widget.deviceAddress)")
        print("[BLE-COMMUNICATION] in updateUI viewModel.parameterID = \(viewModel.widget.parameterID)")
        guard ref.addressDevice == viewModel.widget.deviceAddress,
              ref.parameterID   == viewModel.widget.parameterID else { return }
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        print("[BLE-COMMUNICATION] in updateUI for ref = \(ref)")
        
        
        viewModel.widget.commandUnified?.clickCommand

    }
}
