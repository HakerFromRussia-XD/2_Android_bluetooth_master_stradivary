package com.bailout.stickk.ubi4.versions.v3.domain.blelog

import kotlinx.coroutines.flow.Flow

data class V3BleLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val isOutgoing: Boolean,
    val bytesHex: String,
)

interface V3BleLogRepository {
    /** Each subscription starts with the current log, followed by new entries only. */
    fun observeEntryBatches(): Flow<List<V3BleLogEntry>>
    fun restoreGraphStreamFilter(): Boolean
    fun setGraphStreamHidden(hidden: Boolean)
}
