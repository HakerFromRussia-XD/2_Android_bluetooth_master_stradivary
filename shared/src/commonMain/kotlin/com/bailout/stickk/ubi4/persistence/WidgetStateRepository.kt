package com.bailout.stickk.ubi4.persistence

import com.bailout.stickk.ubi4.db.Ubi4Db
import com.bailout.stickk.ubi4.utility.logging.platformLog

import kotlinx.datetime.Clock


class WidgetStateRepository(private val db: Ubi4Db) {

    fun upsertState(
        deviceAddr: Int,
        widgetId: Int,
        widgetCode: Int,
        parameterId: Int,
        dataCode: Int,
        dataOffset: Int,
        valueText: String?,
        valueI1: Long?, // используем Long, SQL INTEGER = 64-bit
        valueI2: Long?,
        valueI3: Long?,
        tsMs: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        db.widget_stateQueries.upsertState(
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
        platformLog("WidgetRepo", "inserted/updated state for widget=$widgetId param=$parameterId")
        val row = db.widget_stateQueries.selectByKey(
            device_addr = deviceAddr.toLong(),
            widget_id = widgetId.toLong(),
            widget_code = widgetCode.toLong(),
            parameter_id = parameterId.toLong(),
            data_code = dataCode.toLong(),
            data_offset = dataOffset.toLong()
        ).executeAsOneOrNull()

        platformLog(
            "WidgetRepo",
            buildString {
                append("ROW: key=(")
                append("$deviceAddr,$widgetId,$widgetCode,$parameterId,$dataCode,$dataOffset) ")
                append("i1=${row?.value_i1} i2=${row?.value_i2} i3=${row?.value_i3} ")
                append("text=${row?.value_text?.take(24)}...")
            }
        )
    }

    fun getState(
        deviceAddr: Int,
        widgetId: Int,
        widgetCode: Int,
        parameterId: Int,
        dataCode: Int,
        dataOffset: Int
    ) = db.widget_stateQueries.selectByKey(
        device_addr = deviceAddr.toLong(),
        widget_id = widgetId.toLong(),
        widget_code = widgetCode.toLong(),
        parameter_id = parameterId.toLong(),
        data_code = dataCode.toLong(),
        data_offset = dataOffset.toLong()
    ).executeAsOneOrNull()


    fun debugCount() {
        val count = db.widget_stateQueries.countAll().executeAsOne()
        platformLog("WidgetRepo", "DB has $count rows in widget_state")
    }
}