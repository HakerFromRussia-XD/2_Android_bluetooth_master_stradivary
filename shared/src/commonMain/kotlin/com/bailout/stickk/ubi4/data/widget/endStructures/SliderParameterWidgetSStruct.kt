package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
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

@Serializable(with = SliderParameterWidgetSSerializer::class)
data class SliderParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(), "NOT VALID"),
    val minProgress: Int = 0,
    val maxProgress: Int = 0
)

object SliderParameterWidgetSSerializer : KSerializer<SliderParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SliderParameterWidgetSStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SliderParameterWidgetSStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(), "NOT VALID")
        var minProgress = 0
        var maxProgress = 0

        if (string.length >= 84) {
            baseParameterWidgetSStruct = Json.decodeFromString(
                BaseParameterWidgetSStruct.serializer(),
                "\"${string.substring(0, 80)}\""
            )
            // Если нужно перевести minProgress в signed, доработай здесь:
            minProgress = string.substring(80, 82).toInt(16).toByte().toInt()
//            minProgress = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            maxProgress = castUnsignedCharToInt(string.substring(82, 84).toInt(16).toByte())
            platformLog("TestMinProgress", "minProgress -S = $minProgress")
            platformLog("TestMinProgress", "maxProgress -S = $maxProgress")
            platformLog("TestMinProgress", "substring  = ${string.substring(80,82)}")
        }

        return SliderParameterWidgetSStruct(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            minProgress = minProgress,
            maxProgress = maxProgress
        )
    }

    override fun serialize(encoder: Encoder, value: SliderParameterWidgetSStruct) {
        encoder.encodeString("")
    }
}