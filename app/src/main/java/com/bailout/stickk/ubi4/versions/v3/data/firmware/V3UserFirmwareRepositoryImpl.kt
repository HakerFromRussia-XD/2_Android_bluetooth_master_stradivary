package com.bailout.stickk.ubi4.versions.v3.data.firmware

import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareArchive
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareHost
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareUpdates
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareRepository
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwarePolicy
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.zip.ZipFile

/** Android environment for the unchanged shared updater. No Activity or dialog is retained. */
class V3UserFirmwareRepositoryImpl(
    private val preferences: SharedPreferences,
    private val connectivity: ConnectivityManager,
    private val directory: String,
    private val prepareTransfer: suspend () -> Boolean,
    private val setSessionActive: (Boolean) -> Unit,
    private val resumeAfterUpdate: () -> Unit,
    private val isV3: () -> Boolean = { UiState.isInterfaceV3Activated },
    private val createUpdates: (String, UserFirmwareHost) -> UserFirmwareUpdates = ::UserFirmwareUpdates,
) : V3UserFirmwareRepository, UserFirmwareHost {
    private val state = MutableStateFlow(V3UserFirmwareStatus())
    private var updates: UserFirmwareUpdates? = null
    private var scope: CoroutineScope? = null
    private var observation: Job? = null
    private var roleListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun observe(): Flow<V3UserFirmwareStatus> {
        if (updates == null) open()
        return state.asStateFlow()
    }

    private fun open() {
        state.value = V3UserFirmwareStatus()
        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = sessionScope
        val session = createUpdates(directory, this)
        updates = session
        roleListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (updates === session && key == PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED) refreshRole()
        }.also(preferences::registerOnSharedPreferenceChangeListener)
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                sessionScope.launch { if (updates === session) session.environmentChanged() }
            }
        }.also(connectivity::registerDefaultNetworkCallback)
        observation = session.observe { update ->
            if (updates !== session) return@observe
            val completedNow = update.phase == "complete" && state.value.phase != "complete"
            setSessionActive(update.blocksInteraction && update.phase !in listOf("offered", "complete"))
            if (completedNow) resumeAfterUpdate()
            if (update.phase == "unavailable") Log.w("USER_DFU", update.detail)
            state.value = V3UserFirmwareStatus(
                update.phase, update.boardNumber, update.boardCount, update.progress, update.blocksInteraction,
            )
        }
        refreshRole()
    }

    private fun refreshRole() {
        val role = preferences.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, 2)
        val v3 = isV3()
        val enabled = V3UserFirmwarePolicy.isEnabled(v3, role)
        Log.i("USER_DFU", "roleGate enabled=$enabled v3=$v3 role=$role")
        updates?.setUserRole(enabled)
    }

    override fun refreshEnvironment() {
        if (updates == null) return
        refreshRole()
        updates?.environmentChanged()
    }

    override fun startUpdate() { updates?.start() }
    override fun postponeUpdate() { updates?.postpone() }
    override fun acknowledgeCompletion() { updates?.acknowledge() }

    override fun close() {
        val session = updates ?: return
        updates = null
        roleListener?.let(preferences::unregisterOnSharedPreferenceChangeListener)
        roleListener = null
        networkCallback?.let(connectivity::unregisterNetworkCallback)
        networkCallback = null
        observation?.cancel()
        observation = null
        session.close()
        scope?.cancel()
        scope = null
        setSessionActive(false)
    }

    override fun read(path: String, callback: (UserFirmwareArchive?) -> Unit) {
        scope?.launch {
            val archive = withContext(Dispatchers.IO) {
                runCatching {
                    ZipFile(path).use { zip ->
                        val entries = zip.entries().toList().filterNot { it.isDirectory }
                        val descriptor = entries.single { it.name.substringAfterLast('/').equals("FW_ini.ini", true) }
                        val image = entries.single { it.name.endsWith(".bin", true) }
                        UserFirmwareArchive(
                            zip.getInputStream(descriptor).use { it.readBytes().toString(Charsets.UTF_8) },
                            zip.getInputStream(image).use { it.readBytes() },
                        )
                    }
                }.getOrNull()
            }
            callback(archive)
        }
    }

    override fun prepareTransfer(callback: (Boolean) -> Unit) {
        scope?.launch { callback(prepareTransfer()) }
    }
}
