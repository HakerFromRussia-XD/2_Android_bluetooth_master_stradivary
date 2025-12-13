package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.data.local.MLModelSettings
import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MLModelSettingsSerializer : KSerializer<MLModelSettings> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MLModelSettings", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): MLModelSettings {
        val hex = decoder.decodeString().padEnd(16, '0')

        val modelCode = hex.substring(0, 2).toInt(16)
        val modelVersion = hex.substring(2, 16).decodeHex().trimEnd('\u0000')

        return MLModelSettings(
            modelCode = modelCode,
            modelVersion = modelVersion
        )
    }

    override fun serialize(encoder: Encoder, value: MLModelSettings) =
        encoder.encodeString("")
}