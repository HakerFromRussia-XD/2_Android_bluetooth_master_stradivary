package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentSpecialSettingsBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class SpecialSettingsFragment: BaseWidgetsFragment() {
    private lateinit var binding: Ubi4FragmentSpecialSettingsBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val display = 2


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = Ubi4FragmentSpecialSettingsBinding.inflate(inflater,container,false)
        if (activity != null){
            main = activity as MainActivityUBI4
        }

        //настоящие виджеты
        widgetListUpdater()
        adapterWidgets.swapData(mDataFactory.prepareData(display))


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.specialSettingsRv.layoutManager = LinearLayoutManager(context)
        binding.specialSettingsRv.adapter = adapterWidgets
        return binding.root
    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect { _ ->
                val newData = mDataFactory.prepareData(display)
                Log.d("SprGestureFragment", "New data size: ${newData.size}")
                adapterWidgets.swapData(mDataFactory.prepareData(0))
                binding.refreshLayout.setRefreshing(false)
            }
        }
    }
}