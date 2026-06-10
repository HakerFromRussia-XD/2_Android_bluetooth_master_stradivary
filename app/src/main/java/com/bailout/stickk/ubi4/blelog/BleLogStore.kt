package com.bailout.stickk.ubi4.blelog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

data class BleLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val direction: BleLogDirection,
    val bytesHex: String
)

enum class BleLogDirection {
    OUTGOING,
    INCOMING
}

object BleLogStore {
    private val nextId = AtomicLong(0L)
    private val lock = Any()
    private val storedEntries = ArrayList<BleLogEntry>()
    private val mutableVersion = MutableStateFlow(0L)

    val version: StateFlow<Long> = mutableVersion.asStateFlow()

    @JvmStatic
    fun logOutgoing(data: ByteArray?) {
        append(BleLogDirection.OUTGOING, data)
    }

    @JvmStatic
    fun logIncoming(data: ByteArray?) {
        append(BleLogDirection.INCOMING, data)
    }

    fun snapshot(): List<BleLogEntry> =
        synchronized(lock) {
            storedEntries.toList()
        }

    fun entriesAfter(id: Long): List<BleLogEntry> =
        synchronized(lock) {
            val startIndex = storedEntries.indexOfFirst { it.id > id }
            if (startIndex == -1) {
                emptyList()
            } else {
                storedEntries.subList(startIndex, storedEntries.size).toList()
            }
        }

    private fun append(direction: BleLogDirection, data: ByteArray?) {
        if (data == null || data.isEmpty()) return
        val entry = BleLogEntry(
            id = nextId.incrementAndGet(),
            timestampMillis = System.currentTimeMillis(),
            direction = direction,
            bytesHex = data.toHexBytes()
        )

        synchronized(lock) {
            storedEntries.add(entry)
        }
        mutableVersion.value = entry.id
    }

    private fun ByteArray.toHexBytes(): String =
        joinToString(separator = " ") { byte ->
            val value = byte.toInt() and 0xFF
            val high = HEX[value ushr 4]
            val low = HEX[value and 0x0F]
            "$high$low"
        }

    private const val HEX = "0123456789ABCDEF"
}
