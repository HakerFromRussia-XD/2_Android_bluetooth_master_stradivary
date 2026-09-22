package com.bailout.stickk.ubi4.versions.v3.domain.main

class ObserveMainUpdatesUseCaseV3(private val repository: V3MainRepository) {
    operator fun invoke() = repository.observeUpdates()
    fun deviceIdentity() = repository.observeDeviceIdentity()
}
