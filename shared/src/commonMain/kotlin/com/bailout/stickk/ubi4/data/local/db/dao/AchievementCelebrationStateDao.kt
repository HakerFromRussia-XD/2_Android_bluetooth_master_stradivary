package com.bailout.stickk.ubi4.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bailout.stickk.ubi4.data.local.db.entity.AchievementCelebrationStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementCelebrationStateDao {

    @Query("SELECT * FROM achievement_celebration_state")
    fun observeAll(): Flow<List<AchievementCelebrationStateEntity>>

    @Upsert
    suspend fun upsert(state: AchievementCelebrationStateEntity)
}
