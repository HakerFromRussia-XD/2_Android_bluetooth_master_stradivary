package com.bailout.stickk.ubi4.data.state

import kotlin.properties.Delegates

object UiState {
    var listWidgets: MutableSet<Any> by Delegates.notNull<MutableSet<Any>>()

    init {
        listWidgets = mutableSetOf()
    }

}