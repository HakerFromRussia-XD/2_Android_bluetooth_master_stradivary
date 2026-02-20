package com.bailout.stickk.ubi4.data.local.db.payload

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.GestureOpticParameterWidgetEStruct
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
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4

// ------------------------------------------------------------------------
// Struct → Payload
// ------------------------------------------------------------------------

internal fun BaseParameterInfoStruct.toPayload(): BaseParameterInfoPayload =
    BaseParameterInfoPayload(
        ID = ID,
        broadcastID = broadcastID,
        dataCode = dataCode,
        dataInstance = dataInstance,
        parameterDataSize = parameterDataSize,
        flagShift = flagShift,
        optimisation = optimisation,
        valueLimit = valueLimit,
        initRead = initRead,
        initWrite = initWrite,
        synchType = synchType,
        synchDirection = synchDirection,
        synchPeriod = synchPeriod,
        type = type,
        saveInMaster = saveInMaster,
        saveInSlave = saveInSlave,
        saveReserv = saveReserv,
        additionalInfoSize = additionalInfoSize,
        relatedParameterID = relatedParameterID,
        relatedDataCode = relatedDataCode,
        additionalInfoRefSet = additionalInfoRefSet.map { it.toPayload() },
        data = data,
        firstReceiveDataFlag = firstReceiveDataFlag
    )

internal fun BaseParameterWidgetStruct.toPayload(
    labelCode: Int = 0,
    label: String? = null
): BaseParameterWidgetPayload =
    BaseParameterWidgetPayload(
        widgetType = widgetType,
        widgetLabelType = widgetLabelType,
        widgetCode = widgetCode,
        display = display,
        widgetPosition = widgetPosition,
        deviceId = deviceId,
        widgetId = widgetId,
        dataOffset = dataOffset,
        dataSize = dataSize,
        channelOffset = channelOffset,
        parameterInfoSet = parameterInfoSet.map { it.toPayload() },
        keyMobileSettings = keyMobileSettings,
        labelCode = labelCode,
        label = label,

    )

internal fun ParameterInfo<Int, Int, Int, Int>.toPayload(): ParameterInfoPayload =
    ParameterInfoPayload(
        parameterID = parameterID,
        dataCode = dataCode,
        deviceAddress = deviceAddress,
        dataOffset = dataOffsets
    )

internal fun BaseSubDeviceInfoStruct.toPayload(): BaseSubDeviceInfoPayload =
    BaseSubDeviceInfoPayload(
        deviceType = deviceType,
        deviceCode = deviceCode,
        deviceRole = deviceRole,
        deviceVersion = deviceVersion,
        deviceSubVersion = deviceSubVersion,
        deviceAddress = deviceAddress,
        parametersNum = parametersNum,
        subDeviceNum = subDeviceNum,
        defaultPort = defaultPort,
        parametersList = parametersList.map { it.toPayload() }
    )

/**
 * EndStruct → BaseParameterWidgetPayload.
 * Сохраняем базу + либо labelCode (E), либо string label (S).
 */
