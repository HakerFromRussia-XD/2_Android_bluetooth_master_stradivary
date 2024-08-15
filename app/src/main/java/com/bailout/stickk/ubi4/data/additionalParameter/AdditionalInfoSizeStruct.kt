package com.bailout.stickk.ubi4.data.additionalParameter

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = AdditionalInfoSizeSerializer::class)
data class AdditionalInfoSizeStruct(
    val infoType: Int,//2 байта
    val infoSize: Int,//2 байта
    val info: Int)    //4 байта

object AdditionalInfoSizeSerializer: KSerializer<AdditionalInfoSizeStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): AdditionalInfoSizeStruct {
        val string = decoder.decodeString()
        var infoType = 0
        var infoSize = 0
        var info = 0

        if (string.length >= 16) {
            infoType = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte()) +
                       castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte()) * 256
            infoSize = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte()) +
                       castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte()) * 256
            info =  castUnsignedCharToInt(string.substring(8,  10).toInt(16).toByte()) +
                    castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte()) * 256 +
                    castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte()) * 256 * 256 +
                    castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte()) * 256 * 256 * 256
        }

        return AdditionalInfoSizeStruct (
            infoType = infoType,
            infoSize = infoSize,
            info = info,
        )
    }

    override fun serialize(encoder: Encoder, value: AdditionalInfoSizeStruct) {
        val code: String = value.infoSize.toString()
        encoder.encodeString("$code")
    }
}