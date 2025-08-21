package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = FullInicializeConnectionSerializer::class)
data class FullInicializeConnectionStruct(
    val deviceName: String,
    val deviceVersion: Int,
    val deviceSubVersion: Int,
    val deviceLabel: String,
    val deviceType: Int,
    val deviceCode: Int,

    val deviceAddress: Int,

    val deviceUUID_Prefix: String,
    val deviceUUID: Long,

    val parametrsNum: Int,
    val subDeviceNum: Int,
    val programType: Int,
    val defaultPort: Int
)

object FullInicializeConnectionSerializer : KSerializer<FullInicializeConnectionStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FullInicializeConnectionStruct {
        val string = decoder.decodeString()

        var deviceName = ""
        var deviceVersion = 0
        var deviceSubVersion = 0
        var deviceLabel = ""
        var deviceType = 0
        var deviceCode = 0

        var deviceAddress = 0
        var deviceUUID_Prefix = ""
        var deviceUUID = 0L

        var parametrsNum = 0
        var subDeviceNum = 0
        var programType = 0
        var defaultPort = 0

        if (string.length >= 154) {
            deviceName = string.substring(0, 64).decodeHex()
            deviceVersion = castUnsignedCharToInt(string.substring(64, 66).toInt(16).toByte())
            deviceSubVersion = castUnsignedCharToInt(string.substring(66, 68).toInt(16).toByte())
            deviceLabel = string.substring(68, 100) // или decodeHex() - см. твой код
            deviceType = castUnsignedCharToInt(string.substring(100, 102).toInt(16).toByte())
            deviceCode = castUnsignedCharToInt(string.substring(102, 104).toInt(16).toByte())

            deviceAddress = castUnsignedCharToInt(string.substring(104, 106).toInt(16).toByte())

            deviceUUID_Prefix = string.substring(106, 138).decodeHex()
            deviceUUID = (castUnsignedCharToInt(string.substring(138, 140).toInt(16).toByte()).toLong() +
                    castUnsignedCharToInt(string.substring(140, 142).toInt(16).toByte()).toLong() * 256 +
                    castUnsignedCharToInt(string.substring(142, 144).toInt(16).toByte()).toLong() * 256 * 256 +
                    castUnsignedCharToInt(string.substring(144, 146).toInt(16).toByte()).toLong() * 256 * 256 * 256)

            parametrsNum = castUnsignedCharToInt(string.substring(146, 148).toInt(16).toByte())
            subDeviceNum = castUnsignedCharToInt(string.substring(148, 150).toInt(16).toByte())
            programType = castUnsignedCharToInt(string.substring(150, 152).toInt(16).toByte())
            defaultPort = castUnsignedCharToInt(string.substring(152, 154).toInt(16).toByte())
        }

        return FullInicializeConnectionStruct(
            deviceName = deviceName,
            deviceVersion = deviceVersion,
            deviceSubVersion = deviceSubVersion,
            deviceLabel = deviceLabel,
            deviceType = deviceType,
            deviceCode = deviceCode,
            deviceAddress = deviceAddress,
            deviceUUID_Prefix = deviceUUID_Prefix,
            deviceUUID = deviceUUID,
            parametrsNum = parametrsNum,
            subDeviceNum = subDeviceNum,
            programType = programType,
            defaultPort = defaultPort
        )
    }

    override fun serialize(encoder: Encoder, value: FullInicializeConnectionStruct) {
        val code = "" // здесь формируешь строку, если надо
        encoder.encodeString(code)
    }
}