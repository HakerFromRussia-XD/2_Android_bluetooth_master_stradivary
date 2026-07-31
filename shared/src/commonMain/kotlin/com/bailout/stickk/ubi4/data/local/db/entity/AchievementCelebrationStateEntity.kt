package com.bailout.stickk.ubi4.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_celebration_state")
data class AchievementCelebrationStateEntity(
    @PrimaryKey val achievement_id: String,
    val highest_tier: String
)
