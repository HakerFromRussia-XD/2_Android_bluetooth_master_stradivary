package com.bailout.stickk.ubi4.ui.fragments.achievements

import com.bailout.stickk.R

object AchievementsCatalog {
    val items: List<AchievementUiModel> = listOf(
        achievement(
            id = AchievementId.BIONIC,
            titleRes = R.string.achievement_bionic_title,
            bronzeRes = R.string.achievement_bionic_bronze,
            silverRes = R.string.achievement_bionic_silver,
            goldRes = R.string.achievement_bionic_gold
        ),
        achievement(
            id = AchievementId.CYBORG,
            titleRes = R.string.achievement_cyborg_title,
            bronzeRes = R.string.achievement_cyborg_bronze,
            silverRes = R.string.achievement_cyborg_silver,
            goldRes = R.string.achievement_cyborg_gold
        ),
        achievement(
            id = AchievementId.STREAK,
            titleRes = R.string.achievement_streak_title,
            bronzeRes = R.string.achievement_streak_bronze,
            silverRes = R.string.achievement_streak_silver,
            goldRes = R.string.achievement_streak_gold
        ),
        achievement(
            id = AchievementId.LONG_HAUL,
            titleRes = R.string.achievement_long_haul_title,
            bronzeRes = R.string.achievement_long_haul_bronze,
            silverRes = R.string.achievement_long_haul_silver,
            goldRes = R.string.achievement_long_haul_gold
        ),
        achievement(
            id = AchievementId.SCIENTIST,
            titleRes = R.string.achievement_scientist_title,
            bronzeRes = R.string.achievement_scientist_bronze,
            silverRes = R.string.achievement_scientist_silver,
            goldRes = R.string.achievement_scientist_gold
        ),
        achievement(
            id = AchievementId.DAILY_CHALLENGE,
            titleRes = R.string.achievement_daily_challenge_title,
            bronzeRes = R.string.achievement_daily_challenge_bronze,
            silverRes = R.string.achievement_daily_challenge_silver,
            goldRes = R.string.achievement_daily_challenge_gold
        ),
        achievement(
            id = AchievementId.PRECISION,
            titleRes = R.string.achievement_precision_title,
            bronzeRes = R.string.achievement_precision_bronze,
            silverRes = R.string.achievement_precision_silver,
            goldRes = R.string.achievement_precision_gold
        ),
        achievement(
            id = AchievementId.POWER,
            titleRes = R.string.achievement_power_title,
            bronzeRes = R.string.achievement_power_bronze,
            silverRes = R.string.achievement_power_silver,
            goldRes = R.string.achievement_power_gold
        ),
        achievement(
            id = AchievementId.GET_A_GRIP,
            titleRes = R.string.achievement_get_a_grip_title,
            bronzeRes = R.string.achievement_get_a_grip_bronze,
            silverRes = R.string.achievement_get_a_grip_silver,
            goldRes = R.string.achievement_get_a_grip_gold
        ),
        achievement(
            id = AchievementId.ALTER_EGO,
            titleRes = R.string.achievement_alter_ego_title,
            bronzeRes = R.string.achievement_alter_ego_bronze,
            silverRes = R.string.achievement_alter_ego_silver,
            goldRes = R.string.achievement_alter_ego_gold
        ),
        achievement(
            id = AchievementId.ANNIVERSARY,
            titleRes = R.string.achievement_anniversary_title,
            bronzeRes = R.string.achievement_anniversary_bronze,
            silverRes = R.string.achievement_anniversary_silver,
            goldRes = R.string.achievement_anniversary_gold
        ),
        achievement(
            id = AchievementId.SQUARE_EYES,
            titleRes = R.string.achievement_square_eyes_title,
            bronzeRes = R.string.achievement_square_eyes_bronze,
            silverRes = R.string.achievement_square_eyes_silver,
            goldRes = R.string.achievement_square_eyes_gold
        ),
        achievement(
            id = AchievementId.PERSONALISATION,
            titleRes = R.string.achievement_personalisation_title,
            bronzeRes = R.string.achievement_personalisation_bronze,
            silverRes = R.string.achievement_personalisation_silver,
            goldRes = R.string.achievement_personalisation_gold
        ),
        achievement(
            id = AchievementId.ALWAYS_CONNECTED,
            titleRes = R.string.achievement_always_connected_title,
            bronzeRes = R.string.achievement_always_connected_bronze,
            silverRes = R.string.achievement_always_connected_silver,
            goldRes = R.string.achievement_always_connected_gold
        ),
        achievement(
            id = AchievementId.CHAMPION,
            titleRes = R.string.achievement_champion_title,
            bronzeRes = R.string.achievement_champion_bronze,
            silverRes = R.string.achievement_champion_silver,
            goldRes = R.string.achievement_champion_gold
        )
    )

    private fun achievement(
        id: AchievementId,
        titleRes: Int,
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
        iconRes = R.drawable.trophy
    )
}
