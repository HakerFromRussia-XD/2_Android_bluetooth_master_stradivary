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

@Serializable(with = OpticStartLearningWidgetSSerializer::class)
data class OpticStartLearningWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct
    // Дополнительные параметры можно добавить по необходимости
)

object OpticStartLearningWidgetSSerializer : KSerializer<OpticStartLearningWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OpticStartLearningWidgetSStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OpticStartLearningWidgetSStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetSStruct: BaseParameterWidgetSStruct

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

        return OpticStartLearningWidgetSStruct(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct
        )
    }

    override fun serialize(encoder: Encoder, value: OpticStartLearningWidgetSStruct) {
        encoder.encodeString("")
    }
}