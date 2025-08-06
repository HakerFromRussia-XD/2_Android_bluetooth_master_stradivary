package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = SliderParameterWidgetESerializer::class)
data class SliderParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val minProgress: Int = 0,
    val maxProgress: Int = 0
)

object SliderParameterWidgetESerializer : KSerializer<SliderParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SliderParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SliderParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()
        var minProgress = 0
        var maxProgress = 0

        if (string.length >= 22) {
            baseParameterWidgetEStruct = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )
            minProgress = string.substring(18, 20).toInt(16)
            maxProgress = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            platformLog("TestMinProgress", "minProgress -E = $minProgress")

        }

        return SliderParameterWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            minProgress = minProgress,
            maxProgress = maxProgress
        )
    }

    override fun serialize(encoder: Encoder, value: SliderParameterWidgetEStruct) {
        encoder.encodeString("")
    }
}