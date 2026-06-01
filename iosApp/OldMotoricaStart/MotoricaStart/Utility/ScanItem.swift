import Foundation
import CoreBluetooth

class ScanItem {
    var title: String
    var rssi: NSNumber
    var peripheral: CBPeripheral
    
    init(title: String, rssi: NSNumber, peripheral: CBPeripheral) {
        self.title = title
        self.rssi = rssi
        self.peripheral = peripheral
    }
}
