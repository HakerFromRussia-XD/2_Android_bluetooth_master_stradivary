package com.bailout.stickk.ubi4.persistence.preference

import BaseSubDeviceInfoDao
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.db.BaseParameterInfoDao
import com.bailout.stickk.ubi4.data.local.db.BaseParameterInfoEntity
import com.bailout.stickk.ubi4.data.local.db.BaseSubDeviceInfoEntity
import com.bailout.stickk.ubi4.data.local.db.ListWidgetsDao
import com.bailout.stickk.ubi4.data.local.db.ListWidgetsEntity
import com.bailout.stickk.ubi4.data.local.db.WidgetStateDao
import com.bailout.stickk.ubi4.data.local.db.WidgetStateEntity
import com.bailout.stickk.ubi4.data.local.db.payload.BaseParameterInfoPayload
import com.bailout.stickk.ubi4.data.local.db.payload.BaseParameterWidgetPayload
import com.bailout.stickk.ubi4.data.local.db.payload.BaseSubDeviceInfoPayload
import com.bailout.stickk.ubi4.data.local.db.payload.toModel
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface WidgetRepository {
    suspend fun upsert(entity: WidgetStateEntity)
    suspend fun upsertBatch(entities: List<WidgetStateEntity>)
    suspend fun upsertState(
        deviceAddr: Int,
        widgetId: Int,
        widgetCode: Int,
        parameterId: Int,
        dataCode: Int,
        dataOffset: Int,
        tsMs: Long,
        valueText: String?,
        valueI1: Long?,
        valueI2: Long?,
        valueI3: Long?
    )

    suspend fun upsertParameterInfo(
        deviceAddr: Int,
        parameterId: Int,
        dataCode: Int,
        tsMs: Long,
        info: BaseParameterInfoStruct
    )

    suspend fun upsertSubDevice(
        deviceAddr: Int,
        sub: BaseSubDeviceInfoStruct,
        tsMs: Long
    )

    fun observeByWidget(widgetId: Long): Flow<List<WidgetStateEntity>>
    fun observeByKey(
        deviceAddr: Long, widgetId: Long, widgetCode: Long,
        parameterId: Long, dataCode: Long, dataOffset: Long
    ): Flow<WidgetStateEntity?>
    suspend fun clearByDevice(deviceAddr: Long)
    suspend fun count(): Long
    fun observeAll(): Flow<List<WidgetStateEntity>>

    suspend fun upsertWidgetsSnapshot(
        deviceAddr: Int,
        widgets: List<Any>
    )

    suspend fun loadAllMasterParams(deviceAddr: Int): List<BaseParameterInfoStruct>

    suspend fun loadAllSubDevices(deviceAddr: Int): Set<BaseSubDeviceInfoStruct>

    suspend fun loadWidgetsSnapshot(mac: String): List<BaseParameterWidgetPayload>?

    suspend fun loadLastState(
        deviceAddr: Int,
        parameterId: Int,
        dataCode: Int
    ): WidgetStateEntity?
}

