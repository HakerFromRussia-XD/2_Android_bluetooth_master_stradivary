package com.bailout.stickk.ubi4.versions.v3.domain.sync

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RequestSyncInitializationUseCaseV3(private val repository: V3SyncInitializationRepository) {
    operator fun invoke() = repository.requestInitialization()
}

class ObserveSyncUseCaseV3(private val repository: V3SyncRepository) {
    private val callbackScope by lazy { MainScope() }
    operator fun invoke() = repository.observe()

    fun observeWidgetsLoadCompletion(callback: () -> Unit): Job = callbackScope.launch {
        repository.observeWidgetsLoadCompletion().collect { callback() }
    }

    fun observeInitializationInfo(callback: (V3SyncInitializationInfo) -> Unit): Job = callbackScope.launch {
        repository.observeInitializationInfo().collect(callback)
    }

    fun observeWidgetsLoadingProgress(callback: (V3SyncLoadingProgress) -> Unit): Job = callbackScope.launch {
        repository.observeWidgetsLoadingProgress().collect(callback)
    }
}
