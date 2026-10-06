package com.bailout.stickk.ubi4.blelog

import com.bailout.stickk.ubi4.utility.currentTimeMillis
import com.bailout.stickk.ubi4.utility.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private var nextId = 0L
    private val lock = Any()
    private val storedEntries = ArrayList<BleLogEntry>()
    private val mutableVersion = MutableStateFlow(0L)
    private var hideGraphStream = true

    val version: StateFlow<Long> = mutableVersion.asStateFlow()

    fun logOutgoing(data: ByteArray?) {
        append(BleLogDirection.OUTGOING, data)
    }

    fun logIncoming(data: ByteArray?) {
        append(BleLogDirection.INCOMING, data)
    }

    fun shouldLogIncoming(isGraphStream: Boolean): Boolean =
        !isGraphStream || !hideGraphStream

    fun setHideGraphStream(hide: Boolean) {
        hideGraphStream = hide
    }

    fun isHideGraphStreamEnabled(): Boolean = hideGraphStream

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
        val entry = synchronized(lock) {
            nextId += 1
            BleLogEntry(
                id = nextId,
                timestampMillis = currentTimeMillis(),
                direction = direction,
                bytesHex = data.toHexBytes()
            ).also { storedEntries.add(it) }
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
