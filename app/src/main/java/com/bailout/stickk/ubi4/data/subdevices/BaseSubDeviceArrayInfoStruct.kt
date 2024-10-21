package com.bailout.stickk.ubi4.data.subdevices

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.BASE_SUB_DEVICE_STRUCT_SIZE
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
    val count: Int,
    val baseSubDeviceInfoStructArray: ArrayList<BaseSubDeviceInfoStruct>
)

object BaseSubDeviceArrayInfoSerializer: KSerializer<BaseSubDeviceArrayInfoStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BaseSubDeviceArrayInfoSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseSubDeviceArrayInfoStruct {
        val string = decoder.decodeString()
        var count = 0
        val baseSubDeviceInfoStructArray = ArrayList<BaseSubDeviceInfoStruct>()

        if (string.length >= 4) {
            count = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            for (i in 0 until count) {
                baseSubDeviceInfoStructArray.add(Json.decodeFromString<BaseSubDeviceInfoStruct>("\"${string.substring(i*BASE_SUB_DEVICE_STRUCT_SIZE*2,(i+1)*BASE_SUB_DEVICE_STRUCT_SIZE*2)}\""))
            }
        }

        return BaseSubDeviceArrayInfoStruct (
            count = count,
            baseSubDeviceInfoStructArray = baseSubDeviceInfoStructArray
        )
    }

    override fun serialize(encoder: Encoder, value: BaseSubDeviceArrayInfoStruct) {
        encoder.encodeString("")
    }
}
