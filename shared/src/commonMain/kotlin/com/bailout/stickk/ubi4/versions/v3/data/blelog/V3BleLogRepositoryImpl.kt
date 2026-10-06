package com.bailout.stickk.ubi4.versions.v3.data.blelog

import com.bailout.stickk.ubi4.blelog.BleLogDirection
import com.bailout.stickk.ubi4.blelog.BleLogEntry
import com.bailout.stickk.ubi4.blelog.BleLogStore
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.V3BleLogEntry
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.V3BleLogRepository
import kotlinx.coroutines.flow.flow

class V3BleLogRepositoryImpl(
    private val readGraphStreamHidden: () -> Boolean,
    private val saveGraphStreamHidden: (Boolean) -> Unit,
) : V3BleLogRepository {
    override fun observeEntryBatches() = flow {
        val initial = BleLogStore.snapshot()
        var lastId = initial.lastOrNull()?.id ?: 0L
        emit(initial.map { it.toDomain() })
        BleLogStore.version.collect {
            val added = BleLogStore.entriesAfter(lastId)
            if (added.isNotEmpty()) {
                lastId = added.last().id
                emit(added.map { it.toDomain() })
            }
        }
    }

    override fun restoreGraphStreamFilter(): Boolean =
        readGraphStreamHidden().also(BleLogStore::setHideGraphStream)

    override fun setGraphStreamHidden(hidden: Boolean) {
        saveGraphStreamHidden(hidden)
        BleLogStore.setHideGraphStream(hidden)
    }

    private fun BleLogEntry.toDomain() = V3BleLogEntry(
        id, timestampMillis, direction == BleLogDirection.OUTGOING, bytesHex,
    )
}
