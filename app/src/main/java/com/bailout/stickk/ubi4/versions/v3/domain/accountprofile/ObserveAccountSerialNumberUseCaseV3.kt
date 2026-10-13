package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class ObserveAccountSerialNumberUseCaseV3(private val repository: V3AccountProfileLocalRepository) {
    operator fun invoke() = repository.observeSerialNumber()
        .map { it?.replace("\u0000", "")?.trim()?.takeIf(String::isNotEmpty) }
        .distinctUntilChanged()
        .onEach(repository::selectSerialNumber)
}
