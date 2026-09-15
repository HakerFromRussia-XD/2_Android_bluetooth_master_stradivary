package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeV3DeviceInfoRepository : V3DeviceInfoRepository {
    override val interactionEnabled = MutableStateFlow(true)
    val current = mutableMapOf<V3DeviceInfoField, String?>()
    val writes = mutableListOf<Pair<V3DeviceInfoField, String>>()
    var canPrepare = true
    override fun getTextForInput(field: V3DeviceInfoField) = current[field]
    override fun sendText(field: V3DeviceInfoField, text: String): Boolean {
        if (!canPrepare) return false
        writes += field to text
        return true
    }
}
