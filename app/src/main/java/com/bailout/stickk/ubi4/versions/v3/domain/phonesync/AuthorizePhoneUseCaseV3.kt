package com.bailout.stickk.ubi4.versions.v3.domain.phonesync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The compatibility decision belongs to a connection, never to the saved device address. */
class AuthorizePhoneUseCaseV3(private val repository: V3PhoneSyncRepository) {
    private val mutex = Mutex()
    private var authorizedSession: Long? = null

    suspend operator fun invoke(): Boolean = mutex.withLock {
        val session = repository.sessionId
        if (repository.state.value == V3PhoneSyncState.Failed || session == 0L) return@withLock false
        if (authorizedSession == session) return@withLock true

        fun finish(allowed: Boolean, legacy: Boolean = false): Boolean {
            if (session != repository.sessionId || repository.state.value == V3PhoneSyncState.Failed) return false
            repository.publish(session, when {
                !allowed -> V3PhoneSyncState.Failed
                legacy -> V3PhoneSyncState.LegacyAllowed
                else -> V3PhoneSyncState.Authorized
            })
            if (allowed) authorizedSession = session
            return allowed
        }

        repository.publish(session, V3PhoneSyncState.Probing)
        when (repository.request(session, V3PhoneSyncCommand.Echo, ECHO_TIMEOUT_MS)) {
            V3PhoneSyncReply.Echo -> return@withLock finish(true, legacy = true)
            null -> Unit // Only the missing ECHO response selects the new protocol.
            else -> return@withLock finish(false)
        }
        repository.publish(session, V3PhoneSyncState.Checking)
        when (repository.request(session, V3PhoneSyncCommand.Check)) {
            V3PhoneSyncReply.Bound -> return@withLock finish(true)
            V3PhoneSyncReply.NotBound -> Unit
            else -> return@withLock finish(false)
        }
        repository.publish(session, V3PhoneSyncState.Starting)
        when (repository.request(session, V3PhoneSyncCommand.Start)) {
            V3PhoneSyncReply.Success -> return@withLock finish(true)
            V3PhoneSyncReply.Started -> Unit
            else -> return@withLock finish(false)
        }
        repository.publish(session, V3PhoneSyncState.WaitingForUser)
        // A human action has no timeout. Cancellation/disconnection ends the session instead.
        finish(repository.awaitResult(session) == V3PhoneSyncReply.Success)
    }

    companion object { const val ECHO_TIMEOUT_MS = 300L }
}
