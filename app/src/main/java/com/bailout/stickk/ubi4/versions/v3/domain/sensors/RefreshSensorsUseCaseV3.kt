package com.bailout.stickk.ubi4.versions.v3.domain.sensors

class RefreshSensorsUseCaseV3(private val repository: V3SensorsCommandsRepository) {
    operator fun invoke(deviceAddress: String): Boolean =
        !repository.refreshInProgress.value && repository.refreshSensors(deviceAddress)
}