internal fun Any.toWidgetPayloadOrNull(): BaseParameterWidgetPayload? =
    when (this) {
        // Базовые E/S (если вдруг кто-то напрямую их пихнёт в сохранение)
        is BaseParameterWidgetEStruct ->
            this.baseParameterWidgetStruct.toPayload(labelCode = labelCode)

        is BaseParameterWidgetSStruct ->
            this.baseParameterWidgetStruct.toPayload(label = label)

        // -------- COMMAND (BUTTON) --------
        is CommandParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct
                .toPayload(labelCode = baseParameterWidgetEStruct.labelCode)
                .copy(
                    clickCommand   = clickCommand,
                    pressedCommand = pressedCommand,
                    releasedCommand= releasedCommand
                )

        is CommandParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct
                .toPayload(label = baseParameterWidgetSStruct.label)
                .copy(
                    clickCommand   = clickCommand,
                    pressedCommand = pressedCommand,
                    releasedCommand= releasedCommand
                )

        // -------- PLOT --------
        is PlotParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload(
                labelCode = baseParameterWidgetEStruct.labelCode
            )

        is PlotParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload(
                label = baseParameterWidgetSStruct.label
            )

        // -------- SLIDER --------
        is SliderParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload(
                labelCode = baseParameterWidgetEStruct.labelCode
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress,
                increment = increment
            )

        is SliderParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload(
                label = baseParameterWidgetSStruct.label
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress,
                increment = increment
            )

        is ToggleSliderParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload(
                labelCode = baseParameterWidgetEStruct.labelCode
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress,
                increment = increment
            )

        is ToggleSliderParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload(
                label = baseParameterWidgetSStruct.label
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress,
                increment = increment
            )

        // -------- SWITCH --------
        is SwitchParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload(
                labelCode = baseParameterWidgetEStruct.labelCode
            )

        is SwitchParameterWidgetSStruct ->
            baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload(
                label = baseParameterWidgetSStruct.label
            )

        // -------- SPINNER --------
        is SpinnerParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload(
                labelCode = baseParameterWidgetEStruct.labelCode
            )

        is SpinnerParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload(
                label = baseParameterWidgetSStruct.label
            )
        is OpticStartLearningWidgetEStruct ->
            baseParameterWidgetEStruct.baseParameterWidgetStruct
                .toPayload()
                .copy(
                    labelCode = baseParameterWidgetEStruct.labelCode,
                    label = null
                )


        is OpticStartLearningWidgetSStruct ->
            baseParameterWidgetSStruct.baseParameterWidgetStruct
                .toPayload()
                .copy(
                    // для S-структур обычно label у нас строкой
                    labelCode = 0,
                    label = baseParameterWidgetSStruct.label
                )

        is GestureOpticParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct
                .toPayload()
                .copy(
                    labelCode = baseParameterWidgetEStruct.labelCode,
                    label = null
                )

        // -------- TOGGLE SLIDER --------
        is ToggleSliderParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload(
                labelCode = baseParameterWidgetEStruct.labelCode
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress
            )

        is ToggleSliderParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload(
                label = baseParameterWidgetSStruct.label
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress
            )
        is SliderParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload(
                labelCode = baseParameterWidgetEStruct.labelCode
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress,
                increment = increment // ✅
            )

        is SliderParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload(
                label = baseParameterWidgetSStruct.label
            ).copy(
                minProgress = minProgress,
                maxProgress = maxProgress,
                increment = increment // ✅
            )


        else -> null
    }

// ------------------------------------------------------------------------
// Payload → Struct
// ------------------------------------------------------------------------

internal fun ParameterInfoPayload.toModel(): ParameterInfo<Int, Int, Int, Int> =
    ParameterInfo(
        parameterID   = parameterID,
        dataCode      = dataCode,
        deviceAddress = deviceAddress,
        dataOffsets    = dataOffset
    )

internal fun BaseParameterWidgetPayload.toBaseStruct(): BaseParameterWidgetStruct =
    BaseParameterWidgetStruct(
        widgetType       = widgetType,
        widgetLabelType  = widgetLabelType,
        widgetCode       = widgetCode,
        display          = display,
        widgetPosition   = widgetPosition,
        deviceId         = deviceId,
        widgetId         = widgetId,
        dataOffset       = dataOffset,
        dataSize         = dataSize,
        channelOffset    = channelOffset,
        parameterInfoSet = parameterInfoSet.map { it.toModel() }.toMutableSet(),
        keyMobileSettings = keyMobileSettings
    )

/**
 * Восстановить endStruct (Slider/Plot/Switch/Command/Spinner/…)
 * из payload, который мы сохранили в Room.
 *
 * ВАЖНО: здесь мы обратно подкладываем labelCode / label.
 */
