package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

import kotlinx.coroutines.flow.Flow

data class V3BaseSettingsOffer(val serial: String, val revision: Long)

data class V3BaseSettingsEnvironment(
    val serial: String = "",
    val isUser: Boolean = false,
    val canApply: Boolean = false,
    val revision: Long = 0,
    val handledRevision: Long = 0,
    val online: Boolean = true,
) {
    val offer: V3BaseSettingsOffer? get() = if (isUser && serial.isNotBlank() && revision > handledRevision)
        V3BaseSettingsOffer(serial, revision) else null
}

interface V3BaseSettingsUpdatesRepository {
    fun observeEnvironment(): Flow<V3BaseSettingsEnvironment>
    suspend fun refresh(serial: String)
    suspend fun respond(offer: V3BaseSettingsOffer, accept: Boolean): Boolean
}

class ObserveBaseSettingsUpdatesUseCaseV3(private val repository: V3BaseSettingsUpdatesRepository) {
    operator fun invoke() = repository.observeEnvironment()
}

class RefreshBaseSettingsUpdatesUseCaseV3(private val repository: V3BaseSettingsUpdatesRepository) {
    suspend operator fun invoke(serial: String) { if (serial.isNotBlank()) repository.refresh(serial) }
}

class RespondToBaseSettingsUpdateUseCaseV3(private val repository: V3BaseSettingsUpdatesRepository) {
    suspend operator fun invoke(offer: V3BaseSettingsOffer, accept: Boolean): Boolean {
        require(offer.serial.isNotBlank() && offer.revision > 0)
        return repository.respond(offer, accept)
    }
}
