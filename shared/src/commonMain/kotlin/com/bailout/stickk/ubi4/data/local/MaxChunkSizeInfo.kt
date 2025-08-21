package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local

import io.ktor.utils.io.core.ByteOrder

data class MaxChunkSizeInfo(
    val chunkSize: Int,
    val timeoutMs: Int,
    val bytesInterval: Int,
    val flashClearDelayMs: Int
)

fun ByteArray.toMaxChunkSizeInfo(): MaxChunkSizeInfo {
    // 1) Минимум 2 байта
    require(this.size >= 2) { "GET_MAX_CHANK_SIZE payload too small: ${this.size}" }

    // 2) Первые два байта — размер чанка (UInt16 LE)
    val chunkSize = (this[0].toUByte().toInt() or (this[1].toUByte().toInt() shl 8))

    // 3) Если старый формат — ровно 2 байта
    if (this.size == 2) {
        return MaxChunkSizeInfo(
            chunkSize         = chunkSize,
            timeoutMs         = 0,
            bytesInterval     = 0,
            flashClearDelayMs = 0
        )
    }

    // 4) Новый формат: минимум 8+2 резерв = 10 байт
    require(this.size >= 10) { "GET_MAX_CHANK_SIZE payload corrupted: ${this.size} B" }


    val bytesInterval     = (this[2].toUByte().toInt() or (this[3].toUByte().toInt() shl 8))
    val timeoutMs         = (this[6].toUByte().toInt() or (this[7].toUByte().toInt() shl 8))
    val flashClearDelayMs = (this[8].toUByte().toInt() or (this[9].toUByte().toInt() shl 8))

    return MaxChunkSizeInfo(
        chunkSize         = chunkSize,
        timeoutMs         = timeoutMs,
        bytesInterval     = bytesInterval,
        flashClearDelayMs = flashClearDelayMs
    )
}
