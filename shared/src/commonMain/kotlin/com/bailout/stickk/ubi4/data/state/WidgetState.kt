package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.Volatile
import kotlin.properties.Delegates

data class TelemetryGestureCounters(
    val baseGestureMovementCount: List<Long> = emptyList(),
    val customGestureMovementCount: List<Long> = emptyList(),
    val telemetryVersion: Int? = null,
    val telemetrySubversion: Int? = null,
    val deviceUuid: String = "",
    val receivedAtMillis: Long = 0L
)

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
    var widgetsMergeEventFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
    val activeGestureState = MutableStateFlow<Int?>(null)
    val selectGestureModeState = MutableStateFlow(false)

    var thresholdFlowV3 by Delegates.notNull<MutableSharedFlow<ParameterInfo<Int, Int, Int, Int>>>()
    var sliderFlowV3 by Delegates.notNull<MutableSharedFlow<ParameterInfo<Int, Int, Int, Int>>>()
    var spinnerFlowV3 by Delegates.notNull<MutableSharedFlow<ParameterInfo<Int, Int, Int, Int>>>()
    var switcherFlowV3 by Delegates.notNull<MutableSharedFlow<ParameterInfo<Int, Int, Int, Int>>>()
    var currentGestureFlowV3 by Delegates.notNull<MutableSharedFlow<ParameterInfo<Int, Int, Int, Int>>>()
    var gestureGroupFlowV3 by Delegates.notNull<MutableSharedFlow<ParameterInfo<Int, Int, Int, Int>>>()
    val telemetryGestureCountersFlow = MutableStateFlow(TelemetryGestureCounters())

    @Volatile
    var dbSnapshotAppliedWithCrc: Boolean = false

    @Volatile
    var pausePlotPointsDuringTransition: Boolean = false


    var countBinding by Delegates.notNull<Int>()
    var graphThreadFlag by Delegates.notNull<Boolean>()

    init {
        plotArrayFlow = MutableStateFlow( PlotParameterRef(0, 0, arrayListOf()))
        rotationGroupFlow = MutableSharedFlow(replay = 1)
        plotArray = arrayListOf()
        slidersFlow = MutableSharedFlow()
        thresholdFlow = MutableSharedFlow()
        switcherFlow = MutableSharedFlow()
        bindingGroupFlow = MutableSharedFlow(replay = 1)
        activeGestureFlow = MutableSharedFlow(replay = 1)
        selectGestureModeFlow = MutableSharedFlow()
        spinnerFlow = MutableSharedFlow()
        stateOpticTrainingFlow = MutableStateFlow(PreferenceKeysUbi4.TrainingModelState.BASE)
        rotationGroupGestures = arrayListOf()
        bindingGroupGestures = arrayListOf()
        countBinding = 0
        graphThreadFlag = true
        bmsStatusFlow = MutableSharedFlow()
        batteryPercentFlow = MutableSharedFlow(replay = 1)
        widgetsMergeEventFlow = MutableSharedFlow()

        thresholdFlowV3 = MutableSharedFlow(replay = 1)
        sliderFlowV3 = MutableSharedFlow(replay = 1)
        spinnerFlowV3 = MutableSharedFlow(replay = 1)
        switcherFlowV3 = MutableSharedFlow(replay = 1)
        currentGestureFlowV3 = MutableSharedFlow(replay = 1)
        gestureGroupFlowV3 = MutableSharedFlow(replay = 1)
    }
}
