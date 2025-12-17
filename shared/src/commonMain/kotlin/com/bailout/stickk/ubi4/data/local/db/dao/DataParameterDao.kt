package com.bailout.stickk.ubi4.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bailout.stickk.ubi4.data.local.db.entity.DataParameterEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface DataParameterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: DataParameterEntity)

    @Query("SELECT * FROM widget_state WHERE device_mac = :mac AND widget_id = :widgetId ORDER BY ts_ms DESC")
    fun observeByWidget(mac: String, widgetId: Long): Flow<List<DataParameterEntity>>

    @Query("""
        SELECT * FROM widget_state
        WHERE device_mac = :mac
          AND device_addr = :deviceAddr AND widget_id = :widgetId AND widget_code = :widgetCode
          AND parameter_id = :parameterId AND data_code = :dataCode AND data_offset = :dataOffset
        LIMIT 1
    """)
    fun observeByKey(
        mac: String,
        deviceAddr: Long, widgetId: Long, widgetCode: Long,
        parameterId: Long, dataCode: Long, dataOffset: Long
    ): Flow<DataParameterEntity?>

    @Query("""
    SELECT * FROM widget_state
    WHERE device_mac = :mac
    ORDER BY ts_ms DESC
""")
    fun observeAll(mac: String): Flow<List<DataParameterEntity>>

    @Query("DELETE FROM widget_state WHERE device_mac = :mac AND device_addr = :deviceAddr")
    suspend fun clearByDevice(mac: String, deviceAddr: Long)

    @Query("SELECT COUNT(*) FROM widget_state WHERE device_mac = :mac")
    suspend fun count(mac: String): Long

    @Query(
        """
        SELECT * FROM widget_state
        WHERE device_mac = :mac
          AND parameter_id = :parameterId
          AND data_code = :dataCode
        ORDER BY ts_ms DESC
        LIMIT 1
        """
    )
    suspend fun getLastByMac(
        mac: String,
        parameterId: Long,
        dataCode: Long
    ): DataParameterEntity?
}