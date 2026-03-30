package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state

import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

object FlagState {
    var canSendFlag by Delegates.notNull<Boolean>()
    var canSendNextChunkFlagFlow by Delegates.notNull<MutableSharedFlow<Int>>(
    )

    init {
        canSendNextChunkFlagFlow = MutableSharedFlow()
        canSendFlag = false
    }


}