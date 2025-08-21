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
    private let bleManager = BleManagerKmm()
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
        print("BLE-CONNECT onAppear")
        bleManager.startScanKmm { [weak self] bleDevice in
            guard let self = self else { return }
            // Фильтруем устройства без имени или с "Unknown"
            guard let name = bleDevice.name, !name.isEmpty, name != "Unknown" else {
                print("[BLE] пропускаем устройство без имени или с 'Unknown' 1")
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
                if self.allDevices.firstIndex(where: { $0.name ==  bleDevice.name ?? "Unknown" }) != nil {}
                else {
                    // Если устройства с таким UUID нет, добавляем его в список
                    self.allDevices.append(device)
                }
                self.applyFilter(index: self.selectedFilterIndex)
            }
        }
    }
    func onDisappear() {
        bleManager.stopScanKmm()
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
        let device = devices[index]
        print("[BLE-CONNECT] ViewModel.connectToDevice at index: \(index), device: \(device.name)")
        print("[BLE-CONNECT] ViewModel.connectToDevice at index: \(index), device: \(device.uuid )")
        bleManager.stopScanKmm()
        bleManager.connectToDevice(uuid: device.uuid.uuidString)
    }
    
    func sendBytes() {
        let u8: [UInt8] = [0x40, 0x88, 0x00, 0x01, 0x00, 0x00, 0x06, 0x03]
        let kb = KotlinByteArray(u8)

        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
        bleManager.sendBytesKmm(
            data: kb,
            command: Constants.MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand: Constants.WRITE,
            onChunkSent: {}
        )
    }
}
