package com.bailout.stickk.ubi4.data.widget.subStructures

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


@Serializable(with = BaseParameterWidgetESerializer::class)
data class BaseParameterWidgetEStruct(
    val baseParameterWidgetStruct: BaseParameterWidgetStruct = BaseParameterWidgetStruct(),
    val labelCode: Int = 0
)

object BaseParameterWidgetESerializer: KSerializer<BaseParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetStruct = BaseParameterWidgetStruct()
        var labelCode = 0


        if (string.length >= 18) {
            baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${string.substring(0, 16)}\"")
            labelCode = castUnsignedCharToInt(string.substring(16, 18).toInt(16).toByte())
        }

        return BaseParameterWidgetEStruct (
            baseParameterWidgetStruct = baseParameterWidgetStruct,
            labelCode = labelCode
        )
    }

    override fun serialize(encoder: Encoder, value: BaseParameterWidgetEStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}