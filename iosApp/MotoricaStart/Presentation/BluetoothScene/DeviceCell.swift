//
//  DeviceCell.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 25.04.2025.
//
import UIKit
import shared
 
class DeviceCell: UITableViewCell {
    static let identifier = "DeviceCell"
//    var onTap: (() -> Void)?
    
    @IBOutlet weak var deviceNameText: UILabel!
    @IBOutlet weak var rssi: UILabel!
    @IBOutlet private weak var containerView: UIView!
    
    func setupModel(model: BLEDevice) {
        deviceNameText.text = DeviceNameBridgeV3.shared.displayName(deviceName: model.name)
        rssi.text = String(model.rssi)
        
//        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
//        tapGesture.cancelsTouchesInView = true
//        contentView.addGestureRecognizer(tapGesture)
//        contentView.isUserInteractionEnabled = true
    }
    
//    override func prepareForReuse() {
//        super.prepareForReuse()
////        onTap = nil
//    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
        deviceNameText.font = UIFont(name: "OpenSansRoman-SemiBold", size: 13)
        rssi.font = UIFont(name: "OpenSansRoman-SemiBold", size: 13)
        
        // Отключаем обработку касаний у фонового контейнера, чтобы вся ячейка отвечала на нажатия.
        containerView.isUserInteractionEnabled = false
    }
    
    override func  setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        let message = "[BLE-TAP-TRACE] cellTouchesBegan id=\(accessibilityIdentifier ?? "nil")"
        NSLog("%@", message)
        print(message)
    }
    
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        let message = "[BLE-TAP-TRACE] cellTouchesEnded id=\(accessibilityIdentifier ?? "nil")"
        NSLog("%@", message)
        print(message)
    }
    
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesCancelled(touches, with: event)
        let message = "[BLE-TAP-TRACE] cellTouchesCancelled id=\(accessibilityIdentifier ?? "nil")"
        NSLog("%@", message)
        print(message)
    }
    
//    @objc private func handleTap() {
//        onTap?()
//    }
}
