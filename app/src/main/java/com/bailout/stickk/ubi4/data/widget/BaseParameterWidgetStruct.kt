package com.bailout.stickk.ubi4.data.widget

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = BaseParameterWidgetSerializer::class)
data class BaseParameterWidgetStruct(
    val widgetType: Int,      // 7 bit   младшие биты
    val widgetLabelType: Int, // 1 bit, если 1, то парсим хвост как структуку с enum иначе как структуру с именем в виде char[32]
    val widgetCode: Int,
    val display: Int,
    val widgetPosition: Int
)

object BaseParameterWidgetSerializer: KSerializer<BaseParameterWidgetStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseParameterWidgetStruct {
        val string = decoder.decodeString()
        var widgetType = 0
        var widgetLabelType = 0
        var widgetCode = 0
        var display = 0
        var widgetPosition = 0

        if (string.length >= 8) {
            widgetType = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte()) shr 0 and 0b01111111
            widgetLabelType = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte()) shr 7 and 0b00000001
            widgetCode = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            display = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            widgetPosition = castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
        }

        return BaseParameterWidgetStruct (
            widgetType = widgetType,
            widgetLabelType = widgetLabelType,
            widgetCode = widgetCode,
            display = display,
            widgetPosition = widgetPosition,
        )
    }

    override fun serialize(encoder: Encoder, value: BaseParameterWidgetStruct) {
        val code: String = value.widgetType.toString()
        encoder.encodeString("$code")
    }
}