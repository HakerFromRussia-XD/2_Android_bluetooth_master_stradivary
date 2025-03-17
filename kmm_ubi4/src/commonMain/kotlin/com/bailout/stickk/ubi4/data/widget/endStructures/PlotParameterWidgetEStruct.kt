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

@Serializable(with = PlotParameterWidgetESerializer::class)
data class PlotParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val color: Int = 0,
    val maxSize: Int = 0,
    val minSize: Int = 0
)

object PlotParameterWidgetESerializer : KSerializer<PlotParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PlotParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): PlotParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()
        var color = 0
        var maxSize = 0
        var minSize = 0

        if (string.length >= 24) {
            baseParameterWidgetEStruct = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )
            color = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            maxSize = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            minSize = castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
        }

        return PlotParameterWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            color = color,
            maxSize = maxSize,
            minSize = minSize
        )
    }

    override fun serialize(encoder: Encoder, value: PlotParameterWidgetEStruct) {
        encoder.encodeString("")
    }
}