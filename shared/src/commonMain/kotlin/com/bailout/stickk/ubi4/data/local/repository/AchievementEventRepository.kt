package com.bailout.stickk.ubi4.data.local.repository

import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.data.local.db.dao.AchievementUniqueEventDao
import com.bailout.stickk.ubi4.data.local.db.entity.AchievementUniqueEventEntity
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import com.bailout.stickk.ubi4.utility.logging.platformLog
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.concurrent.Volatile

class AchievementEventRepository(
    private val dao: AchievementUniqueEventDao
) {
    fun observeUniqueSubjectCount(achievementId: AchievementId): Flow<Long> =
        dao.observeSubjectCount(achievementId.name)

    suspend fun recordUnique(
        achievementId: AchievementId,
        subjectId: String
    ) {
        require(subjectId.isNotBlank()) { "Achievement subject ID must not be blank" }
        dao.insert(
            AchievementUniqueEventEntity(
                achievement_id = achievementId.name,
                subject_id = subjectId,
                created_ts_ms = getTimeMillis()
            )
        )
    }
}

object AchievementEventRepositoryProvider {
    @Volatile
    private var repository: AchievementEventRepository? = null

    fun init(dao: AchievementUniqueEventDao): AchievementEventRepository =
        (repository ?: AchievementEventRepository(dao)).also { repository = it }

    fun get(): AchievementEventRepository =
        checkNotNull(repository) { "AchievementEventRepositoryProvider not init" }
}

object AchievementEventManager {
    private const val CUSTOM_DEVICE_NAME_SUBJECT_ID = "custom-device-name-set"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeUniqueSubjectCount(achievementId: AchievementId): Flow<Long> =
        AchievementEventRepositoryProvider.get().observeUniqueSubjectCount(achievementId)

    fun recordCustomGripConfiguration(gestureId: Int) {
        if (!gestureId.isCustomGestureId()) return

        recordUniqueInBackground(
            achievementId = AchievementId.GET_A_GRIP,
            subjectId = gestureId.toString()
        )
    }

    fun recordSuccessfulBleConnection() {
        recordUniqueInBackground(
            achievementId = AchievementId.ALWAYS_CONNECTED,
            subjectId = connectionDaySubjectId()
        )
    }

    fun recordDeviceNameCustomization() {
        recordUniqueInBackground(
            achievementId = AchievementId.ALTER_EGO,
            subjectId = CUSTOM_DEVICE_NAME_SUBJECT_ID
        )
    }

    private fun recordUniqueInBackground(
        achievementId: AchievementId,
        subjectId: String
    ) {
        scope.launch {
            runCatching {
                AchievementEventRepositoryProvider.get().recordUnique(
                    achievementId = achievementId,
                    subjectId = subjectId
                )
            }.onFailure {
                platformLog(
                    "AchievementEventManager",
                    "recordUnique failed for $achievementId: ${it.message}"
                )
            }
        }
    }
}

internal fun connectionDaySubjectId(
    instant: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String = instant.toLocalDateTime(timeZone).date.toString()

internal fun Int.isCustomGestureId(): Boolean =
    this in GestureEnum.GESTURE_CUSTOM_0.number..GestureEnum.GESTURE_CUSTOM_7.number
