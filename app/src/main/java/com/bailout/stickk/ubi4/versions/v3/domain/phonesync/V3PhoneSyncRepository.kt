package com.bailout.stickk.ubi4.versions.v3.domain.phonesync

import kotlinx.coroutines.flow.StateFlow

enum class V3PhoneSyncState {
    Idle, Probing, Checking, Starting, WaitingForUser, LegacyAllowed, Authorized, Failed;

    val isPending: Boolean
        get() = this == Probing || this == Checking || this == Starting || this == WaitingForUser
}

enum class V3PhoneSyncCommand { Echo, Check, Start }
enum class V3PhoneSyncReply { Echo, NotBound, Bound, Started, Success, Failure, Disconnected, SendFailed }

interface V3PhoneSyncStateRepository {
    val state: StateFlow<V3PhoneSyncState>
    fun cancel()
}

interface V3PhoneSyncRepository : V3PhoneSyncStateRepository {
    val sessionId: Long
    fun publish(session: Long, value: V3PhoneSyncState)
    suspend fun request(session: Long, command: V3PhoneSyncCommand, timeoutMs: Long? = null): V3PhoneSyncReply?
    suspend fun awaitResult(session: Long): V3PhoneSyncReply
}
