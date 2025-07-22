//
//  BluetoothRepositoryImpl.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import Combine
import CoreBluetooth

/// Assistant: Реализация BluetoothRepository через CoreBluetooth
public final class BluetoothRepositoryImpl: NSObject, BluetoothRepository {
    private let connectionSubject = PassthroughSubject<UUID, Never>()  // паблишер для didConnect
    public var connectionPublisher: AnyPublisher<UUID, Never> {
        connectionSubject.eraseToAnyPublisher()
    }
    public var scannedDevicesPublisher: AnyPublisher<[BLEDevice], Never> {
        scannedDevicesSubject.eraseToAnyPublisher()
    }

    private let scannedDevicesSubject = CurrentValueSubject<[BLEDevice], Never>([])
    private var centralManager: CBCentralManager!
    private var discovered: [UUID: (device: BLEDevice, peripheral: CBPeripheral)] = [:]
    /// Время последнего обнаружения для каждого устройства
    private var lastSeenTimestamps: [UUID: Date] = [:]
    /// Таймер для периодической очистки «протухших» устройств
    private var cleanupTimer: Timer?

    public override init() {
        super.init()
        print("[BLE] Инициализация CBCentralManager")
        centralManager = CBCentralManager(delegate: self, queue: .main)
        // Assistant: запускаем таймер для очистки устройств, не виденных >30 сек
        cleanupTimer = Timer.scheduledTimer(
            timeInterval: 5.0,
            target: self,
            selector: #selector(cleanupExpiredDevices),
            userInfo: nil,
            repeats: true
        )
    }
    
    deinit {
        cleanupTimer?.invalidate()
    }

    public func startScanning() {
        print("[BLE] startScanning() called, current state: \(centralManager.state)")
        guard centralManager.state == .poweredOn else {
            print("[BLE] Невозможно сканировать, т.к. Bluetooth не poweredOn")
            return }
        discovered.removeAll()
        scannedDevicesSubject.send([])
        centralManager.scanForPeripherals(withServices: nil, options: nil)
        print("[BLE] Сканирование запущено")
    }

    public func stopScanning() {
        centralManager.stopScan()
        print("[BLE] Сканирование остановлено")
    }
    
    public func connect(to device: BLEDevice) {
        print("[BLE-CONNECT] Repository.connect called for device: \(device.name) [\(device.uuid)]")
        let uuid = device.uuid
        guard let entry = discovered[uuid] else { return }
        centralManager.connect(entry.peripheral, options: nil)
    }
    
}

// MARK: - CBCentralManagerDelegate
extension BluetoothRepositoryImpl: CBCentralManagerDelegate {
    public func centralManagerDidUpdateState(_ manager: CBCentralManager) {
        // при включении BLE можно запускать сканирование или уведомить ошибку
        print("[BLE-CONNECT] centralManagerDidUpdateState: \(manager.state)")
        print("[BLE] State changed to: \(manager.state.rawValue) — \(manager.state)")
        switch manager.state {
            case .poweredOn:
                print("[BLE] Bluetooth включён, запускаем сканирование")
//                manager.scanForPeripherals(withServices: nil,
//                                           options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
            default:
                print("[BLE] Bluetooth недоступен (не включён или нет прав)")
                discovered.removeAll()
                scannedDevicesSubject.send([])
        }
    }
    public func centralManager(_ manager: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("[BLE-CONNECT] centralManager didConnect peripheral: \(peripheral.identifier)")
        // Assistant: уведомляем ViewModel о подключении
        connectionSubject.send(peripheral.identifier)
    }

    public func centralManager(
        _ manager: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        // 1) Фильтруем устройства без имени
        guard let name = peripheral.name, !name.isEmpty, name != "Unknown" else {
            print("[BLE] пропускаем устройство без имени или с 'Unknown'")
            return
        }
        
        let uuid = peripheral.identifier
        // 2) Используем uuid как id, чтобы не было дублирования
        let device = BLEDevice(
            id: uuid,
            name: name,
            uuid: uuid,
            rssi: RSSI.intValue
        )
        
        print("[BLE] didDiscover: name='\(name)', id=\(peripheral.identifier), rssi=\(RSSI)")
        print("[BLE] advertisementData keys: \(advertisementData.keys)")
        
        // 1) сохраняем/обновляем устройство
        discovered[uuid] = (device: device, peripheral: peripheral)
        // 2) обновляем timestamp
        lastSeenTimestamps[uuid] = Date()
        scannedDevicesSubject.send(discovered.values.map { $0.device })
    }
}
private extension BluetoothRepositoryImpl {
    /// Удаляет устройства, которые не были обнаружены в течение последних 30 секунд
    @objc func cleanupExpiredDevices() {
        let now = Date()
        let expirationInterval: TimeInterval = 10.0
        var didRemove = false

        for (uuid, lastSeen) in lastSeenTimestamps {
            if now.timeIntervalSince(lastSeen) > expirationInterval {
                discovered.removeValue(forKey: uuid)
                lastSeenTimestamps.removeValue(forKey: uuid)
                didRemove = true
            }
        }
        if didRemove {
            DispatchQueue.main.async {
                self.scannedDevicesSubject.send(self.discovered.values.map { $0.device })
            }
        }
    }
}
