package com.bailout.stickk.ubi4.versions.v3.domain.service

class GetDeviceInfoTextUseCaseV3(private val repository: V3DeviceInfoRepository) {
    operator fun invoke(field: V3DeviceInfoField): String? = repository.getTextForInput(field)
}