class WidgetRepositoryImpl(
    private val dao: WidgetStateDao,
    private val parameterInfoDao: BaseParameterInfoDao,
    private val subDeviceDao: BaseSubDeviceInfoDao?,
    private val listWidgetsDao: ListWidgetsDao,
    private val cache: WidgetMemoryCache = WidgetMemoryCache(),
) : WidgetRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private fun mac() = WidgetRepoProvider.mac()

    override suspend fun upsert(entity: WidgetStateEntity) = withContext(Dispatchers.IO) {
        val e = entity.copy(device_mac = mac())
        cache.put(mac(), e)
        dao.upsert(e)
    }

    override suspend fun upsertBatch(entities: List<WidgetStateEntity>) = withContext(Dispatchers.IO) {
        if (entities.isEmpty()) return@withContext
        val withMac = entities.map { it.copy(device_mac = mac()) }
        cache.putAll(mac(), withMac)
        for (e in withMac) dao.upsert(e)
    }

    override suspend fun upsertState(
        deviceAddr: Int, widgetId: Int, widgetCode: Int,
        parameterId: Int, dataCode: Int, dataOffset: Int,
        tsMs: Long, valueText: String?, valueI1: Long?, valueI2: Long?, valueI3: Long?
    ) = upsert(
        WidgetStateEntity(
            device_mac = mac(),
            device_addr = deviceAddr.toLong(),
            widget_id = widgetId.toLong(),
            widget_code = widgetCode.toLong(),
            parameter_id = parameterId.toLong(),
            data_code = dataCode.toLong(),
            data_offset = dataOffset.toLong(),
            ts_ms = tsMs,
            value_text = valueText,
            value_i1 = valueI1,
            value_i2 = valueI2,
            value_i3 = valueI3
        )
    )

    override suspend fun upsertParameterInfo(
        deviceAddr: Int,
        parameterId: Int,
        dataCode: Int,
        tsMs: Long,
        info: BaseParameterInfoStruct
    ) = withContext(Dispatchers.IO) {
        val entity = BaseParameterInfoEntity.create(
            mac = mac(),
            deviceAddr = deviceAddr,
            parameterId = parameterId,
            dataCode = dataCode,
            tsMs = tsMs,
            info = info
        )
        parameterInfoDao.upsert(entity)
        platformLog(
            "PARAM_INFO_DB",
            "upsert ok: mac=${mac()} addr=$deviceAddr pid=$parameterId dcode=$dataCode ts=$tsMs data.len=${info.data.length} widgets=${info.additionalInfoRefSet}"
        )
    }

    override suspend fun upsertSubDevice(
        deviceAddr: Int,
        sub: BaseSubDeviceInfoStruct,
        tsMs: Long
    ) = withContext(Dispatchers.IO) {
        val entity = BaseSubDeviceInfoEntity.create(
            mac = mac(),
            tsMs = tsMs,
            sub = sub
        )
        subDeviceDao?.upsert(entity)
        platformLog(
            "SUBDEV_INFO_DB",
            "upsert ok: mac=${mac()} addr=$deviceAddr subAddr=${sub.deviceAddress} ts=$tsMs params=${sub.parametersList.size}"
        )
    }

    override fun observeByWidget(widgetId: Long): Flow<List<WidgetStateEntity>> =
        dao.observeByWidget(mac(), widgetId)

    override fun observeByKey(
        deviceAddr: Long, widgetId: Long, widgetCode: Long,
        parameterId: Long, dataCode: Long, dataOffset: Long
    ): Flow<WidgetStateEntity?> =
        dao.observeByKey(mac(), deviceAddr, widgetId, widgetCode, parameterId, dataCode, dataOffset)

    override fun observeAll(): Flow<List<WidgetStateEntity>> =
        dao.observeAll(mac())

    override suspend fun upsertWidgetsSnapshot(
        deviceAddr: Int,
        widgets: List<Any>
    ) = withContext(Dispatchers.IO) {
        val entity = ListWidgetsEntity.create(
            mac = mac(),
            deviceAddr = deviceAddr,
            widgets = widgets
        )
        listWidgetsDao.upsert(entity)
        platformLog(
            "DB_WRITE_WIDGETS",
            "snapshot: mac=${mac()} dev=$deviceAddr widgets=${widgets.size}"
        )
    }

    override suspend fun loadAllMasterParams(deviceAddr: Int): List<BaseParameterInfoStruct> =
        withContext(Dispatchers.IO) {
            val mac = mac()

            // тащим все строки по mac
            val rows = parameterInfoDao.getAllForMac(mac)
            platformLog("BOOTSTRAP_DB", "base_parameter_info rows=${rows.size} for mac=$mac")

            rows.mapNotNull { entity ->
                runCatching {
                    val payload = json.decodeFromString(
                        BaseParameterInfoPayload.serializer(),
                        entity.payload
                    )
                    payload.toModel()
                }.onFailure {
                    platformLog("BOOTSTRAP_DB", "decode PARAM_INFO error: ${it.message}")
                }.getOrNull()
            }
        }

    override suspend fun loadAllSubDevices(deviceAddr: Int): Set<BaseSubDeviceInfoStruct> =
        withContext(Dispatchers.IO) {
            val mac = mac()
            val daoLocal = subDeviceDao ?: return@withContext emptySet()

            // Тут используй тот метод, который ты добавишь в DAO:
            // либо getAllForMac(mac), либо getAllForMaster(mac, deviceAddr.toLong())
            val rows = daoLocal.getAllForMac(mac)

            rows.mapNotNull { entity ->
                runCatching {
                    val payload = json.decodeFromString(
                        BaseSubDeviceInfoPayload.serializer(),
                        entity.payload
                    )
                    payload.toModel()          // BaseSubDeviceInfoPayload.toModel()
                }.getOrNull()
            }.toSet()
        }

    override suspend fun loadWidgetsSnapshot(mac: String): List<BaseParameterWidgetPayload>? =
        withContext(Dispatchers.IO) {

            val row = listWidgetsDao.getSnapshot(mac) ?: return@withContext null

            runCatching {
                json.decodeFromString(
                    ListSerializer(BaseParameterWidgetPayload.serializer()),
                    row.payload
                )
            }.onFailure {
                platformLog("DB_READ_WIDGETS", "decode error: ${it.message}")
            }.getOrNull()
        }

    override suspend fun loadLastState(
        deviceAddr: Int,
        parameterId: Int,
        dataCode: Int
    ): WidgetStateEntity? =
        withContext(Dispatchers.IO) {
            val mac = mac()
            val row = dao.getLastByMac(
                mac        = mac,
                parameterId = parameterId.toLong(),
                dataCode    = dataCode.toLong()
            )
            if (row == null) {
                platformLog(
                    "BOOTSTRAP_DB",
                    "no last state: mac=$mac pid=$parameterId dcode=$dataCode"
                )
            }
            row
        }

    override suspend fun clearByDevice(deviceAddr: Long) = withContext(Dispatchers.IO) {
        cache.clearByDevice(mac(), deviceAddr)
        dao.clearByDevice(mac(), deviceAddr)
    }

    override suspend fun count(): Long = withContext(Dispatchers.IO) { dao.count(mac()) }
}