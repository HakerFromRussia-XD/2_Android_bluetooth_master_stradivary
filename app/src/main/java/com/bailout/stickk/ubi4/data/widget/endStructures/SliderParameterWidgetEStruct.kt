package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import android.util.Pair


@Serializable(with = SliderParameterWidgetESerializer::class)
data class SliderParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct,
    val progress: Int,
)

object SliderParameterWidgetESerializer: KSerializer<SliderParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SliderParameterWidgetESerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SliderParameterWidgetEStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetEStruct: BaseParameterWidgetEStruct
        var progress = 0


        if (string.length >= 20) {
            baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${string.substring(0, 18)}\"")
            progress = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
        } else {
            baseParameterWidgetEStruct = BaseParameterWidgetEStruct (BaseParameterWidgetStruct(0, 0, 0, 0, 0, 0, 0, 0, 0, mutableSetOf(Pair(0,0))),0)
        }

        return SliderParameterWidgetEStruct (
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            progress = progress,
        )
    }

    override fun serialize(encoder: Encoder, value: SliderParameterWidgetEStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}