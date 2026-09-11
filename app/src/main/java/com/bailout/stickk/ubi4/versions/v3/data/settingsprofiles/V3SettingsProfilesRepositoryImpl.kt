package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepository
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfile
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfiles
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfilesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class V3SettingsProfilesRepositoryImpl(
    private val applyProfileValues: (List<SettingsProfileApplyValue>) -> Unit,
) : V3SettingsProfilesRepository {
    override val updates = V3SettingsProfilesUpdates.updates
    override fun currentSerial() = SettingsProfileManager.serial()

    override suspend fun selectProfile(serial: String, profileId: Int) = updateActiveProfile(serial) { repository, _ ->
        val (state, values) = repository.switchToProfile(serial, profileId)
        check(state.activeProfileId == profileId) { "Settings profile was not selected" }
        values
    }

    override suspend fun createProfile(serial: String) = updateActiveProfile(serial) { repository, previous ->
        if (!previous.canCreate) return@updateActiveProfile null
        val (state, values) = repository.createProfileFromActive(serial)
        // The shared repository can return the existing profile if the limit was reached.
        if (previous.profiles.any { it.profileId == state.activeProfileId }) null else values
    }

    override suspend fun renameProfile(serial: String, profileId: Int, name: String) = withContext(Dispatchers.Main.immediate) {
        val address = WidgetRepoProvider.mac()
        checkDevice(serial, address)
        val repository = checkNotNull(SettingsProfileRepositoryProvider.getOrNull()) { "Settings profiles are unavailable" }
        try {
            val renamed = repository.renameProfile(serial, profileId, name)
            checkDevice(serial, address)
            checkNotNull(renamed) { "Settings profile was not renamed" }
            Unit
        } finally {
            V3SettingsProfilesUpdates.notifyChanged()
        }
    }

    private suspend fun checkDevice(serial: String, address: String) {
        currentCoroutineContext().ensureActive()
        if (serial != currentSerial() || address != WidgetRepoProvider.mac()) {
            throw CancellationException("Settings profile device changed")
        }
        check(UiState.isInterfaceV3Activated && UiState.v3WidgetsInteractionEnabled.value) { "Device settings are unavailable" }
    }

    private suspend fun updateActiveProfile(
        serial: String,
        changeProfile: suspend (SettingsProfileRepository, V3SettingsProfiles) -> List<SettingsProfileApplyValue>?,
    ) = withContext(Dispatchers.Main.immediate) {
        val address = WidgetRepoProvider.mac()
        checkDevice(serial, address)
        SettingsProfileManager.awaitPendingWrites(serial)
        checkDevice(serial, address)
        val repository = checkNotNull(SettingsProfileRepositoryProvider.getOrNull()) { "Settings profiles are unavailable" }
        try {
            val previous = getProfiles(serial)
            checkDevice(serial, address)
            val values = changeProfile(repository, previous)
            checkDevice(serial, address)
            if (values == null) return@withContext
            val profiles = getProfiles(serial).normalized()
            checkDevice(serial, address)
            cacheSelection(serial, profiles)
            applyProfileValues(values)
        } finally {
            // The DB may have changed even if cancellation or applying values prevented completion.
            V3SettingsProfilesUpdates.notifyChanged()
        }
    }

    override suspend fun getProfiles(serial: String): V3SettingsProfiles {
        // Pin this read to its serial, even if the current device changes during the Room query.
        val profiles = SettingsProfileRepositoryProvider.getOrNull()?.getProfiles(serial).orEmpty()
        return V3SettingsProfiles(
            profiles.map { V3SettingsProfile(it.profileId, it.customName) },
            profiles.firstOrNull { it.isActive }?.profileId ?: profiles.firstOrNull()?.profileId ?: 1,
        )
    }

    override fun cacheSelection(serial: String, profiles: V3SettingsProfiles) {
        if (serial != currentSerial()) return
        val index = profiles.profiles.indexOfFirst { it.profileId == profiles.activeProfileId }
        if (index < 0) return
        val meta = requireNotNull(ParameterInfoRegistry.getMeta(P_KEY_SETTINGS_PROFILE))
        val value = ParameterTypedValueV3.Spinner(SpinnerV3(index))
        ParameterStoreV3.put(meta.parameterInfo, value)
        ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, value)?.let {
            ParameterProvider.getParameterV3(meta.parameterInfo).data = it
        }
    }
}
