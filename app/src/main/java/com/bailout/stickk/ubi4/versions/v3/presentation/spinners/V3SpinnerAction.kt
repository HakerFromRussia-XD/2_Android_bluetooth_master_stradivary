package com.bailout.stickk.ubi4.versions.v3.presentation.spinners

sealed interface V3SpinnerAction {
    data class SpinnerValueSelected(val parameterKey: String, val value: Int) : V3SpinnerAction
}
