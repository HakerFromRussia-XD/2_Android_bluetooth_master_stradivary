package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoRepository

enum class V3DeviceInfoWriteResult { SENT, EMPTY, PREPARATION_FAILED, BLOCKED }

class SetDeviceInfoTextUseCaseV3(
    private val repository: V3DeviceInfoRepository,
    private val requireInteractionEnabled: Boolean = true,
) {
    private val editText = EditDeviceInfoTextUseCaseV3()

    operator fun invoke(field: V3DeviceInfoField, text: String): V3DeviceInfoWriteResult {
        if (requireInteractionEnabled && !repository.interactionEnabled.value) return V3DeviceInfoWriteResult.BLOCKED
        val value = editText(field, text).text.trim()
        return send(field, value)
    }

    /** Send a platform-prepared edit without applying different Unicode/whitespace rules again. */
    operator fun invoke(field: V3DeviceInfoField, value: V3DeviceInfoTextEdit): V3DeviceInfoWriteResult {
        if (requireInteractionEnabled && !repository.interactionEnabled.value) return V3DeviceInfoWriteResult.BLOCKED
        return send(field, value.text)
    }

    private fun send(field: V3DeviceInfoField, value: String): V3DeviceInfoWriteResult {
        if (value.isEmpty()) return V3DeviceInfoWriteResult.EMPTY
        return if (repository.sendText(field, value)) V3DeviceInfoWriteResult.SENT else V3DeviceInfoWriteResult.PREPARATION_FAILED
    }
}
