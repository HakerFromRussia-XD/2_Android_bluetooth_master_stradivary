package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import kotlin.properties.Delegates

object ConnectionState {

    var fullInicializeConnectionStruct by Delegates.notNull<FullInicializeConnectionStruct>()

    var connectedDeviceName by Delegates.notNull<String>()
    var connectedDeviceAddress by Delegates.notNull<String>()


}