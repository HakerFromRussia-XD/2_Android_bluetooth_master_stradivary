import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bailout.stickk.ubi4.data.local.db.BaseSubDeviceInfoEntity

@Dao
interface BaseSubDeviceInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BaseSubDeviceInfoEntity)

    @Query(
        """
        SELECT * FROM base_sub_device_info
        WHERE device_mac = :mac
        """
    )
    suspend fun getAllForMac(mac: String): List<BaseSubDeviceInfoEntity>
}