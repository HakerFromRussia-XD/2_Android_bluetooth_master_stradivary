package com.bailout.stickk.ubi4.versions.v3.domain.sync

import kotlinx.coroutines.flow.Flow

interface V3SyncRepository {
    fun observe(): Flow<V3SyncEvent>
}

sealed interface V3SyncEvent {
    data class Progress(val startup: Boolean, val fullInit: Boolean, val current: Int, val total: Int) : V3SyncEvent
    data object Ready : V3SyncEvent
}
