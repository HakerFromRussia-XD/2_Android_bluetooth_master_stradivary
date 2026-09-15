package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

enum class V3DeviceInfoField { DEVICE_NAME, SERIAL_NUMBER }

interface V3DeviceInfoRepository {
    val interactionEnabled: StateFlow<Boolean>
    fun getTextForInput(field: V3DeviceInfoField): String?
    /** Returns whether the command was prepared and queued, not acknowledged by the device. */
    fun sendText(field: V3DeviceInfoField, text: String): Boolean
}
