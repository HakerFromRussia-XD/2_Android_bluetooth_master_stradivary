package com.bailout.stickk.ubi4.ui.fragments

import com.bailout.stickk.ubi4.versions.v3.di.createSettingsProfileValueApplier
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentServiceBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters.SpinnerDelegateAdapter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceTextInputUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3TextInputMessage
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3RolePinDialogHost
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceAction
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceViewModel
import com.bailout.stickk.ubi4.versions.v3.di.V3ServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidgetMapper
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters.BleLogButtonDelegateAdapter
import com.bailout.stickk.ubi4.versions.v3.presentation.service.ProsthesisCalibrationDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.service.TextInputDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderDelegateAdapterV3
import com.bailout.stickk.ubi4.contract.navigator
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ServiceFragment : BaseWidgetsFragment() {
    override val v3SpinnerParameterKeys = V3ServiceViewModel.spinnerParameterKeys + P_KEY_DEVICE_ROLE

    private var _binding: Ubi4FragmentServiceBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val dataFactory = DataFactory()
    private val v3WidgetMapper = V3ServiceWidgetMapper()
    private var v3ServiceViewModel: V3ServiceViewModel? = null
    private val v3CalibrationAdapter by lazy {
        ProsthesisCalibrationDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            onPressed = { v3ServiceViewModel?.onAction(V3ServiceAction.CalibrationButtonPressed(it)) },
            onReleased = { v3ServiceViewModel?.onAction(V3ServiceAction.CalibrationButtonReleased(it)) },
        )
    }
    private val v3TextInputAdapter by lazy {
        TextInputDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            onTextChanged = { field, text -> v3ServiceViewModel?.onAction(V3ServiceAction.TextInputChanged(field, text)) },
            onPrefillRequested = { v3ServiceViewModel?.onAction(V3ServiceAction.TextInputPrefillRequested(it)) },
            onSendClicked = { v3ServiceViewModel?.onAction(V3ServiceAction.TextInputSendClicked(it)) },
        )
    }
    private val v3SpinnerAdapter by lazy {
        SpinnerDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            parameterKeys = v3SpinnerParameterKeys,
            onAction = ::onV3SpinnerAction,
            applyProfileValues = createSettingsProfileValueApplier(requireContext()),
        )
    }
    private val v3SliderAdapter by lazy {
        SliderDelegateAdapterV3(
            onDestroyParent = ::registerDelegateCleanup,
            onAction = { v3ServiceViewModel?.onAction(V3ServiceAction.SliderAction(it)) },
            animationsEnabled = ::areV3WidgetAnimationsEnabled,
        )
    }
    protected override val adapterWidgets: CompositeDelegateAdapter by lazy {
        if (UiState.isInterfaceV3Activated) {
            CompositeDelegateAdapter(
                v3CalibrationAdapter,
                BleLogButtonDelegateAdapter(onClick = { navigator().showBleLogScreen() }),
                v3SpinnerAdapter,
                v3TextInputAdapter,
                v3SliderAdapter,
            )
        } else {
            super.adapterWidgets
        }
    }
    private var widgetsStateJob: Job? = null
    private var renderedV3Widgets: List<V3ServiceWidget>? = null
    private var renderedSliders: Map<String, SliderUiStateV3>? = null
    private var renderedSpinners: Map<String, SpinnerUiStateV3>? = null
    private var renderedTextInputs: Map<V3DeviceInfoField, V3ServiceTextInputUiState>? = null
    private var renderedRoleOptions: List<String>? = null
    private val rolePinDialog = V3RolePinDialogHost()
    private var v3AnimationsEnabled = true
    private var pendingRender: Runnable? = null

    protected override fun loadGestureNameList() {
        if (!UiState.isInterfaceV3Activated) super.loadGestureNameList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.serviceFragmentRv.layoutManager = LinearLayoutManager(requireContext())
        binding.serviceFragmentRv.adapter = adapterWidgets
        if (UiState.isInterfaceV3Activated) bindV3Service() else bindUbi4Widgets()
    }

    override fun onPause() {
        super.onPause()
        SpinnerDelegateAdapter.dismissAll()
    }

    private fun bindV3Service() {
        val viewModel = ViewModelProvider(this, V3ServiceViewModelFactory.create(
            context = requireContext(),
            owner = requireActivity(),
        ))[V3ServiceViewModel::class.java]
        v3ServiceViewModel = viewModel
        renderV3Service(viewModel.uiState.value)
        val owner = viewLifecycleOwner
        widgetsStateJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onAction(V3ServiceAction.ViewAttached)
                try {
                    viewModel.uiState.collect(::renderV3Service)
                } finally {
                    viewModel.onAction(V3ServiceAction.ViewDetached)
                    rolePinDialog.dismiss()
                }
            }
        }
    }

    private fun onV3SpinnerAction(action: V3SpinnerAction) {
        if (action is V3SpinnerAction.SpinnerValueSelected && action.parameterKey == P_KEY_DEVICE_ROLE) {
            v3ServiceViewModel?.uiState?.value?.role?.roles?.getOrNull(action.value)?.let {
                v3ServiceViewModel?.onAction(V3ServiceAction.RoleSelected(it))
            }
            return
        }
        v3ServiceViewModel?.onAction(V3ServiceAction.SpinnerAction(action))
    }

    override fun areV3WidgetAnimationsEnabled(): Boolean =
        if (v3ServiceViewModel != null) v3AnimationsEnabled else super.areV3WidgetAnimationsEnabled()

    private fun renderV3Service(state: V3ServiceUiState) {
        val currentBinding = _binding ?: return
        val recyclerView = currentBinding.serviceFragmentRv
        pendingRender?.let(recyclerView::removeCallbacks)
        pendingRender = null
        if (recyclerView.isComputingLayout) {
            pendingRender = Runnable {
                if (_binding === currentBinding) v3ServiceViewModel?.uiState?.value?.let(::renderV3Service)
            }.also(recyclerView::post)
            return
        }
        v3AnimationsEnabled = state.animationsEnabled
        v3CalibrationAdapter.render(state.calibration)
        if (renderedTextInputs != state.textInputs) {
            v3TextInputAdapter.renderTextInputs(state.textInputs)
            renderedTextInputs = state.textInputs
        }
        val role = state.role
        val spinners = state.spinners + (role?.let {
            mapOf(P_KEY_DEVICE_ROLE to SpinnerUiStateV3(it.displayedIndex, it.isEnabled))
        } ?: emptyMap())
        if (renderedSpinners != spinners) {
            v3SpinnerAdapter.renderSpinners(spinners)
            renderedSpinners = spinners
        }
        if (renderedSliders != state.sliders) {
            v3SliderAdapter.renderSliders(state.sliders)
            renderedSliders = state.sliders
        }
        val roleOptions = role?.roles?.map { roleName(it) }
        if (renderedV3Widgets != state.widgets || renderedRoleOptions != roleOptions) {
            adapterWidgets.swapData(v3WidgetMapper.toItems(state.widgets, roleOptions))
            renderedV3Widgets = state.widgets
            renderedRoleOptions = roleOptions
        }
        rolePinDialog.render(requireContext(), role?.pinRequest,
            onSubmit = { id, pin -> v3ServiceViewModel?.onAction(V3ServiceAction.RolePinSubmitted(id, pin)) },
            onCancel = { id -> v3ServiceViewModel?.onAction(V3ServiceAction.RolePinCancelled(id)) })
        role?.pinFeedback?.let {
            val message = if (it.accessGranted) SharedRes.strings.ubi4_v3_pin_access_granted else SharedRes.strings.ubi4_v3_pin_invalid
            Toast.makeText(requireContext(), getString(message.resourceId), Toast.LENGTH_SHORT).show()
            v3ServiceViewModel?.onAction(V3ServiceAction.RolePinFeedbackShown(it.requestId))
        }
        state.textInputFeedback?.let {
            val message = when (it.message) {
                V3TextInputMessage.LIMIT_REACHED -> SharedRes.strings.text_limit_reached
                V3TextInputMessage.ENTER_TEXT -> SharedRes.strings.enter_text
                V3TextInputMessage.SENT -> if (it.field == V3DeviceInfoField.DEVICE_NAME) SharedRes.strings.name_set else SharedRes.strings.serial_number_set
                V3TextInputMessage.PREPARATION_FAILED -> if (it.field == V3DeviceInfoField.SERIAL_NUMBER) SharedRes.strings.failed_prepare_serial_number else SharedRes.strings.failed_prepare_command
            }
            Toast.makeText(requireContext(), getString(message.resourceId), Toast.LENGTH_SHORT).show()
            v3ServiceViewModel?.onAction(V3ServiceAction.TextInputFeedbackShown(it.id))
        }
    }

    private fun roleName(role: V3DeviceRole): String = getString(when (role) {
        V3DeviceRole.SERVICE_ENGINEER -> SharedRes.strings.ubi4_v3_role_service_engineer.resourceId
        V3DeviceRole.USER -> SharedRes.strings.ubi4_v3_role_user.resourceId
    })

    private fun bindUbi4Widgets() {
        adapterWidgets.swapData(dataFactory.prepareData(display = 4))
        widgetsStateJob = viewLifecycleOwner.lifecycleScope.launch {
            UiState.updateFlow.collect {
                val currentBinding = _binding ?: return@collect
                val data = dataFactory.prepareData(display = 4)
                val recyclerView = currentBinding.serviceFragmentRv
                pendingRender?.let(recyclerView::removeCallbacks)
                pendingRender = null
                if (recyclerView.isComputingLayout) {
                    pendingRender = Runnable {
                        if (_binding === currentBinding) adapterWidgets.swapData(data)
                    }.also(recyclerView::post)
                } else {
                    adapterWidgets.swapData(data)
                }
            }
        }
    }

    override fun onDestroyView() {
        v3ServiceViewModel?.onAction(V3ServiceAction.ViewDestroyed)
        widgetsStateJob?.cancel()
        widgetsStateJob = null
        rolePinDialog.dismiss()
        pendingRender?.let { _binding?.serviceFragmentRv?.removeCallbacks(it) }
        pendingRender = null
        _binding?.serviceFragmentRv?.adapter = null
        renderedV3Widgets = null
        renderedSliders = null
        renderedSpinners = null
        renderedTextInputs = null
        renderedRoleOptions = null
        v3ServiceViewModel = null
        _binding = null
        super.onDestroyView()
    }
}
