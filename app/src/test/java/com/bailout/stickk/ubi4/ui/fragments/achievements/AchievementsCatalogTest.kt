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
                R.drawable.achievement_bionic,
                R.drawable.achievement_cyborg,
                R.drawable.achievement_streak,
                R.drawable.achievement_long_haul,
                R.drawable.achievement_scientist,
                R.drawable.achievement_daily_challenge,
                R.drawable.achievement_precision,
                R.drawable.achievement_power,
                R.drawable.achievement_get_a_grip,
                R.drawable.achievement_alter_ego,
                R.drawable.achievement_anniversary,
                R.drawable.achievement_personalisation,
                R.drawable.achievement_phone,
                R.drawable.achievement_champion
            ),
            items.map(AchievementUiModel::iconRes)
        )
        assertTrue(items.all { (it.artworkBounds?.width ?: 0) > 0 && (it.artworkBounds?.height ?: 0) > 0 })
        assertTrue(items.all { it.achievedTier == null })
        assertTrue(items.all { it.progress.nextTarget > 0L })
        assertTrue(
            items.all { item ->
                item.stages.map(AchievementStageUiModel::tier) == AchievementTier.entries
            }
        )
    }
}
