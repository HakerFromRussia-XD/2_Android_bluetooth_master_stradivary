package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
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
import android.util.Pair


@Serializable(with = CommandParameterWidgetESerializer::class)
data class CommandParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct,
    val clickCommand: Int = 0,
    val pressedCommand: Int = 0,
    val releasedCommand: Int = 0,
)

object CommandParameterWidgetESerializer: KSerializer<CommandParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CommandParameterWidgetEStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetEStruct: BaseParameterWidgetEStruct
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        if (string.length >= 24) {
            baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${string.substring(0, 18)}\"")
            clickCommand = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            pressedCommand = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            releasedCommand = castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
        } else {
            baseParameterWidgetEStruct = BaseParameterWidgetEStruct (BaseParameterWidgetStruct(0, 0, 0, 0, 0, 0, 0, 0, 0, mutableSetOf(Pair(0,0))),0)
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