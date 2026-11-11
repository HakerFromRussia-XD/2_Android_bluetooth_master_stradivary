package com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles

import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfile

data class V3SettingsProfilesUiState(
    val profiles: List<V3SettingsProfile> = emptyList(),
    val activeProfileId: Int? = null,
    val canCreate: Boolean = false,
    val isEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val operation: V3SettingsProfileOperation? = null,
    val failedOperation: V3SettingsProfileOperation? = null,
    val nameEditor: V3SettingsProfileNameEditorUiState? = null,
    val deleteConfirmation: V3SettingsProfile? = null,
    val showSendAction: Boolean = false,
)

enum class V3SettingsProfileOperation { SELECT, CREATE, RENAME, DELETE, COPY_BASE, SEND }

data class V3SettingsProfileNameEditorUiState(val requestId: Long, val profile: V3SettingsProfile)
