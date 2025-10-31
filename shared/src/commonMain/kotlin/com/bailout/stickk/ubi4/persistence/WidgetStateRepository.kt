package com.bailout.stickk.ubi4.persistence

import com.bailout.stickk.ubi4.data.local.db.DbProvider
import com.bailout.stickk.ubi4.data.local.db.WidgetStateDao
import com.bailout.stickk.ubi4.data.local.db.WidgetStateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetStateRepository(
    private val dao: WidgetStateDao
) {
    suspend fun upsertState(
        deviceAddr: Int, widgetId: Int, widgetCode: Int,
        parameterId: Int, dataCode: Int, dataOffset: Int,
        tsMs: Long,
        valueText: String?, valueI1: Long?, valueI2: Long?, valueI3: Long?
    ) = withContext(Dispatchers.Default) {
        dao.upsert(
            WidgetStateEntity(
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
    }

    suspend fun selectByKey(
        deviceAddr: Int, widgetId: Int, widgetCode: Int,
        parameterId: Int, dataCode: Int, dataOffset: Int
    ) = dao.selectByKey(
        deviceAddr.toLong(), widgetId.toLong(), widgetCode.toLong(),
        parameterId.toLong(), dataCode.toLong(), dataOffset.toLong()
    )

    suspend fun count(): Long = dao.count()
}

// Глобальный провайдер (как раньше)
object WidgetRepoProvider {
    private val repo by lazy { WidgetStateRepository(DbProvider.instance().widgetStateDao()) }
    fun get(): WidgetStateRepository = repo
}