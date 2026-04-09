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
    private enum StubConstants {
        static let fakeDeviceName = "UBIv4_CPU_Roma"
        static let fakeDeviceUUID = UUID(uuidString: "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE")!
    }
    private var allDevices: [BLEDevice] = [] // хранение полного списка устройств
    @Published private(set) var devices: [BLEDevice] = [] // список устройств для отображения в ViewController
    @Published var connectedDeviceID: UUID? // ID подключенного устройства
    private var selectedFilterIndex: Int = 0 // сохраняем текущий индекс фильтра
//    private let filterKey = "selectedFilterIndex" // Ключ для UserDefaults
    let bleManager : BleManagerKmm
    private var lastSeenTimestamps: [UUID: Date] = [:] // Храним время последнего обнаружения устройства
    
    private let repository: BluetoothRepository
    private let keyValueStorage: KeyValueStorage
    private var cancellables = Set<AnyCancellable>()
    
    var currentFilterIndex: Int { selectedFilterIndex }
    
    init(
        bleManager: BleManagerKmm,
        repository: BluetoothRepository = BluetoothRepositoryImpl(),
        keyValueStorage: KeyValueStorage
    ) {
        self.bleManager = bleManager
        self.repository = repository
        // При инициализации читаем сохранённый фильтр
//        selectedFilterIndex = UserDefaults.standard.integer(forKey: filterKey)
        self.keyValueStorage = keyValueStorage
        restorePersistedState()
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
        resetDevices()
        bleManager.startScanKmm { [weak self] bleDevice in
            guard let self = self else { return }
            let candidateName = (bleDevice.name ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            // Фильтруем устройства без имени или с "Unknown"
            guard !candidateName.isEmpty, candidateName != "Unknown" else {
                print("[BLE] пропускаем устройство без имени или с 'Unknown' 1")
                return
            }
            guard let uuid = UUID(uuidString: bleDevice.id) else { return }
            
            let device = BLEDevice(
                id: uuid,
                name: candidateName,
                uuid: uuid,
                rssi: Int(bleDevice.rssi)
            )
            
            DispatchQueue.main.async {
                // Работаем только по UUID, чтобы одно и то же устройство не дублировалось
                // с разными именами в разных пакетах сканирования.
                if let index = self.allDevices.firstIndex(where: { $0.id == device.id }) {
                    let current = self.allDevices[index]
                    let preferredName = self.preferredScanName(current: current.name, candidate: device.name)
                    self.allDevices[index] = BLEDevice(
                        id: current.id,
                        name: preferredName,
                        uuid: current.uuid,
                        rssi: device.rssi
                    )
                } else {
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
//        UserDefaults.standard.set(index, forKey: filterKey)
        do {
            try keyValueStorage.save(index, for: BluetoothStorageKeys.selectedFilterIndexStorageKey)
        } catch {
            print("[Storage] failed to persist filter index: \(error)")
        }
        
        selectedFilterIndex = index
        if index == 0 {
            print("[BLE-Filter] allDevices")
            devices = allDevices
        } else {
            print("[BLE-Filter] ubi4 or v3 family")
            devices = allDevices.filter {
                UiInterfaceModeBridgeV3.shared.isUbiDeviceFamily(deviceName: $0.name)
            }
        }
    }
    
    // подключение к устройству и сохранение состояний
    func connectToDevice(at index: Int) {
        let device = devices[index]
        print("[BLE-CONNECT] ViewModel.connectToDevice at index: \(index), device: \(device.name)")
        print("[BLE-CONNECT] ViewModel.connectToDevice at index: \(index), device: \(device.uuid )")
        do {
            try keyValueStorage.save(device.name, for: BluetoothStorageKeys.selectedDeviceNameStorageKey)
        } catch {
            print("[Storage] failed to persist selected device name: \(error)")
        }
        _ = UiInterfaceModeBridgeV3.shared.updateFromDeviceName(deviceName: device.name)
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
    }
    
    /// Добавляет в список устройств заглушку для тестового подключения и возвращает индекс устройства.
    @discardableResult
    func prepareFakeDeviceForTesting() -> Int? {
        let fakeDevice = BLEDevice(
            id: StubConstants.fakeDeviceUUID,
            name: StubConstants.fakeDeviceName,
            uuid: StubConstants.fakeDeviceUUID,
            rssi: -45
        )

        if !allDevices.contains(where: { $0.id == fakeDevice.id }) {
            allDevices.append(fakeDevice)
//            persistDevices()
        }

        applyFilter(index: selectedFilterIndex)
        return devices.firstIndex(where: { $0.id == fakeDevice.id })
    }
    
    // MARK: - Private
//    private func persistDevices() {
//        do {
//            try keyValueStorage.save(allDevices, for: BluetoothStorageKeys.devicesStorageKey)
//        } catch {
//            print("[Storage] failed to persist devices: \(error)")
//        }
//    }

    private func restorePersistedState() {
        selectedFilterIndex = (try? keyValueStorage.load(for: BluetoothStorageKeys.selectedFilterIndexStorageKey)) ?? 0
//        if let storedDevices = try? keyValueStorage.load(for: BluetoothStorageKeys.devicesStorageKey) {
//            allDevices = storedDevices
//        }
        applyFilter(index: selectedFilterIndex)
    }
    
    private func resetDevices() {
        allDevices.removeAll()
        devices.removeAll()
        keyValueStorage.removeValue(for: BluetoothStorageKeys.devicesStorageKey)
        applyFilter(index: selectedFilterIndex)
    }

    private func preferredScanName(current: String, candidate: String) -> String {
        let normalizedCurrent = current.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedCandidate = candidate.trimmingCharacters(in: .whitespacesAndNewlines)

        if normalizedCurrent.isEmpty { return normalizedCandidate }
        if normalizedCandidate.isEmpty { return normalizedCurrent }

        let currentUpper = normalizedCurrent.uppercased()
        let candidateUpper = normalizedCandidate.uppercased()

        // Никогда не затираем осмысленное имя на временный плейсхолдер "NAME".
        if currentUpper != "NAME", candidateUpper == "NAME" {
            return normalizedCurrent
        }
        if currentUpper == "NAME", candidateUpper != "NAME" {
            return normalizedCandidate
        }

        let currentHasPrefix = DeviceNameBridgeV3.shared.hasTransportPrefix(deviceName: normalizedCurrent)
        let candidateHasPrefix = DeviceNameBridgeV3.shared.hasTransportPrefix(deviceName: normalizedCandidate)

        if currentHasPrefix, !candidateHasPrefix {
            return normalizedCurrent
        }
        if !currentHasPrefix, candidateHasPrefix {
            return normalizedCandidate
        }

        return normalizedCandidate.count >= normalizedCurrent.count ? normalizedCandidate : normalizedCurrent
    }
}
