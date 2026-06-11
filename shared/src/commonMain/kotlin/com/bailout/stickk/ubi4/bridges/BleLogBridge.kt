package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.blelog.BleLogDirection
import com.bailout.stickk.ubi4.blelog.BleLogEntry
import com.bailout.stickk.ubi4.blelog.BleLogStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class BleLogEntryUi(
    val id: Long,
    val timestampMillis: Long,
    val isOutgoing: Boolean,
    val bytesHex: String
)

object BleLogBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    fun snapshot(): List<BleLogEntryUi> =
        BleLogStore.snapshot().map { it.toUi() }

    fun entriesAfter(id: Long): List<BleLogEntryUi> =
        BleLogStore.entriesAfter(id).map { it.toUi() }

    fun observeVersion(callback: (Long) -> Unit): Job =
        coroutineScope.launch {
            BleLogStore.version.collect { callback(it) }
        }

    fun setHideGraphStream(hide: Boolean) {
        BleLogStore.setHideGraphStream(hide)
    }

    private fun BleLogEntry.toUi(): BleLogEntryUi =
        BleLogEntryUi(
            id = id,
            timestampMillis = timestampMillis,
            isOutgoing = direction == BleLogDirection.OUTGOING,
            bytesHex = bytesHex
        )
}
