package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.BLEState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * iOS bridge for observing BLE connection state from shared KMM layer.
 */
object BLEStateBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    fun observeState(callback: (Int) -> Unit): Job =
        coroutineScope.launch {
            BLEState.state.collect { callback(it.ordinal) }
        }

    fun currentStateOrdinal(): Int = BLEState.state.value.ordinal
}
