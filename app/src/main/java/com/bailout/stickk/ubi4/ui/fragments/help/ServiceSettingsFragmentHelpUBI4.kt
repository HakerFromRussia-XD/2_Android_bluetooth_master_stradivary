package com.bailout.stickk.ubi4.ui.fragments.help

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import kotlinx.coroutines.launch

/** Empty help page until the service-settings instruction design is approved. */
class ServiceSettingsFragmentHelpUBI4 : Fragment(R.layout.ubi4_fragment_service_settings_help) {
    private val navigationPreferences by lazy {
        requireContext().getSharedPreferences(PreferenceKeysUbi4.NAME, Context.MODE_PRIVATE)
    }
    private val serviceVisibilityListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PreferenceKeysUbi4.KEY_SECRET_ITEM_VISIBLE) checkServiceAccess()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { parentFragmentManager.popBackStack() }
        })
        viewLifecycleOwner.lifecycleScope.launch {
            UiState.updateFlow.collect { checkServiceAccess() }
        }
    }

    override fun onStart() {
        super.onStart()
        navigationPreferences.registerOnSharedPreferenceChangeListener(serviceVisibilityListener)
        checkServiceAccess()
    }

    override fun onStop() {
        navigationPreferences.unregisterOnSharedPreferenceChangeListener(serviceVisibilityListener)
        super.onStop()
    }

    private fun checkServiceAccess() {
        val main = activity as? MainActivityUBI4 ?: return
        if (parentFragmentManager.findFragmentById(R.id.fragmentContainer) !== this) return
        main.refreshBottomNavVisibility()
        if (!UiState.isInterfaceV3Activated || !main.getBottomNavigationController().isItemVisible(R.id.page_secret)) {
            parentFragmentManager.popBackStack()
        }
    }
}
