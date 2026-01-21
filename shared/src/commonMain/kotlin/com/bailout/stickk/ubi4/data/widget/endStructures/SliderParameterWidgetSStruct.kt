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
    val maxProgress: Int = 0,
    val increment: Float = 1.0f
)

object SliderParameterWidgetSSerializer : KSerializer<SliderParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SliderParameterWidgetSStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SliderParameterWidgetSStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(), "NOT VALID")
        var minProgress = 0
        var maxProgress = 0
        var increment = 1.0f

        if (string.length >= 84) {
            baseParameterWidgetSStruct = Json.decodeFromString(
                BaseParameterWidgetSStruct.serializer(),
                "\"${string.substring(0, 80)}\""
            )
            minProgress = string.substring(80, 82).toInt(16).toByte().toInt()
            maxProgress = castUnsignedCharToInt(string.substring(82, 84).toInt(16).toByte())
            
            if (string.length >= 86) {
                val incByte = string.substring(84, 86).toInt(16)
                val isDiv = (incByte and 0x80) != 0
                val value = (incByte and 0x7F).toFloat()
                
                increment = if (isDiv) {
                    if (value != 0f) 1.0f / value else 1.0f
                } else {
                    if (value != 0f) value else 1.0f
                }
            }
            
            platformLog("TestMinProgress", "minProgress -S = $minProgress, inc = $increment")
        }

        return SliderParameterWidgetSStruct(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            minProgress = minProgress,
            maxProgress = maxProgress,
            increment = increment
        )
    }

    override fun serialize(encoder: Encoder, value: SliderParameterWidgetSStruct) {
        encoder.encodeString("")
    }
}
