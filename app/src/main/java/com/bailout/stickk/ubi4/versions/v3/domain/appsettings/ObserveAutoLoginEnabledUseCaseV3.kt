package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

import kotlinx.coroutines.flow.Flow

class ObserveAutoLoginEnabledUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeAutoLoginEnabled()
}
