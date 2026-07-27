package com.bailout.stickk.ubi4.ui.fragments.achievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementProgress
import com.bailout.stickk.ubi4.achievements.AchievementTier

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
    val progress: AchievementProgress = AchievementProgress()
) {
    val achievedTier: AchievementTier?
        get() = progress.achievedTier
}
