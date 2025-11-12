import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.bailout.stickk.ubi4.data.local.db.BaseSubDeviceInfoEntity

@Dao
interface BaseSubDeviceInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BaseSubDeviceInfoEntity)
}