package com.bailout.stickk.ubi4.data.local.db

import androidx.room.Entity
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "base_parameter_info",
    primaryKeys = [
        "device_mac",
        "device_addr",
        "parameter_id",
        "data_code"
    ]
)
data class BaseParameterInfoEntity(
    val device_mac: String,
    val device_addr: Long,
    val parameter_id: Long,
    val data_code: Long,
    val ts_ms: Long,
    val payload: String
) {
    companion object {
        private val json = Json {
            encodeDefaults = true
        }

        fun create(
            mac: String,
            deviceAddr: Int,
            parameterId: Int,
            dataCode: Int,
            tsMs: Long,
            info: BaseParameterInfoStruct
        ): BaseParameterInfoEntity {
            return BaseParameterInfoEntity(
                device_mac = mac,
                device_addr = deviceAddr.toLong(),
                parameter_id = parameterId.toLong(),
                data_code = dataCode.toLong(),
                ts_ms = tsMs,
                payload = json.encodeToString(info.toPayload())
            )
        }
    }
}

@Serializable
private data class BaseParameterInfoPayload(
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
private data class BaseParameterWidgetPayload(
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
    val keyMobileSettings: String
)

@Serializable
private data class ParameterInfoPayload(
    val parameterID: Int,
    val dataCode: Int,
    val deviceAddress: Int,
    val dataOffset: Int
)

private fun BaseParameterInfoStruct.toPayload(): BaseParameterInfoPayload =
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

private fun BaseParameterWidgetStruct.toPayload(): BaseParameterWidgetPayload =
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

private fun ParameterInfo<Int, Int, Int, Int>.toPayload(): ParameterInfoPayload =
    ParameterInfoPayload(
        parameterID = parameterID,
        dataCode = dataCode,
        deviceAddress = deviceAddress,
        dataOffset = dataOffset
    )