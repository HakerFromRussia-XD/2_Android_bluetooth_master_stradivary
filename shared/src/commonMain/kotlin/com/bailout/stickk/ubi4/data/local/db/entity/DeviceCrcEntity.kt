package com.bailout.stickk.ubi4.data.local.db.entity

import androidx.room.Entity

@Entity(
    tableName = "device_crc",
    primaryKeys = [
        "device_mac",
        "device_addr"
    ]
)
data class DeviceCrcEntity(
    val device_mac: String,
    val device_addr: Long,
    val ts_ms: Long,
    val crc: Long
)