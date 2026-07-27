package com.bailout.stickk.ubi4.achievements

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AchievementProgressCalculatorTest {

    @Test
    fun `every achievement has a progress definition`() {
        assertEquals(AchievementId.entries.toSet(), AchievementDefinitions.all.keys)
    }

    @Test
    fun `progress is interpolated continuously across all three stages`() {
        assertProgress(gestureCount = 0L, expectedTier = null, expectedFraction = 0f)
        assertProgress(gestureCount = 50L, expectedTier = null, expectedFraction = 1f / 6f)
        assertProgress(
            gestureCount = 100L,
            expectedTier = AchievementTier.BRONZE,
            expectedFraction = 1f / 3f
        )
        assertProgress(
            gestureCount = 550L,
            expectedTier = AchievementTier.BRONZE,
            expectedFraction = 1f / 2f
        )
        assertProgress(
            gestureCount = 1_000L,
            expectedTier = AchievementTier.SILVER,
            expectedFraction = 2f / 3f
        )
        assertProgress(
            gestureCount = 5_500L,
            expectedTier = AchievementTier.SILVER,
            expectedFraction = 5f / 6f
        )
        assertProgress(
            gestureCount = 10_000L,
            expectedTier = AchievementTier.GOLD,
            expectedFraction = 1f
        )
    }

    @Test
    fun `negative gesture count is treated as zero`() {
        val progress = calculate(-1L)

        assertEquals(0L, progress.currentValue)
        assertEquals(100L, progress.nextTarget)
        assertNull(progress.achievedTier)
        assertEquals(0f, progress.progressFraction)
    }

    @Test
    fun `calculator supports achievements with a different number of stages and targets`() {
        val definition = AchievementDefinition(
            stages = listOf(
                AchievementStageTarget(AchievementTier.BRONZE, target = 10L),
                AchievementStageTarget(AchievementTier.GOLD, target = 30L)
            )
        )

        val progress = AchievementProgressCalculator.calculate(
            currentValue = 20L,
            definition = definition
        )

        assertEquals(AchievementTier.BRONZE, progress.achievedTier)
        assertEquals(30L, progress.nextTarget)
        assertTrue(abs(progress.progressFraction - 0.75f) < 0.0001f)
    }

    private fun assertProgress(
        gestureCount: Long,
        expectedTier: AchievementTier?,
        expectedFraction: Float
    ) {
        val progress = calculate(gestureCount)

        assertEquals(gestureCount, progress.currentValue)
        assertEquals(expectedTier, progress.achievedTier)
        assertTrue(abs(progress.progressFraction - expectedFraction) < 0.0001f)
    }

    private fun calculate(gestureCount: Long): AchievementProgress =
        AchievementProgressCalculator.calculate(
            currentValue = gestureCount,
            definition = AchievementDefinitions[AchievementId.CYBORG]
        )
}
