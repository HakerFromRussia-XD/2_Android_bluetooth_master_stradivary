package com.bailout.stickk.ubi4.versions.v3.data.phonesync

import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncCommand
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncReply
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncRepository
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/** Receives only Android serial notifications; it does not change the shared parser or iOS. */
class V3PhoneSyncRepositoryImpl(
    private val store: V3PhoneSyncStateStore,
    private val send: (ByteArray) -> Boolean,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) : V3PhoneSyncRepository {
    override val state get() = store.state
    override var sessionId = 0L
        private set
    private var replies = Channel<V3PhoneSyncReply>(Channel.UNLIMITED)
    private var echoDeadline: Long? = null
    @Volatile private var connected = false

    val accessGranted: Boolean get() = connected &&
        (state.value == V3PhoneSyncState.LegacyAllowed || state.value == V3PhoneSyncState.Authorized)

    fun beginSession() {
        replies.close()
        replies = Channel(Channel.UNLIMITED)
        sessionId++
        connected = true
        echoDeadline = null
        store.publish(V3PhoneSyncState.Probing)
    }

    fun endSession() {
        if (connected && state.value.isPending) store.cancel()
        connected = false
        echoDeadline = null
        replies.close()
    }

    override fun cancel() = store.cancel()

    override fun publish(session: Long, value: V3PhoneSyncState) {
        if (session == sessionId && connected) store.publish(value)
    }

    fun onPacket(bytes: ByteArray): Boolean {
        if (!V3PhoneSyncProtocol.isControlPacket(bytes)) return false
        val reply = V3PhoneSyncProtocol.decode(bytes) ?: return true
        if (!connected || !state.value.isPending) return true
        if (reply == V3PhoneSyncReply.Echo &&
            (state.value != V3PhoneSyncState.Probing || echoDeadline?.let { clockMillis() > it } != false)) return true
        replies.trySend(reply)
        return true
    }

    override suspend fun request(session: Long, command: V3PhoneSyncCommand, timeoutMs: Long?): V3PhoneSyncReply? {
        if (!connected || session != sessionId || state.value == V3PhoneSyncState.Failed) return V3PhoneSyncReply.Disconnected
        val channel = replies
        val sentAt = clockMillis()
        if (command == V3PhoneSyncCommand.Echo) echoDeadline = sentAt + requireNotNull(timeoutMs)
        if (!send(V3PhoneSyncProtocol.packet(command))) return V3PhoneSyncReply.SendFailed
        suspend fun receive(): V3PhoneSyncReply {
            while (true) {
                val reply = channel.receiveCatching().getOrNull() ?: return V3PhoneSyncReply.Disconnected
                if (session != sessionId || state.value == V3PhoneSyncState.Failed) return V3PhoneSyncReply.Disconnected
                if (when (command) {
                    V3PhoneSyncCommand.Echo -> reply == V3PhoneSyncReply.Echo
                    V3PhoneSyncCommand.Check -> reply == V3PhoneSyncReply.Bound || reply == V3PhoneSyncReply.NotBound
                    V3PhoneSyncCommand.Start -> reply == V3PhoneSyncReply.Started || reply == V3PhoneSyncReply.Success || reply == V3PhoneSyncReply.Failure
                }) return reply
            }
        }
        return if (timeoutMs == null) receive() else {
            val remaining = timeoutMs - (clockMillis() - sentAt)
            if (remaining <= 0) null else withTimeoutOrNull(remaining) { receive() }
        }
    }

    override suspend fun awaitResult(session: Long): V3PhoneSyncReply {
        val channel = replies
        while (connected && session == sessionId && state.value != V3PhoneSyncState.Failed) {
            val reply = channel.receiveCatching().getOrNull() ?: return V3PhoneSyncReply.Disconnected
            if (reply == V3PhoneSyncReply.Success || reply == V3PhoneSyncReply.Failure) return reply
        }
        return V3PhoneSyncReply.Disconnected
    }
}
