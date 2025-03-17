package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = ThresholdParameterWidgetESerializer::class)
data class ThresholdParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val openThresholdUpper: Int = 0,
    val openThresholdLower: Int = 0,
    val closeThresholdUpper: Int = 0,
    val closeThresholdLower: Int = 0
)

object ThresholdParameterWidgetESerializer : KSerializer<ThresholdParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ThresholdParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ThresholdParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()
        var openThresholdUpper = 0
        var openThresholdLower = 0
        var closeThresholdUpper = 0
        var closeThresholdLower = 0

        // Логирование заменено на println – можно убрать, если не нужно
        // println("ThresholdParameterWidgetEStruct: string.length = ${string.length}")
        if (string.length >= 18) {
            baseParameterWidgetEStruct = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )
            // Раскомментируй и реализуй парсинг пороговых значений, если потребуется:
            // openThresholdUpper = castUnsignedCharToInt(...)
            // openThresholdLower = castUnsignedCharToInt(...)
            // closeThresholdUpper = castUnsignedCharToInt(...)
            // closeThresholdLower = castUnsignedCharToInt(...)
        }

        return ThresholdParameterWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            openThresholdUpper = openThresholdUpper,
            openThresholdLower = openThresholdLower,
            closeThresholdUpper = closeThresholdUpper,
            closeThresholdLower = closeThresholdLower
        )
    }

    override fun serialize(encoder: Encoder, value: ThresholdParameterWidgetEStruct) {
        encoder.encodeString("")
    }
}