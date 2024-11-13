package com.bailout.stickk.ubi4.data.widget.subStructures

import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


@Serializable(with = BaseParameterWidgetSSerializer::class)
data class BaseParameterWidgetSStruct(
    val baseParameterWidgetStruct: BaseParameterWidgetStruct,
    val label: String = ""
)

object BaseParameterWidgetSSerializer: KSerializer<BaseParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseParameterWidgetSStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${string.substring(0, 16)}\"")
        var label = ""


        if (string.length >= 80) {
            label = string.substring(16, 80).decodeHex()
        }

        return BaseParameterWidgetSStruct (
            baseParameterWidgetStruct = baseParameterWidgetStruct,
            label = label
        )
    }

    override fun serialize(encoder: Encoder, value: BaseParameterWidgetSStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}