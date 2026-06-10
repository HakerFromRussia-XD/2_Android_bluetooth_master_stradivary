package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
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
     * Подписка на switcherFlow.
     * @param callback вызывается с каждым новым параметром.
     */
    fun observeSwitchers(callback: (ParameterRef) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.switcherFlow.collect { callback(it) }
        }


    /**
     * Подписка на plotArrayFlow.
     * @param callback вызывается с каждым новым значением графика.
     */
    fun observePlotArray(callback: (PlotParameterRef) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.plotArrayFlow.collect { callback(it) }
        }

    /**
     * Подписка на thresholdFlow.
     * @param callback вызывается с каждым новым значением графика.
     */
    fun observeThresholdFlow (callback: (ParameterRef) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.thresholdFlow.collect { callback(it) }
        }
    /**
     * Подписка на уровень батареи (0..100).
     * @param callback вызывается с каждым новым процентом заряда.
     */
    fun observeBatteryPercent(callback: (Int) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.batteryPercentFlow.collect { callback(it) }
        }


    /**
     * Подписка на rotationGroupFlow.
     * @param callback вызывается с каждым новым значением группы ротации.
     */
    fun observeRotationGroup(callback: (ParameterRef) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.rotationGroupFlow.collect { callback(it) }
        }


    /**
     * Подписка на activeGestureFlow.
     * @param callback вызывается с каждым обновлением активного жеста.
     */
    fun observeActiveGesture(callback: (ParameterRef) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.activeGestureFlow.collect { callback(it) }
        }

    /**
     * Подписка на bindingGroupGestures.
     * @param callback вызывается с каждым обновлением активного жеста.
     */
    fun observeBindingGroup(callback: (ParameterRef) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.bindingGroupFlow.collect { callback(it) }
        }

    /**
     * Подписка на счетчики использования жестов из телеметрии.
     * @param callback вызывается с каждым обновлением счетчиков.
     */
    fun observeTelemetryGestureCounters(callback: (TelemetryGestureCounters) -> Unit): Job =
        coroutineScope.launch {
            WidgetState.telemetryGestureCountersFlow.collect { callback(it) }
        }
}
