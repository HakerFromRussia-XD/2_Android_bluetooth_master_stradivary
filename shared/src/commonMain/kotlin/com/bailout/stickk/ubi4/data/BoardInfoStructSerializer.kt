package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.data.local.BoardInfoStruct
import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BoardInfoStructSerializer : KSerializer<BoardInfoStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BoardInfoStruct", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BoardInfoStruct {
        val hex = decoder.decodeString().padEnd(90, '0')

        fun hexByte(pos: Int): Int {
            return if (pos + 2 <= hex.length) {
                hex.substring(pos, pos + 2).toInt(16)
            }
            else {
                0
            }
        }

        fun hexLE16(pos: Int): Int {
            return if (pos + 4 <= hex.length) {
                val lowByte = hex.substring(pos, pos + 2).toInt(16)
                val highByte = hex.substring(pos + 2, pos + 4).toInt(16)
                lowByte or (highByte shl 8)
            }
            else {
                0
            }
        }

        fun hexLE32(pos: Int): Long {
            return if (pos + 8 <= hex.length) {
                val bytes = hex.substring(pos, pos + 8).chunked(2).map { it.toInt(16) }
                bytes[0].toLong() or
                        (bytes[1].toLong() shl 8) or
                        (bytes[2].toLong() shl 16) or
                        (bytes[3].toLong() shl 24)
            }
            else {
                0L
            }
        }

        return BoardInfoStruct(
            boardName = hex.substring(0, 64).decodeHex().trimEnd('\u0000'),
            boardVersion = hexByte(64),
            boardSubVersion = hexByte(66),
            boardRev = hexByte(68),
            boardSubRev = hexByte(70),
            boardBuild = hexLE16(72),
            boardType = hexByte(76),
            boardCode = hexByte(78),
            boardAdditionalInfoType = hexByte(80),
            boardAdditionalInfo = hexLE32(82)
        )
    }

    override fun serialize(encoder: Encoder, value: BoardInfoStruct) =
        encoder.encodeString("")
}