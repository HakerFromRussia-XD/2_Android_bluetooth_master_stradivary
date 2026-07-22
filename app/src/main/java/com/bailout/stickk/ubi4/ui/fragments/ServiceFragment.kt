package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentServiceBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters.SpinnerDelegateAdapter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServiceFragment: BaseWidgetsFragment() {

    private var _binding: Ubi4FragmentServiceBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 4


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentServiceBinding.inflate(inflater, container, false)

        setupRecycler()
        adapterWidgets.swapData(buildServiceData())
        subscribeUpdates()
        return binding.root
    }


    override fun onPause() {
        super.onPause()
        SpinnerDelegateAdapter.dismissAll()
    }

    private fun setupRecycler() {
        binding.serviceFragmentRv.layoutManager = LinearLayoutManager(requireContext())
        binding.serviceFragmentRv.adapter = adapterWidgets
    }

    private fun buildServiceData(): List<Any> = mDataFactory.prepareData(display)

    private fun updateServiceData() {
        val data = buildServiceData()
        if (binding.serviceFragmentRv.isComputingLayout) {
            binding.serviceFragmentRv.post { adapterWidgets.swapData(data) }
        } else {
            adapterWidgets.swapData(data)
        }
    }

    private fun subscribeUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            updateFlow.collect {
                withContext(Dispatchers.Main) {
                    updateServiceData()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.serviceFragmentRv.adapter = null
        _binding = null
        super.onDestroyView()
    }

}
