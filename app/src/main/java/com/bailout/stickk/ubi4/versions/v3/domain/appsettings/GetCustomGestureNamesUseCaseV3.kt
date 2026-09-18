package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class GetCustomGestureNamesUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(): V3CustomGestureNames = repository.getCustomGestureNames()
}
