package com.bailout.stickk.ubi4.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bailout.stickk.ubi4.data.local.db.entity.AchievementUniqueEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementUniqueEventDao {
    @Query(
        """
        SELECT COUNT(*) FROM achievement_unique_events
        WHERE achievement_id = :achievementId
        """
    )
    fun observeSubjectCount(achievementId: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: AchievementUniqueEventEntity)
}
