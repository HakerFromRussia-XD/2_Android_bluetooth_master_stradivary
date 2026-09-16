package com.bailout.stickk.ubi4.ui.fragments

import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceSessionRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.V3SensorsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.V3SensorsUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.V3SensorsViewModel
import com.bailout.stickk.ubi4.versions.v3.di.V3SensorsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.DataFactoryV3SensorsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetMapper
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SensorsFragment : BaseWidgetsFragment() {
    private var _binding: Ubi4FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var main: MainActivityUBI4? = null
    private val dataFactory = DataFactory()
    private var v3SensorsViewModel: V3SensorsViewModel? = null
    private var widgetsStateJob: Job? = null
    private val v3WidgetMapper = V3SensorsWidgetMapper()
    private var renderedV3Widgets: List<V3SensorsWidget>? = null
    private var renderedSliders: Map<String, SliderUiStateV3>? = null
    private var renderedButtons: V3SensorsButtonsUiState? = null
    private var renderedRefreshIndicator: Boolean? = null
    private var v3AnimationsEnabled = true
    private var pendingRender: Runnable? = null

    override fun onResume() {
        super.onResume()
        UiState.updateFlow.tryEmit(0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        main = activity as? MainActivityUBI4
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            val viewModel = v3SensorsViewModel
            if (viewModel != null) {
                viewModel.onAction(V3SensorsAction.RefreshRequested)
                // The pull control starts its animation before this callback, even if the request is rejected.
                renderRefreshIndicator(viewModel.uiState.value.isRefreshIndicatorVisible, force = true)
            } else {
                refreshWidgetsList()
            }
        }
        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
        if (UiState.isInterfaceV3Activated) bindV3Sensors() else bindUbi4Widgets()
    }

    private fun bindV3Sensors() {
        val viewModel = ViewModelProvider(
            this, V3SensorsViewModelFactory(
                createV3DeviceSettingsRepository(), DataFactoryV3SensorsWidgetsSource(), createV3SensorsPlotRepository(),
                createV3SensorsCommandsRepository(),
                sessionRepository = V3DeviceSessionRepositoryImpl(),
            ),
        )[V3SensorsViewModel::class.java]
        v3SensorsViewModel = viewModel
        renderV3Sensors(viewModel.uiState.value)
        val owner = viewLifecycleOwner
        widgetsStateJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onAction(V3SensorsAction.ViewAttached)
                try {
                    viewModel.uiState.collect(::renderV3Sensors)
                } finally {
                    viewModel.onAction(V3SensorsAction.ViewDetached)
                }
            }
        }
    }

    override fun onV3SensorsButtonsAction(action: V3SensorsButtonsAction) {
        v3SensorsViewModel?.onAction(V3SensorsAction.ButtonsAction(action))
    }

    override fun onV3PlotAction(action: V3PlotAction) {
        v3SensorsViewModel?.onAction(V3SensorsAction.PlotAction(action))
    }

    override fun onV3SliderAction(action: V3SliderAction) {
        v3SensorsViewModel?.onAction(V3SensorsAction.SliderAction(action))
    }

    override fun areV3WidgetAnimationsEnabled(): Boolean =
        if (v3SensorsViewModel != null) v3AnimationsEnabled else super.areV3WidgetAnimationsEnabled()

    private fun renderV3Sensors(state: V3SensorsUiState) {
        val currentBinding = _binding ?: return
        val recyclerView = currentBinding.homeRv
        pendingRender?.let(recyclerView::removeCallbacks)
        pendingRender = null
        if (recyclerView.isComputingLayout) {
            pendingRender = Runnable {
                if (_binding === currentBinding) v3SensorsViewModel?.uiState?.value?.let(::renderV3Sensors)
            }.also(recyclerView::post)
            return
        }
        v3AnimationsEnabled = state.animationsEnabled
        renderV3Plot(state.plot)
        if (renderedButtons != state.buttons) {
            renderV3SensorsButtons(state.buttons)
            renderedButtons = state.buttons
        }
        if (renderedSliders != state.sliders) {
            renderV3Sliders(state.sliders)
            renderedSliders = state.sliders
        }
        if (renderedV3Widgets != state.widgets) {
            adapterWidgets.swapData(v3WidgetMapper.toItems(state.widgets))
            renderedV3Widgets = state.widgets
            main?.refreshBottomNavVisibility()
        }
        renderRefreshIndicator(state.isRefreshIndicatorVisible)
    }

    private fun renderRefreshIndicator(visible: Boolean, force: Boolean = false) {
        val currentBinding = _binding ?: return
        if (force || renderedRefreshIndicator != visible) {
            currentBinding.refreshLayout.setRefreshing(visible)
            renderedRefreshIndicator = visible
        }
    }

    private fun bindUbi4Widgets() {
        adapterWidgets.swapData(dataFactory.prepareData(display = 1))
        main?.refreshBottomNavVisibility()
        widgetsStateJob = viewLifecycleOwner.lifecycleScope.launch {
            UiState.updateFlow.collect {
                val currentBinding = _binding ?: return@collect
                val data = dataFactory.prepareData(display = 1)
                val recyclerView = currentBinding.homeRv
                pendingRender?.let(recyclerView::removeCallbacks)
                pendingRender = null
                if (recyclerView.isComputingLayout) {
                    pendingRender = Runnable {
                        if (_binding === currentBinding) {
                            adapterWidgets.swapData(data)
                            main?.refreshBottomNavVisibility()
                        }
                    }.also(recyclerView::post)
                } else {
                    adapterWidgets.swapData(data)
                    main?.refreshBottomNavVisibility()
                }
                currentBinding.refreshLayout.setRefreshing(false)
            }
        }
    }

    override fun onDestroyView() {
        v3SensorsViewModel?.onAction(V3SensorsAction.ViewDetached)
        widgetsStateJob?.cancel()
        widgetsStateJob = null
        pendingRender?.let { _binding?.homeRv?.removeCallbacks(it) }
        pendingRender = null
        _binding?.homeRv?.adapter = null
        renderedV3Widgets = null
        renderedSliders = null
        renderedButtons = null
        renderedRefreshIndicator = null
        v3SensorsViewModel = null
        main = null
        _binding = null
        super.onDestroyView()
    }
}
