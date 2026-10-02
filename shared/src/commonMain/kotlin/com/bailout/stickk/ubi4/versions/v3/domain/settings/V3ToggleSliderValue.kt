package com.bailout.stickk.ubi4.versions.v3.domain.settings

/** Time in tenths of a second; incoming zero is preserved when changing only the flag. */
data class V3ToggleSliderValue(val timeTenths: Int = 0, val isEnabled: Boolean = false)
