package com.bailout.stickk.ubi4.versions.v3.domain.sync

import kotlinx.coroutines.flow.Flow

interface V3SyncRepository {
    fun observe(): Flow<V3SyncEvent>
    fun observeWidgetsLoadCompletion(): Flow<Unit>
    fun observeInitializationInfo(): Flow<V3SyncInitializationInfo>
    fun observeWidgetsLoadingProgress(): Flow<V3SyncLoadingProgress>
}

interface V3SyncInitializationRepository {
    fun requestInitialization()
}

data class V3SyncInitializationInfo(val parametersNum: Int, val subDeviceNum: Int)
data class V3SyncLoadingProgress(val current: Int, val total: Int)

sealed interface V3SyncEvent {
    data class Progress(val startup: Boolean, val fullInit: Boolean, val current: Int, val total: Int) : V3SyncEvent
    data object Ready : V3SyncEvent
}
