package com.bailout.stickk.ubi4.data.local.db

import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.widget.subStructures.*
import com.bailout.stickk.ubi4.data.widget.endStructures.*
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
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

        // 2) для всех виджетов, которые подвязаны к этому параметру — upsert в widget_state
        var rows = 0

        listWidgets.forEach { w ->
            val base = when (w) {
                is BaseParameterWidgetEStruct   -> w.baseParameterWidgetStruct
                is BaseParameterWidgetSStruct   -> w.baseParameterWidgetStruct
                is GestureParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is GestureOpticParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is CommandParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is CommandParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                is PlotParameterWidgetEStruct   -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is PlotParameterWidgetSStruct   -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                is SliderParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is SliderParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                is SwitchParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is SwitchParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                is ThresholdParameterWidgetEStruct -> w.baseParameterWidgetEStruct.baseParameterWidgetStruct
                is ThresholdParameterWidgetSStruct -> w.baseParameterWidgetSStruct.baseParameterWidgetStruct
                else -> null
            } ?: return@forEach

            base.parameterInfoSet
                .asSequence()
                .filter { it.deviceAddress == deviceAddr && it.parameterID == parameterId && it.dataCode == dataCode }
                .forEach { info ->
                    val b = hexByteAt(raw, info.dataOffset) ?: 0
                    rows++

                    platformLog(
                        "ROOM_PERSIST",
                        "widget_state WRITE → mac=${WidgetRepoProvider.mac()} dev=$deviceAddr " +
                                "wid=${base.widgetId} wcode=${base.widgetCode} " +
                                "pid=$parameterId dcode=$dataCode offset=${info.dataOffset} " +
                                "byte=$b raw=$raw"
                    )

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

        if (rows == 0) {
            platformLog(
                "ROOM_PERSIST",
                "widget_state SKIP → НИ ОДНОГО виджета не найдено " +
                        "для dev=$deviceAddr param=$parameterId code=$dataCode raw=$raw"
            )
        }
    }
    fun persisListWidgets(
        scope: CoroutineScope,
        deviceAddr: Int,
        listWidgets: List<Any>
    ) {
        logWidgetsSnapshot("IN-MEM listWidgets before save", listWidgets)

        val repo = WidgetRepoProvider.get()

        scope.launch(Dispatchers.IO) {
            repo.upsertWidgetsSnapshot(
                deviceAddr = deviceAddr,
                widgets = listWidgets
            )
        }

        platformLog("WIDGET_PERSIST", "saveWidgets: deviceAddr=$deviceAddr size=${listWidgets.size}")

        listWidgets.forEachIndexed { index, w ->
            platformLog("WIDGET_PERSIST", "[$index] ${extractKey(w)} type=${w::class.simpleName}")
        }
    }


//    suspend fun loadDeviceCrc(deviceAddr: Int): Long? {
//        val repo = WidgetRepoProvider.get()
//        return repo.loadDeviceCrc(deviceAddr)
//    }
//
//    /** Безопасно достать байт из hex-строки по смещению (в байтах), вернуть null если не влезает. */
    private fun hexByteAt(raw: String, offset: Int): Int? {
        val i = offset * 2
        return if (i + 2 <= raw.length) raw.substring(i, i + 2).toInt(16) else null
    }
//
//    fun persistDeviceCrc(
//        scope: CoroutineScope,
//        deviceAddr: Int,
//        crc: Long
//    ) {
//        val repo = WidgetRepoProvider.get()
//        val ts = getTimeMillis()
//
//        platformLog(
//            "ROOM_PERSIST",
//            "device_crc WRITE → mac=${WidgetRepoProvider.mac()} dev=$deviceAddr crc=$crc"
//        )
//
//        scope.launch(Dispatchers.IO) {
//            repo.upsertDeviceCrc(
//                deviceAddr = deviceAddr,
//                crc        = crc,
//                tsMs       = ts
//            )
//        }
//    }

    suspend fun loadDeviceCrc(deviceAddr: Int): Long? {
        val repo = WidgetRepoProvider.get()
        val mac = WidgetRepoProvider.mac()


        platformLog(
            "ROOM_PERSIST",
            "device_crc READ → mac=$mac dev=$deviceAddr"
        )

        return repo.loadDeviceCrc(
            mac  = mac,
            addr = deviceAddr
        )
    }

    fun persistDeviceCrc(
        scope: CoroutineScope,
        deviceAddr: Int,
        crc: Long
    ) {
        val repo = WidgetRepoProvider.get()
        val mac = WidgetRepoProvider.mac()
        val ts = getTimeMillis()

        platformLog(
            "ROOM_PERSIST",
            "device_crc WRITE → mac=$mac dev=$deviceAddr crc=$crc"
        )

        scope.launch(Dispatchers.IO) {
            repo.upsertDeviceCrc(
                mac       = mac,
                deviceAddr = deviceAddr,
                crc        = crc,
                tsMs       = ts
            )
        }
    }
}




fun extractKey(widget: Any): String {
    val base = when (widget) {
        is BaseParameterWidgetEStruct   -> widget.baseParameterWidgetStruct
        is BaseParameterWidgetSStruct   -> widget.baseParameterWidgetStruct
        is SliderParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SliderParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is PlotParameterWidgetEStruct   -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is PlotParameterWidgetSStruct   -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is SwitchParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SwitchParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is ThresholdParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is ThresholdParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is CommandParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is CommandParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is GestureParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is OpticStartLearningWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
        else -> return "NO_BASE"
    }

    val param = base.parameterInfoSet.firstOrNull()
    return if (param != null) {
        "dev=${param.deviceAddress} wid=${base.widgetId} wcode=${base.widgetCode} " +
                "pid=${param.parameterID} dcode=${param.dataCode} offset=${param.dataOffset}"
    } else {
        "dev=${base.deviceId} wid=${base.widgetId} wcode=${base.widgetCode} NO_PARAM"
    }
}

fun logWidgetsSnapshot(title: String, list: List<Any>) {
    val head = list.take(5).joinToString { it::class.simpleName ?: "Unknown" }
    platformLog(
        "SNAPSHOT_WIDGETS",
        "$title: count=${list.size} head=[ $head${if (list.size > 5) ", …" else ""} ]"
    )



}