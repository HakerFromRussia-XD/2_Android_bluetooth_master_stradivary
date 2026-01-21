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
    val maxProgress: Int = 0,
    val increment: Float = 1.0f
)

object SliderParameterWidgetESerializer : KSerializer<SliderParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SliderParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SliderParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()
        var minProgress = 0
        var maxProgress = 0
        var increment = 1.0f

        if (string.length >= 22) {
            baseParameterWidgetEStruct = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )

            minProgress = string.substring(18, 20).toInt(16).toByte().toInt()
            maxProgress = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            
            if (string.length >= 24) {
                val incByte = string.substring(22, 24).toInt(16)
                val isDiv = (incByte and 0x80) != 0
                val value = (incByte and 0x7F).toFloat()
                
                increment = if (isDiv) {
                    if (value != 0f) 1.0f / value else 1.0f
                } else {
                    if (value != 0f) value else 1.0f
                }
            }
            
            platformLog("TestMinProgress", "minProgress -E = $minProgress, inc = $increment")
        }

        return SliderParameterWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            minProgress = minProgress,
            maxProgress = maxProgress,
            increment = increment
        )
    }

    override fun serialize(encoder: Encoder, value: SliderParameterWidgetEStruct) {
        encoder.encodeString("")
    }
}
