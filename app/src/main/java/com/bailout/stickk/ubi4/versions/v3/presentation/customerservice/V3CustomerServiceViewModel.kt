package com.bailout.stickk.ubi4.versions.v3.presentation.customerservice

import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.versions.v3.domain.customerservice.GetCustomerServiceInfoUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.customerservice.GetCustomerServiceManagerPhoneUseCaseV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class V3CustomerServiceViewModel(
    private val getInfo: GetCustomerServiceInfoUseCaseV3,
    private val getManagerPhone: GetCustomerServiceManagerPhoneUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3CustomerServiceUiState())
    val uiState = state.asStateFlow()
    private var attached = false
    private var cleared = false

    fun onAction(action: V3CustomerServiceAction) {
        if (cleared) return
        when (action) {
            V3CustomerServiceAction.ViewAttached -> if (!attached) {
                state.value = V3CustomerServiceUiState(info = getInfo())
                attached = true
            }
            V3CustomerServiceAction.ViewDetached -> {
                attached = false
                state.value = state.value.copy(phoneToDial = null)
            }
            V3CustomerServiceAction.ManagerClicked -> if (attached) {
                state.value = state.value.copy(phoneToDial = getManagerPhone())
            }
            V3CustomerServiceAction.DialerHandled -> state.value = state.value.copy(phoneToDial = null)
        }
    }

    override fun onCleared() {
        cleared = true
        attached = false
        super.onCleared()
    }
}
