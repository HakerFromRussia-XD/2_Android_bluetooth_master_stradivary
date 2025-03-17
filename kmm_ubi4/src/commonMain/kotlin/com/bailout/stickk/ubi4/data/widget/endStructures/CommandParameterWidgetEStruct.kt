package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

// Функция для корректного преобразования байта в беззнаковое целое значение в KMM.
internal fun castUnsignedCharToInt(byte: Byte): Int = byte.toInt() and 0xFF

@Serializable(with = CommandParameterWidgetESerializer::class)
data class CommandParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val clickCommand: Int = 0,
    val pressedCommand: Int = 0,
    val releasedCommand: Int = 0,
)

object CommandParameterWidgetESerializer : KSerializer<CommandParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("CommandParameterWidgetEStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CommandParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0

        if (string.length >= 24) {
            baseParameterWidgetEStruct = Json.decodeFromString(
                BaseParameterWidgetEStruct.serializer(),
                "\"${string.substring(0, 18)}\""
            )
            clickCommand = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            pressedCommand = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            releasedCommand = castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
        }

        return CommandParameterWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            clickCommand = clickCommand,
            pressedCommand = pressedCommand,
            releasedCommand = releasedCommand
        )
    }

    override fun serialize(encoder: Encoder, value: CommandParameterWidgetEStruct) {
        // Пока не реализовано: сериализация по необходимости.
        encoder.encodeString("")
    }
}