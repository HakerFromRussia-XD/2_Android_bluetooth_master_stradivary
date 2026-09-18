package com.bailout.stickk.ubi4.versions.v3.presentation.customerservice

import com.bailout.stickk.ubi4.versions.v3.domain.customerservice.V3CustomerServiceInfo

data class V3CustomerServiceUiState(
    val info: V3CustomerServiceInfo? = null,
    val phoneToDial: String? = null,
)
