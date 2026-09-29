package com.bailout.stickk.ubi4.ui.dialog

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Dialog
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bailout.stickk.ubi4.versions.v3.presentation.firmware.V3UserFirmwareAction
import com.bailout.stickk.ubi4.versions.v3.presentation.firmware.V3UserFirmwareUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.firmware.V3UserFirmwareViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4

/** Owns only the modal window; update work continues while the Activity is in the background. */
class UserFirmwareUpdateDialogHost(
    private val activity: MainActivityUBI4,
    viewModel: V3UserFirmwareViewModel,
) {
    private val observation = activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state ->
                val manager = activity.supportFragmentManager
                val existing = manager.findFragmentByTag(TAG) as? UserFirmwareUpdateDialog
                if (state.isVisible) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    if (existing == null && !manager.isStateSaved) UserFirmwareUpdateDialog().showNow(manager, TAG)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    existing?.dismissAllowingStateLoss()
                }
            }
        }
    }

    fun close() { observation.cancel() }

    companion object { const val TAG = "user_firmware_update" }
}

class UserFirmwareUpdateDialog : DialogFragment() {
    private var progress: ProgressBar? = null
    private var title: TextView? = null
    private var message: TextView? = null
    private var action: TextView? = null
    private var actionArea: View? = null
    private var secondaryAction: TextView? = null
    private var secondaryActionArea: View? = null
    private var layout: String? = null
    private val viewModel: V3UserFirmwareViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
    override fun onStart() { super.onStart(); render(viewModel.uiState.value) }
    private fun render(state: V3UserFirmwareUiState) {
        if (dialog == null) return
        val context = requireContext()
        val requestedLayout = when (state.phase) {
            "offered" -> "offer"
            "complete" -> "complete"
            else -> "progress"
        }
        if (layout != requestedLayout) inflateUbiV3Layout(requestedLayout)
        val needsProgressLayout = requestedLayout == "progress"
        val status = when (state.phase) {
            "offered" -> context.getString(SharedRes.strings.user_firmware_offer.resourceId)
            "complete" -> context.getString(SharedRes.strings.user_firmware_complete.resourceId)
            else -> context.getString(SharedRes.strings.user_firmware_updating.resourceId, state.boardNumber, state.boardCount)
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
                if (state.phase == "complete") viewModel.onAction(V3UserFirmwareAction.CompletionAcknowledged) else viewModel.onAction(V3UserFirmwareAction.InstallClicked)
            }
            isEnabled = true
        }
        actionArea?.setOnClickListener {
            if (state.phase == "complete") viewModel.onAction(V3UserFirmwareAction.CompletionAcknowledged) else viewModel.onAction(V3UserFirmwareAction.InstallClicked)
        }
        secondaryAction?.text = context.getString(SharedRes.strings.user_firmware_remind_later.resourceId)
        secondaryActionArea?.setOnClickListener { viewModel.onAction(V3UserFirmwareAction.RemindLaterClicked) }
    }

    /** Uses the shipped UBIv3 XML dialogs directly; no parallel visual design. */
    private fun inflateUbiV3Layout(layoutType: String) {
        val content = LayoutInflater.from(requireContext()).inflate(
            when (layoutType) {
                "offer" -> R.layout.ubi4_dialog_disconnection
                "complete" -> R.layout.ubi4_dialog_confirm_finish_training
                else -> R.layout.ubi4_dialog_progressbar_firmware
            },
            null
        )
        dialog?.setContentView(content)
        layout = layoutType
        progress = null
        title = null
        message = null
        action = null
        actionArea = null
        secondaryAction = null
        secondaryActionArea = null
        if (layoutType == "progress") {
            title = content.findViewById(R.id.dialogTitleTv)
            progress = content.findViewById(R.id.loadingFirmwareProgressBar)
            return
        }

        if (layoutType == "offer") {
            title = content.findViewById(R.id.ubi4DialogConfirmDisconnectionTitleTv)
            message = content.findViewById(R.id.ubi4DialogDisconnectionMassageTv)
            action = content.findViewById(R.id.ok_text)
            actionArea = content.findViewById(R.id.ubi4DialogConfirmDisconnectionBtn)
            secondaryAction = content.findViewById(R.id.cancel_text)
            secondaryActionArea = content.findViewById(R.id.ubi4DialogCancelDisconnectionBtn)
            return
        }

        title = content.findViewById(R.id.ubi4DialogRotationGroupTitleTv)
        message = content.findViewById(R.id.ubi4DialogRotationGroupMessageTv)
        actionArea = content.findViewById(R.id.ubi4CompletedTrainingBtn)
        action = content.findActionLabel(title, message)
    }

    override fun onDestroyView() {
        progress = null
        title = null
        message = null
        action = null
        actionArea = null
        secondaryAction = null
        secondaryActionArea = null
        layout = null
        super.onDestroyView()
    }

    private fun View.findActionLabel(title: TextView?, message: TextView?): TextView? {
        if (this is TextView && this !== title && this !== message) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) getChildAt(index).findActionLabel(title, message)?.let { return it }
        return null
    }
}
