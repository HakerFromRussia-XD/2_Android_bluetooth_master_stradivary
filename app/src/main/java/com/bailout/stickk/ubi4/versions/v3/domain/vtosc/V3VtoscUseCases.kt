package com.bailout.stickk.ubi4.versions.v3.domain.vtosc

class ObserveVtoscConfigUseCaseV3(private val repository: V3VtoscRepository) {
    operator fun invoke() = repository.observeConfig()
}

class ObserveVtoscLoadingUseCaseV3(private val repository: V3VtoscRepository) {
    operator fun invoke() = repository.observeLoading()
}

class LoadVtoscConfigUseCaseV3(private val repository: V3VtoscRepository) {
    operator fun invoke() = repository.loadConfig()
}

class WriteVtoscConfigUseCaseV3(private val repository: V3VtoscRepository) {
    operator fun invoke(config: V3VtoscConfig) = repository.writeConfig(config)
}

class SaveVtoscConfigUseCaseV3(private val repository: V3VtoscRepository) {
    operator fun invoke() = repository.saveConfig()
}
