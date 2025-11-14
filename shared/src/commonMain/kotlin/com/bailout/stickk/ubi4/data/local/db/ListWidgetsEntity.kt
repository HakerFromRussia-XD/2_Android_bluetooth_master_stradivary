package com.bailout.stickk.ubi4.data.local.db

import androidx.room.Entity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.bailout.stickk.ubi4.data.local.db.payload.BaseParameterWidgetPayload
import com.bailout.stickk.ubi4.data.local.db.payload.toWidgetPayloadOrNull
import kotlinx.datetime.Clock

@Entity(
    tableName = "list_widgets_snapshot",
    primaryKeys = [
        "device_mac",
        "device_addr"
    ]
)
data class ListWidgetsEntity(
    val device_mac: String,
    val device_addr: Long,
    val ts_ms: Long,
    val payload: String // JSON: List<BaseParameterWidgetPayload>
) {
    companion object {
        private val json = Json { encodeDefaults = true }

        fun create(
            mac: String,
            deviceAddr: Int,
            widgets: List<Any>
        ): ListWidgetsEntity {
            val payloadList: List<BaseParameterWidgetPayload> =
                widgets.mapNotNull { it.toWidgetPayloadOrNull() }

            return ListWidgetsEntity(
                device_mac = mac,
                device_addr = deviceAddr.toLong(),
                ts_ms = Clock.System.now().toEpochMilliseconds(),
                payload = json.encodeToString(payloadList)
            )
        }
    }
}