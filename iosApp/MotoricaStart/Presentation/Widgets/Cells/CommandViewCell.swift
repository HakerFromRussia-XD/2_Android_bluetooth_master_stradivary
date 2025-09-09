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
//            CustomSwitcher(title: viewModel.title, isOn: $isOn)
        }
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
        print("[BLE-COMMUNICATION] in updateUI viewModel.deviceAddress = \(viewModel.deviceAddress)")
        print("[BLE-COMMUNICATION] in updateUI viewModel.parameterID = \(viewModel.parameterID)")
        //        guard ref.addressDevice == viewModel.deviceAddress,
        //              ref.parameterID   == viewModel.parameterID else { return }
        let parameter = ParameterProvider.Companion()
            .getParameter(deviceAddress: ref.addressDevice, parameterID: ref.parameterID)
        print("[BLE-COMMUNICATION] in updateUI for ref = \(ref)")
        
        let ordinal = Int(parameter.type)
        print("[BLE-COMMUNICATION] in updateUI for ordinal = \(ordinal)")
        let entries = ParameterTypeEnum.values()
        print("[BLE-COMMUNICATION] in updateUI for entries = \(entries)")
        let count = Int(entries.size)
        print("[BLE-COMMUNICATION] in updateUI for count = \(count)")
        guard ordinal >= 0 && ordinal < count,
              let entry = entries.get(index: Int32(ordinal)) else { return }
        print("[BLE-COMMUNICATION] in updateUI for entry = \(entry)")
        let sizeOf = Int(entry.sizeOf)
        print("[BLE-COMMUNICATION] in updateUI for sizeOf = \(sizeOf)")
        
        
        
        let hex = parameter.data
        let end = hex.index(hex.startIndex, offsetBy: sizeOf * 2)
        let valueHex = String(hex[..<end])
        let value = Int(valueHex, radix: 16) ?? 0
        
//        DispatchQueue.main.async { [weak self] in
//            self?.provider?.value_1 = Float(value)
//        }
    }
}
