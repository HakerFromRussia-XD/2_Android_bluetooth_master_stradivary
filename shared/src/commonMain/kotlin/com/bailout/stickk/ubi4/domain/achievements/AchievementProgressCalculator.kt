package com.bailout.stickk.ubi4.achievements

data class AchievementStageTarget(
    val tier: AchievementTier,
    val target: Long
)

data class AchievementDefinition(
    val stages: List<AchievementStageTarget>
) {
    init {
        require(stages.isNotEmpty()) { "Achievement must contain at least one stage" }
        require(stages.all { it.target > 0L }) { "Achievement targets must be positive" }
        require(stages.zipWithNext().all { (current, next) -> current.target < next.target }) {
            "Achievement targets must be strictly increasing"
        }
        require(stages.map(AchievementStageTarget::tier).distinct().size == stages.size) {
            "Achievement tiers must be unique"
        }
    }
}


object AchievementProgressCalculator {

    fun calculate(
        currentValue: Long,
        definition: AchievementDefinition
    ): AchievementProgress {
        val safeCurrentValue = currentValue.coerceAtLeast(0L)
        val completedStages = definition.stages.takeWhile { safeCurrentValue >= it.target }
        val nextStage = definition.stages.getOrNull(completedStages.size)
        val progressFraction = if (nextStage == null) {
            1f
        } else {
            val stageStart = completedStages.lastOrNull()?.target ?: 0L
            val stageFraction =
                (safeCurrentValue - stageStart).toFloat() / (nextStage.target - stageStart).toFloat()
            (completedStages.size + stageFraction) / definition.stages.size
        }

        return AchievementProgress(
            currentValue = safeCurrentValue,
            nextTarget = nextStage?.target ?: definition.stages.last().target,
            achievedTier = completedStages.lastOrNull()?.tier,
            progressFraction = progressFraction.coerceIn(0f, 1f)
        )
    }
}
