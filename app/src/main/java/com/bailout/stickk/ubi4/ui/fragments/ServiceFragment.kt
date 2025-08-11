package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentServiceBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServiceFragment: BaseWidgetsFragment() {

    private lateinit var binding: Ubi4FragmentServiceBinding
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 4


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentServiceBinding.inflate(inflater, container, false)
        setupRecycler()
        subscribeUpdates()
        return binding.root
    }

    private fun setupRecycler() {
        binding.serviceFragmentRv.layoutManager = LinearLayoutManager(requireContext())
        adapterWidgets.swapData(mDataFactory.prepareData(display))
        binding.serviceFragmentRv.adapter = adapterWidgets
    }

    private fun subscribeUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            updateFlow.collect {
                withContext(Dispatchers.Main) {
                    adapterWidgets.swapData(mDataFactory.prepareData(display))
                }
            }
        }
    }

    companion object {
        fun newInstance(): ServiceFragment = ServiceFragment()
    }

}