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

@Serializable(with = BaseSubDeviceArrayInfoSerializer::class)
data class BaseSubDeviceArrayInfoStruct(
    val size: Int,
    val count: Int,
    val itemSize: Int, //2 байта
    val baseSubDeviceInfoStruct: ArrayList<BaseSubDeviceInfoStruct>
)

object BaseSubDeviceArrayInfoSerializer: KSerializer<BaseSubDeviceArrayInfoStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BaseSubDeviceArrayInfoSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseSubDeviceArrayInfoStruct {
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

        return BaseSubDeviceArrayInfoStruct (
            size = size,
            count = count,
            itemSize = itemSize,
            baseSubDeviceInfoStruct = baseSubDeviceInfoStruct
        )
    }

    override fun serialize(encoder: Encoder, value: BaseSubDeviceArrayInfoStruct) {
        encoder.encodeString("")
    }
}
