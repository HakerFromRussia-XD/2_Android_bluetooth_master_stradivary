package com.bailout.stickk.ubi4.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "settings_profile_values",
    primaryKeys = ["serial_number", "profile_id", "setting_key"],
    indices = [
        Index(value = ["serial_number", "profile_id"]),
        Index(value = ["serial_number", "profile_id", "target"])
    ]
)
data class SettingsProfileValueEntity(
    val serial_number: String,
    val profile_id: Int,
    val setting_key: String,
    val target: String,
    val parameter_id: Int,
    val data_code: Int,
    val data_offset: Int,
    val device_address: Int,
    val codec_id: String,
    val value_text: String?,
    val value_i1: Long?,
    val value_i2: Long?,
    val value_i3: Long?,
    val updated_ts_ms: Long
)
