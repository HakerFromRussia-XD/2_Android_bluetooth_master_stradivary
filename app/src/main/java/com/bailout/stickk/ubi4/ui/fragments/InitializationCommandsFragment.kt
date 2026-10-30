package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentInitializationCommandsBinding
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.versions.v3.di.V3InitializationCommandsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization.InitializationCommandDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization.V3InitializationCommandsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization.V3InitializationCommandsUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization.V3InitializationCommandsViewModel
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.launch

class InitializationCommandsFragment : Fragment(R.layout.ubi4_fragment_initialization_commands) {
    private var binding: Ubi4FragmentInitializationCommandsBinding? = null
    private var viewModel: V3InitializationCommandsViewModel? = null
    private var pendingRender: Runnable? = null
    private val adapter by lazy {
        CompositeDelegateAdapter(InitializationCommandDelegateAdapterV3 { command ->
            viewModel?.onAction(V3InitializationCommandsAction.CommandClicked(command))
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentBinding = Ubi4FragmentInitializationCommandsBinding.bind(view)
        binding = currentBinding
        currentBinding.initializationCommandsBack.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) parentFragmentManager.popBackStack()
            else navigator().showSecretScreen()
        }
        currentBinding.initializationCommandsRv.layoutManager = LinearLayoutManager(requireContext())
        currentBinding.initializationCommandsRv.adapter = adapter
        val model = ViewModelProvider(this, V3InitializationCommandsViewModelFactory())[
            V3InitializationCommandsViewModel::class.java]
        viewModel = model
        render(model.uiState.value)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.onAction(V3InitializationCommandsAction.ViewAttached)
                try {
                    model.uiState.collect(::render)
                } finally {
                    model.onAction(V3InitializationCommandsAction.ViewDetached)
                }
            }
        }
    }

    private fun render(state: V3InitializationCommandsUiState) {
        val currentBinding = binding ?: return
        val recyclerView = currentBinding.initializationCommandsRv
        pendingRender?.let(recyclerView::removeCallbacks)
        pendingRender = null
        if (recyclerView.isComputingLayout) {
            pendingRender = Runnable {
                if (binding === currentBinding) viewModel?.uiState?.value?.let(::render)
            }.also(recyclerView::post)
        } else {
            adapter.swapData(state.commands)
        }
    }

    override fun onDestroyView() {
        viewModel?.onAction(V3InitializationCommandsAction.ViewDetached)
        binding?.initializationCommandsRv?.let { recyclerView ->
            pendingRender?.let(recyclerView::removeCallbacks)
            recyclerView.adapter = null
        }
        pendingRender = null
        viewModel = null
        binding = null
        super.onDestroyView()
    }
}
