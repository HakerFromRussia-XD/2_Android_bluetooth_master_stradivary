package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
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


@Serializable(with = SliderParameterWidgetSSerializer::class)
data class SliderParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct,
    val progress: Int,
)

object SliderParameterWidgetSSerializer: KSerializer<SliderParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SliderParameterWidgetSSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SliderParameterWidgetSStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetSStruct: BaseParameterWidgetSStruct
        var progress = 0


        if (string.length >= 82) {
            baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${string.substring(0, 80)}\"")
            progress = castUnsignedCharToInt(string.substring(80, 82).toInt(16).toByte())
        } else {
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct (BaseParameterWidgetStruct(0, 0, 0, 0, 0, 0, 0, 0, 0, mutableSetOf(Pair(0,0))),"")
        }

        return SliderParameterWidgetSStruct (
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            progress = progress,
        )
    }

    override fun serialize(encoder: Encoder, value: SliderParameterWidgetSStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}