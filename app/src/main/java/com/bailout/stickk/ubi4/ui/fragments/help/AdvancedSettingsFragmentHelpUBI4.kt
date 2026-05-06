package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentAdvancedSettingsHelpBinding
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import kotlinx.coroutines.launch

class AdvancedSettingsFragmentHelpUBI4 :
    Fragment(R.layout.ubi4_fragment_advanced_settings_help) {

    private var _binding: Ubi4FragmentAdvancedSettingsHelpBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var lastRenderedKeysOrder: List<String>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentAdvancedSettingsHelpBinding.bind(view)

        setupSystemBack()
        initUi()
        renderVisibility()
        observeWidgetsUpdates()
    }

    private fun setupSystemBack() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBackPress()
            }
        )
    }

    private fun initUi() = with(binding) {
        ubi4TitleClickBlockBtn.setOnClickListener { }
        ubi4BackBtn.setOnClickListener { handleBackPress() }

        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    private fun observeWidgetsUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            UiState.updateFlow.collect {
                renderVisibility()
            }
        }
    }

    private fun renderVisibility() = with(binding) {
        val keysOrder = UiState.listWidgets
            .mapNotNull { widget ->
                val baseStruct = widget.baseStructOrNull()
                if (baseStruct?.display != ADVANCED_SETTINGS_DISPLAY) return@mapNotNull null

                widget.extractParameterKeys()
                    .firstOrNull()
                    ?.let { key -> key to baseStruct.widgetPosition }
            }
            .sortedBy { (_, widgetPosition) -> widgetPosition }
            .map { (key, _) -> key }
            .distinct()

        if (lastRenderedKeysOrder == keysOrder) return@with

        val cardsByKey = mapOf(
            P_KEY_EMG_CHANGE_GESTURE to ubi4EmgChangeGestureCard,
            P_KEY_EMG_MOVEMENT_LOCK to ubi4EmgMovementLockCard,
            P_KEY_SCREEN_TIMEOUT to ubi4ScreenTimeoutCard,
            P_KEY_EMG_MAX_GAIN_VALUE to ubi4EmgMaxGainCard,
            P_KEY_FORCE_SETTINGS to ubi4ForceSettingsCard,
            P_KEY_SPEED_SETTINGS to ubi4SpeedSettingsCard,
            P_KEY_HAND_CONTROL_MODE to ubi4HandControlModeCard,
            P_KEY_GESTURE_CHANGE_MODE to ubi4GestureChangeModeCard
        )

        cardsByKey.values.forEach { card ->
            setVisibleIfChanged(card, false)
            ubi4AdvancedSettingsContentLl.removeView(card)
        }

        keysOrder.forEach { key ->
            cardsByKey[key]?.let { card ->
                setVisibleIfChanged(card, true)
                ubi4AdvancedSettingsContentLl.addView(card)
            }
        }

        lastRenderedKeysOrder = keysOrder
    }

    private fun setVisibleIfChanged(view: View, isVisible: Boolean) {
        if (view.isVisible != isVisible) {
            view.isVisible = isVisible
        }
    }

    private fun Any.extractParameterKeys(): List<String> {
        val parameterInfoSet = baseStructOrNull()?.parameterInfoSet.orEmpty()
        return ADVANCED_SETTINGS_PARAMETER_KEYS.filter { key ->
            val expected = ParameterInfoRegistry.require(key)
            parameterInfoSet.any { parameterInfo -> parameterInfo.sameAs(expected) }
        }
    }

    private fun ParameterInfo<Int, Int, Int, Int>.sameAs(other: ParameterInfo<Int, Int, Int, Int>): Boolean {
        return parameterID == other.parameterID &&
                dataCode == other.dataCode &&
                deviceAddress == other.deviceAddress &&
                dataOffsets == other.dataOffsets
    }

    private fun Any.baseStructOrNull(): BaseParameterWidgetStruct? = when (this) {
        is BaseParameterWidgetEStruct -> baseParameterWidgetStruct
        is BaseParameterWidgetSStruct -> baseParameterWidgetStruct

        is CommandParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
        is CommandParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct

        is PlotParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
        is PlotParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct

        is OpticStartLearningWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct

        is ToggleSliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
        is ToggleSliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct

        is SwitchParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SwitchParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct

        is SliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct

        is SpinnerParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SpinnerParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct

        else -> null
    }

    private fun handleBackPress() {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ADVANCED_SETTINGS_DISPLAY = 2

        private val ADVANCED_SETTINGS_PARAMETER_KEYS = listOf(
            P_KEY_EMG_CHANGE_GESTURE,
            P_KEY_EMG_MOVEMENT_LOCK,
            P_KEY_SCREEN_TIMEOUT,
            P_KEY_EMG_MAX_GAIN_VALUE,
            P_KEY_FORCE_SETTINGS,
            P_KEY_SPEED_SETTINGS,
            P_KEY_HAND_CONTROL_MODE,
            P_KEY_GESTURE_CHANGE_MODE
        )
    }
}
