package com.bailout.stickk.ubi4.versions.v3.domain.main

class UpdateMainDeviceIdentityUseCaseV3(private val repository: V3MainRepository) {
    fun loadConnectedDevice() = repository.loadDeviceIdentity()
    fun applySerialNumber(serial: String) = repository.applySerialNumber(serial)
}
