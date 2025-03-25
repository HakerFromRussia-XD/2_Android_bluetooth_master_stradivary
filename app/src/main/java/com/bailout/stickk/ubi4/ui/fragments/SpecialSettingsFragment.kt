package com.bailout.stickk.ubi4.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSpecialSettingsBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState.activeSettingsFragmentFilterFlow
import com.bailout.stickk.ubi4.data.state.UiState.isMobileSettings
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class SpecialSettingsFragment : BaseWidgetsFragment() {

    private lateinit var binding: Ubi4FragmentSpecialSettingsBinding
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 2


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

        binding.prostheticSettingsBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUBI4.LAST_ACTIVE_SETTINGS_FILTER, 1)
            activeSettingsFragmentFilterFlow.value = 1
            if (isMobileSettings) {
                isMobileSettings = false
                updateUI()
            }
        }

        binding.mobileSettingsBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUBI4.LAST_ACTIVE_SETTINGS_FILTER, 2)
            activeSettingsFragmentFilterFlow.value = 2
            if (!isMobileSettings) {
                isMobileSettings = true
                updateUI()
            }
        }

        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)

        val savedFilter = main.getInt(PreferenceKeysUBI4.LAST_ACTIVE_SETTINGS_FILTER, 1)
        when(savedFilter) {
            1 -> isMobileSettings = false
            2 -> isMobileSettings = true
        }

        binding.settingsSelectorContainer.post {
            updateUI()
        }

    }


    private fun updateUI() {
        binding.settingsRecyclerView.adapter = adapterWidgets
        if (isMobileSettings) {
            adapterWidgets.swapData(mDataFactory.mobileWidgets())
        } else {
            adapterWidgets.swapData(mDataFactory.prepareData(display))
        }
        updateSelectorUI()

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