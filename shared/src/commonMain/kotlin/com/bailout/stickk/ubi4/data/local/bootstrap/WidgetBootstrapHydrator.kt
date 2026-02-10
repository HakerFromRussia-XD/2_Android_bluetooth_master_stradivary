package com.bailout.stickk.ubi4.data.local.bootstrap

import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.local.db.RoomPersistence
import com.bailout.stickk.ubi4.data.local.db.extractKey
import com.bailout.stickk.ubi4.data.local.db.payload.BaseParameterWidgetPayload
import com.bailout.stickk.ubi4.data.local.db.payload.toEndStruct
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.data.state.RestoredState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.GestureOpticParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.GestureParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ThresholdParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ThresholdParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetBootstrapHydrator {

    /**
     * 1) Читаем всё из Room:
     *    - master-параметры
     *    - сабдевайсы
     *    - snapshot виджетов
     *
     * 2) Заполняем RestoredState, GlobalParameters и UiState.listWidgets.
     */
    suspend fun restoreFromDb(deviceAddr: Int) {
        val repo = WidgetRepoProvider.get()

        val mac = WidgetRepoProvider.mac()
        if (mac.isBlank()) {
            platformLog("WIDGET_SOURCE", "restoreFromDb: mac is blank → кеш не используем")
            return
        }

        val crcFromDb = RoomPersistence.loadDeviceCrc(deviceAddr)
        if (crcFromDb == null) {
            platformLog(
                "WIDGET_SOURCE",
                "restoreFromDb: mac=$mac dev=$deviceAddr → CRC нет в БД, пропускаем восстановление из кеша"
            )
            return
        }
        platformLog("WIDGET_SOURCE", "restoreFromDb: mac=$mac → читаем из БД")

        // 1. Параметры мастера
        val paramsFromDb = repo.loadAllMasterParams(deviceAddr)
        RestoredState.baseParamsFromDb = paramsFromDb.toMutableList()

        GlobalParameters.baseParameterInfoStructArray.clear()
        GlobalParameters.baseParameterInfoStructArray.addAll(paramsFromDb)

        // 2. Сабдевайсы
        val subsFromDb = repo.loadAllSubDevices(deviceAddr)
        RestoredState.subDevicesFromDb = subsFromDb.toMutableSet()

        GlobalParameters.baseSubDevicesInfoStructSet.clear()
        GlobalParameters.baseSubDevicesInfoStructSet.addAll(subsFromDb)

        // 3. Snapshot ВИДЖЕТОВ ТЕПЕРЬ ГРУЗИМ ПО MAC
        val payloads: List<BaseParameterWidgetPayload> =
            repo.loadWidgetsSnapshot(mac) ?: emptyList()

        val restored = payloads.map { it.toEndStruct() }.toMutableSet()
        UiState.listWidgets = restored
        platformLog("WIDGET_PERSIST", "restoreWidgets: mac=$mac size=${restored.size}")
        restored.forEachIndexed { index, w ->
            platformLog(
                "WIDGET_PERSIST",
                "RESTORE[$index] ${extractKey(w)} type=${w::class.simpleName}"
            )
        }
        platformLog(
            "WIDGET_SOURCE",
            "restoreFromDb: mac=$mac widgets_from_DB=${restored.size}"
        )

        platformLog(
            "BOOTSTRAP_TETS",
            "restoreFromDb: dev=$deviceAddr, params=${paramsFromDb.size}, subs=${subsFromDb.size}, widgets=${restored.size}"
        )

        ConnectionState.fullInicializeConnectionStruct = FullInicializeConnectionStruct(
            deviceName = "Cached device",
            deviceVersion = 0,
            deviceSubVersion = 0,
            deviceLabel = "",
            deviceType = 0,
            deviceCode = 0,
            deviceAddress = deviceAddr,
            deviceUUID_Prefix = "",
            deviceUUID = 0L,
            parametersNum = paramsFromDb.size,
            subDeviceNum = subsFromDb.size,
            programType = 0,
            defaultPort = 0
        )

        platformLog(
            "BOOTSTRAP_FAKE_INIT",
            "FullInit from cache: dev=$deviceAddr params=${paramsFromDb.size}, subs=${subsFromDb.size}"
        )

    }

    /**
     * Заполнить ParameterProvider из БД:
     * - тип параметра
     * - dataCode
     * - размер
     * - последнее value_text из widget_state (если есть)
     */
    suspend fun hydrateParameterProviderFromDb(masterAddr: Int) {
        val repo = WidgetRepoProvider.get()

        // 1. Параметры мастера
        val masterParams = repo.loadAllMasterParams(masterAddr)

        // 2. Сабдевайсы (и их параметры)
        val subDevices = repo.loadAllSubDevices(masterAddr)

        var hydratedCount = 0

        withContext(Dispatchers.Default) {
            // --- мастер ---
            masterParams.forEach { info ->
                val p = ParameterProvider.Companion.getParameter(masterAddr, info.ID)
                if (info.dataCode == PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number) {
                    platformLog(
                        "BOOTSTRAP_BINDING",
                        "try hydrate binding:  pid=${info.ID} dcode=${info.dataCode}"
                    )
                }
                val lastState = repo.loadLastState(
                    deviceAddr = masterAddr,
                    parameterId = info.ID,
                    dataCode = info.dataCode
                )

                if (lastState?.value_text != null) {
                    p.data = lastState.value_text
                    p.firstReceiveDataFlag = false
                    hydratedCount++
                    platformLog(
                        "BOOTSTRAP_DB",
                        "master hydrated: addr=$masterAddr pid=${info.ID} dcode=${info.dataCode} value=${lastState.value_text}"
                    )
                } else {
                    platformLog(
                        "BOOTSTRAP_DB",
                        "no last state (master): addr=$masterAddr pid=${info.ID} dcode=${info.dataCode}"
                    )
                }
                //  ДОБАВЛЯЕМ ВОССТАНОВЛЕНИЕ АКТИВНОГО ЖЕСТА
                if (info.dataCode == PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number) {
                    val gestureId: Int? =
                        lastState?.value_i1?.toInt() ?: lastState?.value_text?.toIntOrNull()
                    WidgetState.activeGestureState.value = gestureId
                    platformLog(
                        "BOOTSTRAP_GESTURE",
                        "hydrate: activeGestureState from DB: addr=$masterAddr pid=${info.ID} gestureId=$gestureId"
                    )
                }



            }


            // --- сабдевайсы ---
            subDevices.forEach { sub ->
                val subAddr = sub.deviceAddress

                sub.parametersList.forEach { info ->
                    val p = ParameterProvider.Companion.getParameter(subAddr, info.ID)

                    val lastState = repo.loadLastState(
                        deviceAddr = subAddr,
                        parameterId = info.ID,
                        dataCode = info.dataCode
                    )

                    if (lastState?.value_text != null) {
                        p.data = lastState.value_text
                        p.firstReceiveDataFlag = false
                        hydratedCount++
                        platformLog(
                            "BOOTSTRAP_DB",
                            "sub hydrated: addr=$subAddr pid=${info.ID} dcode=${info.dataCode} value=${lastState.value_text}"
                        )
                    } else {
                        platformLog(
                            "BOOTSTRAP_DB",
                            "no last state (sub): addr=$subAddr pid=${info.ID} dcode=${info.dataCode}"
                        )
                    }
                }
            }
        }

        platformLog(
            "BOOTSTRAP",
            "hydrateParameterProviderFromDb: master=$masterAddr " +
                    "masterParams=${masterParams.size}, subDevices=${subDevices.size}, hydrated=$hydratedCount"
        )
    }

    /**
     * Эмулируем события, как будто BLE прислал уведомления.
     * Это "будит" адаптеры (Slider, Switch, Plot и т.д.).
     */
    suspend fun replayWidgetEventsFromDb(deviceAddr: Int) {
        UiState.listWidgets.forEach { widget ->
            val base = widget.baseStructOrNull() ?: return@forEach

            if (base.widgetCode == PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT.number.toInt()) {
                platformLog(
                    "PLOT_BOOTSTRAP",
                    "replay: dev=${base.deviceId} wid=${base.widgetId} display=${base.display} params=${base.parameterInfoSet.size}"
                )
            }

            // ВАЖНО: идём по всем параметрам виджета, а не только по первому
            base.parameterInfoSet.forEach { info ->
                val ref = ParameterRef(
                    addressDevice = info.deviceAddress,
                    parameterID = info.parameterID,
                    dataCode = info.dataCode
                )

                when (base.widgetCode) {
                    PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt(),
                    PreferenceKeysUbi4.ParameterWidgetCode.PWCE_TOGGLE_SLIDER.number.toInt() -> {
                        WidgetState.slidersFlow.tryEmit(ref)
                    }

                    PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
                        WidgetState.switcherFlow.tryEmit(ref)
                    }

                    PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT.number.toInt(),
                    PreferenceKeysUbi4.ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                        platformLog(
                            "PLOT_BOOTSTRAP",
                            "emit threshold/merge: dev=${ref.addressDevice} pid=${ref.parameterID} dcode=${ref.dataCode}"
                        )
                        WidgetState.thresholdFlow.tryEmit(ref)
                        WidgetState.widgetsMergeEventFlow.tryEmit(ref)
                    }
                }

                // А вот это — завязка на конкретные dataCode
                when (info.dataCode) {
                    PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number -> {
                        WidgetState.rotationGroupFlow.tryEmit(ref)
                    }

                    PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number -> {
                        WidgetState.bindingGroupFlow.tryEmit(ref)
                    }
                }
            }
        }

        platformLog(
            "BOOTSTRAP",
            "replayWidgetEventsFromDb: dev=$deviceAddr widgets=${UiState.listWidgets.size}"
        )
        platformLog("PLOT_BOOTSTRAP", "emit updateFlow")
        UiState.updateFlow.tryEmit(0)
    }

    // ——— Вспомогательный метод: достать BaseParameterWidgetStruct из любого endStruct ———
    suspend fun rebuildParameterLinksFromDb(masterAddr: Int) {
        // На всякий случай не чистим ничего в ParameterProvider —
        // fast-путь вызывается один раз после восстановления.
        var linksCount = 0

        UiState.listWidgets.forEach { widget ->
            val base = widget.baseStructOrNull() ?: return@forEach

            // parameterInfoSet мы уже сохранили в БД и подняли обратно
            base.parameterInfoSet.forEach { info ->
                if (
                    base.widgetCode == PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT.number.toInt() &&
                    info.dataCode == PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number
                ) {
                    return@forEach
                }

                val param = ParameterProvider.getParameter(
                    info.deviceAddress,
                    info.parameterID
                )
                // additionalInfoRefSet — это как раз тот набор, который читает updateAllUI
                if (param.additionalInfoRefSet.add(base)) {
                    linksCount++
                }
            }
        }

        platformLog(
            "BOOTSTRAP",
            "rebuildParameterLinksFromDb: master=$masterAddr widgets=${UiState.listWidgets.size} links=$linksCount"
        )
    }

    fun requestParametersDataKmm(
        sendCommand: (ByteArray) -> Unit
    ) {
        // чтобы не долбить один и тот же параметр по 10 раз
        val requested = mutableSetOf<Triple<Int, Int, Int>>()  // (addr, pid, dcode)
        // ---------------------------------------------------------
        // 1. ЗАПРОСЫ ОТ ВИДЖЕТОВ
        // ---------------------------------------------------------
        UiState.listWidgets.forEach { widget ->
            val primary = widget.primaryParamOrNull() ?: run {
                platformLog(
                    "WIDGET_REQ",
                    "skip widget=${widget::class.simpleName} (нет параметров)"
                )
                return@forEach
            }

            val key = Triple(primary.deviceAddr, primary.parameterId, primary.dataCode)
            if (!requested.add(key)) {
                platformLog(
                    "WIDGET_REQ",
                    "dup skip dev=${primary.deviceAddr} pid=${primary.parameterId} dcode=${primary.dataCode}"
                )
                return@forEach
            }

            when (widget) {

                // ---------- SLIDER ----------
                is SliderParameterWidgetEStruct,
                is SliderParameterWidgetSStruct,
                is ToggleSliderParameterWidgetEStruct,
                is ToggleSliderParameterWidgetSStruct -> {
                    val cmd = BLECommands.requestSlider(primary.deviceAddr, primary.parameterId)
                    sendCommand(cmd)
                    platformLog(
                        "WIDGET_REQ",
                        "SLIDER/TOGGLE_SLIDER: dev=${primary.deviceAddr} pid=${primary.parameterId}"
                    )
                }

                // ---------- SWITCH ----------
                is SwitchParameterWidgetEStruct,
                is SwitchParameterWidgetSStruct -> {
                    val cmd = BLECommands.requestSwitcher(primary.deviceAddr, primary.parameterId)
                    sendCommand(cmd)
                    platformLog(
                        "WIDGET_REQ",
                        "SWITCHER: dev=${primary.deviceAddr} pid=${primary.parameterId}"
                    )
                }

                // ---------- THRESHOLD ----------
                is ThresholdParameterWidgetEStruct,
                is ThresholdParameterWidgetSStruct -> {
                    val cmd = BLECommands.requestThresholds(primary.deviceAddr, primary.parameterId)
                    sendCommand(cmd)
                    platformLog(
                        "WIDGET_REQ",
                        "THRESHOLD: dev=${primary.deviceAddr} pid=${primary.parameterId}"
                    )
                }

                // ---------- GESTURE (селектор активного жеста как виджет) ----------
                is GestureParameterWidgetEStruct -> {
                    val cmd = BLECommands.requestActiveGesture(primary.deviceAddr, primary.parameterId)
                    sendCommand(cmd)
                    platformLog(
                        "WIDGET_REQ",
                        "ACTIVE_GESTURE(widget): dev=${primary.deviceAddr} pid=${primary.parameterId}"
                    )
                }

                // ---------- OPTIC BINDING как виджет ----------
                is GestureOpticParameterWidgetEStruct -> {
                    val cmd = BLECommands.requestBindingGroup(primary.deviceAddr, primary.parameterId)
                    sendCommand(cmd)
                    platformLog(
                        "WIDGET_REQ",
                        "OPTIC_BINDING(widget): dev=${primary.deviceAddr} pid=${primary.parameterId}"
                    )
                }

                // ---------- BASE WIDGETS — тут как раз РОТАЦИИ, БИНДИНГИ, БАТАРЕЙКА ----------
                is BaseParameterWidgetEStruct,
                is BaseParameterWidgetSStruct -> {
                    when (primary.dataCode) {

                        // ROTATION GROUP
                        PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number -> {
                            val cmd = BLECommands.requestRotationGroup(primary.deviceAddr, primary.parameterId)
                            sendCommand(cmd)
                            platformLog(
                                "WIDGET_REQ",
                                "ROTATION_GROUP(widget): dev=${primary.deviceAddr} pid=${primary.parameterId}"
                            )
                        }

                        // BINDING GROUP
                        PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number -> {
                            val cmd = BLECommands.requestBindingGroup(primary.deviceAddr, primary.parameterId)
                            sendCommand(cmd)
                            platformLog(
                                "WIDGET_REQ",
                                "BINDING_GROUP(widget): dev=${primary.deviceAddr} pid=${primary.parameterId}"
                            )
                        }


                        else -> {
                            platformLog(
                                "WIDGET_REQ",
                                "BASE_WIDGET(widget): dev=${primary.deviceAddr} pid=${primary.parameterId} dcode=${primary.dataCode} → skip"
                            )
                        }
                    }
                }

                // ---------- Остальное ----------
                else -> {
                    platformLog("WIDGET_REQ", "UNHANDLED widget=${widget::class.simpleName} → skip")
                }
            }
        }

        GlobalParameters.baseSubDevicesInfoStructSet.forEach { sub ->
            val addr = sub.deviceAddress

            sub.parametersList.forEach { info ->
                val pid   = info.ID
                val dcode = info.dataCode

                val key = Triple(addr, pid, dcode)
                if (!requested.add(key)) return@forEach

                when (dcode) {
                    PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number -> {
                        val cmd = BLECommands.requestActiveGesture(addr, pid)
                        sendCommand(cmd)
                        platformLog("WIDGET_REQ", "ACTIVE_GESTURE(sub): addr=$addr pid=$pid")
                    }

                    PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number -> {
                        val cmd = BLECommands.requestBindingGroup(addr, pid)
                        sendCommand(cmd)
                        platformLog("WIDGET_REQ", "BINDING_GROUP(sub): addr=$addr pid=$pid")
                    }

                    PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number -> {
                        val cmd = BLECommands.requestRotationGroup(addr, pid)
                        sendCommand(cmd)
                        platformLog("WIDGET_REQ", "ROTATION_GROUP(sub): addr=$addr pid=$pid")
                    }
                }
            }
        }
    }
}

    private fun Any.baseStructOrNull() =
        when (this) {
            is BaseParameterWidgetEStruct -> this.baseParameterWidgetStruct
            is BaseParameterWidgetSStruct -> this.baseParameterWidgetStruct

            // CODE_LABEL виджеты
            is CommandParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
            is SwitchParameterWidgetEStruct  -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
            is SliderParameterWidgetEStruct  -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
            is PlotParameterWidgetEStruct    -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
            is SpinnerParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
            is OpticStartLearningWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
            is ThresholdParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
            is ToggleSliderParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct


            // STRING_LABEL виджеты
            is CommandParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            is SwitchParameterWidgetSStruct  -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            is SliderParameterWidgetSStruct  -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            is PlotParameterWidgetSStruct    -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            is SpinnerParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            is OpticStartLearningWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            is ThresholdParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            is ToggleSliderParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
            // На всякий случай — чистые Base-виджеты, если они вдруг лежат напрямую
            is BaseParameterWidgetStruct -> this
            else -> null
        }

    fun Any.primaryParamOrNull(): WidgetParamRef? {
        val base = baseStructOrNull() ?: return null
        val info = base.parameterInfoSet.firstOrNull() ?: return null

        return WidgetParamRef(
            deviceAddr  = info.deviceAddress,
            parameterId = info.parameterID,
            dataCode    = info.dataCode
        )
    }




    data class WidgetParamRef(
        val deviceAddr: Int,
        val parameterId: Int,
        val dataCode: Int
    )

