package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.data.local.FirmwareInfoStruct
import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


/** 76-байтный `base_FWInfo_struct` → Kotlin-объект */
object FirmwareInfoStructSerializer : KSerializer<FirmwareInfoStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FirmwareInfoStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FirmwareInfoStruct {
        val hex = decoder.decodeString().padEnd(152, '0')        // 76 байт = 152 hex-цифры

        fun hexByte(pos: Int) = hex.substring(pos, pos + 2).toInt(16)
        fun hexLE32(pos: Int) = hex.substring(pos, pos + 8)
            .chunked(2).reversed().joinToString("").toLong(16)

        return FirmwareInfoStruct(
            fwName         = hex.substring(  0, 64).decodeHex().trimEnd('\u0000'),
            fwMajor        = hexByte(64),
            fwMinor        = hexByte(66),
            fwQuickFix     = hexByte(68),
            fwSinceLastTag = hexByte(70),

            fwLabel        = hex.substring(72, 104).decodeHex().trimEnd('\u0000'),
            fwType         = hexByte(104),
            fwCode         = hexByte(106),

            fwStartAddress = hexLE32(108),
            fwSize         = hexLE32(116),
            fwCrc          = hexLE32(124),

            sdkMajor       = hexByte(132),
            sdkMinor       = hexByte(134),
            sdkQuickFix    = hexByte(136),
            sdkSinceLastTag= hexByte(138),

            fwAdditionalInfoType = hexByte(140),
            fwAdditionalInfo     = hexLE32(142)
        )
    }

    override fun serialize(encoder: Encoder, value: FirmwareInfoStruct) =
        encoder.encodeString("")         // не нужен
}