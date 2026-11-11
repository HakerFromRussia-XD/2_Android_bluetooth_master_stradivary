package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

import kotlinx.coroutines.flow.Flow

data class V3BaseSettingsOffer(
    val serial: String,
    val revision: Long,
    val profileId: Int = V3SettingsProfiles.BASE_PROFILE_ID,
    val profileName: String? = null,
) {
    val isPersonal get() = profileId != V3SettingsProfiles.BASE_PROFILE_ID
}

data class V3BaseSettingsEnvironment(
    val serial: String = "",
    val isUser: Boolean = false,
    val canApply: Boolean = false,
    val revision: Long = 0,
    val handledRevision: Long = 0,
    val online: Boolean = true,
    val isEngineer: Boolean = false,
    val personalOffer: V3BaseSettingsOffer? = null,
    val isConnected: Boolean = false,
) {
    val offer: V3BaseSettingsOffer? get() = when {
        !isUser || serial.isBlank() -> null
        revision > handledRevision -> V3BaseSettingsOffer(serial, revision)
        else -> personalOffer
    }
}

interface V3BaseSettingsUpdatesRepository {
    fun observeEnvironment(): Flow<V3BaseSettingsEnvironment>
    suspend fun refresh(serial: String, discardEngineerDraftsBefore: Long? = null)
    suspend fun respond(offer: V3BaseSettingsOffer, accept: Boolean): Boolean
}

class ObserveBaseSettingsUpdatesUseCaseV3(private val repository: V3BaseSettingsUpdatesRepository) {
    operator fun invoke() = repository.observeEnvironment()
}

class RefreshBaseSettingsUpdatesUseCaseV3(private val repository: V3BaseSettingsUpdatesRepository) {
    suspend operator fun invoke(serial: String, discardEngineerDraftsBefore: Long? = null) {
        if (serial.isNotBlank()) repository.refresh(serial, discardEngineerDraftsBefore)
    }
}

class RespondToBaseSettingsUpdateUseCaseV3(private val repository: V3BaseSettingsUpdatesRepository) {
    suspend operator fun invoke(offer: V3BaseSettingsOffer, accept: Boolean): Boolean {
        require(offer.serial.isNotBlank() && offer.revision > 0)
        return repository.respond(offer, accept)
    }
}
