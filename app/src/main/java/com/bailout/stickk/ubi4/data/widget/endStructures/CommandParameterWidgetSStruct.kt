package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


@Serializable(with = CommandParameterWidgetSSerializer::class)
data class CommandParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct,
    val clickCommand: Int,
    val pressedCommand: Int,
    val releasedCommand: Int
)

object CommandParameterWidgetSSerializer: KSerializer<CommandParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CommandParameterWidgetSStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${string.substring(0, 72)}\"")
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        if (string.length >= 78) {
            clickCommand = castUnsignedCharToInt(string.substring(72, 74).toInt(16).toByte())
            pressedCommand = castUnsignedCharToInt(string.substring(74, 76).toInt(16).toByte())
            releasedCommand = castUnsignedCharToInt(string.substring(76, 78).toInt(16).toByte())
        }

        return CommandParameterWidgetSStruct (
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            clickCommand = clickCommand,
            pressedCommand = pressedCommand,
            releasedCommand = releasedCommand
        )
    }

    override fun serialize(encoder: Encoder, value: CommandParameterWidgetSStruct) {
        val code = ""
        encoder.encodeString("$code")
    }
}