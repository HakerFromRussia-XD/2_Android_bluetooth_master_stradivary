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

@Serializable(with = SwitchParameterWidgetESerializer::class)
data class SwitchParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val switchChecked: Boolean = false,
    val clickCommand: Int = 0
)

object SwitchParameterWidgetESerializer : KSerializer<SwitchParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SwitchParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SwitchParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()
        var switchChecked = false

        if (string.length >= 20) {
            baseParameterWidgetEStruct = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )
            switchChecked = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte()) != 0
        }

        return SwitchParameterWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            switchChecked = switchChecked,
            clickCommand = 0 // Если нужно распарсить clickCommand, доработай здесь
        )
    }

    override fun serialize(encoder: Encoder, value: SwitchParameterWidgetEStruct) {
        encoder.encodeString("")
    }
}