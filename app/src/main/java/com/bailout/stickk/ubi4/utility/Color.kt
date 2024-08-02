package com.bailout.stickk.ubi4.utility

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okhttp3.internal.toHexString

//@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable(with = SingleColorSerializer::class)
data class Color(val r: Int, val g: Int, val b: Int)

object SingleColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Color {
        val string = decoder.decodeString()
        return Color(1, 1, 1)
    }

    override fun serialize(encoder: Encoder, value: Color) {
        val code: String =
            value.r.toHexString() +
                    value.g.toHexString() +
                    value.b.toHexString()

        encoder.encodeString("#$code")
    }
}