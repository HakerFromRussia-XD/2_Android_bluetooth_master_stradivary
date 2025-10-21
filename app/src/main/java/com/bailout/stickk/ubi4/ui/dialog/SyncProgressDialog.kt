package com.bailout.stickk.ubi4.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SyncProgressDialog(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val owner: LifecycleOwner
) {
    private var dialog: Dialog? = null
    private var job: Job? = null

    val isShowing: Boolean get() = dialog?.isShowing == true

    @SuppressLint("InflateParams")
    fun show() {
        // закрыть прошлый, если был
        dismiss()

        val view = inflater.inflate(R.layout.ubi4_dialog_sync_progress, null)
        val lottie = view.findViewById<LottieAnimationView>(R.id.sync_dialog_lottie_av)
        val bar = view.findViewById<ProgressBar>(R.id.sync_dialog_progress_pb)


        dialog = Dialog(context,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).apply {
            setContentView(view)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setOnDismissListener { cancelCollect() }
            show()
        }

        lottie.playAnimation()

        // подписка на прогресс
        job = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UiState.widgetsLoadingProgressFlow.collect { p ->
                    val total = p.total.coerceAtLeast(1)
                    val current = p.current.coerceAtMost(total)
                    val target = (current * 100 / total).coerceIn(0, 100)

                    val next = maxOf(bar.progress, target) // не откатываемся назад

                    if (Build.VERSION.SDK_INT >= 24) {
                        bar.setProgress(next, true)
                    } else {
                        bar.progress = next
                    }
                }
            }
        }
    }

    fun dismiss() {
        cancelCollect()
        dialog?.dismiss()
        dialog = null
    }

    private fun cancelCollect() {
        job?.cancel()
        job = null
    }
}