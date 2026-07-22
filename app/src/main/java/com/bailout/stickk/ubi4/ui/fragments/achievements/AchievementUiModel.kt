package com.bailout.stickk.ubi4.ui.fragments.achievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

enum class AchievementId {
    BIONIC,
    CYBORG,
    STREAK,
    LONG_HAUL,
    SCIENTIST,
    DAILY_CHALLENGE,
    PRECISION,
    POWER,
    GET_A_GRIP,
    ALTER_EGO,
    ANNIVERSARY,
    SQUARE_EYES,
    PERSONALISATION,
    ALWAYS_CONNECTED,
    CHAMPION
}

enum class AchievementTier(val level: Int) {
    BRONZE(level = 1),
    SILVER(level = 2),
    GOLD(level = 3)
}

@Immutable
data class AchievementStageUiModel(
    val tier: AchievementTier,
    @StringRes val descriptionRes: Int
)

@Immutable
data class AchievementUiModel(
    val id: AchievementId,
    @StringRes val titleRes: Int,
    val stages: List<AchievementStageUiModel>,
    @DrawableRes val iconRes: Int,
    val achievedTier: AchievementTier? = null
)
