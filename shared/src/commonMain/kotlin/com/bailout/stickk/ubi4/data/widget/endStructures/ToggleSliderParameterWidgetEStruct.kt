package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = ToggleSliderParameterWidgetESerializer::class)
data class ToggleSliderParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val minProgress: Int = 0,
    val maxProgress: Int = 0
)

object ToggleSliderParameterWidgetESerializer : KSerializer<ToggleSliderParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ToggleSliderParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ToggleSliderParameterWidgetEStruct {
        val string = decoder.decodeString()

        var base = BaseParameterWidgetEStruct()
        var minProgress = 0
        var maxProgress = 0

        // base(18) + min(2) + max(2) = 22
        if (string.length >= 22) {
            base = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )

            minProgress = string.substring(18, 20).toInt(16).toByte().toInt()
            maxProgress = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
        }

        return ToggleSliderParameterWidgetEStruct(
            baseParameterWidgetEStruct = base,
            minProgress = minProgress,
            maxProgress = maxProgress
        )
    }

    override fun serialize(encoder: Encoder, value: ToggleSliderParameterWidgetEStruct) {
        encoder.encodeString("")
    }
}