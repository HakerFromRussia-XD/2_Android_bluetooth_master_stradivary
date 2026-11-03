package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.di.V3VtoscViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.V3VtoscParameter
import com.bailout.stickk.ubi4.versions.v3.presentation.vtosc.V3VtoscAction
import com.bailout.stickk.ubi4.versions.v3.presentation.vtosc.V3VtoscUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.vtosc.V3VtoscViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.vtosc.VtoscSliderDelegateAdapter
import com.bailout.stickk.ubi4.versions.v3.presentation.vtosc.VtoscSliderItem
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * VTFB (ВТОС) settings screen. Independent from Sensors EMG UI.
 * Writes DTCE_VTFB_SETTINGS (dataCode 35) to device address 0x30 via DataTable.
 */
class VtoscSensorsFragment : Fragment() {
    private var _binding: Ubi4FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var viewModel: V3VtoscViewModel? = null
    private var stateJob: Job? = null
    private val destroyCallbacks = mutableListOf<() -> Unit>()
    private val sliderAdapter by lazy {
        VtoscSliderDelegateAdapter(
            onDestroyParent = { destroyCallbacks += it },
            onAction = { viewModel?.onAction(it) },
        )
    }
    private val adapter by lazy { CompositeDelegateAdapter(sliderAdapter) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBackPress()
            },
        )
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            viewModel?.onAction(V3VtoscAction.RefreshRequested)
        }
        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapter
        adapter.swapData(V3VtoscParameter.entries.map(::VtoscSliderItem))

        val vm = ViewModelProvider(this, V3VtoscViewModelFactory.create())[V3VtoscViewModel::class.java]
        viewModel = vm
        // Request DTCE_VTFB_SETTINGS immediately on open so pull-to-refresh is not required.
        vm.onAction(V3VtoscAction.ViewAttached)
        render(vm.uiState.value)
        val owner = viewLifecycleOwner
        stateJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect(::render)
            }
        }
    }

    private fun render(state: V3VtoscUiState) {
        val current = _binding ?: return
        sliderAdapter.render(state.sliders)
        current.refreshLayout.setRefreshing(state.isLoading)
    }

    private fun handleBackPress() {
        val mainActivity = activity as? MainActivityUBI4
        mainActivity?.showTopStatusBar()
        mainActivity?.setStatusBarBackMode(false)
        mainActivity?.showBottomNavigation()
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        stateJob?.cancel()
        stateJob = null
        viewModel?.onAction(V3VtoscAction.ViewDetached)
        destroyCallbacks.toList().forEach { it.invoke() }
        destroyCallbacks.clear()
        _binding?.homeRv?.adapter = null
        viewModel = null
        _binding = null
        super.onDestroyView()
    }
}
