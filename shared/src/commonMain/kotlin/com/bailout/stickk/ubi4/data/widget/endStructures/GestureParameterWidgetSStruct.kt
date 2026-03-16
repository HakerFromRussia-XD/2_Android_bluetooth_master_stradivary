package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = GestureParameterWidgetSSerializer::class)
data class GestureParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct = BaseParameterWidgetSStruct()
)

object GestureParameterWidgetSSerializer : KSerializer<GestureParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("GestureParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): GestureParameterWidgetSStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetSStruct: BaseParameterWidgetSStruct

        if (string.length >= 82) {
            baseParameterWidgetSStruct = Json.decodeFromString(
                BaseParameterWidgetSStruct.serializer(),
                "\"${string.substring(0, 80)}\""
            )
        } else {
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(
                baseParameterWidgetStruct = BaseParameterWidgetStruct(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    mutableSetOf(ParameterInfo(0, 0, 0, 0))
                ),
                label = ""
            )
        }

        return GestureParameterWidgetSStruct(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct
        )
    }

    override fun serialize(encoder: Encoder, value: GestureParameterWidgetSStruct) {
        encoder.encodeString("")
    }
}