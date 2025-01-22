package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

data class GestureParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
)

object GestureParameterWidgetESerializer: KSerializer<GestureParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): GestureParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()


        if (string.length >= 20) {
            baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${string.substring(0, 18)}\"")
        }

        return GestureParameterWidgetEStruct (
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
        )
    }

    override fun serialize(encoder: Encoder, value: GestureParameterWidgetEStruct) {
        val code = ""
        encoder.encodeString("$code")
    }

}
