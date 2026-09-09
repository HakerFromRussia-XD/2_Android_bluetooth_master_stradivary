package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SetSliderValueUseCaseV3Test {
    private val parameterLimits = mapOf(
        P_KEY_SPEED_SETTINGS to 100,
        P_KEY_FORCE_SETTINGS to 100,
        P_KEY_EMG_MAX_GAIN_VALUE to 250,
        P_KEY_EMG_GAIN_OPEN_VALUE to 100,
        P_KEY_EMG_GAIN_CLOSE_VALUE to 100,
        P_KEY_GLOBAL_THUMB_CLOSED_POSITION to 100,
        P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION to 100,
    )
    private val writes = mutableListOf<Pair<String, Int>>()
    private val interactionEnabled = MutableStateFlow(true)
    private val repository = object : V3DeviceSettingsRepository {
        override val sliderInteractionEnabled = interactionEnabled
        override fun observeSliderValue(parameterKey: String) = MutableStateFlow<Int?>(null)
        override fun getSliderValue(parameterKey: String): Int? = null
        override fun setSliderValue(parameterKey: String, value: Int) { writes.add(parameterKey to value) }
    }
    private val useCase = SetSliderValueUseCaseV3(repository)

    @Test
    fun `domain rejects values outside device limits without any repository write`() {
        parameterLimits.forEach { (key, upper) ->
            assertThrows(IllegalArgumentException::class.java) { useCase(key, -1) }
            assertThrows(IllegalArgumentException::class.java) { useCase(key, upper + 1) }
        }
        assertEquals(emptyList<Pair<String, Int>>(), writes)
    }

    @Test
    fun `domain accepts both boundaries for every slider including sensitivity above 100`() {
        parameterLimits.forEach { (key, upper) ->
            useCase(key, 0)
            useCase(key, upper)
        }
        assertEquals(parameterLimits.flatMap { (key, upper) -> listOf(key to 0, key to upper) }, writes)
    }

    @Test
    fun `unknown parameters and a current device lock cannot reach a write`() {
        assertThrows(IllegalArgumentException::class.java) { useCase("unknown-slider", 50) }
        interactionEnabled.value = false
        parameterLimits.keys.forEach { key -> useCase(key, 50) }
        assertEquals(emptyList<Pair<String, Int>>(), writes)
    }
}
