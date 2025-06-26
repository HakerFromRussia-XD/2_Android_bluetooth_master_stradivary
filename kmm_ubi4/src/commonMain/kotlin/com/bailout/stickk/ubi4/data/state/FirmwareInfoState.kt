package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.FirmwareInfoStruct
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FirmwareInfoState {
    private val _firmwareInfoFlow = MutableSharedFlow<FirmwareInfoStruct>(replay = 1)
    val firmwareInfoFlow: SharedFlow<FirmwareInfoStruct> = _firmwareInfoFlow.asSharedFlow()

    fun emitFirmwareInfo(fw: FirmwareInfoStruct) {
        _firmwareInfoFlow.tryEmit(fw)
    }
}