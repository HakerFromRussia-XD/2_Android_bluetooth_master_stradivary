package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


@Suppress("DEPRECATION")
class AdvancedFragment : BaseWidgetsFragment() {
    private var _binding: Ubi4FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val disposables = CompositeDisposable()

    private val display = 2

    @SuppressLint("CheckResult", "LogNotTimber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }

        //настоящие виджеты
        widgetListUpdater()

        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
        val initialData = mDataFactory.prepareData(display)
        adapterWidgets.swapData(initialData)
        return binding.root
    }

    override fun onDestroyView() {
        disposables.clear()
        main = null
        _binding = null
        super.onDestroyView()
    }
    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect {
                val data = mDataFactory.prepareData(display)
                Log.d("widgetListUpdater", "$data")

                if (binding.homeRv.isComputingLayout) {
                    binding.homeRv.post {
                        adapterWidgets.swapData(data)
                    }
                } else {
                    adapterWidgets.swapData(data)
                }

                binding.refreshLayout.setRefreshing(false)
            }
        }
    }

}
