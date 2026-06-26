package com.bailout.stickk.ubi4.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "settings_profiles",
    primaryKeys = ["serial_number", "profile_id"],
    indices = [
        Index(value = ["serial_number", "is_active"])
    ]
)
data class SettingsProfileEntity(
    val serial_number: String,
    val profile_id: Int,
    val name: String,
    val is_active: Boolean,
    val created_ts_ms: Long,
    val updated_ts_ms: Long
)
