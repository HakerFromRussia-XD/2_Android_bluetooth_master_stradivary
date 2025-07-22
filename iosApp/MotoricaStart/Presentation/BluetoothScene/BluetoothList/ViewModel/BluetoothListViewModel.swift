//
//  BluetoothListViewModel.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import Foundation
import Combine
import shared

final class BluetoothListViewModel {
    private var allDevices: [BLEDevice] = [] // хранение полного списка устройств
    @Published private(set) var devices: [BLEDevice] = [] // список устройств для отображения в ViewController
    @Published var connectedDeviceID: UUID? // ID подключенного устройства
    private var selectedFilterIndex: Int = 0 // сохраняем текущий индекс фильтра
    private let filterKey = "selectedFilterIndex" // Ключ для UserDefaults
    private let bleManager = BleManager_fromTestProj()
    private var lastSeenTimestamps: [UUID: Date] = [:] // Храним время последнего обнаружения устройства
    
    private let repository: BluetoothRepository
    private var cancellables = Set<AnyCancellable>()
    
    init(repository: BluetoothRepository = BluetoothRepositoryImpl()) {
        self.repository = repository
        // При инициализации читаем сохранённый фильтр
        selectedFilterIndex = UserDefaults.standard.integer(forKey: filterKey)
        // Подписываемся на поток найденных устройств
//        repository.scannedDevicesPublisher
//            .receive(on: DispatchQueue.main)
//            .sink{ devices in
//                let info = devices
//                            .map { "\($0.name)(rssi:\($0.rssi))" }
//                            .joined(separator: ", ")
//                print("[BLE-VM] received devices: \(info)")
//                self.allDevices = devices
//                self.applyFilter(index: self.selectedFilterIndex)
//            }
//            .store(in: &cancellables)
        
        // Подписываемся на поток подключённых устройств
        repository.connectionPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] uuid in
                print("[BLE-CONNECT] ViewModel received connect callback for: \(uuid)")
                self?.connectedDeviceID = uuid
            }
            .store(in: &cancellables)
    }
    
    func onAppear() {
        print("test log from BLEViewModel")
        bleManager.startScan { [weak self] bleDevice in
            guard let self = self else { return }
            // Фильтруем устройства без имени или с "Unknown"
            guard let name = bleDevice.name, !name.isEmpty, name != "Unknown" else {
                print("[BLE] пропускаем устройство без имени или с 'Unknown'")
                return
            }
            guard let uuid = UUID(uuidString: bleDevice.id) else { return }
            
            let device = BLEDevice(
                id: uuid,
                name: bleDevice.name ?? "Unknown",
                uuid: uuid,
                rssi: Int(bleDevice.rssi)

            )
            
            DispatchQueue.main.async {
                // Проверяем, есть ли устройство с таким UUID в списке
                if let index = self.allDevices.firstIndex(where: { $0.id == uuid }) {
                    // Если устройство уже существует, обновляем его данные
                    self.allDevices[index] = device
                } else {
                    // Если устройства с таким UUID нет, добавляем его в список
                    self.allDevices.append(device)
                }
                
                // Обновляем время последнего обнаружения для устройства
//                self.lastSeenTimestamps[uuid] = Date()
                                
                // Выполняем очистку устаревших устройств
//                self.cleanupExpiredDevices()
                
                
                self.applyFilter(index: self.selectedFilterIndex)
            }
        }
    }
    
    func onDisappear() {
//        repository.stopScanning()
        bleManager.stopScan()
    }
    
    // метод для фильтрации списка по сегменту
    func applyFilter(index: Int) {
        // Сохраняем состояние фильтра между запусками
        UserDefaults.standard.set(index, forKey: filterKey)
        
        selectedFilterIndex = index
        if index == 0 {
            print("[BLE-Filter] allDevices")
            devices = allDevices
        } else {
            print("[BLE-Filter] name.contains(UBI4)")
            devices = allDevices.filter { $0.name.contains("UBIv4") }
        }
    }
    
    // подключение к устройству и сохранение состояний
    func connectToDevice(at index: Int) {
        print("[BLE-CONNECT] ViewModel.connectToDevice at index: \(index), device: \(devices[index].name)")
        let device = devices[index]
        repository.connect(to: device)
    }
    
//    Удаляет устройства, которые не были обнаружены в течение последних 10 секунд
//    private func cleanupExpiredDevices() {
//        let now = Date()
//        let expirationInterval: TimeInterval = 20.0
//        var didRemove = false
//
//        for (uuid, lastSeen) in lastSeenTimestamps {
//            if now.timeIntervalSince(lastSeen) > expirationInterval {
//                // Удаляем устройство из allDevices и lastSeenTimestamps
//                if let index = allDevices.firstIndex(where: { $0.id == uuid }) {
//                    allDevices.remove(at: index)
//                }
//                lastSeenTimestamps.removeValue(forKey: uuid)
//                didRemove = true
//            }
//        }
//        
//        // Если устройства были удалены, обновляем список
//        if didRemove {
//            DispatchQueue.main.async {
//                self.devices = self.allDevices
//            }
//        }
//    }
}
