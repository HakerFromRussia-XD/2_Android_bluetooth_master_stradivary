package com.bailout.stickk.ubi4.data.local.db

import androidx.room.Entity
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.db.payload.toPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "base_parameter_info",
    primaryKeys = [
        "device_mac", "device_addr", "parameter_id", "data_code"
    ]
)
data class BaseParameterInfoEntity(
    val device_mac: String,
    val device_addr: Long,
    val parameter_id: Long,
    val data_code: Long,
    val ts_ms: Long,
    val payload: String
) {
    companion object {
        private val json = Json { encodeDefaults = true }

        fun create(
            mac: String,
            deviceAddr: Int,
            parameterId: Int,
            dataCode: Int,
            tsMs: Long,
            info: BaseParameterInfoStruct
        ): BaseParameterInfoEntity =
            BaseParameterInfoEntity(
                device_mac = mac,
                device_addr = deviceAddr.toLong(),
                parameter_id = parameterId.toLong(),
                data_code = dataCode.toLong(),
                ts_ms = tsMs,
                payload = json.encodeToString(info.toPayload())
            )
    }
}