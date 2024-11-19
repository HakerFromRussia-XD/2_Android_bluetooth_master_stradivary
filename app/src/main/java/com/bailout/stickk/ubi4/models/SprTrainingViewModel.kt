package com.bailout.stickk.ubi4.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SprTrainingViewModel : ViewModel() {
    private val _currentState = MutableLiveData<Int>(0)
    val currentState: LiveData<Int> get() = _currentState

    fun setState(state: Int) {
        _currentState.value = state
    }
}
