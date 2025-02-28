package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = DeviceInfoStructsSerializer::class)
data class DeviceInfoStructs(
    var productName: String = "",
    var productVersion: Int = 0,
    var productSubVersion: Int = 0,
    var productLabel: String = "",
    var productType: Int = 0,
    var productCode: Int = 0,
    var productUUIDPrefix: String = "",
    var productUUID: Int = 0,
    var productAdditionalInfoType: Int = 0,
    var productAdditionalInfo: Int = 0,
)

object DeviceInfoStructsSerializer: KSerializer<DeviceInfoStructs> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DeviceInfoStructsSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DeviceInfoStructs {
        val string = decoder.decodeString()
        val paddedString = string.padEnd(142, '0')

        val productName = string.substring(0, 64).decodeHex()
        val productVersion = castUnsignedCharToInt(paddedString.substring(64, 66).toInt(16).toByte())
        val productSubVersion = castUnsignedCharToInt(paddedString.substring(66, 68).toInt(16).toByte())
        val productLabel = string.substring(68, 100).decodeHex()
        val productType = castUnsignedCharToInt(paddedString.substring(100, 102).toInt(16).toByte())
        val productCode = castUnsignedCharToInt(paddedString.substring(102, 104).toInt(16).toByte())
        val productUUIDPrefix = string.substring(104, 136).decodeHex()
        val productUUID = castUnsignedCharToInt(paddedString.substring(136, 138).toInt(16).toByte())
        val productAdditionalInfoType = castUnsignedCharToInt(paddedString.substring(138, 140).toInt(16).toByte())
        val productAdditionalInfo = castUnsignedCharToInt(paddedString.substring(140, 142).toInt(16).toByte())


        return DeviceInfoStructs (
            productName                 = productName,
            productVersion              = productVersion,
            productSubVersion           = productSubVersion,
            productLabel                = productLabel,
            productType                 = productType,
            productCode                 = productCode,
            productUUIDPrefix           = productUUIDPrefix,
            productUUID                 = productUUID,
            productAdditionalInfoType   = productAdditionalInfoType,
            productAdditionalInfo       = productAdditionalInfo,
        )
    }

    override fun serialize(encoder: Encoder, value: DeviceInfoStructs) {
        val code = ""
        encoder.encodeString("$code")
    }
}