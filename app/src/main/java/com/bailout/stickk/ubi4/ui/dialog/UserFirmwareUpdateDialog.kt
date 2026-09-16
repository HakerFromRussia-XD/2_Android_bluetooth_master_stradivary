package com.bailout.stickk.ubi4.ui.dialog

import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Log
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.WindowManager
import android.app.Dialog
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.firmware.user.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import kotlinx.coroutines.*
import java.io.File
import java.util.zip.ZipFile

class UserFirmwareUpdateController(private val activity: MainActivityUBI4) : UserFirmwareHost {
    private val preferences = activity.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, 0)
    val updates = UserFirmwareUpdates(File(activity.filesDir, "user_firmware").apply { mkdirs() }.path, this)
    private val roleListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED) refreshRole()
    }
    private val connectivity = activity.getSystemService(ConnectivityManager::class.java)
    private val network = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { activity.runOnUiThread { updates.environmentChanged() } }
    }
    private var lastState = UserFirmwareUiState()
    private val observation: Job
    init {
        preferences.registerOnSharedPreferenceChangeListener(roleListener)
        connectivity.registerDefaultNetworkCallback(network)
        observation = updates.observe(::render)
        refreshRole()
    }
    private fun render(state: UserFirmwareUiState) {
        val completedNow = state.phase == "complete" && lastState.phase != "complete"
        lastState = state
        activity.getBLEController().setFirmwareUpdateSessionActive(state.blocksInteraction && state.phase !in listOf("offered", "complete"))
        if (completedNow) activity.getBLEController().resumeAfterUserFirmwareUpdate()
        if (state.blocksInteraction) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val existing = activity.supportFragmentManager.findFragmentByTag(TAG) as? UserFirmwareUpdateDialog
            if (existing != null) existing.render(state)
            else if (!activity.supportFragmentManager.isStateSaved) UserFirmwareUpdateDialog().showNow(activity.supportFragmentManager, TAG)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            (activity.supportFragmentManager.findFragmentByTag(TAG) as? UserFirmwareUpdateDialog)?.dismissAllowingStateLoss()
            if (state.phase == "unavailable") Log.w("USER_DFU", state.detail)
        }
    }
    fun state() = lastState
    private fun refreshRole() {
        val role = preferences.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, 2)
        val enabled = UiState.isInterfaceV3Activated && role == 2
        Log.i("USER_DFU", "roleGate enabled=$enabled v3=${UiState.isInterfaceV3Activated} role=$role")
        updates.setUserRole(enabled)
    }
    fun foreground() { refreshRole(); updates.environmentChanged(); render(lastState) }
    fun close() {
        preferences.unregisterOnSharedPreferenceChangeListener(roleListener)
        connectivity.unregisterNetworkCallback(network)
        observation.cancel()
        updates.close()
        activity.getBLEController().setFirmwareUpdateSessionActive(false)
    }
    override fun read(path: String, callback: (UserFirmwareArchive?) -> Unit) {
        activity.lifecycleScope.launch {
            val archive = withContext(Dispatchers.IO) {
                runCatching {
                    ZipFile(path).use { zip ->
                        val entries = zip.entries().toList().filterNot { it.isDirectory }
                        val descriptor = entries.single { it.name.substringAfterLast('/').equals("FW_ini.ini", true) }
                        val image = entries.single { it.name.endsWith(".bin", true) }
                        UserFirmwareArchive(zip.getInputStream(descriptor).use { it.readBytes().toString(Charsets.UTF_8) },
                            zip.getInputStream(image).use { it.readBytes() })
                    }
                }.getOrNull()
            }
            callback(archive)
        }
    }
    override fun prepareTransfer(callback: (Boolean) -> Unit) {
        activity.lifecycleScope.launch { callback(activity.getBLEController().prepareFirmwareSessionNotifications()) }
    }
    companion object { const val TAG = "user_firmware_update" }
}

class UserFirmwareUpdateDialog : DialogFragment() {
    private var label: TextView? = null
    private var progress: ProgressBar? = null
    private var title: TextView? = null
    private var action: TextView? = null
    private val controller get() = (requireActivity() as MainActivityUBI4).userFirmwareUpdates
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); isCancelable = false }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = LayoutInflater.from(requireContext())
            .inflate(R.layout.ubi4_dialog_user_firmware_update, null)
        title = content.findViewById(R.id.user_firmware_dialog_title_tv)
        label = content.findViewById(R.id.user_firmware_dialog_message_tv)
        progress = content.findViewById(R.id.user_firmware_dialog_progress_pb)
        action = content.findViewById(R.id.user_firmware_dialog_action_tv)
        return Dialog(requireContext()).apply {
            setContentView(content)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
    override fun onStart() { super.onStart(); controller?.let { render(it.state()) } }
    fun render(state: UserFirmwareUiState) {
        if (dialog == null) return
        val context = requireContext()
        title?.text = context.getString(if (state.phase == "complete") SharedRes.strings.user_firmware_complete_title.resourceId else SharedRes.strings.user_firmware_title.resourceId)
        label?.text = when (state.phase) {
            "offered" -> context.getString(SharedRes.strings.user_firmware_offer.resourceId)
            "complete" -> context.getString(SharedRes.strings.user_firmware_complete.resourceId)
            "updating" -> context.getString(SharedRes.strings.user_firmware_updating.resourceId, state.boardNumber, state.boardCount)
            "verifying" -> context.getString(SharedRes.strings.user_firmware_verifying.resourceId)
            "waiting" -> context.getString(SharedRes.strings.user_firmware_waiting.resourceId)
            else -> context.getString(SharedRes.strings.user_firmware_preparing.resourceId)
        }
        progress?.apply {
            visibility = if (state.phase in listOf("offered", "complete")) android.view.View.GONE else android.view.View.VISIBLE
            isIndeterminate = state.phase != "updating"
            progress = state.progress
        }
        action?.apply {
            visibility = if (state.phase in listOf("offered", "complete")) android.view.View.VISIBLE else android.view.View.GONE
            text = context.getString(if (state.phase == "complete") SharedRes.strings.ok.resourceId else SharedRes.strings.user_firmware_install.resourceId)
            setOnClickListener {
                isEnabled = false
                if (state.phase == "complete") controller?.updates?.acknowledge() else controller?.updates?.start()
            }
            isEnabled = true
        }
    }
}
