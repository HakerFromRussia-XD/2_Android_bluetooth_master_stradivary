package com.bailout.stickk.ubi4.versions.v3.domain.main

class ManageMainConnectionUseCaseV3(private val repository: V3MainRepository) {
    fun rememberLastConnection() = repository.rememberLastConnection()
    fun restoreSavedConnection() = repository.restoreSavedConnection()
    fun resetLastConnection() = repository.resetLastConnection()
    fun initializeWidgetStorage() = repository.initializeWidgetStorage()
}

class DisconnectDeviceUseCaseV3(private val repository: V3DeviceDisconnectionRepository) {
    operator fun invoke() = repository.disconnectDevice()
}

class ConnectDeviceUseCaseV3(private val repository: V3DeviceConnectionRepository) {
    fun prepareConnection(deviceName: String, deviceAddress: String) =
        repository.prepareConnection(deviceName, deviceAddress)

    operator fun invoke(deviceAddress: String) = repository.connectDevice(deviceAddress)
}
