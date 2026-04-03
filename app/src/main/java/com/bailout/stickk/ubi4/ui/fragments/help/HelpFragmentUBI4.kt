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
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import kotlinx.coroutines.launch

class HelpFragmentUBI4 : Fragment(R.layout.ubi4_fragment_help) {

    private var _binding: Ubi4FragmentHelpBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var lastRenderedVisibilityState: VisibilityState? = null

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
            val emptyState = VisibilityState(
                hasSensors = false,
                hasGestures = false,
                hasTraining = false,
                hasSpecialSettings = false
            )
            if (lastRenderedVisibilityState == emptyState) return@with

            setVisibleIfChanged(ubi4SensorsSettingsBtn, false)
            setVisibleIfChanged(ubi4GestureCustomizationRl, false)
            setVisibleIfChanged(ubi4AdvancedSettingsRl, false)
            setVisibleIfChanged(ubi4TrainingRl, false)
            lastRenderedVisibilityState = emptyState
            return@with
        }

        val displays = widgets.mapNotNull { it.extractDisplayOrNull() }.toSet()

        val hasSensors = 1 in displays
        val hasGestures = (0 in displays)
        val hasTraining = 3 in displays
        val hasSpecialSettings = 2 in displays

        val newState = VisibilityState(
            hasSensors = hasSensors,
            hasGestures = hasGestures,
            hasTraining = hasTraining,
            hasSpecialSettings = hasSpecialSettings
        )
        if (lastRenderedVisibilityState == newState) return@with

        // Sensors: у строки нет id контейнера, поэтому минимум — убрать кликабельную кнопку.
        // Идеально: добавь android:id="@+id/ubi4SensorsSettingsRl" и скрывай именно его.
        setVisibleIfChanged(ubi4SensorsSettingsBtn, hasSensors)

        // Gestures: строка контейнер есть -> скрываем красиво
        setVisibleIfChanged(ubi4GestureCustomizationRl, hasGestures)

        // Training: строка контейнер есть -> скрываем красиво
        setVisibleIfChanged(ubi4TrainingRl, hasTraining)

        setVisibleIfChanged(ubi4AdvancedSettingsRl, hasSpecialSettings)
        lastRenderedVisibilityState = newState
    }

    private fun setVisibleIfChanged(view: View, isVisible: Boolean) {
        if (view.isVisible != isVisible) {
            view.isVisible = isVisible
        }
    }

    private fun Any.extractDisplayOrNull(): Int? = when (this) {
        is BaseParameterWidgetEStruct -> baseParameterWidgetStruct.display

        is CommandParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is CommandParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is PlotParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is PlotParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is OpticStartLearningWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        is ToggleSliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is ToggleSliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is SwitchParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is SwitchParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display

        is SliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display
        is SliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display



        else -> null
    }

    private fun openScreen(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out_next,
                R.anim.slide_in_next,
                R.anim.slide_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleBackPress() {
        val main = activity as? MainActivityUBI4
        val source = arguments?.getString(ARG_SOURCE_FRAGMENT).orEmpty()
        main?.showTopStatusBar()
        main?.setStatusBarBackMode(false)
        main?.showBottomNavigation()

        if (parentFragmentManager.backStackEntryCount > 0) {
            if (source == SensorsFragment::class.java.name) {
                main?.pausePlotPointsForTransition()
            }
            parentFragmentManager.popBackStack()
            return
        }

        when (source) {
            SensorsFragment::class.java.name -> main?.showSensorsScreen()
            SpecialSettingsFragment::class.java.name -> main?.showSpecialScreen()
            SprTrainingFragment::class.java.name -> main?.showOpticTrainingGesturesScreen()
            SprGestureFragment::class.java.name -> main?.showOpticGesturesScreen()
            GesturesFragment::class.java.name -> main?.showGesturesScreen()
            AdvancedFragment::class.java.name -> main?.showAdvancedScreen()
            else -> parentFragmentManager.popBackStack()
        }
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

    private data class VisibilityState(
        val hasSensors: Boolean,
        val hasGestures: Boolean,
        val hasTraining: Boolean,
        val hasSpecialSettings: Boolean
    )
}
