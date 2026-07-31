package com.bailout.stickk.ubi4.achievements

data class AchievementCelebration(
    val achievementId: AchievementId,
    val tier: AchievementTier
)

object PendingAchievementCelebrationCalculator {

    fun findFirst(
        progressByAchievement: Map<AchievementId, AchievementProgress>,
        highestCelebratedTierByAchievement: Map<AchievementId, AchievementTier>
    ): AchievementCelebration? = AchievementId.entries
        .firstNotNullOfOrNull { achievementId ->
            val achievedTier = progressByAchievement[achievementId]?.achievedTier
                ?: return@firstNotNullOfOrNull null
            val celebratedTier = highestCelebratedTierByAchievement[achievementId]

            if (celebratedTier == null || achievedTier.ordinal > celebratedTier.ordinal) {
                AchievementCelebration(achievementId, achievedTier)
            } else {
                null
            }
        }
}
