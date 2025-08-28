package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates

object WidgetState {
    var plotArrayFlow: MutableStateFlow<PlotParameterRef> by Delegates.notNull()
    var rotationGroupFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var slidersFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var switcherFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var bindingGroupFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var activeGestureFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var selectGestureModeFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var thresholdFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
    var spinnerFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var stateOpticTrainingFlow: MutableStateFlow<PreferenceKeysUbi4.TrainingModelState> by Delegates.notNull()
    var rotationGroupGestures: ArrayList<Gesture> by Delegates.notNull()
    var bindingGroupGestures: ArrayList<Pair<Int, Int>> by Delegates.notNull()
    var plotArray by Delegates.notNull<ArrayList<Int>>()
    var bmsStatusFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var batteryPercentFlow: MutableSharedFlow<Int> by Delegates.notNull()


    var countBinding by Delegates.notNull<Int>()
    var graphThreadFlag by Delegates.notNull<Boolean>()

    init {
        plotArrayFlow = MutableStateFlow( PlotParameterRef(0, 0, arrayListOf()))
        rotationGroupFlow = MutableSharedFlow(replay = 1)
        plotArray = arrayListOf()
        slidersFlow = MutableSharedFlow(replay = 1)
        thresholdFlow = MutableSharedFlow(replay = 1)
        switcherFlow = MutableSharedFlow(replay = 1)
        bindingGroupFlow = MutableSharedFlow(replay = 1)
        activeGestureFlow = MutableSharedFlow(replay = 1)
        selectGestureModeFlow = MutableSharedFlow(replay = 1)
        spinnerFlow = MutableSharedFlow(replay = 1)
        stateOpticTrainingFlow = MutableStateFlow(PreferenceKeysUbi4.TrainingModelState.BASE)
        rotationGroupGestures = arrayListOf()
        bindingGroupGestures = arrayListOf()
        countBinding = 0
        graphThreadFlag = true
        bmsStatusFlow = MutableSharedFlow()
        batteryPercentFlow = MutableSharedFlow(replay = 1)

    }
}