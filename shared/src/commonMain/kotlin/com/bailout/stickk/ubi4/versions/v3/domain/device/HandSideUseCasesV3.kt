package com.bailout.stickk.ubi4.versions.v3.domain.device

import kotlinx.coroutines.Job

interface V3HandSideRepository {
    fun getHandSide(): Int?
    fun observeHandSideResponses(onResponse: (Int?) -> Unit): Job
    fun storeHandSide(side: Int)
}

class GetHandSideUseCaseV3(private val repository: V3HandSideRepository) {
    operator fun invoke(): Int? = repository.getHandSide()?.takeIf { it == 0 || it == 1 }
}

class ObserveHandSideResponsesUseCaseV3(private val repository: V3HandSideRepository) {
    operator fun invoke(onResponse: (Int) -> Unit): Job = repository.observeHandSideResponses { side ->
        if (side == 0 || side == 1) onResponse(requireNotNull(side))
    }
}

/** Stores a local selection; does not send a command or save a settings profile. */
class StoreHandSideUseCaseV3(private val repository: V3HandSideRepository) {
    operator fun invoke(side: Int) {
        if (side == 0 || side == 1) repository.storeHandSide(side)
    }
}
