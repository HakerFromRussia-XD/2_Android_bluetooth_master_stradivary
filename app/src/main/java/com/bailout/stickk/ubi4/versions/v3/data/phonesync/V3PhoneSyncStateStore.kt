package com.bailout.stickk.ubi4.versions.v3.data.phonesync

import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncState
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A failed attempt stays terminal; choosing a device from scanning creates a new attempt. */
class V3PhoneSyncStateStore : V3PhoneSyncStateRepository {
    private val mutableState = MutableStateFlow(V3PhoneSyncState.Idle)
    override val state = mutableState.asStateFlow()

    @Synchronized fun publish(value: V3PhoneSyncState) {
        if (mutableState.value != V3PhoneSyncState.Failed) mutableState.value = value
    }

    override fun cancel() = publish(V3PhoneSyncState.Failed)
}
