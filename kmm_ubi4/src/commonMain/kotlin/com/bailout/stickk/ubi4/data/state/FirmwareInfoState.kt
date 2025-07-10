package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.FirmwareInfoStruct
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FirmwareInfoState {
    private val _firmwareInfoFlow = MutableSharedFlow<FirmwareInfoStruct>(replay = 1)
    val firmwareInfoFlow: SharedFlow<FirmwareInfoStruct> = _firmwareInfoFlow.asSharedFlow()
    val runProgramTypeFlow = MutableSharedFlow<Pair<Int, PreferenceKeysUBI4.RunProgramType>>(replay = 0, extraBufferCapacity = 1)
    val bootloaderStatusFlow = MutableSharedFlow<PreferenceKeysUBI4.BootloaderStatus>(extraBufferCapacity = 4)
    val bootloaderInfoFlow = MutableSharedFlow<List<Int>>(replay = 1)
    val startSystemUpdateFlow = MutableSharedFlow<Int>(replay = 1)
    val checkNewFwFlow = MutableSharedFlow<Int>(replay = 1)


    fun emitFirmwareInfo(fw: FirmwareInfoStruct) {
        _firmwareInfoFlow.tryEmit(fw)
    }
}