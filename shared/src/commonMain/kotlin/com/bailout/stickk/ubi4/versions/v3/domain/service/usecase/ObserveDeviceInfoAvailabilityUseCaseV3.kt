package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import kotlinx.coroutines.flow.StateFlow
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoRepository

class ObserveDeviceInfoAvailabilityUseCaseV3(private val repository: V3DeviceInfoRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
