package com.bailout.stickk.ubi4.utility

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.state.WidgetState.selectGestureModeFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BorderAnimator(
    private val view: View,
    private val strokeWidth: Int = 3,
    private val activeColor: Int = ContextCompat.getColor(main, R.color.ubi4_active),
    private val animationDuration: Long = 1000L
) {
    // Неактивный цвет границы (например, цвет по умолчанию)
    private val inactiveColor = ContextCompat.getColor(main, R.color.ubi4_gray_border)
    private var blinkingAnimator: ValueAnimator? = null
    private var fadeOutAnimator: ValueAnimator? = null
    private var collectJob: Job? = null
    private var _selectGestureMode = 0

    fun checkStateSelectGestureMode() {
        collectJob?.cancel()
        collectJob = main.lifecycleScope.launch {
            selectGestureModeFlow.collect { selectGestureModeParameterRef ->
                val parameter = ParameterProvider.getParameter(
                    selectGestureModeParameterRef.addressDevice,
                    selectGestureModeParameterRef.parameterID
                )
                Log.e("BorderAnimator", "Parameter =${parameter.data}")

                val selectGestureModeHex = parameter.data.takeLast(2)
                val selectGestureMode = selectGestureModeHex.toIntOrNull(16)
                _selectGestureMode = selectGestureMode ?: 0
                if (selectGestureMode == 1) startGestureSelectionAnimation()
                if (selectGestureMode == 0) cancelAnimation()
            }
        }
    }
    fun startGestureSelectionAnimation() {
        animateGesturePulseCycle()
        object : CountDownTimer(animationDuration * 2, animationDuration * 2) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if(_selectGestureMode == 1) { startGestureSelectionAnimation()
                    Log.e("BorderAnimator", "_selectGestureMode = $_selectGestureMode")
                }
                else{
                    Log.e("BorderAnimator", "_selectGestureMode = $_selectGestureMode")
                }
            }
        }.start()
    }
    private fun animateGesturePulseCycle() {
        val drawable = view.background as? GradientDrawable ?: run {
            return
        }
        blinkingAnimator = ValueAnimator.ofArgb(activeColor, inactiveColor).apply {
            duration = animationDuration
            repeatMode = ValueAnimator.REVERSE
            repeatCount = 1
            addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                drawable.setStroke(strokeWidth, animatedColor)
            }
            start()
        }

        main.lifecycleScope.launch { delay(animationDuration) }
        fadeOutAnimator = ValueAnimator.ofArgb(inactiveColor, activeColor).apply {
            duration = animationDuration
            repeatMode = ValueAnimator.REVERSE
            repeatCount = 1
            addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                drawable.setStroke(strokeWidth, animatedColor)
            }
            start()
        }

    }
    private fun cancelAnimation(){
        val drawable = view.background as? GradientDrawable ?: run {
            return
        }
        blinkingAnimator?.cancel()
        fadeOutAnimator?.cancel()
        drawable.setStroke(1,inactiveColor)
    }
    fun destroyCoroutines() {
        collectJob?.cancel()
    }
}
