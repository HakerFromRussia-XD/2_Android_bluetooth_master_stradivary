package com.bailout.stickk.ubi4.persistence

import com.bailout.stickk.ubi4.db.Ubi4Db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock


class WidgetStateRepository(private val db: Ubi4Db) {

    /**
     * Сохраняет текущее значение для уникального ключа.
     * Старое перезапишется (INSERT OR REPLACE).
     */
    suspend fun save(
        deviceAddr: Int,
        widgetId: Int,
        widgetCode: Int,
        parameterId: Int,
        dataCode: Int,
        dataOffset: Int,
        valueText: String?,
        v1: Int? = null,
        v2: Int? = null,
        v3: Int? = null
    ) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.widget_stateQueries.upsertState(
            device_addr  = deviceAddr.toLong(),
            widget_id    = widgetId.toLong(),
            widget_code  = widgetCode.toLong(),
            parameter_id = parameterId.toLong(),
            data_code    = dataCode.toLong(),
            data_offset  = dataOffset.toLong(),
            ts_ms        = now,
            value_text   = valueText,
            value_i1     = v1?.toLong(),
            value_i2     = v2?.toLong(),
            value_i3     = v3?.toLong()
        )
    }
}