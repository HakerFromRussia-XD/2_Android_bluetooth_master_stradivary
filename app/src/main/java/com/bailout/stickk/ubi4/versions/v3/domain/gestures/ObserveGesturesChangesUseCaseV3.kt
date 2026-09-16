package com.bailout.stickk.ubi4.versions.v3.domain.gestures

import kotlinx.coroutines.flow.Flow

class ObserveGesturesChangesUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(): Flow<Unit> = repository.updates
}
