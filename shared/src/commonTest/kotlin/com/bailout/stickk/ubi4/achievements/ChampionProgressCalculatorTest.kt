package com.bailout.stickk.ubi4.achievements

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChampionProgressCalculatorTest {

    @Test
    fun `bronze requires bronze or higher in five other achievements`() {
        val progress = calculate(
            bronzeCount = 3,
            silverCount = 1,
            goldCount = 1
        )

        assertEquals(AchievementTier.BRONZE, progress.achievedTier)
        assertEquals(2L, progress.currentValue)
        assertEquals(10L, progress.nextTarget)
    }

    @Test
    fun `silver requires silver or higher in ten other achievements`() {
        val progress = calculate(
            bronzeCount = 2,
            silverCount = 7,
            goldCount = 3
        )

        assertEquals(AchievementTier.SILVER, progress.achievedTier)
        assertEquals(3L, progress.currentValue)
        assertEquals(14L, progress.nextTarget)
    }

    @Test
    fun `gold requires gold in every other achievement`() {
        val otherAchievementCount = AchievementId.entries.size - 1
        val progress = calculate(
            bronzeCount = 0,
            silverCount = 0,
            goldCount = otherAchievementCount
        )

        assertEquals(AchievementTier.GOLD, progress.achievedTier)
        assertEquals(otherAchievementCount.toLong(), progress.currentValue)
        assertEquals(otherAchievementCount.toLong(), progress.nextTarget)
        assertEquals(1f, progress.progressFraction)
    }

    @Test
    fun `champion does not count itself`() {
        val progressByAchievement = achievementsWithTiers(
            bronzeCount = 4,
            silverCount = 0,
            goldCount = 0
        ) + (
            AchievementId.CHAMPION to
                AchievementProgress(achievedTier = AchievementTier.GOLD)
            )

        val progress = ChampionProgressCalculator.calculate(progressByAchievement)

        assertNull(progress.achievedTier)
        assertEquals(4L, progress.currentValue)
        assertTrue(abs(progress.progressFraction - 4f / 15f) < 0.0001f)
    }

    private fun calculate(
        bronzeCount: Int,
        silverCount: Int,
        goldCount: Int
    ): AchievementProgress = ChampionProgressCalculator.calculate(
        achievementsWithTiers(bronzeCount, silverCount, goldCount)
    )

    private fun achievementsWithTiers(
        bronzeCount: Int,
        silverCount: Int,
        goldCount: Int
    ): Map<AchievementId, AchievementProgress> {
        val tiers =
            List(bronzeCount) { AchievementTier.BRONZE } +
                List(silverCount) { AchievementTier.SILVER } +
                List(goldCount) { AchievementTier.GOLD }

        return AchievementId.entries
            .filterNot { it == AchievementId.CHAMPION }
            .zip(tiers)
            .associate { (id, tier) -> id to AchievementProgress(achievedTier = tier) }
    }
}
