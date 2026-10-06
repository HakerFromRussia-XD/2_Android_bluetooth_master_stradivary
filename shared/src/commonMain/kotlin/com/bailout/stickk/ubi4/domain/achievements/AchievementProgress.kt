package com.bailout.stickk.ubi4.achievements

enum class AchievementTier {
    BRONZE,
    SILVER,
    GOLD
}

data class AchievementProgress(
    val currentValue: Long = 0L,
    val nextTarget: Long = 0L,
    val achievedTier: AchievementTier? = null,
    val progressFraction: Float = 0f
)
