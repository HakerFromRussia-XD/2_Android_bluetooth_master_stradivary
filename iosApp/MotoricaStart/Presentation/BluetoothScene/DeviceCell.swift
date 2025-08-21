//
//  DeviceCell.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 25.04.2025.
//
import UIKit
 
class DeviceCell: UITableViewCell {
    static let identifier = "DeviceCell"
    
    @IBOutlet weak var deviceNameText: UILabel!
    @IBOutlet weak var rssi: UILabel!
    
    func setupModel(model: BLEDevice) {
        deviceNameText.text = model.name
        rssi.text = String(model.rssi)
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
    }
    
    override func  setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }
}
