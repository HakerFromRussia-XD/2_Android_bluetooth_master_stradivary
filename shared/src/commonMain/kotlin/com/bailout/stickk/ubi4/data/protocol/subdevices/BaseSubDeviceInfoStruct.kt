package com.bailout.stickk.ubi4.data.subdevices

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BaseSubDeviceInfoSerializer::class)
data class BaseSubDeviceInfoStruct(
    val deviceType: Int = 0,
    val deviceCode: Int = 0,
    val deviceRole: Int = 0,
    val deviceVersion: Int = 0,
    val deviceSubVersion: Int = 0,
    val deviceAddress: Int = 0,
    val parametersNum: Int = 0,
    val subDeviceNum: Int = 0,
    val defaultPort: Int = 0,
    var parametersList: ArrayList<BaseParameterInfoStruct> = arrayListOf(),

    val isBoot: Int = 0,
    val fwVersion: String = ""
    )

object BaseSubDeviceInfoSerializer: KSerializer<BaseSubDeviceInfoStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BaseSubDeviceInfoSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseSubDeviceInfoStruct {
        val string = decoder.decodeString()
        var deviceType = 0
        var deviceCode = 0
        var deviceRole = 0

        var deviceVersion = 0
        var deviceSubVersion = 0

        var deviceAddress = 0
        var parametersNum = 0
        var subDeviceNum = 0
        var defaultPort = 0

        val parametersList = ArrayList<BaseParameterInfoStruct>()

        if (string.length >= 18) {
            deviceType = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            deviceCode = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            deviceRole = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())

            deviceVersion = castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            deviceSubVersion = castUnsignedCharToInt(string.substring(8, 10).toInt(16).toByte())

            deviceAddress = castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            parametersNum = castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
            subDeviceNum = castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
            defaultPort = castUnsignedCharToInt(string.substring(16, 18).toInt(16).toByte())
        }

        return BaseSubDeviceInfoStruct (
            deviceType = deviceType,
            deviceCode = deviceCode,
            deviceRole = deviceRole,
            deviceVersion = deviceVersion,
            deviceSubVersion = deviceSubVersion,
            deviceAddress = deviceAddress,
            parametersNum = parametersNum,
            subDeviceNum = subDeviceNum,
            defaultPort = defaultPort,
            parametersList = parametersList
        )
    }

    override fun serialize(encoder: Encoder, value: BaseSubDeviceInfoStruct) {
        encoder.encodeString("")
    }
}
