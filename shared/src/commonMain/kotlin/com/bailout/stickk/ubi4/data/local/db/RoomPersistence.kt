package com.bailout.stickk.ubi4.data.local.db

import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.widget.subStructures.*
import com.bailout.stickk.ubi4.data.widget.endStructures.*
import com.bailout.stickk.ubi4.persistence.preference.WidgetRepoProvider
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.utility.logging.platformLog
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

object RoomPersistence {

    /** Сохранить ВСЕ master-параметры после их полного чтения. (мастер = 0) */
    fun persistAllMasterParams(scope: CoroutineScope) {
        val repo = WidgetRepoProvider.get()
        val ts = getTimeMillis()

        scope.launch(Dispatchers.IO) {
            var saved = 0
            baseParametrInfoStructArray.forEach { info ->
                repo.upsertParameterInfo(
                    deviceAddr  = 0,
                    parameterId = info.ID,
                    dataCode    = info.dataCode,
                    tsMs        = ts,
                    info        = info
                )
                saved++
            }
            platformLog("ROOM_PERSIST", "master params saved: count=$saved")
        }
    }

    /** Сохранить все саб-девайсы (bulk) после того, каких распарсил. */
    fun persistAllSubDevices(scope: CoroutineScope) {
        val repo = WidgetRepoProvider.get()
        val ts = getTimeMillis()

        scope.launch(Dispatchers.IO) {
            baseSubDevicesInfoStructSet.forEach { sub ->
                repo.upsertSubDevice(
                    deviceAddr = 0,   // мастер как контекст; адрес саба внутри payload и sub_device_addr
                    sub = sub,
                    tsMs = ts
                )
            }
            platformLog("ROOM_PERSIST", "bulk saved: count=${baseSubDevicesInfoStructSet.size}")
        }
    }

    /**
     * Точечное сохранение апдейта параметра + связанных widget_state.
     * Вызывай каждый раз, когда пришли новые данные по (deviceAddr, parameterId, dataCode).
     */
    fun persistParamUpdate(
        scope: CoroutineScope,
        deviceAddr: Int,
        parameterId: Int,
        dataCode: Int,
        listWidgets: List<Any>
    ) {
        val repo = WidgetRepoProvider.get()
        val p = ParameterProvider.getParameter(deviceAddr, parameterId)
        val raw = p.data
        val ts = getTimeMillis()


        platformLog(
            "ROOM_PERSIST",
            "persistParamUpdate → device=$deviceAddr, param=$parameterId, code=$dataCode, rawLen=${raw.length}"
        )
        // 1) сохраняем BaseParameterInfo
        scope.launch(Dispatchers.IO) {
            repo.upsertParameterInfo(
                deviceAddr = deviceAddr,
                parameterId = parameterId,
                dataCode = dataCode,
                tsMs = ts,
                info = p
            )
        }

        // 2) для всех виджетов, которые «подвязаны» к этому параметру — upsert в widget_state
        listWidgets.forEach { w ->
            val base = when (w) {
                is BaseParameterWidgetEStruct -> w.baseParameterWidgetStruct
                is BaseParameterWidgetSStruct -> w.baseParameterWidgetStruct
                is CommandParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is CommandParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                is PlotParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is PlotParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                is SliderParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is SliderParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                is SwitchParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is SwitchParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                else -> null
            } ?: return@forEach

            base.parameterInfoSet
                .asSequence()
                .filter { it.deviceAddress == deviceAddr && it.parameterID == parameterId && it.dataCode == dataCode }
                .forEach { info ->
                    val b = hexByteAt(raw, info.dataOffset) ?: 0
                    scope.launch(Dispatchers.IO) {
                        repo.upsertState(
                            deviceAddr  = deviceAddr,
                            widgetId    = base.widgetId,
                            widgetCode  = base.widgetCode,
                            parameterId = parameterId,
                            dataCode    = dataCode,
                            dataOffset  = info.dataOffset,
                            tsMs        = ts,
                            valueText   = raw,
                            valueI1     = b.toLong(),
                            valueI2     = null,
                            valueI3     = null
                        )
                    }
                }
        }
    }

    /** Безопасно достать байт из hex-строки по смещению (в байтах), вернуть null если не влезает. */
    private fun hexByteAt(raw: String, offset: Int): Int? {
        val i = offset * 2
        return if (i + 2 <= raw.length) raw.substring(i, i + 2).toInt(16) else null
    }
}

