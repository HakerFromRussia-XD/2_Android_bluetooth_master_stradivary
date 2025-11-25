package com.bailout.stickk.ubi4.data.local.db.entity

import androidx.room.Entity
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.local.db.payload.toPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "base_sub_device_info",
    primaryKeys = [
        "device_mac", "sub_device_addr"
    ]
)
data class BaseSubDeviceInfoEntity(
    val device_mac: String,
    val sub_device_addr: Long,
    val ts_ms: Long,
    val payload: String
) {
    companion object {
        private val json = Json { encodeDefaults = true }

        fun create(
            mac: String,
            tsMs: Long,
            sub: BaseSubDeviceInfoStruct
        ): BaseSubDeviceInfoEntity =
            BaseSubDeviceInfoEntity(
                device_mac = mac,
                sub_device_addr = sub.deviceAddress.toLong(),
                ts_ms = tsMs,
                payload = json.encodeToString(sub.toPayload())
            )
    }
}