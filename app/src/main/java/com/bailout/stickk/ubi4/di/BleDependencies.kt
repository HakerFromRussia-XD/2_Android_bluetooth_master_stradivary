package com.bailout.stickk.ubi4.di

import com.bailout.stickk.ubi4.ble.BleCommandWriter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.data.transport.V3CommandTransport

internal object BleDependencies {
    val v3CommandTransport = V3CommandTransport { MainActivityUBI4.main }

    fun createCommandWriter(dispatch: (ByteArray?, String, String) -> Boolean) =
        BleCommandWriter(dispatch)
}
