package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = DeviceInfoStructsSerializer::class)
data class DeviceInfoStructs(
    var deviceName: String = "",
    var deviceVersion: Int = 0,
    var deviceSubVersion: Int = 0,
    var deviceLabel: String = "",
    var deviceType: Int = 0,
    var deviceCode: Int = 0,
    var deviceRole: Int = 0,
    var deviceAddress: Int = 0,
    var deviceUUIDPrefix: String = "",
    var deviceUUID: Int = 0,
    var deviceAdditionalInfoType: Int = 0,
    var deviceAdditionalInfo: Int = 0,
) {
    val formattedDeviceUUID: String
        get() = deviceUUID.toString().padStart(5, '0')
}

object DeviceInfoStructsSerializer : KSerializer<DeviceInfoStructs> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DeviceInfoStructsSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DeviceInfoStructs {
        val string = decoder.decodeString()
        // Подстраховка, чтобы не упасть, если строка короче нужного
        val paddedString = string.padEnd(158, '0')

        val deviceName = paddedString.substring(0, 64).decodeHex()
        val deviceVersion = paddedString.substring(64, 66).toInt(16)
        val deviceSubVersion = paddedString.substring(66, 68).toInt(16)
        val deviceLabel = paddedString.substring(68, 100).decodeHex()
        val deviceType = paddedString.substring(100, 102).toInt(16)
        val deviceCode = paddedString.substring(102, 104).toInt(16)
        val deviceRole = paddedString.substring(104, 106).toInt(16)
        val deviceAddress = paddedString.substring(106, 108).toInt(16)
        val deviceUUIDPrefix = paddedString.substring(108, 140).decodeHex().trim('\u0000')

        val deviceUUIDHex = paddedString.substring(140, 148)
        // Переворачиваем байты, т.к. little-endian
        val deviceUUID = deviceUUIDHex.chunked(2)
            .reversed()
            .joinToString("")
            .toInt(16)

        val deviceAdditionalInfoType = paddedString.substring(148, 150).toInt(16)
        val deviceAdditionalInfo = paddedString.substring(150, 158).toInt(16)

        return DeviceInfoStructs(
            deviceName = deviceName,
            deviceVersion = deviceVersion,
            deviceSubVersion = deviceSubVersion,
            deviceLabel = deviceLabel,
            deviceType = deviceType,
            deviceCode = deviceCode,
            deviceRole = deviceRole,
            deviceAddress = deviceAddress,
            deviceUUIDPrefix = deviceUUIDPrefix,
            deviceUUID = deviceUUID,
            deviceAdditionalInfoType = deviceAdditionalInfoType,
            deviceAdditionalInfo = deviceAdditionalInfo
        )
    }

    override fun serialize(encoder: Encoder, value: DeviceInfoStructs) {
        val code = "" // Тут формируешь строку по необходимости
        encoder.encodeString(code)
    }
}

