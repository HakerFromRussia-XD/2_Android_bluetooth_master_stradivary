package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object WidgetStateBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    /**
     * Подписка на slidersFlow.
     * @param callback вызывается с каждым новым параметром.
     */
    fun observeSliders(callback: (ParameterRef) -> Unit) {
        coroutineScope.launch {
            WidgetState.slidersFlow.collect {
                callback(it)
            }
        }
    }
}