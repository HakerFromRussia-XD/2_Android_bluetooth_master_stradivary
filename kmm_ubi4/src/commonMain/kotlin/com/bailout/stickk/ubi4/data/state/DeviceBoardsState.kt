package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import kotlinx.coroutines.flow.MutableStateFlow

object DeviceBoardsState {
    val flow = MutableStateFlow<Set<BaseSubDeviceInfoStruct>>(emptySet())

}