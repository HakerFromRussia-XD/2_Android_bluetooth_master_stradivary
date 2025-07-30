package com.bailout.stickk.ubi4.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSpecialSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SwitcherDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.WidgetSwitchInfo
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState.activeSettingsFragmentFilterFlow
import com.bailout.stickk.ubi4.data.state.UiState.isMobileSettings
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.switcherFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SpecialSettingsFragment : BaseWidgetsFragment() {

    private lateinit var binding: Ubi4FragmentSpecialSettingsBinding
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 2
    private var previousMobileSettings: Boolean? = null
    private var isMobileSettings = false




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentSpecialSettingsBinding.inflate(inflater, container, false)

        // Обработчики переключения режимов
        widgetListUpdater()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isMobileSettings = main.getBoolean(PreferenceKeysUBI4.LAST_ACTIVE_SETTINGS_FILTER, false)
        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.settingsRecyclerView.adapter = adapterWidgets

        binding.prostheticSettingsBtn.setOnClickListener {
            main.saveBoolean(PreferenceKeysUBI4.LAST_ACTIVE_SETTINGS_FILTER, false)
            activeSettingsFragmentFilterFlow.value = 1
            if (isMobileSettings) {
                isMobileSettings = false
                binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
                binding.settingsRecyclerView.adapter = adapterWidgets
                updateUI()
            }
        }

        binding.mobileSettingsBtn.setOnClickListener {
            main.saveBoolean(PreferenceKeysUBI4.LAST_ACTIVE_SETTINGS_FILTER, true)
            activeSettingsFragmentFilterFlow.value = 2
            if (!isMobileSettings) {
                isMobileSettings = true
                binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
                binding.settingsRecyclerView.adapter = adapterWidgets
                updateUI()
            }
        }


        binding.settingsSelectorContainer.post { updateUI() }
    }


    private fun updateUI() {
        binding.settingsRecyclerView.post {
            clearSwitcherCache()
            if (isMobileSettings) {
                adapterWidgets.swapData(mDataFactory.mobileWidgets())
            } else {
                adapterWidgets.swapData(mDataFactory.prepareData(display))
            }
            if (previousMobileSettings == null || previousMobileSettings != isMobileSettings) {
                updateSelectorUI()
                previousMobileSettings = isMobileSettings
            }
        }

    }



    private fun updateSelectorUI() {
        val duration = 200L
        val selectedColor = requireContext().getColor(R.color.white)
        val unselectedColor = requireContext().getColor(android.R.color.darker_gray)
        val containerWidth = binding.settingsSelectorContainer.width
        val halfWidth = containerWidth / 2f

        val targetX = if (isMobileSettings) halfWidth else 0f
        ObjectAnimator.ofFloat(binding.selectorIndicator, "translationX", targetX)
            .setDuration(duration)
            .start()

        val leftColorAnim = ObjectAnimator.ofInt(
            binding.prostheticSettingsBtn,
            "textColor",
            if (!isMobileSettings) unselectedColor else selectedColor,
            if (!isMobileSettings) selectedColor else unselectedColor
        ).apply {
            this.duration = duration
            setEvaluator(ArgbEvaluator())
        }
        leftColorAnim.start()

        val rightColorAnim = ObjectAnimator.ofInt(
            binding.mobileSettingsBtn,
            "textColor",
            if (isMobileSettings) unselectedColor else selectedColor,
            if (isMobileSettings) selectedColor else unselectedColor
        ).apply {
            this.duration = duration
            setEvaluator(ArgbEvaluator())
        }
        rightColorAnim.start()
    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect {
                updateUI()
            }
        }
    }


}