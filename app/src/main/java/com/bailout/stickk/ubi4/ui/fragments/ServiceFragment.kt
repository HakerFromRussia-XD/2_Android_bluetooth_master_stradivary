package com.bailout.stickk.ubi4.ui.fragments

import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceSessionRepositoryImpl
import android.os.Bundle
import android.content.Context
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
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.data.service.V3DeviceRoleRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.service.V3DeviceInfoRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.service.V3ProsthesisCalibrationRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceTextInputUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3TextInputMessage
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.EXTRAS_DEVICE_NAME
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3RolePinDialogHost
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceAction
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceViewModel
import com.bailout.stickk.ubi4.versions.v3.di.V3ServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.DataFactoryV3ServiceWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidgetMapper
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ServiceFragment : BaseWidgetsFragment() {
    override val v3SpinnerParameterKeys = V3ServiceViewModel.spinnerParameterKeys + P_KEY_DEVICE_ROLE

    private var _binding: Ubi4FragmentServiceBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val dataFactory = DataFactory()
    private val v3WidgetMapper = V3ServiceWidgetMapper()
    private var v3ServiceViewModel: V3ServiceViewModel? = null
    private var widgetsStateJob: Job? = null
    private var renderedV3Widgets: List<V3ServiceWidget>? = null
    private var renderedSliders: Map<String, SliderUiStateV3>? = null
    private var renderedSpinners: Map<String, SpinnerUiStateV3>? = null
    private var renderedTextInputs: Map<V3DeviceInfoField, V3ServiceTextInputUiState>? = null
    private var renderedRoleOptions: List<String>? = null
    private val rolePinDialog = V3RolePinDialogHost()
    private var v3AnimationsEnabled = true
    private var pendingRender: Runnable? = null

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
        val repository = createV3DeviceSettingsRepository()
        val roleRepository = V3DeviceRoleRepositoryImpl(
            requireContext().getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE), repository)
        val deviceInfoRepository = V3DeviceInfoRepositoryImpl(
            currentSerial = { MainActivityUBI4.main.getCurrentSerial() },
            deviceName = { MainActivityUBI4.main.mDeviceName },
            intentDeviceName = { MainActivityUBI4.main.intent?.getStringExtra(EXTRAS_DEVICE_NAME) },
            applyDeviceName = { MainActivityUBI4.main.applyDeviceNameImmediately(it) },
            enqueuePacket = { packet, onSent -> MainActivityUBI4.main.bleCommandWithQueue(packet, SERIALPORTCHAR_UUID, WRITE, onSent) },
        )
        val viewModel = ViewModelProvider(this, V3ServiceViewModelFactory(
            repository, DataFactoryV3ServiceWidgetsSource(), repository, roleRepository, deviceInfoRepository,
            V3ProsthesisCalibrationRepositoryImpl { packet ->
                MainActivityUBI4.main.bleCommandWithQueue(packet, SERIALPORTCHAR_UUID, WRITE) {}
            },
            sessionRepository = V3DeviceSessionRepositoryImpl(),
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

    override fun onV3SliderAction(action: V3SliderAction) {
        v3ServiceViewModel?.onAction(V3ServiceAction.SliderAction(action))
    }

    override fun onV3CalibrationButtonPressed(pressId: Long) {
        v3ServiceViewModel?.onAction(V3ServiceAction.CalibrationButtonPressed(pressId))
    }

    override fun onV3CalibrationButtonReleased(pressId: Long) {
        v3ServiceViewModel?.onAction(V3ServiceAction.CalibrationButtonReleased(pressId))
    }

    override fun onV3TextInputChanged(field: V3DeviceInfoField, text: String) {
        v3ServiceViewModel?.onAction(V3ServiceAction.TextInputChanged(field, text))
    }
    override fun onV3TextInputPrefillRequested(field: V3DeviceInfoField) {
        v3ServiceViewModel?.onAction(V3ServiceAction.TextInputPrefillRequested(field))
    }
    override fun onV3TextInputSendClicked(field: V3DeviceInfoField) {
        v3ServiceViewModel?.onAction(V3ServiceAction.TextInputSendClicked(field))
    }

    override fun onV3SpinnerAction(action: V3SpinnerAction) {
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
        renderV3Calibration(state.calibration)
        if (renderedTextInputs != state.textInputs) {
            renderV3TextInputs(state.textInputs)
            renderedTextInputs = state.textInputs
        }
        val role = state.role
        val spinners = state.spinners + (role?.let {
            mapOf(P_KEY_DEVICE_ROLE to SpinnerUiStateV3(it.displayedIndex, it.isEnabled))
        } ?: emptyMap())
        if (renderedSpinners != spinners) {
            renderV3Spinners(spinners)
            renderedSpinners = spinners
        }
        if (renderedSliders != state.sliders) {
            renderV3Sliders(state.sliders)
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
