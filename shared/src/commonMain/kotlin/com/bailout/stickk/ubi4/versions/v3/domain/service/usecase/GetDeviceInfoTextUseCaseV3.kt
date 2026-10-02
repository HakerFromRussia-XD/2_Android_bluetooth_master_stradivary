package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoRepository

class GetDeviceInfoTextUseCaseV3(private val repository: V3DeviceInfoRepository) {
    operator fun invoke(field: V3DeviceInfoField): String? = repository.getTextForInput(field)
}
