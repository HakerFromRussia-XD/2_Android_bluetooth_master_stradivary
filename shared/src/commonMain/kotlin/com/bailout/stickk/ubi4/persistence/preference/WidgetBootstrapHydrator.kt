package com.bailout.stickk.ubi4.persistence.preference
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.db.extractKey
import com.bailout.stickk.ubi4.data.local.db.payload.BaseParameterWidgetPayload
import com.bailout.stickk.ubi4.data.local.db.payload.toEndStruct
import com.bailout.stickk.ubi4.data.state.RestoredState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.bindingGroupFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.rotationGroupFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.slidersFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.switcherFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.thresholdFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.widgetsMergeEventFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.GestureOpticParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.GestureParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ThresholdParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ThresholdParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
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




        platformLog("WIDGET_SOURCE", "restoreFromDb: mac=$mac → читаем из БД")

        // 1. Параметры мастера
        val paramsFromDb = repo.loadAllMasterParams(deviceAddr)
        RestoredState.baseParamsFromDb = paramsFromDb.toMutableList()

        baseParametrInfoStructArray.clear()
        baseParametrInfoStructArray.addAll(paramsFromDb)

        // 2. Сабдевайсы
        val subsFromDb = repo.loadAllSubDevices(deviceAddr)
        RestoredState.subDevicesFromDb = subsFromDb.toMutableSet()

        baseSubDevicesInfoStructSet.clear()
        baseSubDevicesInfoStructSet.addAll(subsFromDb)

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
            "restoreFromDb: mac=$mac widgets_from_DB=${restored.size}")

        platformLog(
            "BOOTSTRAP_TETS",
            "restoreFromDb: dev=$deviceAddr, params=${paramsFromDb.size}, subs=${subsFromDb.size}, widgets=${restored.size}"
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
                val p = ParameterProvider.getParameter(masterAddr, info.ID)

                val lastState = repo.loadLastState(
                    deviceAddr  = masterAddr,
                    parameterId = info.ID,
                    dataCode    = info.dataCode
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
                    val p = ParameterProvider.getParameter(subAddr, info.ID)

                    val lastState = repo.loadLastState(
                        deviceAddr  = subAddr,
                        parameterId = info.ID,
                        dataCode    = info.dataCode
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
            val info = base.parameterInfoSet.firstOrNull() ?: return@forEach

            val ref = ParameterRef(
                addressDevice = info.deviceAddress,
                parameterID = info.parameterID,
                dataCode = info.dataCode
            )

            when (base.widgetCode) {
                PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
                    slidersFlow.tryEmit(ref)
                }

                PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
                    switcherFlow.tryEmit(ref)
                }

                PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT.number.toInt(),
                PreferenceKeysUbi4.ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                    thresholdFlow.tryEmit(ref)
                    widgetsMergeEventFlow.tryEmit(ref)
                }

                PreferenceKeysUbi4.ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {

                }
            }
            when (info.dataCode) {
                PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number -> {
                    // группа ротации
                    rotationGroupFlow.tryEmit(ref)
                }

                PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number -> {
                    // биндинги (SPR)
                    bindingGroupFlow.tryEmit(ref)
                }
            }


            platformLog(
                "BOOTSTRAP",
                "replayWidgetEventsFromDb: dev=$deviceAddr widgets=${UiState.listWidgets.size}"
            )
        }
    }

    // ——— Вспомогательный метод: достать BaseParameterWidgetStruct из любого endStruct ———

    private fun Any.baseStructOrNull() =
        when (this) {
            is BaseParameterWidgetEStruct   -> baseParameterWidgetStruct
            is BaseParameterWidgetSStruct   -> baseParameterWidgetStruct
            is SliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            is SliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct
            is PlotParameterWidgetEStruct   -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            is PlotParameterWidgetSStruct   -> baseParameterWidgetSStruct.baseParameterWidgetStruct
            is SwitchParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            is SwitchParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct
            is ThresholdParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            is ThresholdParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct
            is CommandParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            is CommandParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct
            is GestureParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            is GestureOpticParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            is OpticStartLearningWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
            else -> null
        }


}