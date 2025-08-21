package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
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
    var stateOpticTrainingFlow: MutableStateFlow<PreferenceKeysUBI4.TrainingModelState> by Delegates.notNull()
    var rotationGroupGestures: ArrayList<Gesture> by Delegates.notNull()
    var bindingGroupGestures: ArrayList<Pair<Int, Int>> by Delegates.notNull()
    var plotArray by Delegates.notNull<ArrayList<Int>>()
    var bmsStatusFlow: MutableSharedFlow<ParameterRef> by Delegates.notNull()
    var batteryPercentFlow: MutableSharedFlow<Int> by Delegates.notNull()


    var countBinding by Delegates.notNull<Int>()
    var graphThreadFlag by Delegates.notNull<Boolean>()

    init {
        plotArrayFlow = MutableStateFlow(PlotParameterRef(0, 0, arrayListOf()))
        rotationGroupFlow = MutableSharedFlow()
        plotArray = arrayListOf()
        slidersFlow = MutableSharedFlow()
        thresholdFlow = MutableSharedFlow()
        switcherFlow = MutableSharedFlow()
        bindingGroupFlow = MutableSharedFlow()
        activeGestureFlow = MutableSharedFlow()
        selectGestureModeFlow = MutableSharedFlow()
        spinnerFlow = MutableSharedFlow()
        stateOpticTrainingFlow = MutableStateFlow(PreferenceKeysUBI4.TrainingModelState.BASE)
        rotationGroupGestures = arrayListOf()
        bindingGroupGestures = arrayListOf()
        countBinding = 0
        graphThreadFlag = true
        bmsStatusFlow = MutableSharedFlow()
        batteryPercentFlow = MutableSharedFlow(replay = 1)

    }
}