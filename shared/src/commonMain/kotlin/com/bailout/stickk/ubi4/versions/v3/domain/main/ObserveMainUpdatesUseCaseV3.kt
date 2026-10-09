package com.bailout.stickk.ubi4.versions.v3.domain.main

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ObserveMainUpdatesUseCaseV3(private val repository: V3MainRepository) {
    operator fun invoke() = repository.observeUpdates()
    fun deviceIdentity() = repository.observeDeviceIdentity()
}

class ObserveMainStatusUseCaseV3(private val repository: V3MainStatusReader) {
    private val callbackScope by lazy { MainScope() }

    fun currentConnectionReady(): Boolean = repository.currentConnectionReady()

    fun observeConnectionReady(callback: (Boolean) -> Unit): Job = callbackScope.launch {
        repository.observeConnectionReady().collect(callback)
    }

    fun observeBatteryPercent(callback: (Int) -> Unit): Job = callbackScope.launch {
        repository.observeBatteryPercent().collect(callback)
    }
}
