package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class SetAutoLoginEnabledUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(enabled: Boolean) {
        if (repository.getAutoLoginEnabled() == enabled) return
        repository.setAutoLoginEnabled(enabled)
    }
}
