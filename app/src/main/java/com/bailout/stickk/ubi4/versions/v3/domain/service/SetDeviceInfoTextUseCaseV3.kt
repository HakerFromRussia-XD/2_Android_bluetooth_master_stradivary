package com.bailout.stickk.ubi4.versions.v3.domain.service

enum class V3DeviceInfoWriteResult { SENT, EMPTY, PREPARATION_FAILED, BLOCKED }

class SetDeviceInfoTextUseCaseV3(private val repository: V3DeviceInfoRepository) {
    private val editText = EditDeviceInfoTextUseCaseV3()

    operator fun invoke(field: V3DeviceInfoField, text: String): V3DeviceInfoWriteResult {
        if (!repository.interactionEnabled.value) return V3DeviceInfoWriteResult.BLOCKED
        val value = editText(field, text).text.trim()
        if (value.isEmpty()) return V3DeviceInfoWriteResult.EMPTY
        return if (repository.sendText(field, value)) V3DeviceInfoWriteResult.SENT else V3DeviceInfoWriteResult.PREPARATION_FAILED
    }
}
