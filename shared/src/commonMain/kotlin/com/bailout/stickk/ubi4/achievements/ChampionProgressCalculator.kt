package com.bailout.stickk.ubi4.achievements

object ChampionProgressCalculator {

    fun calculate(
        progressByAchievement: Map<AchievementId, AchievementProgress>
    ): AchievementProgress {
        val otherAchievements = AchievementId.entries
            .filterNot { it == AchievementId.CHAMPION }
            .mapNotNull(progressByAchievement::get)
        val bronzeCount = otherAchievements.countAtLeast(AchievementTier.BRONZE)
        val silverCount = otherAchievements.countAtLeast(AchievementTier.SILVER)
        val goldCount = otherAchievements.countAtLeast(AchievementTier.GOLD)
        val definition = AchievementDefinitions[AchievementId.CHAMPION]
        val bronzeTarget = definition.targetFor(AchievementTier.BRONZE)
        val silverTarget = definition.targetFor(AchievementTier.SILVER)
        val goldTarget = definition.targetFor(AchievementTier.GOLD)

        return when {
            goldCount >= goldTarget -> AchievementProgress(
                currentValue = goldCount,
                nextTarget = goldTarget,
                achievedTier = AchievementTier.GOLD,
                progressFraction = 1f
            )

            silverCount >= silverTarget -> AchievementProgress(
                currentValue = goldCount,
                nextTarget = goldTarget,
                achievedTier = AchievementTier.SILVER,
                progressFraction = stageProgress(
                    completedStageCount = 2,
                    currentValue = goldCount,
                    target = goldTarget
                )
            )

            bronzeCount >= bronzeTarget -> AchievementProgress(
                currentValue = silverCount,
                nextTarget = silverTarget,
                achievedTier = AchievementTier.BRONZE,
                progressFraction = stageProgress(
                    completedStageCount = 1,
                    currentValue = silverCount,
                    target = silverTarget
                )
            )

            else -> AchievementProgress(
                currentValue = bronzeCount,
                nextTarget = bronzeTarget,
                achievedTier = null,
                progressFraction = stageProgress(
                    completedStageCount = 0,
                    currentValue = bronzeCount,
                    target = bronzeTarget
                )
            )
        }
    }

    private fun List<AchievementProgress>.countAtLeast(tier: AchievementTier): Long =
        count { progress ->
            progress.achievedTier?.ordinal?.let { it >= tier.ordinal } == true
        }.toLong()

    private fun AchievementDefinition.targetFor(tier: AchievementTier): Long =
        stages.first { it.tier == tier }.target

    private fun stageProgress(
        completedStageCount: Int,
        currentValue: Long,
        target: Long
    ): Float =
        (completedStageCount + currentValue.toFloat() / target.toFloat()) /
            AchievementTier.entries.size
}
