package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BindingGroupSerializer::class)
data class BindingGestureGroup(
    var gestureSpr1Id: Int = 0, var gesture1Id: Int = 0,
    var gestureSpr2Id: Int = 0, var gesture2Id: Int = 0,
    var gestureSpr3Id: Int = 0, var gesture3Id: Int = 0,
    var gestureSpr4Id: Int = 0, var gesture4Id: Int = 0,
    var gestureSpr5Id: Int = 0, var gesture5Id: Int = 0,
    var gestureSpr6Id: Int = 0, var gesture6Id: Int = 0,
    var gestureSpr7Id: Int = 0, var gesture7Id: Int = 0,
    var gestureSpr8Id: Int = 0, var gesture8Id: Int = 0,
    var gestureSpr9Id: Int = 0, var gesture9Id: Int = 0,
    var gestureSpr10Id: Int = 0, var gesture10Id: Int = 0,
    var gestureSpr11Id: Int = 0, var gesture11Id: Int = 0,
    var gestureSpr12Id: Int = 0, var gesture12Id: Int = 0,
) {
    companion object {
        const val PAIR_COUNT = 12
        const val PAYLOAD_SIZE = PAIR_COUNT * 2

        fun fromPayloadBytes(bytes: ByteArray, offset: Int = 0): BindingGestureGroup {
            val group = BindingGestureGroup()
            repeat(PAIR_COUNT) { index ->
                val pairOffset = offset + index * 2
                val sprId = bytes.getOrNull(pairOffset)?.toInt()?.and(0xFF) ?: 0
                val gestureId = bytes.getOrNull(pairOffset + 1)?.toInt()?.and(0xFF) ?: 0
                group.setGestureAt(index, sprId to gestureId)
            }
            return group
        }

        fun fromHexString(hex: String): BindingGestureGroup {
            val normalized = hex.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            val bytes = ByteArray(PAYLOAD_SIZE)
            repeat(PAYLOAD_SIZE) { index ->
                val start = index * 2
                val token = if (start < normalized.length) {
                    normalized.substring(start, (start + 2).coerceAtMost(normalized.length))
                } else {
                    ""
                }
                bytes[index] = token.padEnd(2, '0').toIntOrNull(16)?.toByte() ?: 0
            }
            return fromPayloadBytes(bytes)
        }
    }

    fun toGestureList(): MutableList<Pair<Int, Int>> {
        return listOf(
            Pair(gestureSpr1Id, gesture1Id),
            Pair(gestureSpr2Id, gesture2Id),
            Pair(gestureSpr3Id, gesture3Id),
            Pair(gestureSpr4Id, gesture4Id),
            Pair(gestureSpr5Id, gesture5Id),
            Pair(gestureSpr6Id, gesture6Id),
            Pair(gestureSpr7Id, gesture7Id),
            Pair(gestureSpr8Id, gesture8Id),
            Pair(gestureSpr9Id, gesture9Id),
            Pair(gestureSpr10Id, gesture10Id),
            Pair(gestureSpr11Id, gesture11Id),
            Pair(gestureSpr12Id, gesture12Id),
        ).toMutableList()
    }
    fun setGestureAt(index: Int, pair: Pair<Int, Int>) {
        when (index) {
            0 -> { gestureSpr1Id = pair.first; gesture1Id = pair.second }
            1 -> { gestureSpr2Id = pair.first; gesture2Id = pair.second }
            2 -> { gestureSpr3Id = pair.first; gesture3Id = pair.second }
            3 -> { gestureSpr4Id = pair.first; gesture4Id = pair.second }
            4 -> { gestureSpr5Id = pair.first; gesture5Id = pair.second }
            5 -> { gestureSpr6Id = pair.first; gesture6Id = pair.second }
            6 -> { gestureSpr7Id = pair.first; gesture7Id = pair.second }
            7 -> { gestureSpr8Id = pair.first; gesture8Id = pair.second }
            8 -> { gestureSpr9Id = pair.first; gesture9Id = pair.second }
            9 -> { gestureSpr10Id = pair.first; gesture10Id = pair.second }
            10 -> { gestureSpr11Id = pair.first; gesture11Id = pair.second }
            11 -> { gestureSpr12Id = pair.first; gesture12Id = pair.second }
            else -> throw IndexOutOfBoundsException("Индекс должен быть от 0 до 11")
        }
    }

    fun toPayloadBytes(): ByteArray =
        ByteArray(PAYLOAD_SIZE).also { payload ->
            toGestureList().take(PAIR_COUNT).forEachIndexed { index, pair ->
                payload[index * 2] = pair.first.coerceIn(0, 255).toByte()
                payload[index * 2 + 1] = pair.second.coerceIn(0, 255).toByte()
            }
        }

    fun toHexString(): String =
        toPayloadBytes().joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
}

