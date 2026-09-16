package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class GetAutoLoginEnabledUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(): Boolean = repository.getAutoLoginEnabled()
}
