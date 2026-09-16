package com.bailout.stickk.ubi4.ui.fragments.achievements

import com.bailout.stickk.R
import androidx.compose.ui.unit.IntRect
import com.bailout.stickk.ubi4.achievements.AchievementDefinitions
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementProgressCalculator
import com.bailout.stickk.ubi4.achievements.AchievementTier

object AchievementsCatalog {
    val items: List<AchievementUiModel> = listOf(
        achievement(
            id = AchievementId.BIONIC,
            iconRes = R.drawable.achievement_bionic,
            artworkBounds = IntRect(0, 30, 256, 226),
            titleRes = R.string.achievement_bionic_title,
            bronzeRes = R.string.achievement_bionic_bronze,
            silverRes = R.string.achievement_bionic_silver,
            goldRes = R.string.achievement_bionic_gold
        ),
        achievement(
            id = AchievementId.CYBORG,
            iconRes = R.drawable.achievement_cyborg,
            artworkBounds = IntRect(1, 0, 256, 256),
            titleRes = R.string.achievement_cyborg_title,
            bronzeRes = R.string.achievement_cyborg_bronze,
            silverRes = R.string.achievement_cyborg_silver,
            goldRes = R.string.achievement_cyborg_gold
        ),
        achievement(
            id = AchievementId.STREAK,
            iconRes = R.drawable.achievement_streak,
            artworkBounds = IntRect(0, 0, 256, 256),
            titleRes = R.string.achievement_streak_title,
            bronzeRes = R.string.achievement_streak_bronze,
            silverRes = R.string.achievement_streak_silver,
            goldRes = R.string.achievement_streak_gold
        ),
        achievement(
            id = AchievementId.LONG_HAUL,
            iconRes = R.drawable.achievement_long_haul,
            artworkBounds = IntRect(0, 49, 256, 256),
            titleRes = R.string.achievement_long_haul_title,
            bronzeRes = R.string.achievement_long_haul_bronze,
            silverRes = R.string.achievement_long_haul_silver,
            goldRes = R.string.achievement_long_haul_gold
        ),
        achievement(
            id = AchievementId.SCIENTIST,
            iconRes = R.drawable.achievement_scientist,
            artworkBounds = IntRect(0, 7, 256, 256),
            titleRes = R.string.achievement_scientist_title,
            bronzeRes = R.string.achievement_scientist_bronze,
            silverRes = R.string.achievement_scientist_silver,
            goldRes = R.string.achievement_scientist_gold
        ),
        achievement(
            id = AchievementId.DAILY_CHALLENGE,
            iconRes = R.drawable.achievement_daily_challenge,
            artworkBounds = IntRect(0, 17, 256, 241),
            titleRes = R.string.achievement_daily_challenge_title,
            bronzeRes = R.string.achievement_daily_challenge_bronze,
            silverRes = R.string.achievement_daily_challenge_silver,
            goldRes = R.string.achievement_daily_challenge_gold
        ),
        achievement(
            id = AchievementId.PRECISION,
            iconRes = R.drawable.achievement_precision,
            artworkBounds = IntRect(0, 18, 253, 236),
            titleRes = R.string.achievement_precision_title,
            bronzeRes = R.string.achievement_precision_bronze,
            silverRes = R.string.achievement_precision_silver,
            goldRes = R.string.achievement_precision_gold
        ),
        achievement(
            id = AchievementId.POWER,
            iconRes = R.drawable.achievement_power,
            artworkBounds = IntRect(0, 0, 256, 256),
            titleRes = R.string.achievement_power_title,
            bronzeRes = R.string.achievement_power_bronze,
            silverRes = R.string.achievement_power_silver,
            goldRes = R.string.achievement_power_gold
        ),
        achievement(
            id = AchievementId.GET_A_GRIP,
            iconRes = R.drawable.achievement_get_a_grip,
            artworkBounds = IntRect(0, 0, 256, 256),
            titleRes = R.string.achievement_get_a_grip_title,
            bronzeRes = R.string.achievement_get_a_grip_bronze,
            silverRes = R.string.achievement_get_a_grip_silver,
            goldRes = R.string.achievement_get_a_grip_gold
        ),
        achievement(
            id = AchievementId.ALTER_EGO,
            iconRes = R.drawable.achievement_alter_ego,
            artworkBounds = IntRect(30, 3, 245, 254),
            titleRes = R.string.achievement_alter_ego_title,
            bronzeRes = R.string.achievement_alter_ego_bronze,
            silverRes = R.string.achievement_alter_ego_silver,
            goldRes = R.string.achievement_alter_ego_gold
        ),
        achievement(
            id = AchievementId.ANNIVERSARY,
            iconRes = R.drawable.achievement_anniversary,
            artworkBounds = IntRect(0, 0, 256, 256),
            titleRes = R.string.achievement_anniversary_title,
            bronzeRes = R.string.achievement_anniversary_bronze,
            silverRes = R.string.achievement_anniversary_silver,
            goldRes = R.string.achievement_anniversary_gold
        ),
//        achievement(
//            id = AchievementId.SQUARE_EYES,
//            titleRes = R.string.achievement_square_eyes_title,
//            bronzeRes = R.string.achievement_square_eyes_bronze,
//            silverRes = R.string.achievement_square_eyes_silver,
//            goldRes = R.string.achievement_square_eyes_gold
//        ),
        achievement(
            id = AchievementId.PERSONALISATION,
            iconRes = R.drawable.achievement_personalisation,
            artworkBounds = IntRect(1, 34, 255, 224),
            titleRes = R.string.achievement_personalisation_title,
            bronzeRes = R.string.achievement_personalisation_bronze,
            silverRes = R.string.achievement_personalisation_silver,
            goldRes = R.string.achievement_personalisation_gold
        ),
        achievement(
            id = AchievementId.ALWAYS_CONNECTED,
            iconRes = R.drawable.achievement_phone,
            artworkBounds = IntRect(0, 0, 256, 256),
            titleRes = R.string.achievement_always_connected_title,
            bronzeRes = R.string.achievement_always_connected_bronze,
            silverRes = R.string.achievement_always_connected_silver,
            goldRes = R.string.achievement_always_connected_gold
        ),
        achievement(
            id = AchievementId.CHAMPION,
            iconRes = R.drawable.achievement_champion,
            artworkBounds = IntRect(1, 0, 255, 251),
            titleRes = R.string.achievement_champion_title,
            bronzeRes = R.string.achievement_champion_bronze,
            silverRes = R.string.achievement_champion_silver,
            goldRes = R.string.achievement_champion_gold
        )
    )

    private fun achievement(
        id: AchievementId,
        titleRes: Int,
        iconRes: Int,
        artworkBounds: IntRect,
        bronzeRes: Int,
        silverRes: Int,
        goldRes: Int
    ): AchievementUiModel = AchievementUiModel(
        id = id,
        titleRes = titleRes,
        stages = listOf(
            AchievementStageUiModel(AchievementTier.BRONZE, bronzeRes),
            AchievementStageUiModel(AchievementTier.SILVER, silverRes),
            AchievementStageUiModel(AchievementTier.GOLD, goldRes)
        ),
        iconRes = iconRes,
        artworkBounds = artworkBounds,
        progress = AchievementProgressCalculator.calculate(
            currentValue = 0L,
            definition = AchievementDefinitions[id]
        )
    )

}
