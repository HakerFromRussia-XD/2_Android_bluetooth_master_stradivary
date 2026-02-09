package com.bailout.stickk.ubi4.ui.fragments.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHelpBinding
import com.bailout.stickk.databinding.Ubi4FragmentSensorSettingsBinding
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.ui.fragments.AdvancedFragment
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.data.widget.endStructures.*
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import kotlinx.coroutines.launch

class HelpFragmentUBI4 : Fragment(R.layout.ubi4_fragment_help) {

    private var _binding: Ubi4FragmentHelpBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentHelpBinding.bind(view)

        setupSystemBack()
        initUi()

        // 1) сразу применяем видимость
        renderVisibility()

        // 2) и обновляем при любых изменениях виджетов
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

        // App control
        ubi4SensorsSettingsBtn.setOnClickListener { openScreen(SensorsFragmentHelpUBI4()) }
        ubi4SettingsGestureBtn.setOnClickListener {  }
        ubi4TrainingBtn.setOnClickListener { }
        ubi4AdvancedSettingsBtn.setOnClickListener { }

        // Prostheses use
        ubi4HowProsthesesWorksBtn.setOnClickListener { openScreen(HowProsthesesWorksFragmentUBI4()) }
        ubi4HowToPutOnAProsthesesSocketBtn.setOnClickListener { openScreen(HowToPutOnProsthesesSocketFragmentUBI4()) }
        ubi4ComplectationBtn.setOnClickListener { openScreen(CompleteSetFragmentUBI4()) }
        ubi4ProsthesesChargeBtn.setOnClickListener { openScreen(ChargingFragmentUBI4()) }
        ubi4ProsthesesCareBtn.setOnClickListener { openScreen(CareFragmentUBI4()) }
        ubi4ServiceAndWarrantyBtn.setOnClickListener { openScreen(ServiceWarrantyFragmentUBI4()) }

        // Contact us
        ubi4ContactSupportBtn.setOnClickListener { openDialer("88007077197") }
        ubi4VkBtn.setOnClickListener { openUrl("https://vk.com/motorica") }
        ubi4TelegrammBtn.setOnClickListener { openUrl("https://t.me/motoricans") }

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
        val widgets = UiState.listWidgets

        if (widgets.isEmpty()) {

            ubi4SensorsSettingsBtn.isVisible = false
            ubi4GestureCustomizationRl.isVisible = false
            ubi4AdvancedSettingsRl.isVisible = false
            ubi4TrainingRl.isVisible = false
            return@with
        }

        val displays = widgets.mapNotNull { it.extractDisplayOrNull() }.toSet()

        val hasSensors = 1 in displays
        val hasGestures = (0 in displays)
        val hasTraining = 3 in displays
        val hasSpecialSettings = 2 in displays

        // Sensors: у строки нет id контейнера, поэтому минимум — убрать кликабельную кнопку.
        // Идеально: добавь android:id="@+id/ubi4SensorsSettingsRl" и скрывай именно его.
        ubi4SensorsSettingsBtn.isVisible = hasSensors

        // Gestures: строка контейнер есть -> скрываем красиво
        ubi4GestureCustomizationRl.isVisible = hasGestures

        // Training: строка контейнер есть -> скрываем красиво
        ubi4TrainingRl.isVisible = hasTraining

        ubi4AdvancedSettingsRl.isVisible = hasSpecialSettings    }

    private fun Any.extractDisplayOrNull(): Int? = when (this) {
        is BaseParameterWidgetEStruct -> baseParameterWidgetStruct.display

        is CommandParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is CommandParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is PlotParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is PlotParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is OpticStartLearningWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        is SwitchParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is SwitchParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is SliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is SliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is ToggleSliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is ToggleSliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        else -> null
    }

    private fun openScreen(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleBackPress() {
        val source = arguments?.getString(ARG_SOURCE_FRAGMENT)

        if (source.isNullOrBlank()) {
            parentFragmentManager.popBackStack()
            return
        }

        val target: Fragment? = when (source) {
            SensorsFragment::class.java.name -> SensorsFragment()
            SpecialSettingsFragment::class.java.name -> SpecialSettingsFragment()
            SprTrainingFragment::class.java.name -> SprTrainingFragment()
            SprGestureFragment::class.java.name -> SprGestureFragment()
            else -> null
        }

        if (target == null) {
            parentFragmentManager.popBackStack()
            return
        }

        parentFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, target)
            .commit()
    }

    private fun openDialer(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        runCatching { startActivity(intent) }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { startActivity(intent) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_SOURCE_FRAGMENT = "sourceFragmentClass"
    }
}