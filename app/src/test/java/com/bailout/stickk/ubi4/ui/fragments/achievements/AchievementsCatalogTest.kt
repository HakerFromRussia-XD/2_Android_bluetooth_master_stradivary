package com.bailout.stickk.ubi4.ui.fragments.achievements

import com.bailout.stickk.R
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AchievementsCatalogTest {

    @Test
    fun `catalog contains every achievement once and in declared order`() {
        val items = AchievementsCatalog.items
        val enabledAchievementIds = AchievementId.entries - AchievementId.SQUARE_EYES

        assertEquals(enabledAchievementIds, items.map(AchievementUiModel::id))
        assertEquals(items.size, items.map(AchievementUiModel::id).distinct().size)
        assertEquals(
            listOf(
                R.drawable.ic_achievement_bionic,
                R.drawable.ic_achievement_cyborg,
                R.drawable.ic_achievement_streak,
                R.drawable.ic_achievement_long_haul,
                R.drawable.ic_achievement_scientist,
                R.drawable.ic_achievement_daily_challenge,
                R.drawable.ic_achievement_precision,
                R.drawable.ic_achievement_power,
                R.drawable.ic_achievement_get_a_grip,
                R.drawable.ic_achievement_alter_ego,
                R.drawable.ic_achievement_anniversary,
                R.drawable.ic_achievement_personalisation,
                R.drawable.ic_achievement_always_connected,
                R.drawable.ic_achievement_champion
            ),
            items.map(AchievementUiModel::iconRes)
        )
        assertTrue(items.all { it.achievedTier == null })
        assertTrue(items.all { it.progress.nextTarget > 0L })
        assertTrue(
            items.all { item ->
                item.stages.map(AchievementStageUiModel::tier) == AchievementTier.entries
            }
        )
    }
}
