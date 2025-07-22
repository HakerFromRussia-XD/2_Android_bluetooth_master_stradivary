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
    
    private let repository: BluetoothRepository
    private var cancellables = Set<AnyCancellable>()
    
    init(repository: BluetoothRepository = BluetoothRepositoryImpl()) {
        self.repository = repository
        // При инициализации читаем сохранённый фильтр
        selectedFilterIndex = UserDefaults.standard.integer(forKey: filterKey)
        // Подписываемся на поток найденных устройств
        repository.scannedDevicesPublisher
            .receive(on: DispatchQueue.main)
            .sink{ devices in
                let info = devices
                            .map { "\($0.name)(rssi:\($0.rssi))" }
                            .joined(separator: ", ")
                print("[BLE-VM] received devices: \(info)")
                self.allDevices = devices
                self.applyFilter(index: self.selectedFilterIndex)
            }
            .store(in: &cancellables)
        
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
//        repository.startScanning()
        print("test log from BLEViewModel")
//        bleManager.startScan { BleDevice in
//            print("МЫ НАШЛИ УСТРОЙСТВО \(BleDevice.name)!!!")
//        }
    }
    
    func onDisappear() {
        repository.stopScanning()
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
}
