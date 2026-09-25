package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentBleLogBinding
import com.bailout.stickk.ubi4.adapters.BleLogAdapter
import com.bailout.stickk.ubi4.blelog.BleLogDirection
import com.bailout.stickk.ubi4.blelog.BleLogEntry
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.di.V3BleLogViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.blelog.V3BleLogAction
import com.bailout.stickk.ubi4.versions.v3.presentation.blelog.V3BleLogUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.blelog.V3BleLogViewModel
import kotlinx.coroutines.launch

class BleLogFragment : Fragment(R.layout.ubi4_fragment_ble_log) {

    private var _binding: Ubi4FragmentBleLogBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val adapter = BleLogAdapter()
    private var v3ViewModel: V3BleLogViewModel? = null
    private var renderingV3State = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentBleLogBinding.bind(view)

        setupSystemBack()
        initUi()
        bindV3Log()
    }

    private fun setupSystemBack() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBackPress()
            }
        )
    }

    private fun initUi() = with(binding) {
        bleLogRv.layoutManager = LinearLayoutManager(requireContext())
        bleLogRv.adapter = adapter
        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    private fun bindV3Log() {
        val viewModel = ViewModelProvider(this, V3BleLogViewModelFactory.from(requireContext()))[V3BleLogViewModel::class.java]
        v3ViewModel = viewModel
        viewModel.onAction(V3BleLogAction.ViewCreated)
        binding.graphStreamFilterSwitch.isChecked = viewModel.uiState.value.hideGraphStream
        binding.graphStreamFilterSwitch.setOnCheckedChangeListener { _, checked ->
            if (!renderingV3State) viewModel.onAction(V3BleLogAction.GraphStreamFilterChanged(checked))
        }
        binding.graphStreamFilterContainer.setOnClickListener {
            binding.graphStreamFilterSwitch.toggle()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onAction(V3BleLogAction.ViewStarted)
                var initial = true
                try {
                    viewModel.uiState.collect { state ->
                        renderV3Log(state, initial)
                        initial = false
                    }
                } finally {
                    viewModel.onAction(V3BleLogAction.ViewStopped)
                }
            }
        }
    }

    private fun renderV3Log(state: V3BleLogUiState, initial: Boolean) = with(binding) {
        renderingV3State = true
        graphStreamFilterSwitch.isChecked = state.hideGraphStream
        renderingV3State = false
        val shouldStickToBottom = initial || isNearBottom()
        val entries = (if (initial) state.entries else state.entries.drop(adapter.itemCount)).map {
            BleLogEntry(it.id, it.timestampMillis,
                if (it.isOutgoing) BleLogDirection.OUTGOING else BleLogDirection.INCOMING, it.bytesHex)
        }
        if (initial) adapter.replaceEntries(entries) else adapter.appendEntries(entries)
        emptyTv.isVisible = state.entries.isEmpty()
        if (entries.isNotEmpty() && shouldStickToBottom) bleLogRv.scrollToPosition(adapter.itemCount - 1)
    }

    private fun isNearBottom(): Boolean {
        val layoutManager = binding.bleLogRv.layoutManager as? LinearLayoutManager ?: return true
        val currentCount = adapter.itemCount
        if (currentCount == 0) return true
        return layoutManager.findLastVisibleItemPosition() >= currentCount - 2
    }

    private fun handleBackPress() {
        val main = activity as? MainActivityUBI4
        main?.showTopStatusBar()
        main?.setStatusBarBackMode(false)
        main?.showBottomNavigation()

        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            main?.showSpecialScreen()
        }
    }

    override fun onDestroyView() {
        v3ViewModel?.onAction(V3BleLogAction.ViewDestroyed)
        v3ViewModel = null
        _binding = null
        super.onDestroyView()
    }
}
