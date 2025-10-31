package com.bailout.stickk.ubi4.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface WidgetStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WidgetStateEntity)

    @Query("""
        SELECT * FROM widget_state WHERE
        device_addr=:deviceAddr AND widget_id=:widgetId AND widget_code=:widgetCode AND
        parameter_id=:parameterId AND data_code=:dataCode AND data_offset=:dataOffset
        LIMIT 1
    """)
    suspend fun selectByKey(
        deviceAddr: Long, widgetId: Long, widgetCode: Long,
        parameterId: Long, dataCode: Long, dataOffset: Long
    ): WidgetStateEntity?

    @Query("SELECT COUNT(*) FROM widget_state")
    suspend fun count(): Long

    @Query("SELECT * FROM widget_state WHERE widget_id=:widgetId")
    suspend fun selectByWidget(widgetId: Long): List<WidgetStateEntity>
}