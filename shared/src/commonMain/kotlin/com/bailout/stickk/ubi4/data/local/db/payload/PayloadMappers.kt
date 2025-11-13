package com.bailout.stickk.ubi4.data.local.db.payload

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo

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

internal fun BaseParameterWidgetStruct.toPayload(): BaseParameterWidgetPayload =
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
        keyMobileSettings = keyMobileSettings
    )

internal fun ParameterInfo<Int, Int, Int, Int>.toPayload(): ParameterInfoPayload =
    ParameterInfoPayload(
        parameterID = parameterID,
        dataCode = dataCode,
        deviceAddress = deviceAddress,
        dataOffset = dataOffset
    )

internal fun BaseSubDeviceInfoStruct.toPayload(): BaseSubDeviceInfoPayload =
    BaseSubDeviceInfoPayload(
        deviceType = deviceType,
        deviceCode = deviceCode,
        deviceRole = deviceRole,
        deviceVersion = deviceVersion,
        deviceSubVersion = deviceSubVersion,
        deviceAddress = deviceAddress,
        parametrsNum = parametrsNum,
        subDeviceNum = subDeviceNum,
        defaultPort = defaultPort,
        parametersList = parametersList.map { it.toPayload() }
    )

internal fun Any.toWidgetPayloadOrNull(): BaseParameterWidgetPayload? =
    when (this) {
        is BaseParameterWidgetEStruct -> this.baseParameterWidgetStruct.toPayload()
        is BaseParameterWidgetSStruct -> this.baseParameterWidgetStruct.toPayload()

        is CommandParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload()

        is CommandParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload()

        is PlotParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload()

        is PlotParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload()

        is SliderParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload()

        is SliderParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload()

        is SwitchParameterWidgetEStruct ->
            this.baseParameterWidgetEStruct.baseParameterWidgetStruct.toPayload()

        is SwitchParameterWidgetSStruct ->
            this.baseParameterWidgetSStruct.baseParameterWidgetStruct.toPayload()

        else -> null
    }