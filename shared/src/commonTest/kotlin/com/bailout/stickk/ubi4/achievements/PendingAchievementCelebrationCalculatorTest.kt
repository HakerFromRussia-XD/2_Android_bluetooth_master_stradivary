package com.bailout.stickk.ubi4.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingAchievementCelebrationCalculatorTest {

    @Test
    fun `returns first achievement with an uncelebrated tier`() {
        val pending = PendingAchievementCelebrationCalculator.findFirst(
            progressByAchievement = mapOf(
                AchievementId.CYBORG to progress(AchievementTier.SILVER),
                AchievementId.POWER to progress(AchievementTier.GOLD)
            ),
            highestCelebratedTierByAchievement = mapOf(
                AchievementId.CYBORG to AchievementTier.BRONZE
            )
        )

        assertEquals(
            AchievementCelebration(AchievementId.CYBORG, AchievementTier.SILVER),
            pending
        )
    }

    @Test
    fun `does not return an already celebrated tier`() {
        val pending = PendingAchievementCelebrationCalculator.findFirst(
            progressByAchievement = mapOf(
                AchievementId.CYBORG to progress(AchievementTier.SILVER)
            ),
            highestCelebratedTierByAchievement = mapOf(
                AchievementId.CYBORG to AchievementTier.SILVER
            )
        )

        assertNull(pending)
    }

    @Test
    fun `celebrates only the highest tier after a multi-stage jump`() {
        val pending = PendingAchievementCelebrationCalculator.findFirst(
            progressByAchievement = mapOf(
                AchievementId.CYBORG to progress(AchievementTier.GOLD)
            ),
            highestCelebratedTierByAchievement = emptyMap()
        )

        assertEquals(
            AchievementCelebration(AchievementId.CYBORG, AchievementTier.GOLD),
            pending
        )
    }

    private fun progress(tier: AchievementTier) =
        AchievementProgress(achievedTier = tier)
}
