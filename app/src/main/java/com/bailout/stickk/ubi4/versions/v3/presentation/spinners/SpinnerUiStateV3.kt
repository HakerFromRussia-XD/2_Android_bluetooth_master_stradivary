package com.bailout.stickk.ubi4.versions.v3.presentation.spinners

/** The displayed index includes the existing fallback/clamping; it is not a device acknowledgement. */
data class SpinnerUiStateV3(val selectedIndex: Int?, val isEnabled: Boolean = false)
