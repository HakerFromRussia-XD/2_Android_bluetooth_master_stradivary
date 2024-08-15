package com.bailout.stickk.ubi4.data.widget

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


@Serializable(with = BaseParameterWidgetSSerializer::class)
data class BaseParameterWidgetSStruct(
    val baseParameterWidgetStruct: BaseParameterWidgetStruct,
    val label: String
)

object BaseParameterWidgetSSerializer: KSerializer<BaseParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseParameterWidgetSStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${string.substring(0, 8)}\"")
        var label = ""


        if (string.length >= 72) {
            label = string.substring(8, 32)
        }

        return BaseParameterWidgetSStruct (
            baseParameterWidgetStruct = baseParameterWidgetStruct,
            label = label
        )
    }

    override fun serialize(encoder: Encoder, value: BaseParameterWidgetSStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}