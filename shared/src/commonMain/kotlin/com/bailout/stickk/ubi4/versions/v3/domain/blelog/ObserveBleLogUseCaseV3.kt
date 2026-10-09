package com.bailout.stickk.ubi4.versions.v3.domain.blelog

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ObserveBleLogUseCaseV3(private val repository: V3BleLogReader) {
    private val scope by lazy { MainScope() }

    operator fun invoke() = repository.observeEntryBatches()

    fun snapshot(): List<V3BleLogEntry> = repository.snapshot()

    fun entriesAfter(id: Long): List<V3BleLogEntry> = repository.entriesAfter(id)

    fun observeVersion(callback: (Long) -> Unit): Job = scope.launch {
        repository.observeVersions().collect { callback(it) }
    }
}