object BindingGroupSerializer : KSerializer<BindingGestureGroup> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BindingGroupSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BindingGestureGroup {
        val string = decoder.decodeString()

        var gestureSpr1Id = 0
        var gestureSpr2Id = 0
        var gestureSpr3Id = 0
        var gestureSpr4Id = 0
        var gestureSpr5Id = 0
        var gestureSpr6Id = 0
        var gestureSpr7Id = 0
        var gestureSpr8Id = 0
        var gestureSpr9Id = 0
        var gestureSpr10Id = 0
        var gestureSpr11Id = 0
        var gestureSpr12Id = 0

        var gesture1Id = 0
        var gesture2Id = 0
        var gesture3Id = 0
        var gesture4Id = 0
        var gesture5Id = 0
        var gesture6Id = 0
        var gesture7Id = 0
        var gesture8Id = 0
        var gesture9Id = 0
        var gesture10Id = 0
        var gesture11Id = 0
        var gesture12Id = 0

        if (string.length >= BindingGestureGroup.PAYLOAD_SIZE * 2) {
            gestureSpr1Id = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            gesture1Id = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            gestureSpr2Id = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            gesture2Id = castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            gestureSpr3Id = castUnsignedCharToInt(string.substring(8, 10).toInt(16).toByte())
            gesture3Id = castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            gestureSpr4Id = castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
            gesture4Id = castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
            gestureSpr5Id = castUnsignedCharToInt(string.substring(16, 18).toInt(16).toByte())
            gesture5Id = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            gestureSpr6Id = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            gesture6Id = castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
            gestureSpr7Id = castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte())
            gesture7Id = castUnsignedCharToInt(string.substring(26, 28).toInt(16).toByte())
            gestureSpr8Id = castUnsignedCharToInt(string.substring(28, 30).toInt(16).toByte())
            gesture8Id = castUnsignedCharToInt(string.substring(30, 32).toInt(16).toByte())
            gestureSpr9Id = castUnsignedCharToInt(string.substring(32, 34).toInt(16).toByte())
            gesture9Id = castUnsignedCharToInt(string.substring(34, 36).toInt(16).toByte())
            gestureSpr10Id = castUnsignedCharToInt(string.substring(36, 38).toInt(16).toByte())
            gesture10Id = castUnsignedCharToInt(string.substring(38, 40).toInt(16).toByte())
            gestureSpr11Id = castUnsignedCharToInt(string.substring(40, 42).toInt(16).toByte())
            gesture11Id = castUnsignedCharToInt(string.substring(42, 44).toInt(16).toByte())
            gestureSpr12Id = castUnsignedCharToInt(string.substring(44, 46).toInt(16).toByte())
            gesture12Id = castUnsignedCharToInt(string.substring(46, 48).toInt(16).toByte())
        }

        return BindingGestureGroup(
            gestureSpr1Id = gestureSpr1Id,
            gestureSpr2Id = gestureSpr2Id,
            gestureSpr3Id = gestureSpr3Id,
            gestureSpr4Id = gestureSpr4Id,
            gestureSpr5Id = gestureSpr5Id,
            gestureSpr6Id = gestureSpr6Id,
            gestureSpr7Id = gestureSpr7Id,
            gestureSpr8Id = gestureSpr8Id,
            gestureSpr9Id = gestureSpr9Id,
            gestureSpr10Id = gestureSpr10Id,
            gestureSpr11Id = gestureSpr11Id,
            gestureSpr12Id = gestureSpr12Id,

            gesture1Id = gesture1Id,
            gesture2Id = gesture2Id,
            gesture3Id = gesture3Id,
            gesture4Id = gesture4Id,
            gesture5Id = gesture5Id,
            gesture6Id = gesture6Id,
            gesture7Id = gesture7Id,
            gesture8Id = gesture8Id,
            gesture9Id = gesture9Id,
            gesture10Id = gesture10Id,
            gesture11Id = gesture11Id,
            gesture12Id = gesture12Id,
        )
    }

    override fun serialize(encoder: Encoder, value: BindingGestureGroup) {
        encoder.encodeString(value.toHexString())
    }
}
