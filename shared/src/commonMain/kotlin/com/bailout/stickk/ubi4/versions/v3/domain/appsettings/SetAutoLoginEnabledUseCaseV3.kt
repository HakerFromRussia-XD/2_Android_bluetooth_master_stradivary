package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class SetAutoLoginEnabledUseCaseV3(
    private val repository: V3AutoLoginSettingsRepository,
    private val skipUnchanged: Boolean = true,
) {
    operator fun invoke(enabled: Boolean) {
        if (skipUnchanged && repository.getAutoLoginEnabled() == enabled) return
        repository.setAutoLoginEnabled(enabled)
    }
}
