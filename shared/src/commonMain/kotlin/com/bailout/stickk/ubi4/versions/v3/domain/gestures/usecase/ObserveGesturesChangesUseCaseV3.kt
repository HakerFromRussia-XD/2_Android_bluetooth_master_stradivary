package com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase

import kotlinx.coroutines.flow.Flow
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3GesturesRepository

class ObserveGesturesChangesUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(): Flow<Unit> = repository.updates
}
