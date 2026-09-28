package com.bailout.stickk.ubi4.data.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** A FAM relay may replace the reply address with zero. Never infer its target from row order. */
class RunProgramTypeReplyRouter(private val timeoutMillis: Long = 5_000) {
    private data class Request(val address: Int, val time: Long)
    private val requests = MutableStateFlow<List<Request>>(emptyList())

    fun requested(address: Int, now: Long) {
        requests.update { pending -> pending.filter { now - it.time < timeoutMillis } + Request(address, now) }
    }

    fun resolve(replyAddress: Int, now: Long): Int? {
        while (true) {
            val before = requests.value
            val active = before.filter { now - it.time < timeoutMillis }
            val after = if (replyAddress == 0) emptyList() else active.filter { it.address != replyAddress }
            if (requests.compareAndSet(before, after)) {
                // Unsolicited, expired or overlapping relay responses cannot identify a board.
                return if (replyAddress == 0) active.map { it.address }.distinct().singleOrNull() else replyAddress
            }
        }
    }
}
