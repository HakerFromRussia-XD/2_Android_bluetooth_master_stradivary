package com.bailout.stickk.ubi4.data.widget.endStructures

import android.util.Pair
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


@Serializable(with = CommandParameterWidgetSSerializer::class)
data class CommandParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct,
    val clickCommand: Int = 0,
    val pressedCommand: Int = 0,
    val releasedCommand: Int = 0
)

object CommandParameterWidgetSSerializer: KSerializer<CommandParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CommandParameterWidgetSStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetSStruct: BaseParameterWidgetSStruct
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        if (string.length >= 86) {
            baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${string.substring(0, 80)}\"")
            clickCommand = castUnsignedCharToInt(string.substring(80, 82).toInt(16).toByte())
            pressedCommand = castUnsignedCharToInt(string.substring(82, 84).toInt(16).toByte())
            releasedCommand = castUnsignedCharToInt(string.substring(84, 86).toInt(16).toByte())
        } else {
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct (BaseParameterWidgetStruct(0, 0, 0, 0, 0, 0, 0, 0, 0, mutableSetOf(Pair(0,0))),"")
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