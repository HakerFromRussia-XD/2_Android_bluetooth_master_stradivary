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

    private lateinit var binding: Ubi4FragmentServiceBinding
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 4


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentServiceBinding.inflate(inflater, container, false)

        //fake data
        adapterWidgets.swapData(mDataFactory.fakeData())
        setupRecycler()
        subscribeUpdates()


        return binding.root
    }


    override fun onPause() {
        super.onPause()
        SpinnerDelegateAdapter.dismissAll()
    }

    private fun setupRecycler() {
        binding.serviceFragmentRv.layoutManager = LinearLayoutManager(requireContext())
//        adapterWidgets.swapData(mDataFactory.prepareData(display))
        binding.serviceFragmentRv.adapter = adapterWidgets
    }

    private fun subscribeUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            updateFlow.collect {
                withContext(Dispatchers.Main) {
//                    adapterWidgets.swapData(mDataFactory.prepareData(display))
                }
            }
        }
    }

    companion object {
        fun newInstance(): ServiceFragment = ServiceFragment()
    }

}