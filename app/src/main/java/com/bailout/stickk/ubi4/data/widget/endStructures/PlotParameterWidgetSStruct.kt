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

@Serializable(with = PlotParameterWidgetSSerializer::class)
data class PlotParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(),"NOT VALID"),
    val color: Int = 0,
    val maxSize: Int = 0,
    val minSize: Int = 0
)

object PlotParameterWidgetSSerializer: KSerializer<PlotParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): PlotParameterWidgetSStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(),"NOT VALID")
        var color = 0
        var maxSize = 0
        var minSize = 0


        if (string.length >= 86) {
            baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${string.substring(0, 80)}\"")
            color = castUnsignedCharToInt(string.substring(80, 82).toInt(16).toByte())
            maxSize = castUnsignedCharToInt(string.substring(82, 84).toInt(16).toByte())
            minSize = castUnsignedCharToInt(string.substring(84, 86).toInt(16).toByte())
        }

        return PlotParameterWidgetSStruct (
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            color = color,
            maxSize = maxSize,
            minSize = minSize
        )
    }

    override fun serialize(encoder: Encoder, value: PlotParameterWidgetSStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}
