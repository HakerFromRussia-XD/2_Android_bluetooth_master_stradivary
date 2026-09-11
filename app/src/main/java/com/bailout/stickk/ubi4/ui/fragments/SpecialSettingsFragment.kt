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
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles.V3SettingsProfilesRepositoryImpl
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3.SettingsProfileApplierV3
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.V3ToggleSliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsSection
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.DataFactoryV3SpecialSettingsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetMapper
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SpecialSettingsFragment : BaseWidgetsFragment() {
    override val v3SettingsProfilesFromState = true
    override val v3SpinnerParameterKeys = V3SpecialSettingsViewModel.spinnerParameterKeys
    override val v3ToggleSliderParameterKeys = V3SpecialSettingsViewModel.toggleSliderParameterKeys
    private var _binding: Ubi4FragmentSpecialSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 2
    private var previousMobileSettings: Boolean? = null
    private var isMobileSettings = false
    private var selectorIndicatorAnimator: ObjectAnimator? = null
    private var v3SpecialSettingsViewModel: V3SpecialSettingsViewModel? = null
    private var v3SpecialSettingsStateJob: Job? = null
    private val v3WidgetMapper = V3SpecialSettingsWidgetMapper()
    private var renderedV3Widgets: List<V3SpecialSettingsWidget>? = null
    private var pendingV3Render: Runnable? = null
    private val settingsProfileNameDialogHost = SettingsProfileNameDialogHost()
    private var renderedSettingsProfileNameRequest: Long? = null


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
        isMobileSettings = main.getBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, false)
        activeSettingsFragmentFilterFlow.value = if (isMobileSettings) 2 else 1
        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.settingsRecyclerView.adapter = adapterWidgets
        bindV3SpecialSettings()
        if (v3SpecialSettingsViewModel == null) widgetListUpdater()

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
        val repository = createV3DeviceSettingsRepository()
        val viewModel = ViewModelProvider(
            this,
            V3SpecialSettingsViewModelFactory(
                repository, DataFactoryV3SpecialSettingsWidgetsSource(), repository, repository,
                V3SettingsProfilesRepositoryImpl(SettingsProfileApplierV3::apply),
            ),
        )[V3SpecialSettingsViewModel::class.java]
        v3SpecialSettingsViewModel = viewModel
        viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(currentSettingsSection()))
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

    private fun currentSettingsSection() = if (isMobileSettings) {
        V3SpecialSettingsSection.APPLICATION
    } else {
        V3SpecialSettingsSection.PROSTHESIS
    }

    private fun selectSettingsSection(isMobile: Boolean) {
        main.saveBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, isMobile)
        activeSettingsFragmentFilterFlow.value = if (isMobile) 2 else 1
        val viewModel = v3SpecialSettingsViewModel
        if (viewModel != null) {
            val section = if (isMobile) V3SpecialSettingsSection.APPLICATION else V3SpecialSettingsSection.PROSTHESIS
            viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(section))
        } else if (isMobileSettings != isMobile) {
            isMobileSettings = isMobile
            updateUI()
        }
    }

    override fun onV3SliderAction(action: V3SliderAction) {
        v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SliderAction(action))
    }

    override fun onV3ToggleSliderAction(action: V3ToggleSliderAction) {
        v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.ToggleSliderAction(action))
    }

    override fun onV3SpinnerAction(action: V3SpinnerAction) {
        v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SpinnerAction(action))
    }

    override fun onV3SettingsProfileCreateRequested() {
        v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SettingsProfileCreateRequested)
    }

    override fun onV3SettingsProfileSelected(profileId: Int) {
        v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SettingsProfileSelected(profileId))
    }

    override fun onV3SettingsProfileRenameRequested(profileId: Int) {
        v3SpecialSettingsViewModel?.onAction(V3SpecialSettingsAction.SettingsProfileRenameRequested(profileId))
    }

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
        if (sectionChanged) clearSwitcherCache()
        renderV3Sliders(state.sliders)
        renderV3ToggleSliders(state.toggleSliders)
        renderV3Spinners(state.spinners)
        renderV3SettingsProfiles(state.settingsProfiles)
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
        selectorIndicatorAnimator?.cancel()
        selectorIndicatorAnimator = null
        _binding = null
        super.onDestroyView()
    }

}
