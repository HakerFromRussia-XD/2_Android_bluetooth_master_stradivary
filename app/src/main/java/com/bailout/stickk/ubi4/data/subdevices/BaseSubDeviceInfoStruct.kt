package com.bailout.stickk.ubi4.data.subdevices

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
    val deviceType: Int,
    val deviceCode: Int,
    val deviceRole: Int,

    val deviceVersion: Int,
    val deviceSubVersion: Int,

    val deviceAddress: Int,

    val parametrsNum: Int,

    val subDeviceNum: Int,

    val defaultPort: Int
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
        var parametrsNum = 0
        var subDeviceNum = 0
        var defaultPort = 0

        if (string.length >= 18) {
            deviceType = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            deviceCode = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            deviceRole = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())

            deviceVersion = castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            deviceSubVersion = castUnsignedCharToInt(string.substring(8, 10).toInt(16).toByte())

            deviceAddress = castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            parametrsNum = castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
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
            parametrsNum = parametrsNum,
            subDeviceNum = subDeviceNum,
            defaultPort = defaultPort
        )
    }

    override fun serialize(encoder: Encoder, value: BaseSubDeviceInfoStruct) {
        encoder.encodeString("")
    }
}
