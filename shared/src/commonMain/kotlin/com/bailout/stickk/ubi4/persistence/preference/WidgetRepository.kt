package com.bailout.stickk.ubi4.persistence.preference

import BaseSubDeviceInfoDao
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.db.BaseParameterInfoDao
import com.bailout.stickk.ubi4.data.local.db.BaseParameterInfoEntity
import com.bailout.stickk.ubi4.data.local.db.BaseSubDeviceInfoEntity
import com.bailout.stickk.ubi4.data.local.db.WidgetStateDao
import com.bailout.stickk.ubi4.data.local.db.WidgetStateEntity
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
}

class WidgetRepositoryImpl(
    private val dao: WidgetStateDao,
    private val parameterInfoDao: BaseParameterInfoDao,
    private val subDeviceDao: BaseSubDeviceInfoDao,
    private val cache: WidgetMemoryCache = WidgetMemoryCache(),
) : WidgetRepository {

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
        subDeviceDao.upsert(entity)
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

    override suspend fun clearByDevice(deviceAddr: Long) = withContext(Dispatchers.IO) {
        cache.clearByDevice(mac(), deviceAddr)
        dao.clearByDevice(mac(), deviceAddr)
    }

    override suspend fun count(): Long = withContext(Dispatchers.IO) { dao.count(mac()) }
}