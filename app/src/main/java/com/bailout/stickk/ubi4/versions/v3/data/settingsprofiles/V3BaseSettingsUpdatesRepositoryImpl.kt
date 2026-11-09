package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.network.SettingsProfileUploadWorkScheduler
import com.bailout.stickk.ubi4.data.network.Ubi4SettingsProfileSender
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import java.util.Locale

class V3BaseSettingsUpdatesRepositoryImpl(
    context: Context,
    private val preferences: SharedPreferences,
    private val applyValues: (List<SettingsProfileApplyValue>) -> Unit,
    private val sender: Ubi4SettingsProfileSender = Ubi4SettingsProfileSender(),
    private val scheduleUpload: (String, String) -> Unit = { serial, lang ->
        SettingsProfileUploadWorkScheduler.enqueueEngineerBaseUpload(context.applicationContext, serial, lang)
    },
    private val scheduleUserUpload: (String, String) -> Unit = { serial, lang ->
        SettingsProfileUploadWorkScheduler.enqueueUserProfilesUpload(context.applicationContext, serial, lang)
    },
) : V3BaseSettingsUpdatesRepository {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private fun isOnline(): Boolean = connectivity?.let {
        it.getNetworkCapabilities(it.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    } ?: false
    private val scheduledRevisions = mutableMapOf<String, Long>()
    private val selection = V3SettingsProfilesRepositoryImpl(applyValues)

    private fun role() = preferences.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, V3DeviceRole.USER.wireValue)
    private fun handledKey(serial: String) = "base_settings_handled_revision:$serial"
    private fun matchesConnectedDevice(serial: String): Boolean {
        val value = ParameterStoreV3.get(PreferenceKeysUbi4.ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER))
            as? ParameterTypedValueV3.Text ?: return false
        return value.value.replace("\u0000", "").trim() == serial && SettingsProfileManager.serial() == serial
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeEnvironment(): Flow<V3BaseSettingsEnvironment> {
        val preferenceChanges = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED || key?.startsWith("base_settings_handled_revision:") == true) trySend(Unit)
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(Unit)
            awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        val networkChanges = connectivity?.let { manager -> callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { trySend(Unit) }
                override fun onLost(network: Network) { trySend(Unit) }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { trySend(Unit) }
            }
            manager.registerDefaultNetworkCallback(callback)
            trySend(Unit)
            awaitClose { manager.unregisterNetworkCallback(callback) }
        } } ?: flowOf(Unit)
        return merge(
            preferenceChanges, networkChanges, V3SettingsProfilesUpdates.updates,
            ParameterStoreV3.values.map { SettingsProfileManager.serial() to WidgetRepoProvider.mac() }.distinctUntilChanged().map { Unit },
            UiState.v3WidgetsInteractionEnabled.map { Unit }, UiState.fullInitInProgress.map { Unit }, UiState.startupInProgress.map { Unit },
        ).map {
            val serial = SettingsProfileManager.serial()
            val revision = SettingsProfileRepositoryProvider.getOrNull()?.getBaseSettingsRevision(serial) ?: 0
            val pendingRevision = SettingsProfileRepositoryProvider.getOrNull()?.getPendingEngineerRevision(serial) ?: 0
            val selectedRole = role()
            if (UiState.isInterfaceV3Activated && pendingRevision > 0 && scheduledRevisions[serial] != pendingRevision) {
                scheduleUpload(serial, Locale.getDefault().language)
                scheduledRevisions[serial] = pendingRevision
            }
            V3BaseSettingsEnvironment(
                serial = serial,
                isUser = UiState.isInterfaceV3Activated && selectedRole == V3DeviceRole.USER.wireValue && matchesConnectedDevice(serial),
                canApply = UiState.v3WidgetsInteractionEnabled.value && !UiState.fullInitInProgress.value && !UiState.startupInProgress.value,
                revision = revision,
                handledRevision = preferences.getLong(handledKey(serial), 0),
                online = isOnline(),
            )
        }.distinctUntilChanged()
    }

    override suspend fun refresh(serial: String) {
        if (!isCurrentUser(serial)) return
        val address = WidgetRepoProvider.mac()
        SettingsProfileManager.awaitPendingWrites(serial)
        if (!isCurrentUser(serial) || WidgetRepoProvider.mac() != address) return
        try {
            sender.downloadProfileSettingsForSerial(serial, Locale.getDefault().language, preserveCurrentSettings = true)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            platformLog("BaseSettingsUpdate", "refresh failed serial=$serial error=${error.message}")
            throw error
        }
        // Refresh only imports data. Applying the baseline belongs exclusively to Accept.
        V3SettingsProfilesUpdates.notifyChanged()
        if (isCurrentUser(serial) && WidgetRepoProvider.mac() == address &&
            SettingsProfileRepositoryProvider.getOrNull()?.getProfiles(serial)?.any { it.profileId > 0 } == true)
            scheduleUserUpload(serial, Locale.getDefault().language)
    }

    private fun isCurrentUser(serial: String) = UiState.isInterfaceV3Activated &&
        role() == V3DeviceRole.USER.wireValue && matchesConnectedDevice(serial)

    private fun canApply(serial: String, address: String) = isCurrentUser(serial) && WidgetRepoProvider.mac() == address &&
        UiState.v3WidgetsInteractionEnabled.value && !UiState.fullInitInProgress.value && !UiState.startupInProgress.value

    override suspend fun respond(offer: V3BaseSettingsOffer, accept: Boolean): Boolean {
        if (!isCurrentUser(offer.serial)) return false
        val address = WidgetRepoProvider.mac()
        val repository = SettingsProfileRepositoryProvider.getOrNull() ?: return false
        if (accept) {
            if (!canApply(offer.serial, address)) return false
            SettingsProfileManager.awaitPendingWrites(offer.serial)
            if (!canApply(offer.serial, address)) return false
            val values = repository.acceptBaseSettings(offer.serial, offer.revision) ?: return false
            if (!canApply(offer.serial, address)) return false
            applyValues(values)
            val profiles = selection.getProfiles(offer.serial).normalized()
            if (!isCurrentUser(offer.serial) || WidgetRepoProvider.mac() != address) return false
            selection.cacheSelection(offer.serial, profiles)
        } else if (repository.getBaseSettingsRevision(offer.serial) != offer.revision) return false
        if (!isCurrentUser(offer.serial) || WidgetRepoProvider.mac() != address) return false
        val saved = withContext(Dispatchers.IO) {
            preferences.edit().putLong(handledKey(offer.serial), offer.revision).commit()
        }
        if (!saved) return false
        platformLog("BaseSettingsUpdate", "${if (accept) "accepted" else "skipped"} serial=${offer.serial} revision=${offer.revision}")
        V3SettingsProfilesUpdates.notifyChanged()
        return true
    }
}
