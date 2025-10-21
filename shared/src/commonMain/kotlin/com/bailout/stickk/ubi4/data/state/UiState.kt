package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates

object UiState {
    var listWidgets: MutableSet<Any> by Delegates.notNull()
    var activeGestureFragmentFilterFlow by Delegates.notNull<MutableStateFlow<Int>>()
    var activeSettingsFragmentFilterFlow by Delegates.notNull<MutableStateFlow<Int>>()
    var isMobileSettings by Delegates.notNull<Boolean>()
    var updateFlow by Delegates.notNull<MutableSharedFlow<Int>>()
    val labelCodesByOffset: MutableMap<Int, MutableMap<Int, Int>> = mutableMapOf()

    var widgetsLoadingFlow by Delegates.notNull<MutableSharedFlow<Unit>>()
    var initializationInfoFlow by Delegates.notNull<MutableSharedFlow<FullInicializeConnectionStruct>>()
    var widgetsLoadingProgressFlow by Delegates.notNull<MutableSharedFlow<WidgetsLoadingProgress>>()
    var widgetsLoadingProgressTotal: Int = 0


    init {
        listWidgets = mutableSetOf()
        activeGestureFragmentFilterFlow = MutableStateFlow(1)
        activeSettingsFragmentFilterFlow = MutableStateFlow(4)
        isMobileSettings = false
        updateFlow = MutableSharedFlow()
        widgetsLoadingFlow = MutableSharedFlow()
        initializationInfoFlow = MutableSharedFlow(replay = 1)
        widgetsLoadingProgressFlow = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    }

}