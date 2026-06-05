package com.bailout.stickk.ubi4.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSpecialSettingsBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState.activeSettingsFragmentFilterFlow
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class SpecialSettingsFragment : BaseWidgetsFragment() {

    private var _binding: Ubi4FragmentSpecialSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 2
    private var previousMobileSettings: Boolean? = null
    private var isMobileSettings = false
    private var selectorIndicatorAnimator: ObjectAnimator? = null


    override fun onResume() {
        super.onResume()
        updateFlow.tryEmit(0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentSpecialSettingsBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previousMobileSettings = null
        isMobileSettings = main.getBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, false)
        activeSettingsFragmentFilterFlow.value = if (isMobileSettings) 2 else 1
        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.settingsRecyclerView.adapter = adapterWidgets
        widgetListUpdater()

        binding.prostheticSettingsBtn.setOnClickListener {
            main.saveBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, false)
            activeSettingsFragmentFilterFlow.value = 1
            if (isMobileSettings) {
                isMobileSettings = false
                updateUI()
            }
        }


        binding.mobileSettingsBtn.setOnClickListener {
            main.saveBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, true)
            activeSettingsFragmentFilterFlow.value = 2
            if (!isMobileSettings) {
                isMobileSettings = true
                updateUI()
            }
        }


        binding.settingsSelectorContainer.post { updateUI(animateSelector = false) }
    }


//    private fun updateUI() {
//        binding.settingsRecyclerView.post {
//            clearSwitcherCache()
//            if (isMobileSettings) {
//                adapterWidgets.swapData(mDataFactory.mobileWidgets())
//            } else {
//                adapterWidgets.swapData(mDataFactory.prepareData(display))
//            }
//            if (previousMobileSettings == null || previousMobileSettings != isMobileSettings) {
//                updateSelectorUI()
//                previousMobileSettings = isMobileSettings
//            }
//        }
//
//    }

    private fun updateUI(animateSelector: Boolean = true) {
        val dataSetChanged = (previousMobileSettings == null || previousMobileSettings != isMobileSettings)
        val data = if (isMobileSettings) {
            mDataFactory.mobileWidgets()
        } else {
            mDataFactory.prepareData(display)
        }

        if (dataSetChanged) {
            clearSwitcherCache()
        }

        if (binding.settingsRecyclerView.isComputingLayout) {
            binding.settingsRecyclerView.post {
                adapterWidgets.swapData(data)
            }
        } else {
            adapterWidgets.swapData(data)
        }

        if (dataSetChanged) {
            updateSelectorUI(animateSelector && previousMobileSettings != null)
            previousMobileSettings = isMobileSettings
        }
    }




    private fun updateSelectorUI(animate: Boolean = true) {
        val duration = 200L
        val context = context ?: return
        val selectedColor = context.getColor(R.color.white)
        val unselectedColor = context.getColor(android.R.color.darker_gray)
        val leftTargetColor = if (isMobileSettings) unselectedColor else selectedColor
        val rightTargetColor = if (isMobileSettings) selectedColor else unselectedColor
        val containerWidth = binding.settingsSelectorContainer.width

        if (containerWidth == 0) {
            binding.prostheticSettingsBtn.setTextColor(leftTargetColor)
            binding.mobileSettingsBtn.setTextColor(rightTargetColor)
            binding.settingsSelectorContainer.doOnNextLayout {
                if (_binding != null) {
                    updateSelectorUI(animate = false)
                }
            }
            return
        }

        val halfWidth = containerWidth / 2f

        val targetX = if (isMobileSettings) halfWidth else 0f
        selectorIndicatorAnimator?.cancel()
        if (animate) {
            selectorIndicatorAnimator = ObjectAnimator.ofFloat(
                binding.selectorIndicator,
                "translationX",
                targetX
            ).apply {
                this.duration = duration
                start()
            }
        } else {
            binding.selectorIndicator.translationX = targetX
        }

        if (animate) {
            ObjectAnimator.ofInt(
                binding.prostheticSettingsBtn,
                "textColor",
                binding.prostheticSettingsBtn.currentTextColor,
                leftTargetColor
            ).apply {
                this.duration = duration
                setEvaluator(ArgbEvaluator())
                start()
            }

            ObjectAnimator.ofInt(
                binding.mobileSettingsBtn,
                "textColor",
                binding.mobileSettingsBtn.currentTextColor,
                rightTargetColor
            ).apply {
                this.duration = duration
                setEvaluator(ArgbEvaluator())
                start()
            }
        } else {
            binding.prostheticSettingsBtn.setTextColor(leftTargetColor)
            binding.mobileSettingsBtn.setTextColor(rightTargetColor)
        }
    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect {
                updateUI()
            }
        }
    }

    override fun onDestroyView() {
        selectorIndicatorAnimator?.cancel()
        selectorIndicatorAnimator = null
        _binding = null
        super.onDestroyView()
    }

}
