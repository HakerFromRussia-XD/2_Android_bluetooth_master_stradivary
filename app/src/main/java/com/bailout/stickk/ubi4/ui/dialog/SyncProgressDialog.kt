package com.bailout.stickk.ubi4.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
        platformLog("SyncProgressDialog", "show run")
        if (isShowing) return
        dismiss()

        val view = inflater.inflate(R.layout.ubi4_dialog_sync_progress, null)
        val lottie = view.findViewById<LottieAnimationView>(R.id.sync_dialog_lottie_av)
        progressBar = view.findViewById(R.id.sync_dialog_progress_pb)
//        progressBar?.progress = 0
        progressBar?.apply {
            visibility = View.GONE
            isIndeterminate = false
            progress = 0
        }

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
        platformLog("SyncProgressDialog", "observeSyncProgress() start, isShowing=$isShowing")

        watchJob?.cancel()
        watchJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Храним последние значения, чтобы можно было "перерисоваться"
                // при изменении startup/fullInit даже без нового прогресса.
                var p = UiState.widgetsLoadingProgressFlow.value

                fun applyUiState() {
                    val startup = UiState.startupInProgress.value
                    val fullInit = UiState.fullInitInProgress.value

                    val total = p.total.coerceAtLeast(0)
                    val current = p.current.coerceAtLeast(0).coerceAtMost(total)
                    val percent = if (total > 0) (current * 100 / total).coerceIn(0, 100) else 0

                    val shouldShowDialog = startup || fullInit

                    if (shouldShowDialog && !isShowing) {
                        show()
                    }

                    if (shouldShowDialog && !chromeHidden) {
                        setChromeVisible(false)
                        chromeHidden = true
                    }

                    // -------- STARTUP MODE --------
                    if (startup) {
                        progressBar?.visibility = View.GONE
                        progressBar?.isIndeterminate = true
                        progressBar?.progress = 0
                        return
                    }

                    // -------- NOT FULL INIT (WARM READY) --------
                    if (!fullInit) {
                        if (isShowing) dismiss()
                        if (chromeHidden) {
                            setChromeVisible(true)
                            chromeHidden = false
                        }
                        return
                    }

                    // -------- FULL INIT MODE --------
                    // защита от "100% и залип" при возврате из background
                    if (total > 0 && current >= total) {
                        if (isShowing) dismiss()
                        if (chromeHidden) {
                            setChromeVisible(true)
                            chromeHidden = false
                        }
                        return
                    }

                    // диалог при full init держим даже когда total=0 (проценты ещё не готовы)
                    val needShow = (total <= 0) || (current < total)
                    if (needShow && !isShowing) {
                        show()
                        if (!chromeHidden) {
                            setChromeVisible(false)
                            chromeHidden = true
                        }
                    }

                    // ProgressBar показываем только при реальных процентах
                    progressBar?.visibility = if (total > 0) View.VISIBLE else View.GONE

                    if (total <= 0) {
                        progressBar?.isIndeterminate = true
                        progressBar?.progress = 0
                        return
                    }

                    progressBar?.isIndeterminate = false
                    progressBar?.let { bar ->
                        val next = maxOf(bar.progress, percent)
                        if (Build.VERSION.SDK_INT >= 24) bar.setProgress(next, true)
                        else bar.progress = next
                    }
                }

                merge(
                    // прогресс обновился
                    UiState.widgetsLoadingProgressFlow.map {
                        p = it
                        applyUiState()
                        Unit
                    },

                    // startup/fullInit изменились -> тоже надо перерисоваться
                    UiState.startupInProgress.map {
                        applyUiState()
                        Unit
                    },
                    UiState.fullInitInProgress.map {
                        applyUiState()
                        Unit
                    },

                    // "готово" — закрыть диалог и вернуть chrome
                    UiState.widgetsLoadingFlow.map {
                        if (isShowing) dismiss()
                        if (chromeHidden) {
                            setChromeVisible(true)
                            chromeHidden = false
                        }
                        Unit
                    }
                ).collect()
            }
        }
    }
}






