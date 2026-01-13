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

@Serializable(with = ToggleSliderParameterWidgetSSerializer::class)
data class ToggleSliderParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct =
        BaseParameterWidgetSStruct(BaseParameterWidgetStruct(), "NOT VALID"),
    val minProgress: Int = 0,
    val maxProgress: Int = 0
)

object ToggleSliderParameterWidgetSSerializer : KSerializer<ToggleSliderParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ToggleSliderParameterWidgetSStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ToggleSliderParameterWidgetSStruct {
        val string = decoder.decodeString()

        var base = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(), "NOT VALID")
        var minProgress = 0
        var maxProgress = 0

        // base(80) + min(2) + max(2) = 84
        if (string.length >= 84) {
            base = Json.decodeFromString(
                BaseParameterWidgetSStruct.serializer(),
                "\"${string.substring(0, 80)}\""
            )

            minProgress = string.substring(80, 82).toInt(16).toByte().toInt()
            maxProgress = castUnsignedCharToInt(string.substring(82, 84).toInt(16).toByte())
        }

        return ToggleSliderParameterWidgetSStruct(
            baseParameterWidgetSStruct = base,
            minProgress = minProgress,
            maxProgress = maxProgress
        )
    }

    override fun serialize(encoder: Encoder, value: ToggleSliderParameterWidgetSStruct) {
        encoder.encodeString("")
    }
}