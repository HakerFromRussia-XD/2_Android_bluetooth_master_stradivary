package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.Volatile
import kotlin.properties.Delegates

object UiState {
    var listWidgets: MutableSet<Any> by Delegates.notNull()

    var activeGestureFragmentFilterFlow by Delegates.notNull<MutableStateFlow<Int>>()
    var activeSettingsFragmentFilterFlow by Delegates.notNull<MutableStateFlow<Int>>()
    var isMobileSettings by Delegates.notNull<Boolean>()
    var updateFlow by Delegates.notNull<MutableSharedFlow<Int>>()
    var widgetsLoadingFlow by Delegates.notNull<MutableSharedFlow<Unit>>()
    var initializationInfoFlow by Delegates.notNull<MutableSharedFlow<FullInicializeConnectionStruct>>()
    val fullInitInProgress = MutableStateFlow(false)
    val startupInProgress = MutableStateFlow(false)
    val v3WidgetsInteractionEnabled = MutableStateFlow(false)
    val widgetsLoadingProgressFlow = MutableStateFlow(
        WidgetsLoadingProgress(current = 0, total = 0)
    )
    var isInterfaceV3Activated: Boolean = false


    val labelCodesByOffset: MutableMap<Int, MutableMap<Int, Int>> = mutableMapOf()
    private val requestedWidgetParameters: MutableSet<Long> = mutableSetOf()
    @Volatile
    var animateState: Boolean = false



    init {
        listWidgets = mutableSetOf()
        activeGestureFragmentFilterFlow = MutableStateFlow(1)
        activeSettingsFragmentFilterFlow = MutableStateFlow(4)
        isMobileSettings = false
        updateFlow = MutableSharedFlow(replay = 1, extraBufferCapacity = 64)
        widgetsLoadingFlow = MutableSharedFlow()
        initializationInfoFlow = MutableSharedFlow(replay = 1)
    }
    fun resetWidgetRequests() {
        requestedWidgetParameters.clear()
    }



}
