package com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3GesturesRepository

class ObserveGesturesChangesUseCaseV3(private val repository: V3GesturesRepository) {
    private val callbackScope by lazy { MainScope() }

    operator fun invoke(): Flow<Unit> = repository.updates

    fun observeActiveGesture(callback: (Int) -> Unit): Job = callbackScope.launch {
        repository.observeActiveGesture().filterNotNull().collect { callback(it) }
    }

    fun observeRotationGroup(callback: (List<Int>) -> Unit): Job = callbackScope.launch {
        repository.observeRotationGroup().filterNotNull().collect { callback(it) }
    }
}
