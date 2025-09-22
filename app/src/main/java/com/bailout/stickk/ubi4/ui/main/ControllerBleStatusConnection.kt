package com.bailout.stickk.ubi4.ui.main

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.*
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieOnCompositionLoadedListener
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ble.BluetoothLeService
import kotlin.math.max

class ControllerBleStatusConnection(
    private val context: Context,
    private val indicator: LottieAnimationView
) : DefaultLifecycleObserver {

    // ---- State ----
    private enum class UiState { Connected, Reconnecting, Disconnected }
    @Volatile private var isRegistered = false
    @Volatile private var isPlaying = false
    @Volatile private var isLoopingNow = false
    @Volatile private var pendingState: UiState? = null
    private var compositionListener: LottieOnCompositionLoadedListener? = null

    // ---- Mapping ----
    private fun animRes(state: UiState) = when (state) {
        UiState.Connected     -> R.raw.loader_spinner
        UiState.Reconnecting  -> R.raw.loader_spinner
        UiState.Disconnected  -> R.raw.loader_spinner
    }
    private fun isLooping(state: UiState) = state == UiState.Reconnecting || state == UiState.Disconnected

    private fun play(state: UiState, minSingleMs: Long? = null, loopOverride: Boolean? = null) {
        indicator.post {
            // очистка listeners
            compositionListener?.let { indicator.removeLottieOnCompositionLoadedListener(it) }
            compositionListener = null
            indicator.removeAllAnimatorListeners()
            indicator.addAnimatorListener(animListener)

            isPlaying = true
            isLoopingNow = loopOverride ?: isLooping(state)

            indicator.progress = 0f
            indicator.speed = 1f
            indicator.setAnimation(animRes(state))

            // запуск с учётом кеша композиции
            val comp = indicator.composition
            val start: (Float) -> Unit = { originalMs ->
                if (minSingleMs != null) {
                    // Не ускоряем: только растягиваем, если оригинал короче
                    val target = max(originalMs, minSingleMs.toFloat())
                    val speed = if (originalMs > 0f) originalMs / target else 1f
                    indicator.repeatCount = 0
                    indicator.repeatMode = LottieDrawable.RESTART
                    indicator.speed = speed
                    indicator.playAnimation()
                } else {
                    indicator.repeatCount = if (isLoopingNow) LottieDrawable.INFINITE else 0
                    indicator.repeatMode = LottieDrawable.RESTART
                    indicator.playAnimation()
                }
            }
            if (comp != null) {
                start(comp.duration * 1000f)
            } else {
                compositionListener = LottieOnCompositionLoadedListener { loaded ->
                    start(loaded.duration * 1000f)
                }.also { indicator.addLottieOnCompositionLoadedListener(it) }
            }
        }
    }

    // ---- Безобрывный переход (единственная точка смены) ----
    private fun requestState(state: UiState, minSingleMs: Long? = null, loopOverride: Boolean? = null) {
        if (isPlaying) {
            pendingState = state
            if (isLoopingNow) indicator.post { indicator.repeatCount = 0 } // доиграть текущую итерацию
            return
        }
        play(state, minSingleMs, loopOverride)
    }

    // ---- Animator listener ----
    private val animListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) { /* isPlaying уже true */ }
        override fun onAnimationRepeat(animation: Animator) {}
        override fun onAnimationCancel(animation: Animator) { onAnimationEnd(animation) }
        override fun onAnimationEnd(animation: Animator) {
            isPlaying = false
            isLoopingNow = false
            pendingState?.let { next ->
                pendingState = null
                play(next)
            }
        }
    }

    // ---- BLE events (чёткие правила в одном месте) ----
    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    // Всегда: Disconnected (>= 1000мс, одиночная) -> Reconnecting (луп)
                    // Ставим дальше Reconnecting, но запустится только после окончания Disconnected
                    pendingState = UiState.Reconnecting
                    requestState(UiState.Disconnected, minSingleMs = 1000L, loopOverride = false)
                }
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    // Сервисы ещё не открыты — показываем реконнект (луп), без обрыва текущего кадра
                    requestState(UiState.Reconnecting, loopOverride = true)
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Доигрываем текущую итерацию (если луп), затем Connected (одиночный)
                    requestState(UiState.Connected, loopOverride = false)
                }
            }
        }
    }

    fun startReconnecting() = requestState(UiState.Reconnecting, loopOverride = true)
    fun stopReconnecting()  = requestState(UiState.Disconnected,  loopOverride = true)

    // ---- Lifecycle ----
    override fun onStart(owner: LifecycleOwner) {
        register()
        indicator.removeAllAnimatorListeners()
        indicator.addAnimatorListener(animListener)
        indicator.repeatMode = LottieDrawable.RESTART
        // Стартуем с Reconnecting (луп)
        play(UiState.Reconnecting, loopOverride = true)
    }

    override fun onStop(owner: LifecycleOwner) {
        unregister()
        compositionListener?.let { indicator.removeLottieOnCompositionLoadedListener(it) }
        compositionListener = null
        indicator.removeAllAnimatorListeners()
    }

    // ---- Broadcast registration ----
    private fun register() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            LocalBroadcastManager.getInstance(context).registerReceiver(bleReceiver, filter)
        } else {
            context.registerReceiver(bleReceiver, filter)
        }
        isRegistered = true
    }

    private fun unregister() {
        if (!isRegistered) return
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(bleReceiver)
            } else {
                context.unregisterReceiver(bleReceiver)
            }
        } catch (_: IllegalArgumentException) {}
        isRegistered = false
    }

    object UiBridges {
        @SuppressLint("StaticFieldLeak")
        @Volatile var bleStatusController: ControllerBleStatusConnection? = null
    }
}