//
//  DeviceCell.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 22.05.2023.
//  Copyright © 2023 Brian Advent. All rights reserved.
//

import UIKit
 
class DeviceCell: UITableViewCell {
    @IBOutlet weak var deviceNameText: UILabel!
    @IBOutlet weak var rssi: UILabel!
    
    
    func setupModel(model: ScanItem) {
        deviceNameText.text = model.title
        rssi.text = NumberFormatter().string(from: model.rssi) ?? "default value"
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
    }
    
    override func  setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }
}
