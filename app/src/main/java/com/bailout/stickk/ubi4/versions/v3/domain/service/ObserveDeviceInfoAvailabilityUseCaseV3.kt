package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

class ObserveDeviceInfoAvailabilityUseCaseV3(private val repository: V3DeviceInfoRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