internal fun BaseParameterWidgetPayload.toEndStruct(): Any {
    val baseStruct = toBaseStruct()
    val isStringLabel =
        widgetLabelType == PreferenceKeysUbi4.ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt()

    // Базовые E/S-структуры сразу с нужным labelCode / label
    val baseEStruct = BaseParameterWidgetEStruct(
        baseParameterWidgetStruct = baseStruct,
        labelCode = labelCode
    )

    val baseSStruct = BaseParameterWidgetSStruct(
        baseParameterWidgetStruct = baseStruct,
        label = label ?: ""
    )

    fun sliderStruct(): Any {
        val min = minProgress ?: 0
        val max = maxProgress ?: 100
        val inc = increment ?: 1.0f

        return if (isStringLabel) {
            SliderParameterWidgetSStruct(
                baseParameterWidgetSStruct = baseSStruct,
                minProgress = min,
                maxProgress = max,
                increment = inc
            )
        } else {
            SliderParameterWidgetEStruct(
                baseParameterWidgetEStruct = baseEStruct,
                minProgress = min,
                maxProgress = max,
                increment = inc
            )
        }
    }


    fun plotStruct(): Any =
        if (isStringLabel) {
            PlotParameterWidgetSStruct(
                baseParameterWidgetSStruct = baseSStruct
            )
        } else {
            PlotParameterWidgetEStruct(
                baseParameterWidgetEStruct = baseEStruct
            )
        }

    fun switchStruct(): Any =
        if (isStringLabel) {
            SwitchParameterWidgetSStruct(
                baseParameterWidgetSStruct = baseSStruct
            )
        } else {
            SwitchParameterWidgetEStruct(
                baseParameterWidgetEStruct = baseEStruct
            )
        }

    fun commandStruct(): Any {
        val click  = clickCommand   ?: 0
        val press  = pressedCommand ?: 0
        val release= releasedCommand?: 0

        return if (isStringLabel) {
            CommandParameterWidgetSStruct(
                baseParameterWidgetSStruct = baseSStruct,
                clickCommand   = click,
                pressedCommand = press,
                releasedCommand= release
            )
        } else {
            CommandParameterWidgetEStruct(
                baseParameterWidgetEStruct = baseEStruct,
                clickCommand   = click,
                pressedCommand = press,
                releasedCommand= release
            )
        }
    }

    fun spinnerStruct(): Any =
        if (isStringLabel) {
            SpinnerParameterWidgetSStruct(
                baseParameterWidgetSStruct = baseSStruct
            )
        } else {
            SpinnerParameterWidgetEStruct(
                baseParameterWidgetEStruct = baseEStruct
            )
        }

    return when (widgetCode) {
        PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> sliderStruct()
        PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT.number.toInt()    -> plotStruct()
        PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SWITCH.number.toInt()  -> switchStruct()
        PreferenceKeysUbi4.ParameterWidgetCode.PWCE_BUTTON.number.toInt()  -> commandStruct()
        PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> spinnerStruct()
        // ---------- TOGGLE SLIDER ----------
        PreferenceKeysUbi4.ParameterWidgetCode.PWCE_TOGGLE_SLIDER.number.toInt() -> {
            val min = minProgress ?: 0
            val max = maxProgress ?: 100
            val inc = increment ?: 1.0f

            if (isStringLabel) {
                ToggleSliderParameterWidgetSStruct(
                    baseParameterWidgetSStruct = baseSStruct,
                    minProgress = min,
                    maxProgress = max,
                    increment = inc
                )
            } else {
                ToggleSliderParameterWidgetEStruct(
                    baseParameterWidgetEStruct = baseEStruct,
                    minProgress = min,
                    maxProgress = max,
                    increment = inc
                )
            }
        }

        else -> if (isStringLabel) {
            BaseParameterWidgetSStruct(
                baseParameterWidgetStruct = baseStruct,
                label = label ?: ""
            )
        } else {
            BaseParameterWidgetEStruct(
                baseParameterWidgetStruct = baseStruct,
                labelCode = labelCode
            )
        }
    }

}

internal fun BaseParameterInfoPayload.toModel(): BaseParameterInfoStruct =
    BaseParameterInfoStruct(
        ID                   = ID,
        broadcastID          = broadcastID,
        dataCode             = dataCode,
        dataInstance         = dataInstance,
        parameterDataSize    = parameterDataSize,
        flagShift            = flagShift,
        optimisation         = optimisation,
        valueLimit           = valueLimit,
        initRead             = initRead,
        initWrite            = initWrite,
        synchType            = synchType,
        synchDirection       = synchDirection,
        synchPeriod          = synchPeriod,
        type                 = type,
        saveInMaster         = saveInMaster,
        saveInSlave          = saveInSlave,
        saveReserv           = saveReserv,
        additionalInfoSize   = additionalInfoSize,
        relatedParameterID   = relatedParameterID,
        relatedDataCode      = relatedDataCode,

        // ВРЕМЕННО: не восстанавливаем additionalInfoRefSet
        additionalInfoRefSet = mutableSetOf(),

        data                 = data,
        firstReceiveDataFlag = firstReceiveDataFlag
    )

internal fun BaseSubDeviceInfoPayload.toModel(): BaseSubDeviceInfoStruct =
    BaseSubDeviceInfoStruct(
        deviceType       = deviceType,
        deviceCode       = deviceCode,
        deviceRole       = deviceRole,
        deviceVersion    = deviceVersion,
        deviceSubVersion = deviceSubVersion,
        deviceAddress    = deviceAddress,
        parametersNum     = parametersNum,
        subDeviceNum     = subDeviceNum,
        defaultPort      = defaultPort,
        parametersList   = ArrayList(
            parametersList.map { p -> p.toModel() }
        )
    )