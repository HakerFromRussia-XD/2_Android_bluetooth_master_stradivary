package com.bailout.stickk.ubi4.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentBleLogBinding
import com.bailout.stickk.ubi4.adapters.BleLogAdapter
import com.bailout.stickk.ubi4.blelog.BleLogStore
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import kotlinx.coroutines.launch

class BleLogFragment : Fragment(R.layout.ubi4_fragment_ble_log) {

    private var _binding: Ubi4FragmentBleLogBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val adapter = BleLogAdapter()
    private var lastEntryId = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentBleLogBinding.bind(view)

        setupSystemBack()
        initUi()
        observeLog()
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
        setupGraphStreamFilter()
        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    private fun setupGraphStreamFilter() = with(binding) {
        val prefs = requireContext().getSharedPreferences(
            PreferenceKeysUbi4.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        graphStreamFilterSwitch.isChecked = prefs.getBoolean(
            PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM,
            true
        )
        graphStreamFilterSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, isChecked)
                .apply()
        }
        graphStreamFilterContainer.setOnClickListener {
            graphStreamFilterSwitch.isChecked = !graphStreamFilterSwitch.isChecked
        }
    }

    private fun observeLog() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val initialEntries = BleLogStore.snapshot()
                lastEntryId = initialEntries.lastOrNull()?.id ?: 0L
                adapter.replaceEntries(initialEntries)
                binding.emptyTv.isVisible = initialEntries.isEmpty()
                if (initialEntries.isNotEmpty()) {
                    binding.bleLogRv.scrollToPosition(initialEntries.lastIndex)
                }

                BleLogStore.version.collect {
                    val shouldStickToBottom = isNearBottom()
                    val newEntries = BleLogStore.entriesAfter(lastEntryId)
                    if (newEntries.isEmpty()) return@collect

                    adapter.appendEntries(newEntries)
                    lastEntryId = newEntries.last().id
                    binding.emptyTv.isVisible = false
                    if (shouldStickToBottom) {
                        binding.bleLogRv.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        }
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
        _binding = null
        super.onDestroyView()
    }
}
