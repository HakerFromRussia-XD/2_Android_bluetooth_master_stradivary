package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
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

@Serializable(with = OpticStartLearningWidgetESerializer::class)
data class OpticStartLearningWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct,
    val startLearningButtonId: Int = 0,
    val clickCommand: Int = 0
)

object OpticStartLearningWidgetESerializer : KSerializer<OpticStartLearningWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OpticStartLearningWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OpticStartLearningWidgetEStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetEStruct: BaseParameterWidgetEStruct
        var startLearningButtonId = 0
        var clickCommand = 0

        if (string.length >= 18) {
            baseParameterWidgetEStruct = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )

            // startLearningButtonId = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            // clickCommand = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
        } else {
            baseParameterWidgetEStruct = BaseParameterWidgetEStruct(
                baseParameterWidgetStruct = BaseParameterWidgetStruct(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    mutableSetOf(ParameterInfo(0, 0, 0, 0))
                ),
                labelCode = 0
            )
        }

        return OpticStartLearningWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            startLearningButtonId = startLearningButtonId,
            clickCommand = clickCommand
        )
    }

    override fun serialize(encoder: Encoder, value: OpticStartLearningWidgetEStruct) {
        encoder.encodeString("")
    }
}