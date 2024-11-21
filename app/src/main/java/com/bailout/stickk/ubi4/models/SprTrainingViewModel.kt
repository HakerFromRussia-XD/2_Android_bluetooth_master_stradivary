package com.bailout.stickk.ubi4.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SprTrainingViewModel : ViewModel() {
    private val _stateOpticTrainingFlow = MutableStateFlow(0)
    val stateOpticTrainingFlow: StateFlow<Int> get() = _stateOpticTrainingFlow

    fun updateState(state: Int) {
        _stateOpticTrainingFlow.value = state
    }
}
