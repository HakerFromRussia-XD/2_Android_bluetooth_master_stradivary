package com.bailout.stickk.ubi4.data.subdevices

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = BaseSubDeviceArrayInfoDataSerializer::class)
data class BaseSubDeviceArrayInfoDataStruct(
    val size: Int,
    val count: Int,
    val itemSize: Int, //2 байта
    val baseSubDeviceInfoStruct: ArrayList<BaseSubDeviceInfoStruct>
)

object BaseSubDeviceArrayInfoDataSerializer: KSerializer<BaseSubDeviceArrayInfoDataStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BaseSubDeviceArrayInfoDataSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseSubDeviceArrayInfoDataStruct {
        val string = decoder.decodeString()
        var size = 0
        var count = 0
        var itemSize = 0
        val baseSubDeviceInfoStruct = ArrayList<BaseSubDeviceInfoStruct>()

        if (string.length >= 8) {
            size = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            count = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            itemSize = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            for (i in 0 until size) {
                baseSubDeviceInfoStruct.add(Json.decodeFromString<BaseSubDeviceInfoStruct>("\"${string.substring(i*itemSize*2,(i+1)*itemSize*2)}\""))
            }
        }

        return BaseSubDeviceArrayInfoDataStruct (
            size = size,
            count = count,
            itemSize = itemSize,
            baseSubDeviceInfoStruct = baseSubDeviceInfoStruct
        )
    }

    override fun serialize(encoder: Encoder, value: BaseSubDeviceArrayInfoDataStruct) {
        encoder.encodeString("")
    }
}
