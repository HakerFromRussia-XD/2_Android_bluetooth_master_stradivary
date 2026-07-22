package com.bailout.stickk.ubi4.testing

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.data.local.db.dao.BaseParameterInfoDao
import com.bailout.stickk.ubi4.data.local.db.dao.BaseSubDeviceInfoDao
import com.bailout.stickk.ubi4.data.local.db.dao.DataParameterDao
import com.bailout.stickk.ubi4.data.local.db.dao.DeviceCrcDao
import com.bailout.stickk.ubi4.data.local.db.dao.ListWidgetsDao
import com.bailout.stickk.ubi4.data.local.db.dao.SettingsProfileDao
import com.bailout.stickk.ubi4.data.local.db.entity.BaseParameterInfoEntity
import com.bailout.stickk.ubi4.data.local.db.entity.BaseSubDeviceInfoEntity
import com.bailout.stickk.ubi4.data.local.db.entity.DataParameterEntity
import com.bailout.stickk.ubi4.data.local.db.entity.DeviceCrcEntity
import com.bailout.stickk.ubi4.data.local.db.entity.ListWidgetsEntity
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileEntity
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileValueEntity
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class RecordingBleCommandExecutor : BleCommandExecutor {
    private val queue = BlockingQueueUbi4()

    val queuedPackets = mutableListOf<ByteArray>()
    var serialUpdated: DeviceInfoStructs? = null

    override fun getQueueUBI4(): BlockingQueueUbi4 = queue

    override fun getRemainingTasksCount(): Int = 0

    override fun bleCommandWithQueue(
        byteArray: ByteArray?,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit
    ) {
        if (byteArray != null) {
            queuedPackets += byteArray
        }
        onChunkSent()
    }

    override fun sendWidgetsArray() = Unit

    override fun updateSerialNumber(deviceInfo: DeviceInfoStructs) {
        serialUpdated = deviceInfo
    }
}

class InMemoryDataParameterDao : DataParameterDao {
    private val rows = mutableListOf<DataParameterEntity>()

    override suspend fun upsert(e: DataParameterEntity) {
        rows.removeAll {
            it.device_mac == e.device_mac &&
                it.device_addr == e.device_addr &&
                it.widget_id == e.widget_id &&
                it.widget_code == e.widget_code &&
                it.parameter_id == e.parameter_id &&
                it.data_code == e.data_code &&
                it.data_offset == e.data_offset
        }
        rows += e
    }

    override fun observeByWidget(mac: String, widgetId: Long): Flow<List<DataParameterEntity>> =
        flowOf(rows.filter { it.device_mac == mac && it.widget_id == widgetId })

    override fun observeByKey(
        mac: String,
        deviceAddr: Long,
        widgetId: Long,
        widgetCode: Long,
        parameterId: Long,
        dataCode: Long,
        dataOffset: Long
    ): Flow<DataParameterEntity?> = flowOf(
        rows.firstOrNull {
            it.device_mac == mac &&
                it.device_addr == deviceAddr &&
                it.widget_id == widgetId &&
                it.widget_code == widgetCode &&
                it.parameter_id == parameterId &&
                it.data_code == dataCode &&
                it.data_offset == dataOffset
        }
    )

    override fun observeAll(mac: String): Flow<List<DataParameterEntity>> =
        flowOf(rows.filter { it.device_mac == mac }.sortedByDescending { it.ts_ms })

    override suspend fun clearByDevice(mac: String, deviceAddr: Long) {
        rows.removeAll { it.device_mac == mac && it.device_addr == deviceAddr }
    }

    override suspend fun count(mac: String): Long = rows.count { it.device_mac == mac }.toLong()

    override suspend fun getLastByMac(mac: String, parameterId: Long, dataCode: Long): DataParameterEntity? =
        rows
            .filter { it.device_mac == mac && it.parameter_id == parameterId && it.data_code == dataCode }
            .maxByOrNull { it.ts_ms }
}

class InMemoryBaseParameterInfoDao : BaseParameterInfoDao {
    private val rows = mutableListOf<BaseParameterInfoEntity>()

    override suspend fun upsert(entity: BaseParameterInfoEntity) {
        rows.removeAll {
            it.device_mac == entity.device_mac &&
                it.device_addr == entity.device_addr &&
                it.parameter_id == entity.parameter_id &&
                it.data_code == entity.data_code
        }
        rows += entity
    }

