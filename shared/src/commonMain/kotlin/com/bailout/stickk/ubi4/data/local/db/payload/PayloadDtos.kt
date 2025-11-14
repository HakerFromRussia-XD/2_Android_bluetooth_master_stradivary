package com.bailout.stickk.ubi4.data.local.db.payload

import kotlinx.serialization.Serializable

@Serializable
data class ParameterInfoPayload(
    val parameterID: Int,
    val dataCode: Int,
    val deviceAddress: Int,
    val dataOffset: Int
)

@Serializable
data class BaseParameterWidgetPayload(
    val widgetType: Int,
    val widgetLabelType: Int,
    val widgetCode: Int,
    val display: Int,
    val widgetPosition: Int,
    val deviceId: Int,
    val widgetId: Int,
    val dataOffset: Int,
    val dataSize: Int,
    val channelOffset: Int,
    val parameterInfoSet: List<ParameterInfoPayload>,
    val keyMobileSettings: String,
    val labelCode: Int = 0,
    val label: String? = null
)

@Serializable
data class BaseParameterInfoPayload(
    val ID: Int,
    val broadcastID: Int,
    val dataCode: Int,
    val dataInstance: Int,
    val parameterDataSize: Int,
    val flagShift: Int,
    val optimisation: Int,
    val valueLimit: Int,
    val initRead: Int,
    val initWrite: Int,
    val synchType: Int,
    val synchDirection: Int,
    val synchPeriod: Int,
    val type: Int,
    val saveInMaster: Int,
    val saveInSlave: Int,
    val saveReserv: Int,
    val additionalInfoSize: Int,
    val relatedParameterID: Int,
    val relatedDataCode: Int,
    val additionalInfoRefSet: List<BaseParameterWidgetPayload>,
    val data: String,
    val firstReceiveDataFlag: Boolean
)

@Serializable
data class BaseSubDeviceInfoPayload(
    val deviceType: Int,
    val deviceCode: Int,
    val deviceRole: Int,
    val deviceVersion: Int,
    val deviceSubVersion: Int,
    val deviceAddress: Int,
    val parametrsNum: Int,
    val subDeviceNum: Int,
    val defaultPort: Int,
    val parametersList: List<BaseParameterInfoPayload>
)