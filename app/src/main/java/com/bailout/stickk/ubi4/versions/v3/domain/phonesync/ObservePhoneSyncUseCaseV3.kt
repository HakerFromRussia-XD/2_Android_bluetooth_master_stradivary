package com.bailout.stickk.ubi4.versions.v3.domain.phonesync

class ObservePhoneSyncUseCaseV3(private val repository: V3PhoneSyncStateRepository) {
    operator fun invoke() = repository.state
}
