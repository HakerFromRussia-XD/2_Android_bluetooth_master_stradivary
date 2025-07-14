package com.bailout.stickk.ubi4.data.widget.subStructures

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo

@Serializable(with = BaseParameterWidgetSerializer::class)
data class BaseParameterWidgetStruct(
    val widgetType: Int = 0,      // младшие 7 бит
    val widgetLabelType: Int = 0, // старший 1 бит
    var widgetCode: Int = 0,
    val display: Int = 0,         // номер экрана, на котором располагается виджет
    var widgetPosition: Int = 0,  // позиция виджета
    var deviceId: Int = 0,
    val widgetId: Int = 0,
    val dataOffset: Int = 0,
    var dataSize: Int = 0,
    var channelOffset: Int = 0,   // если понадобится, можно добавить в десериализацию
    var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> =
        mutableSetOf(ParameterInfo(0, 0, 0, 0)),
    var keyMobileSettings: String = ""
)

object BaseParameterWidgetSerializer : KSerializer<BaseParameterWidgetStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BaseParameterWidgetStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseParameterWidgetStruct {
        val string = decoder.decodeString()
        var widgetType = 0
        var widgetLabelType = 0
        var widgetCode = 0
        var display = 0
        var widgetPosition = 0
        var deviceId = 0
        var widgetId = 0
        var dataOffset = 0
        var dataSize = 0
        val parentIDParameter = mutableSetOf<ParameterInfo<Int, Int, Int, Int>>()

        if (string.length >= 16) {
            val firstByte = string.substring(0, 2).toInt(16).toByte()
            widgetType = castUnsignedCharToInt(firstByte) and 0b01111111
            widgetLabelType = castUnsignedCharToInt(firstByte) shr 7 and 0b00000001
            widgetCode = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            display = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            widgetPosition = castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            deviceId = castUnsignedCharToInt(string.substring(8, 10).toInt(16).toByte())
            widgetId = castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            dataOffset = castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
            dataSize = castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
        }

        return BaseParameterWidgetStruct(
            widgetType = widgetType,
            widgetLabelType = widgetLabelType,
            widgetCode = widgetCode,
            display = display,
            widgetPosition = widgetPosition,
            deviceId = deviceId,
            widgetId = widgetId,
            dataOffset = dataOffset,
            dataSize = dataSize,
            parameterInfoSet = parentIDParameter,
            keyMobileSettings = ""
        )
    }

    override fun serialize(encoder: Encoder, value: BaseParameterWidgetStruct) {
        // Пока не реализовано, можно доработать по необходимости
        encoder.encodeString("${value.widgetType}")
    }
}