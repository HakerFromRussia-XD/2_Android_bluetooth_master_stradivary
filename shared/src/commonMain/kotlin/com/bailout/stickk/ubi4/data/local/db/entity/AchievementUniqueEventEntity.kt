package com.bailout.stickk.ubi4.data.local.db.entity

import androidx.room.Entity

@Entity(
    tableName = "achievement_unique_events",
    primaryKeys = ["achievement_id", "subject_id"]
)
data class AchievementUniqueEventEntity(
    val achievement_id: String,
    val subject_id: String,
    val created_ts_ms: Long
)
