package com.bailout.stickk.ubi4.ui.dialog

import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Log
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Dialog
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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
    private var progress: ProgressBar? = null
    private var title: TextView? = null
    private var message: TextView? = null
    private var action: TextView? = null
    private var actionArea: View? = null
    private var isProgressLayout: Boolean? = null
    private val controller get() = (requireActivity() as MainActivityUBI4).userFirmwareUpdates
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); isCancelable = false }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
    override fun onStart() { super.onStart(); controller?.let { render(it.state()) } }
    fun render(state: UserFirmwareUiState) {
        if (dialog == null) return
        val context = requireContext()
        val needsProgressLayout = state.phase !in listOf("offered", "complete")
        if (isProgressLayout != needsProgressLayout) inflateUbiV3Layout(needsProgressLayout, state.phase)
        val status = when (state.phase) {
            "offered" -> context.getString(SharedRes.strings.user_firmware_offer.resourceId)
            "complete" -> context.getString(SharedRes.strings.user_firmware_complete.resourceId)
            "updating" -> context.getString(SharedRes.strings.user_firmware_updating.resourceId, state.boardNumber, state.boardCount)
            "verifying" -> context.getString(SharedRes.strings.user_firmware_verifying.resourceId)
            "waiting" -> context.getString(SharedRes.strings.user_firmware_waiting.resourceId)
            else -> context.getString(SharedRes.strings.user_firmware_preparing.resourceId)
        }
        title?.text = if (needsProgressLayout) status else context.getString(
            if (state.phase == "complete") SharedRes.strings.user_firmware_complete_title.resourceId
            else SharedRes.strings.user_firmware_title.resourceId
        )
        message?.text = status
        progress?.apply {
            isIndeterminate = state.phase != "updating"
            progress = state.progress
        }
        action?.apply {
            text = context.getString(if (state.phase == "complete") SharedRes.strings.ok.resourceId else SharedRes.strings.user_firmware_install.resourceId)
            setOnClickListener {
                isEnabled = false
                if (state.phase == "complete") controller?.updates?.acknowledge() else controller?.updates?.start()
            }
            isEnabled = true
        }
        actionArea?.setOnClickListener {
            if (state.phase == "complete") controller?.updates?.acknowledge() else controller?.updates?.start()
        }
    }

    /** Uses the shipped UBIv3 XML dialogs directly; no parallel visual design. */
    private fun inflateUbiV3Layout(needsProgressLayout: Boolean, phase: String) {
        val content = LayoutInflater.from(requireContext()).inflate(
            if (needsProgressLayout) R.layout.ubi4_dialog_progressbar_firmware
            else R.layout.ubi4_dialog_confirm_finish_training,
            null
        )
        dialog?.setContentView(content)
        isProgressLayout = needsProgressLayout
        progress = null
        title = null
        message = null
        action = null
        actionArea = null
        if (needsProgressLayout) {
            title = content.findViewById(R.id.dialogTitleTv)
            progress = content.findViewById(R.id.loadingFirmwareProgressBar)
            return
        }

        title = content.findViewById(R.id.ubi4DialogRotationGroupTitleTv)
        message = content.findViewById(R.id.ubi4DialogRotationGroupMessageTv)
        actionArea = content.findViewById(R.id.ubi4CompletedTrainingBtn)
        action = content.findActionLabel(title, message)
        val icon = content.findViewById<ImageView>(R.id.successIv)
        if (phase != "complete") {
            icon.visibility = View.GONE
            (title?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
                topToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = (16 * resources.displayMetrics.density).toInt()
                title?.layoutParams = this
            }
        }
    }

    private fun View.findActionLabel(title: TextView?, message: TextView?): TextView? {
        if (this is TextView && this !== title && this !== message) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) childAt(index).findActionLabel(title, message)?.let { return it }
        return null
    }
}
