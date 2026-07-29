package com.bailout.stickk.ubi4.achievements

object AchievementDefinitions {
    val all: Map<AchievementId, AchievementDefinition> = mapOf(
        AchievementId.BIONIC to definition(4L, 8L, 12L),
        AchievementId.CYBORG to definition(100L, 1_000L, 10_000L),
        AchievementId.STREAK to definition(3L, 7L, 30L),
        AchievementId.LONG_HAUL to definition(24L, 72L, 168L),
        AchievementId.SCIENTIST to definition(1L, 7L, 30L),
        AchievementId.DAILY_CHALLENGE to definition(1L, 7L, 30L),
        AchievementId.PRECISION to definition(50L, 250L, 1_000L),
        AchievementId.POWER to definition(50L, 250L, 1_000L),
        AchievementId.GET_A_GRIP to definition(1L, 2L, 3L),
        AchievementId.ALTER_EGO to definition(1L, 7L, 30L),
        AchievementId.ANNIVERSARY to definition(30L, 180L, 365L),
        AchievementId.SQUARE_EYES to definition(1L, 3L, 5L),
        AchievementId.PERSONALISATION to definition(1L, 2L, 3L),
        AchievementId.ALWAYS_CONNECTED to definition(1L, 7L, 30L),
        AchievementId.CHAMPION to definition(
            bronzeTarget = 5L,
            silverTarget = 10L,
            goldTarget = AchievementId.entries.size.toLong() - 1L
        )
    )

    operator fun get(id: AchievementId): AchievementDefinition =
        requireNotNull(all[id]) { "Achievement definition is missing for $id" }

    private fun definition(
        bronzeTarget: Long,
        silverTarget: Long,
        goldTarget: Long
    ) = AchievementDefinition(
        stages = listOf(
            AchievementStageTarget(AchievementTier.BRONZE, target = bronzeTarget),
            AchievementStageTarget(AchievementTier.SILVER, target = silverTarget),
            AchievementStageTarget(AchievementTier.GOLD, target = goldTarget)
        )
    )
}
