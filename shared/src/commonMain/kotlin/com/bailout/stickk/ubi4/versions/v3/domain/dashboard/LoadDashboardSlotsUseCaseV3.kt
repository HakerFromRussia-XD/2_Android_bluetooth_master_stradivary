package com.bailout.stickk.ubi4.versions.v3.domain.dashboard

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class LoadDashboardSlotsUseCaseV3(private val repository: V3DashboardSlotsRepository) {
    operator fun invoke(deviceAddress: Int) = flow {
        repository.requestSlots(deviceAddress)
        coroutineScope {
            launch {
                delay(5_000L)
                repository.requestTimedOut(deviceAddress)
            }
            // Keep observing after timeout: a late device response still replaces the error.
            emitAll(repository.observeSlots())
        }
    }
}
