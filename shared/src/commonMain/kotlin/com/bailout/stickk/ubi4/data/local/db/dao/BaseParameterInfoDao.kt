package com.bailout.stickk.ubi4.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bailout.stickk.ubi4.data.local.db.entity.BaseParameterInfoEntity

@Dao
interface BaseParameterInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BaseParameterInfoEntity)

    @Query(
        """
        SELECT * FROM base_parameter_info
        WHERE device_mac = :mac
        """
    )
    suspend fun getAllForMac(
        mac: String
    ): List<BaseParameterInfoEntity>

}