package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object UiStateBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    /**
     * Observe update events from [UiState.updateFlow].
     * The emitted value corresponds to the display index of the widget that triggered the update.
     * The callback is invoked on the main dispatcher for each emission.
     */
    fun observeUpdates(callback: (Int) -> Unit): Job =
        coroutineScope.launch {
            UiState.updateFlow.collect { callback(it) }
        }

    /**
     * Observe completion events for widgets loading.
     */
    fun observeWidgetsLoadCompletion(callback: () -> Unit): Job =
        coroutineScope.launch {
            UiState.widgetsLoadingFlow.collect { callback() }
        }
}