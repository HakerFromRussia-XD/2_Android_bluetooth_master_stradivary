package com.bailout.stickk.ubi4.versions.v3.domain.main

class GetMainVisibleDisplaysUseCaseV3(private val repository: V3MainRepository) {
    operator fun invoke(): Set<Int> = repository.getVisibleDisplays().toSet()
}
