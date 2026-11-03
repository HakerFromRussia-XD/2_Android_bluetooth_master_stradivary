package com.bailout.stickk.ubi4.versions.v3.presentation.vtosc

import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.V3VtoscConfig
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.V3VtoscParameter

data class V3VtoscSliderUiState(
    val parameter: V3VtoscParameter,
    val value: Int,
    val isEnabled: Boolean = true,
)

data class V3VtoscUiState(
    val sliders: List<V3VtoscSliderUiState> = V3VtoscParameter.entries.map {
        V3VtoscSliderUiState(it, defaultValue(it))
    },
    val isLoading: Boolean = false,
) {
    fun config(): V3VtoscConfig {
        val values = sliders.associate { it.parameter to it.value }
        return V3VtoscConfig(
            fsrThreshold = values.getValue(V3VtoscParameter.FSR_THRESHOLD),
            fsrHysteresis = values.getValue(V3VtoscParameter.FSR_HYSTERESIS),
            motorsPwmPercent = values.getValue(V3VtoscParameter.MOTORS_PWM_PERCENT),
            motorsPulseDurationMs = values.getValue(V3VtoscParameter.MOTORS_PULSE_DURATION_MS),
            motorsPostPulsePauseMs = values.getValue(V3VtoscParameter.MOTORS_POST_PULSE_PAUSE_MS),
        )
    }

    companion object {
        fun from(config: V3VtoscConfig, isLoading: Boolean = false) = V3VtoscUiState(
            sliders = V3VtoscParameter.entries.map { parameter ->
                V3VtoscSliderUiState(parameter, valueOf(config, parameter))
            },
            isLoading = isLoading,
        )

        private fun valueOf(config: V3VtoscConfig, parameter: V3VtoscParameter): Int = when (parameter) {
            V3VtoscParameter.FSR_THRESHOLD -> config.fsrThreshold
            V3VtoscParameter.FSR_HYSTERESIS -> config.fsrHysteresis
            V3VtoscParameter.MOTORS_PWM_PERCENT -> config.motorsPwmPercent
            V3VtoscParameter.MOTORS_PULSE_DURATION_MS -> config.motorsPulseDurationMs
            V3VtoscParameter.MOTORS_POST_PULSE_PAUSE_MS -> config.motorsPostPulsePauseMs
        }

        private fun defaultValue(parameter: V3VtoscParameter): Int = valueOf(V3VtoscConfig(), parameter)
    }
}

sealed interface V3VtoscAction {
    data object ViewAttached : V3VtoscAction
    data object ViewDetached : V3VtoscAction
    data object RefreshRequested : V3VtoscAction
    data class SliderValueChanged(val parameter: V3VtoscParameter, val value: Int) : V3VtoscAction
    data class SliderChangeCommitted(val parameter: V3VtoscParameter, val value: Int) : V3VtoscAction
}

data class VtoscSliderItem(
    val parameter: V3VtoscParameter,
)
