package com.bailout.stickk.ubi4.versions.v3.domain.blelog

class ObserveBleLogUseCaseV3(private val repository: V3BleLogRepository) {
    operator fun invoke() = repository.observeEntryBatches()
}
