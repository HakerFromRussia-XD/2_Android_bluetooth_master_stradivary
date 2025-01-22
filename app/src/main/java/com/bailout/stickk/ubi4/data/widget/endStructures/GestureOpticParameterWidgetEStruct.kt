package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

data class GestureOpticParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
)

object GestureOpticParameterWidgetESerializer: KSerializer<GestureOpticParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): GestureOpticParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()


        if (string.length >= 20) {
            baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${string.substring(0, 18)}\"")
        }

        return GestureOpticParameterWidgetEStruct (
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
        )
    }

    override fun serialize(encoder: Encoder, value: GestureOpticParameterWidgetEStruct) {
        val code = ""
        encoder.encodeString("$code")
    }

}
