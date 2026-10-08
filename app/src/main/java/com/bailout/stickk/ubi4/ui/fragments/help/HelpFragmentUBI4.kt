package com.bailout.stickk.ubi4.ui.fragments.help

import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHelpBinding
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.fragments.AdvancedFragment
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import kotlinx.coroutines.launch

class HelpFragmentUBI4 : Fragment(R.layout.ubi4_fragment_help) {

    private var _binding: Ubi4FragmentHelpBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var lastRenderedVisibilityState: VisibilityState? = null
    private val navigationPreferences by lazy {
        requireContext().getSharedPreferences(PreferenceKeysUbi4.NAME, Context.MODE_PRIVATE)
    }
    private val serviceVisibilityListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PreferenceKeysUbi4.KEY_SECRET_ITEM_VISIBLE && _binding != null) renderVisibility()
    }

    override fun onStart() {
        super.onStart()
        navigationPreferences.registerOnSharedPreferenceChangeListener(serviceVisibilityListener)
    }

    override fun onStop() {
        navigationPreferences.unregisterOnSharedPreferenceChangeListener(serviceVisibilityListener)
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentHelpBinding.bind(view)

        setupSystemBack()
        initUi()
        renderVisibility()
        observeNavigationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            lastRenderedVisibilityState = null
            renderVisibility()
        }
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
        ubi4SettingsGestureBtn.setOnClickListener { openScreen(GestureSettingsFragmentHelpUBI4()) }
        ubi4TrainingBtn.setOnClickListener { }
        ubi4AdvancedSettingsBtn.setOnClickListener { openScreen(AdvancedSettingsFragmentHelpUBI4()) }
        ubi4ServiceSettingsHelpBtn.setOnClickListener {
            if (lastRenderedVisibilityState?.hasServiceSettings == true) {
                openScreen(ServiceSettingsFragmentHelpUBI4())
            }
        }

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

    private fun observeNavigationUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            UiState.updateFlow.collect {
                renderVisibility()
            }
        }
    }


    private fun renderVisibility() = with(binding) {
        val main = activity as? MainActivityUBI4 ?: return@with
        main.refreshBottomNavVisibility()
        val bottomNavigationController = main.getBottomNavigationController()

        val newState = VisibilityState(
            isV3 = UiState.isInterfaceV3Activated,
            hasSensors = bottomNavigationController.isItemVisible(R.id.page_2),
            hasGestures = bottomNavigationController.isItemVisible(R.id.page_1),
            hasTraining = bottomNavigationController.isItemVisible(R.id.page_3),
            hasSpecialSettings = bottomNavigationController.isItemVisible(R.id.page_4),
            hasServiceSettings = UiState.isInterfaceV3Activated && bottomNavigationController.isItemVisible(R.id.page_secret)
        )
        if (lastRenderedVisibilityState == newState) return@with

        applyVisibilityState(newState)
        lastRenderedVisibilityState = newState
    }

    private fun applyVisibilityState(state: VisibilityState) = with(binding) {
        ubi4SpecialSettingsTitleTv.setText(if (state.isV3) R.string.special_settings else R.string.advanced_settings)
        val sensorRow = ubi4SensorsSettingsBtn.parent as View
        val rows = sensorRow.parent as android.view.ViewGroup
        val orderedRows = if (state.isV3) {
            listOf(ubi4GestureCustomizationRl, sensorRow, ubi4AdvancedSettingsRl, ubi4ServiceSettingsHelpRl, ubi4TrainingRl)
        } else {
            listOf(sensorRow, ubi4GestureCustomizationRl, ubi4TrainingRl, ubi4AdvancedSettingsRl, ubi4ServiceSettingsHelpRl)
        }
        orderedRows.forEachIndexed { index, row ->
            if (rows.getChildAt(index) !== row) {
                rows.removeView(row)
                rows.addView(row, index)
            }
        }
        (ubi4SensorsSettingsBtn.parent as? View)?.let {
            setVisibleIfChanged(it, state.hasSensors)
        } ?: setVisibleIfChanged(ubi4SensorsSettingsBtn, state.hasSensors)
        setVisibleIfChanged(ubi4GestureCustomizationRl, state.hasGestures)
        setVisibleIfChanged(ubi4TrainingRl, !state.isV3 && state.hasTraining)
        setVisibleIfChanged(ubi4AdvancedSettingsRl, state.hasSpecialSettings)
        setVisibleIfChanged(ubi4ServiceSettingsHelpRl, state.hasServiceSettings)
        setVisibleIfChanged(ubi4SpecialSettingsDivider, state.hasServiceSettings)
    }

    private fun setVisibleIfChanged(view: View, isVisible: Boolean) {
        if (view.isVisible != isVisible) {
            view.isVisible = isVisible
        }
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
        val isV3: Boolean,
        val hasSensors: Boolean,
        val hasGestures: Boolean,
        val hasTraining: Boolean,
        val hasSpecialSettings: Boolean,
        val hasServiceSettings: Boolean
    )
}
