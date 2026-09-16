package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

import kotlinx.coroutines.flow.Flow

class ObserveSettingsProfilesChangesUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    operator fun invoke(): Flow<Unit> = repository.updates
}
