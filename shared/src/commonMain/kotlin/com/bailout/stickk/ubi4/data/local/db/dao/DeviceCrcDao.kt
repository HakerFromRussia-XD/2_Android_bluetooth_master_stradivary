package com.bailout.stickk.ubi4.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bailout.stickk.ubi4.data.local.db.entity.DeviceCrcEntity

@Dao
interface DeviceCrcDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceCrcEntity)

    @Query(
        """
        SELECT * FROM device_crc
        WHERE device_mac = :mac AND device_addr = :addr
        LIMIT 1
        """
    )
    suspend fun load(
        mac: String,
        addr: Long
    ): DeviceCrcEntity?
}