package com.bailout.stickk.ubi4.ui.fragments.achievements

import androidx.compose.ui.graphics.Color
import com.bailout.stickk.ubi4.achievements.AchievementTier

internal fun AchievementTier.color(): Color = when (this) {
    AchievementTier.BRONZE -> AchievementsColors.Bronze
    AchievementTier.SILVER -> AchievementsColors.Silver
    AchievementTier.GOLD -> AchievementsColors.Gold
}
