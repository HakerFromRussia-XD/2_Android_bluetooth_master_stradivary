package com.bailout.stickk.ubi4.versions.v3.domain.gestures

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class LoadRotationGroupUseCaseV3(private val repository: V3GesturesRepository) {
    suspend operator fun invoke(deviceAddress: String): Boolean = coroutineScope {
        // Subscribe before GET: even a response identical to the cached group completes loading.
        val response = async(start = CoroutineStart.UNDISPATCHED) { repository.rotationGroupUpdates.first() }
        try {
            // Preserve the original request plus five retries, at 400 ms intervals.
            repeat(6) {
                val current = repository.getActiveGesture()
                if (!current.isInteractionEnabled || current.deviceAddress != deviceAddress) return@coroutineScope false
                if (!repository.requestRotationGroup(deviceAddress)) return@coroutineScope false
                if (withTimeoutOrNull(400L) { response.await(); true } == true) {
                    val latest = repository.getActiveGesture()
                    return@coroutineScope latest.isInteractionEnabled && latest.deviceAddress == deviceAddress
                }
            }
            false
        } finally {
            response.cancel()
        }
    }
}