    override suspend fun getAllForMac(mac: String): List<BaseParameterInfoEntity> =
        rows.filter { it.device_mac == mac }
}

class InMemoryBaseSubDeviceInfoDao : BaseSubDeviceInfoDao {
    private val rows = mutableListOf<BaseSubDeviceInfoEntity>()

    override suspend fun upsert(entity: BaseSubDeviceInfoEntity) {
        rows.removeAll {
            it.device_mac == entity.device_mac &&
                it.sub_device_addr == entity.sub_device_addr
        }
        rows += entity
    }

    override suspend fun getAllForMac(mac: String): List<BaseSubDeviceInfoEntity> =
        rows.filter { it.device_mac == mac }
}

class InMemoryListWidgetsDao : ListWidgetsDao {
    private val rows = mutableMapOf<String, ListWidgetsEntity>()

    override suspend fun upsert(entity: ListWidgetsEntity) {
        rows[entity.device_mac] = entity
    }

    override suspend fun getSnapshot(mac: String): ListWidgetsEntity? = rows[mac]
}

class InMemoryDeviceCrcDao : DeviceCrcDao {
    private val rows = mutableMapOf<Pair<String, Long>, DeviceCrcEntity>()

    override suspend fun upsert(entity: DeviceCrcEntity) {
        rows[entity.device_mac to entity.device_addr] = entity
    }

    override suspend fun load(mac: String, addr: Long): DeviceCrcEntity? = rows[mac to addr]
}

class InMemorySettingsProfileDao : SettingsProfileDao {
    private val profiles = mutableListOf<SettingsProfileEntity>()
    private val values = mutableListOf<SettingsProfileValueEntity>()

    override suspend fun getProfiles(serial: String): List<SettingsProfileEntity> =
        profiles.filter { it.serial_number == serial }.sortedBy { it.profile_id }

    override suspend fun getProfile(serial: String, profileId: Int): SettingsProfileEntity? =
        profiles.firstOrNull { it.serial_number == serial && it.profile_id == profileId }

    override suspend fun getActiveProfile(serial: String): SettingsProfileEntity? =
        profiles
            .filter { it.serial_number == serial && it.is_active }
            .minByOrNull { it.profile_id }

    override suspend fun upsertProfile(entity: SettingsProfileEntity) {
        profiles.removeAll {
            it.serial_number == entity.serial_number &&
                it.profile_id == entity.profile_id
        }
        profiles += entity
    }

    override suspend fun deleteProfiles(serial: String) {
        profiles.removeAll { it.serial_number == serial }
    }

    override suspend fun clearActive(serial: String, tsMs: Long) {
        profiles.replaceAll { entity ->
            if (entity.serial_number == serial) {
                entity.copy(is_active = false, updated_ts_ms = tsMs)
            } else {
                entity
            }
        }
    }

    override suspend fun getValues(serial: String, profileId: Int): List<SettingsProfileValueEntity> =
        values
            .filter { it.serial_number == serial && it.profile_id == profileId }
            .sortedWith(compareBy<SettingsProfileValueEntity> { it.target }.thenBy { it.setting_key })

    override suspend fun upsertValue(entity: SettingsProfileValueEntity) {
        values.removeAll {
            it.serial_number == entity.serial_number &&
                it.profile_id == entity.profile_id &&
                it.setting_key == entity.setting_key
        }
        values += entity
    }

    override suspend fun deleteValues(serial: String) {
        values.removeAll { it.serial_number == serial }
    }

    override suspend fun copyValues(
        serial: String,
        sourceProfileId: Int,
        targetProfileId: Int,
        tsMs: Long
    ) {
        values
            .filter { it.serial_number == serial && it.profile_id == sourceProfileId }
            .forEach { source ->
                upsertValue(
                    source.copy(
                        profile_id = targetProfileId,
                        updated_ts_ms = tsMs
                    )
                )
            }
    }
}

fun ensureWidgetRepoInitializedForTests(mac: String = "TEST-MAC") {
    WidgetRepoProvider.setCurrentMac(mac)
    WidgetRepoProvider.init(
        dataParameterDao = InMemoryDataParameterDao(),
        parameterInfoDao = InMemoryBaseParameterInfoDao(),
        listWidgetsDao = InMemoryListWidgetsDao(),
        subDeviceDao = InMemoryBaseSubDeviceInfoDao(),
        deviceCrcDao = InMemoryDeviceCrcDao()
    )
}
