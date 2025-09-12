package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object WidgetStateBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    /**
     * Подписка на slidersFlow.
     * @param callback вызывается с каждым новым параметром.
     */
    fun observeSliders(callback: (ParameterRef) -> Unit): Job =
    coroutineScope.launch {
        WidgetState.slidersFlow.collect { callback(it) }
    }

    /**
     * Подписка на plotArrayFlow.
     * @param callback вызывается с каждым новым значением графика.
     */
    fun observePlotArray(callback: (PlotParameterRef) -> Unit): Job =
    coroutineScope.launch {
        WidgetState.plotArrayFlow.collect { callback(it) }
    }
}