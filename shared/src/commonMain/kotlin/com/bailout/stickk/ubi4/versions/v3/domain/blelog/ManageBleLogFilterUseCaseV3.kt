package com.bailout.stickk.ubi4.versions.v3.domain.blelog

class ManageBleLogFilterUseCaseV3(private val repository: V3BleLogRepository) {
    fun restore() = repository.restoreGraphStreamFilter()
    fun setGraphStreamHidden(hidden: Boolean) = repository.setGraphStreamHidden(hidden)
}
