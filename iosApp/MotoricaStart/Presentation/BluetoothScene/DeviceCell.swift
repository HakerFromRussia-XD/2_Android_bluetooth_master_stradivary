//
//  DeviceCell.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 25.04.2025.
//
import UIKit
 
class DeviceCell: UITableViewCell {
    static let identifier = "DeviceCell"
//    var onTap: (() -> Void)?
    
    @IBOutlet weak var deviceNameText: UILabel!
    @IBOutlet weak var rssi: UILabel!
    @IBOutlet private weak var containerView: UIView!
    
    func setupModel(model: BLEDevice) {
        deviceNameText.text = model.name
        rssi.text = String(model.rssi)
        
//        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
//        tapGesture.cancelsTouchesInView = true
//        contentView.addGestureRecognizer(tapGesture)
//        contentView.isUserInteractionEnabled = true
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
//        onTap = nil
    }
    
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
    
//    @objc private func handleTap() {
//        onTap?()
//    }
}
