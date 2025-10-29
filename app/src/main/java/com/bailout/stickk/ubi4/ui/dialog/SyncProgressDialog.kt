package com.bailout.stickk.ubi4.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.widget.ProgressBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SyncProgressDialog(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val owner: LifecycleOwner
) {
    private var dialog: Dialog? = null
    private var progressBar: ProgressBar? = null
    private var watchJob: Job? = null
    private var chromeHidden = false

    val isShowing: Boolean get() = dialog?.isShowing == true

    @SuppressLint("InflateParams")
    fun show() {
        if (isShowing) return
        dismiss()

        val view = inflater.inflate(R.layout.ubi4_dialog_sync_progress, null)
        val lottie = view.findViewById<LottieAnimationView>(R.id.sync_dialog_lottie_av)
        progressBar = view.findViewById(R.id.sync_dialog_progress_pb)
        progressBar?.progress = 0

        dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).apply {
            setContentView(view)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setOnDismissListener {  }
            show()
        }
        lottie.playAnimation()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        progressBar = null
    }

    fun observeSyncProgress(setChromeVisible: (Boolean) -> Unit) {
        // мгновенно показать
        show()
        if (!chromeHidden) {
            setChromeVisible(false)
            chromeHidden = true
        }

        watchJob?.cancel()
        watchJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UiState.widgetsLoadingProgressFlow.collect { p ->
                    val total   = p.total.coerceAtLeast(1)
                    val current = p.current.coerceAtMost(total)
                    val percent = (current * 100 / total).coerceIn(0, 100)

                    // обновляем прогресс тут, одной подпиской
                    progressBar?.let { bar ->
                        val target = percent
                        val next = maxOf(bar.progress, target)
                        if (Build.VERSION.SDK_INT >= 24) {
                            bar.setProgress(next, true)
                        } else {
                            bar.progress = next
                        }
                    }

                    if (percent < 100 && !isShowing) {
                        show()
                        if (!chromeHidden) {
                            setChromeVisible(false)
                            chromeHidden = true
                        }
                    }

                    if (percent >= 100 && isShowing) {
                        dismiss()
                        if (chromeHidden) {
                            setChromeVisible(true)
                            chromeHidden = false
                        }
                    }
                }
            }
        }
    }
}





