package com.bailout.stickk.ubi4.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index


@Entity(
    tableName = "widget_state",
    primaryKeys = [
        "device_mac",
        "device_addr","widget_id","widget_code",
        "parameter_id","data_code","data_offset"
    ],
    indices = [
        Index(
            value = ["device_addr","widget_id","parameter_id","data_code","data_offset"],
            unique = true
        )
    ]
)
data class WidgetStateEntity(
    val device_mac: String,
    val device_addr: Long,
    val widget_id: Long,
    val widget_code: Long,
    val parameter_id: Long,
    val data_code: Long,
    val data_offset: Long,

    val ts_ms: Long,
    val value_text: String?,
    val value_i1: Long?,
    val value_i2: Long?,
    val value_i3: Long?
)