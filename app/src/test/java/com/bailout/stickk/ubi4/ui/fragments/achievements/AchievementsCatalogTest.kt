package com.bailout.stickk.ubi4.ui.fragments.achievements

import com.bailout.stickk.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AchievementsCatalogTest {

    @Test
    fun `catalog contains every achievement once and in declared order`() {
        val items = AchievementsCatalog.items

        assertEquals(AchievementId.entries, items.map(AchievementUiModel::id))
        assertEquals(items.size, items.map(AchievementUiModel::id).distinct().size)
        assertTrue(items.all { it.iconRes == R.drawable.trophy })
        assertTrue(items.all { it.achievedTier == null })
        assertTrue(
            items.all { item ->
                item.stages.map(AchievementStageUiModel::tier) == AchievementTier.entries
            }
        )
    }
}
