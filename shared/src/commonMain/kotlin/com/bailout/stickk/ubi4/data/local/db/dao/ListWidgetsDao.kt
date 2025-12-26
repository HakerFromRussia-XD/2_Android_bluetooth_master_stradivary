package com.bailout.stickk.ubi4.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bailout.stickk.ubi4.data.local.db.entity.ListWidgetsEntity

@Dao
interface ListWidgetsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ListWidgetsEntity)

    @Query(
        """
        SELECT * FROM list_widgets_snapshot
        WHERE device_mac = :mac
        LIMIT 1
        """
    )
    suspend fun getSnapshot(mac: String): ListWidgetsEntity?
}