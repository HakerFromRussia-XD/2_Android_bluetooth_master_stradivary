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
        val majorModelVersion = hex.substring(2, 4).toInt(16)
        val minorModelVersion = hex.substring(4, 6).toInt(16)
        val quickfixModelVersion = hex.substring(6, 8).toInt(16)

        return MLModelSettings(
            modelCode = modelCode,
            majorModelVersion = majorModelVersion,
            minorModelVersion = minorModelVersion,
            quickfixModelVersion = quickfixModelVersion
        )
    }

    override fun serialize(encoder: Encoder, value: MLModelSettings) =
        encoder.encodeString("")
}