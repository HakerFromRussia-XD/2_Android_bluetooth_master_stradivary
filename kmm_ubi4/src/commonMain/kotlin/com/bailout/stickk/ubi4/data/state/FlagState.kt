package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.properties.Delegates

object FlagState {
    var canSendFlag by Delegates.notNull<Boolean>()
    var canSendNextChunkFlagFlow by Delegates.notNull<MutableSharedFlow<Int>>()

    init {
        canSendNextChunkFlagFlow = MutableSharedFlow()
        canSendFlag = false
    }
}