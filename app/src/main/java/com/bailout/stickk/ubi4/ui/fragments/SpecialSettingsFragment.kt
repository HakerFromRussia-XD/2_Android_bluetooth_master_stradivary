package com.bailout.stickk.ubi4.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSpecialSettingsBinding
import com.bailout.stickk.ubi4.versions.v3.presentation.autologin.AutoLoginDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderDelegateAdapterV3
import com.bailout.stickk.ubi4.ui.dialog.SettingsProfileNameDialogHost
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfileNameRules
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfileNameEditorUiState
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.activeSettingsFragmentFilterFlow
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsAction
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsViewModel
import com.bailout.stickk.ubi4.versions.v3.di.V3SpecialSettingsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetMapper
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SpecialSettingsFragment : BaseWidgetsFragment() {
    private val v3SpinnerParameterKeys = V3SpecialSettingsViewModel.spinnerParameterKeys
    private val v3ToggleSliderParameterKeys = V3SpecialSettingsViewModel.toggleSliderParameterKeys
    private var _binding: Ubi4FragmentSpecialSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 2
    private var previousMobileSettings: Boolean? = null
    private var isMobileSettings = false
    private var selectorIndicatorAnimator: ObjectAnimator? = null
    private var v3SpecialSettingsViewModel: V3SpecialSettingsViewModel? = null
    private val v3AutoLoginAdapter by lazy {
        AutoLoginDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            onCheckedChanged = { v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.AutoLoginChanged(it)) },
            animationsEnabled = ::areV3WidgetAnimationsEnabled,
        )
    }
    private val v3SpinnerAdapter by lazy {
        SpinnerDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            parameterKeys = v3SpinnerParameterKeys,
            onAction = { v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SpinnerAction(it)) },
            settingsProfilesFromState = true,
            onSettingsProfileSelected = {
                v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SettingsProfileSelected(it))
            },
            onSettingsProfileCreateRequested = {
                v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SettingsProfileCreateRequested)
            },
            onSettingsProfileRenameRequested = {
                v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SettingsProfileRenameRequested(it))
            },
        )
    }
    private val v3ToggleSliderAdapter by lazy {
        ToggleSliderDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            parameterKeys = v3ToggleSliderParameterKeys,
            onAction = { v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.ToggleSliderAction(it)) },
            animationsEnabled = ::areV3WidgetAnimationsEnabled,
        )
    }
    private val v3SliderAdapter by lazy {
        SliderDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            onAction = { v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SliderAction(it)) },
            animationsEnabled = ::areV3WidgetAnimationsEnabled,
        )
    }
    protected override val adapterWidgets: CompositeDelegateAdapter by lazy {
        if (UiState.isInterfaceV3Activated) {
            CompositeDelegateAdapter(v3AutoLoginAdapter, v3SpinnerAdapter, v3ToggleSliderAdapter, v3SliderAdapter)
        } else {
            super.adapterWidgets
        }
    }
    private var v3SpecialSettingsStateJob: Job? = null
    private val v3WidgetMapper = V3SpecialSettingsWidgetMapper()
    private var renderedV3Widgets: List<V3SpecialSettingsWidget>? = null
    private var v3AnimationsEnabled = true
    private var pendingV3Render: Runnable? = null
    private val settingsProfileNameDialogHost = SettingsProfileNameDialogHost()
    private var renderedSettingsProfileNameRequest: Long? = null

    protected override fun loadGestureNameList() {
        if (!UiState.isInterfaceV3Activated) super.loadGestureNameList()
    }

    override fun onResume() {
        super.onResume()
        if (!UiState.isInterfaceV3Activated) updateFlow.tryEmit(0)
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
        renderedV3Widgets = null
        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.settingsRecyclerView.adapter = adapterWidgets
        bindV3SpecialSettings()
        if (v3SpecialSettingsViewModel == null) {
            isMobileSettings = main.getBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, false)
            activeSettingsFragmentFilterFlow.value = if (isMobileSettings) 2 else 1
            widgetListUpdater()
        }

        binding.prostheticSettingsBtn.setOnClickListener {
            selectSettingsSection(isMobile = false)
        }


        binding.mobileSettingsBtn.setOnClickListener {
            selectSettingsSection(isMobile = true)
        }


        if (v3SpecialSettingsViewModel == null) {
            binding.settingsSelectorContainer.post {
                if (_binding != null) updateUI(animateSelector = false)
            }
        }
    }

    private fun bindV3SpecialSettings() {
        if (!UiState.isInterfaceV3Activated) return
        val viewModel = ViewModelProvider(
            this,
            V3SpecialSettingsViewModelFactory.create(requireContext()),
        )[V3SpecialSettingsViewModel::class.java]
        v3SpecialSettingsViewModel = viewModel
        val owner = viewLifecycleOwner
        v3SpecialSettingsStateJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onAction(V3SpecialSettingsAction.ViewAttached)
                try {
                    viewModel.uiState.collect(::renderV3SpecialSettings)
                } finally {
                    viewModel.onAction(V3SpecialSettingsAction.ViewDetached)
                    dismissSettingsProfileNameDialog()
                }
            }
        }
    }

    private fun selectSettingsSection(isMobile: Boolean) {
        val viewModel = v3SpecialSettingsViewModel
        if (viewModel != null) {
            val section = if (isMobile) V3SpecialSettingsSection.APPLICATION else V3SpecialSettingsSection.PROSTHESIS
            viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(section))
        } else {
            main.saveBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, isMobile)
            activeSettingsFragmentFilterFlow.value = if (isMobile) 2 else 1
            if (isMobileSettings != isMobile) {
                isMobileSettings = isMobile
                updateUI()
            }
        }
    }

    override fun areV3WidgetAnimationsEnabled(): Boolean =
        if (v3SpecialSettingsViewModel != null) v3AnimationsEnabled else super.areV3WidgetAnimationsEnabled()

    private fun renderSettingsProfileNameDialog(editor: V3SettingsProfileNameEditorUiState?) {
        if (renderedSettingsProfileNameRequest == editor?.requestId) return
        dismissSettingsProfileNameDialog()
        if (editor == null) return
        val viewModel = v3SpecialSettingsViewModel ?: return
        renderedSettingsProfileNameRequest = editor.requestId
        settingsProfileNameDialogHost.show(
            context = requireContext(),
            currentName = editor.profile.customName ?: getString(
                SharedRes.strings.ubi4_v3_settings_profile_number.resourceId, editor.profile.profileId,
            ),
            maxLength = V3SettingsProfileNameRules.MAX_LENGTH,
            onSave = { name ->
                viewModel.onAction(V3SpecialSettingsAction.SettingsProfileNameSubmitted(editor.requestId, name))
            },
            onDismissRequest = {
                viewModel.onAction(V3SpecialSettingsAction.SettingsProfileNameDismissed(editor.requestId))
            },
        )
    }

    private fun dismissSettingsProfileNameDialog() {
        settingsProfileNameDialogHost.dismiss()
        renderedSettingsProfileNameRequest = null
    }

    private fun renderV3SpecialSettings(state: V3SpecialSettingsUiState) {
        val currentBinding = _binding ?: return
        v3AnimationsEnabled = state.animationsEnabled
        // Compatibility mirror for the existing shared UI state; preferences are owned by the ViewModel path.
        activeSettingsFragmentFilterFlow.value = if (state.selectedSection == V3SpecialSettingsSection.APPLICATION) 2 else 1
        renderSettingsProfileNameDialog(state.settingsProfiles?.nameEditor)
        val recyclerView = currentBinding.settingsRecyclerView
        pendingV3Render?.let(recyclerView::removeCallbacks)
        pendingV3Render = null
        if (recyclerView.isComputingLayout) {
            pendingV3Render = Runnable {
                if (_binding === currentBinding) {
                    v3SpecialSettingsViewModel?.uiState?.value?.let(::renderV3SpecialSettings)
                }
            }.also(recyclerView::post)
            return
        }

        isMobileSettings = state.selectedSection == V3SpecialSettingsSection.APPLICATION
        val sectionChanged = previousMobileSettings == null || previousMobileSettings != isMobileSettings
        v3SliderAdapter.renderSliders(state.sliders)
        v3ToggleSliderAdapter.renderToggleSliders(state.toggleSliders)
        v3SpinnerAdapter.renderSpinners(state.spinners)
        v3SpinnerAdapter.renderSettingsProfiles(state.settingsProfiles)
        v3AutoLoginAdapter.render(state.autoLogin)
        if (sectionChanged || renderedV3Widgets != state.widgets) {
            adapterWidgets.swapData(v3WidgetMapper.toItems(state.widgets))
            renderedV3Widgets = state.widgets
        }
        if (sectionChanged) {
            updateSelectorUI(animate = previousMobileSettings != null)
            previousMobileSettings = isMobileSettings
        }
    }

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
        dismissSettingsProfileNameDialog()
        pendingV3Render?.let { _binding?.settingsRecyclerView?.removeCallbacks(it) }
        pendingV3Render = null
        renderedV3Widgets = null
        v3SpecialSettingsStateJob?.cancel()
        v3SpecialSettingsStateJob = null
        v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.ViewDetached)
        v3SpecialSettingsViewModel = null
        _binding?.settingsRecyclerView?.adapter = null
        selectorIndicatorAnimator?.cancel()
        selectorIndicatorAnimator = null
        _binding = null
        super.onDestroyView()
    }

}
