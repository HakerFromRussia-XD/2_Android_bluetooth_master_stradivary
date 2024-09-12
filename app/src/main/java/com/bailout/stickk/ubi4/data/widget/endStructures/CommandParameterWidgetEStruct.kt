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


@Serializable(with = CommandParameterWidgetESerializer::class)
data class CommandParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct,
    val clickCommand: Int,
    val pressedCommand: Int,
    val releasedCommand: Int
)

object CommandParameterWidgetESerializer: KSerializer<CommandParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CommandParameterWidgetEStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${string.substring(0, 10)}\"")
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        if (string.length >= 16) {
            clickCommand = castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            pressedCommand = castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
            releasedCommand = castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
        }

        return CommandParameterWidgetEStruct (
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            clickCommand = clickCommand,
            pressedCommand = pressedCommand,
            releasedCommand = releasedCommand
        )
    }

    override fun serialize(encoder: Encoder, value: CommandParameterWidgetEStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}