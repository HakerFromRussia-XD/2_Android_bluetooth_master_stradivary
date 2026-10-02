package com.bailout.stickk.ubi4.versions.v3.domain.device

class GetDeviceSessionUseCaseV3(private val repository: V3DeviceSessionRepository) {
    operator fun invoke(): V3DeviceSession = repository.getSession()
}
