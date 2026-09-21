package com.bailout.stickk.ubi4.versions.v3.domain.sync

class ObserveSyncUseCaseV3(private val repository: V3SyncRepository) {
    operator fun invoke() = repository.observe()
}
