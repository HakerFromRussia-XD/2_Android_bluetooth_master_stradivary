package com.bailout.stickk.ubi4.data.parser

data class UbiPacketView(
    val type: UbiPacketType,
    val address: Int,
    val command: Int,

    val payloadSize: Int,          // для SHORT = 2, для LONG = из заголовка
    val headerCrcError: Boolean,

    val payloadCrcError: Boolean,

    // View в исходный data:
    val payloadOffset: Int,        // где начинается payload
    val payloadLength: Int,         // сколько байт payload доступно (если не complete — может быть 0)

    val payload: ByteArrayView
)
enum class UbiPacketType { SHORT, LONG }
data class ByteArrayView(val bytes: ByteArray, val offset: Int, val length: Int) {
    init {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) {
            "ByteArrayView out of bounds: bytes=${bytes.size}, offset=$offset, length=$length"
        }
    }

    operator fun get(i: Int): Byte {
        require(i in 0 until length) { "Index out of bounds: $i, length=$length" }
        return bytes[offset + i]
    }

    /** Если вдруг реально нужен отдельный ByteArray (это уже будет аллокация) */
    fun toByteArray(): ByteArray = bytes.copyOfRange(offset, offset + length)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteArrayView

        if (!bytes.contentEquals(other.bytes)) return false
        if (offset != other.offset) return false
        if (length != other.length) return false

        return true
    }
    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + offset
        result = 31 * result + length
        return result
    }
}