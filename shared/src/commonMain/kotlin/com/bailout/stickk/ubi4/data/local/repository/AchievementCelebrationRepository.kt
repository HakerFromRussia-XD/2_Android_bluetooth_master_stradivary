package com.bailout.stickk.ubi4.data.local.repository

import com.bailout.stickk.ubi4.achievements.AchievementCelebration
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementTier
import com.bailout.stickk.ubi4.data.local.db.dao.AchievementCelebrationStateDao
import com.bailout.stickk.ubi4.data.local.db.entity.AchievementCelebrationStateEntity
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class AchievementCelebrationRepository(
    private val dao: AchievementCelebrationStateDao
) {
    fun observeHighestCelebratedTiers(): Flow<Map<AchievementId, AchievementTier>> =
        dao.observeAll().map { states ->
            states.mapNotNull { state ->
                val achievementId = runCatching {
                    AchievementId.valueOf(state.achievement_id)
                }.getOrNull()
                val tier = runCatching {
                    AchievementTier.valueOf(state.highest_tier)
                }.getOrNull()

                if (achievementId != null && tier != null) achievementId to tier else null
            }.toMap()
        }

    suspend fun markCelebrated(celebration: AchievementCelebration) {
        dao.upsert(
            AchievementCelebrationStateEntity(
                achievement_id = celebration.achievementId.name,
                highest_tier = celebration.tier.name
            )
        )
    }
}

object AchievementCelebrationRepositoryProvider {
    @Volatile
    private var repository: AchievementCelebrationRepository? = null

    fun init(dao: AchievementCelebrationStateDao): AchievementCelebrationRepository =
        (repository ?: AchievementCelebrationRepository(dao)).also { repository = it }

    fun get(): AchievementCelebrationRepository =
        checkNotNull(repository) { "AchievementCelebrationRepositoryProvider not init" }
}

object AchievementCelebrationManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeHighestCelebratedTiers(): Flow<Map<AchievementId, AchievementTier>> =
        AchievementCelebrationRepositoryProvider.get().observeHighestCelebratedTiers()

    fun markCelebrated(celebration: AchievementCelebration) {
        scope.launch {
            runCatching {
                AchievementCelebrationRepositoryProvider.get().markCelebrated(celebration)
            }.onFailure {
                platformLog(
                    "AchievementCelebrationManager",
                    "markCelebrated failed for $celebration: ${it.message}"
                )
            }
        }
    }
}
