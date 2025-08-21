//
//  BluetoothRepository.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 23.04.2025.
//
import Foundation
import Combine

public protocol BluetoothRepository {
    /// Публикатор списка найденных устройств
    var scannedDevicesPublisher: AnyPublisher<[BLEDevice], Never> { get }
    var connectionPublisher: AnyPublisher<UUID, Never> { get }
    
    /// Начать сканирование
    func startScanning()
    /// Остановить сканирование
    func stopScanning()
    /// Подключение к устройству
    func connect(to device: BLEDevice)
}
