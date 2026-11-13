package com.bailout.stickk.ubi4.versions.v3.presentation.sync

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R

/** Renders the synchronization screen; its states are owned by V3SyncViewModel. */
class V3SyncFragment : Fragment(R.layout.ubi4_dialog_sync_progress) {
    private var state = V3SyncUiState()
    private var backCallback: OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backCallback = object : OnBackPressedCallback(state.isShowing) {
            override fun handleOnBackPressed() = Unit
        }.also { requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it) }
        render(state)
    }

    fun render(value: V3SyncUiState) {
        state = value
        backCallback?.isEnabled = value.isShowing
        val content = view ?: return
        content.visibility = if (value.isShowing) View.VISIBLE else View.GONE
        val confirmation = value.mode == V3SyncMode.PhoneConfirmation
        content.findViewById<TextView>(R.id.sync_dialog_title_tv).visibility =
            if (confirmation) View.VISIBLE else View.GONE
        content.findViewById<TextView>(R.id.sync_dialog_message_tv).setText(
            if (confirmation) R.string.v3_phone_sync_instruction else R.string.sync_data_progress,
        )
        content.findViewById<ProgressBar>(R.id.sync_dialog_progress_pb).apply {
            visibility = if (value.isProgressVisible && !value.isIndeterminate) View.VISIBLE else View.GONE
            isIndeterminate = false
            if (Build.VERSION.SDK_INT >= 24) setProgress(value.progress, true) else progress = value.progress
        }
        content.findViewById<LottieAnimationView>(R.id.sync_dialog_lottie_av).apply {
            // Both modes intentionally share the existing animation until separate assets are supplied.
            if (value.isShowing && !isAnimating) playAnimation()
            else if (!value.isShowing) pauseAnimation()
        }
    }

    override fun onStart() { super.onStart(); render(state) }
    override fun onStop() {
        view?.findViewById<LottieAnimationView>(R.id.sync_dialog_lottie_av)?.pauseAnimation()
        super.onStop()
    }
    override fun onDestroyView() {
        backCallback = null
        super.onDestroyView()
    }

    companion object { const val TAG = "v3_sync_screen" }
}
