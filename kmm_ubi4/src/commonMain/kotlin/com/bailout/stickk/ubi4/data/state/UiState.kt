package com.bailout.stickk.ubi4.data.state

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


    init {
        listWidgets = mutableSetOf()
        activeGestureFragmentFilterFlow = MutableStateFlow(1)
        activeSettingsFragmentFilterFlow = MutableStateFlow(4)
        isMobileSettings = false
        updateFlow = MutableSharedFlow()
    }

}