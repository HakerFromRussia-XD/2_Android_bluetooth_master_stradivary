package com.bailout.stickk.ubi4.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.presentation.firmware.V3UserFirmwareViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BaseSettingsUpdateDialogHost(
    activity: MainActivityUBI4,
    viewModel: V3BaseSettingsUpdatesViewModel,
    firmware: V3UserFirmwareViewModel,
) {
    private val observation = activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            combine(viewModel.uiState, firmware.uiState) { settings, update -> settings.offer != null && !update.isVisible }
                .collect { visible ->
                    val manager = activity.supportFragmentManager
                    val existing = manager.findFragmentByTag(TAG) as? BaseSettingsUpdateDialog
                    if (visible) {
                        if (existing == null && !manager.isStateSaved) BaseSettingsUpdateDialog().showNow(manager, TAG)
                    } else existing?.dismissAllowingStateLoss()
                }
        }
    }
    fun close() { observation.cancel() }
    companion object { const val TAG = "engineer_base_settings_update" }
}

class BaseSettingsUpdateDialog : DialogFragment() {
    private val viewModel: V3BaseSettingsUpdatesViewModel by activityViewModels()
    private var content: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(requireContext()).apply {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = LayoutInflater.from(context).inflate(R.layout.ubi4_dialog_disconnection, null)
        content = view
        setContentView(view)
    }

    override fun onStart() { super.onStart(); render(viewModel.uiState.value) }

    private fun render(state: V3BaseSettingsUpdatesUiState) {
        val view = content ?: return
        val offer = state.offer ?: return
        view.findViewById<TextView>(R.id.ubi4DialogConfirmDisconnectionTitleTv).setText(SharedRes.strings.base_settings_update_title.resourceId)
        view.findViewById<TextView>(R.id.ubi4DialogDisconnectionMassageTv).setText(
            if (state.failed) SharedRes.strings.base_settings_update_failed.resourceId else SharedRes.strings.base_settings_update_message.resourceId,
        )
        view.findViewById<TextView>(R.id.ok_text).setText(SharedRes.strings.base_settings_update_accept.resourceId)
        view.findViewById<TextView>(R.id.cancel_text).setText(SharedRes.strings.base_settings_update_skip.resourceId)
        listOf(R.id.ok_text, R.id.ubi4DialogConfirmDisconnectionBtn).forEach { id ->
            view.findViewById<View>(id).apply {
                isEnabled = !state.isApplying
                setOnClickListener { viewModel.onAction(V3BaseSettingsUpdatesAction.Respond(offer, true)) }
            }
        }
        listOf(R.id.cancel_text, R.id.ubi4DialogCancelDisconnectionBtn).forEach { id ->
            view.findViewById<View>(id).apply {
                isEnabled = !state.isApplying
                setOnClickListener { viewModel.onAction(V3BaseSettingsUpdatesAction.Respond(offer, false)) }
            }
        }
    }

    override fun onDestroyView() { content = null; super.onDestroyView() }
}
