package com.bailout.stickk.ubi4.versions.v3.domain.blelog

import kotlinx.coroutines.flow.Flow

data class V3BleLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val isOutgoing: Boolean,
    val bytesHex: String,
)

interface V3BleLogReader {
    /** Each subscription starts with the current log, followed by new entries only. */
    fun observeEntryBatches(): Flow<List<V3BleLogEntry>>
    fun snapshot(): List<V3BleLogEntry>
    fun entriesAfter(id: Long): List<V3BleLogEntry>
    /** Current version is replayed; version changes retain the source StateFlow semantics. */
    fun observeVersions(): Flow<Long>
}

interface V3BleLogRepository : V3BleLogReader {
    fun restoreGraphStreamFilter(): Boolean
    fun setGraphStreamHidden(hidden: Boolean)
}
