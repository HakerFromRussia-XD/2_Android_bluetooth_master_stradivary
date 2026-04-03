package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


@Suppress("DEPRECATION")
class SensorsFragment : BaseWidgetsFragment() {
    private var _binding: Ubi4FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val disposables = CompositeDisposable()
    private var onDestroyParentCallbacks = mutableListOf<() -> Unit>()

    private var count = 0
    private val display = 1


    override fun onResume() {
        super.onResume()
        updateFlow.tryEmit(0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false).apply {
            refreshLayout.setLottieAnimation("loader_3.json")
            refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
            refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
            refreshLayout.setOnRefreshListener { refreshWidgetsList() }
//        binding.refreshLayout.isEnabled = false
            homeRv.layoutManager = LinearLayoutManager(context)
            homeRv.adapter = adapterWidgets

        }
        widgetListUpdater()
        main = activity as? MainActivityUBI4

        //настоящие виджеты
        val initialData = mDataFactory.prepareData(display)
        platformLog("BOOTSTRAP_UI", "apply initial widgets in SensorsFragment: size=${initialData.size}")
        adapterWidgets.swapData(initialData)
        main?.refreshBottomNavVisibility()

        return binding.root
    }

    override fun onDestroyView() {
        disposables.clear()
        Log.d("onDestroyParentCallbacks", "========================")
        onDestroyParentCallbacks.forEach {
            Log.d("onDestroyParentCallbacks", " считаем сколько раз")
            it.invoke() }
        onDestroyParentCallbacks.clear()
        main = null
        _binding = null
        super.onDestroyView()
    }



    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect { updateEvent->
                Log.d("WidgetUpdater", "updateFlow event received: $updateEvent")
                val data = mDataFactory.prepareData(display)
                Log.d("widgetListUpdater", "$data")
                platformLog("sendWidgetsArray", "▶️▶\uFE0F widgetListUpdater(), mDataFactory.prepareData=$data")

                if (binding.homeRv.isComputingLayout) {
                    binding.homeRv.post {
                        adapterWidgets.swapData(data)
                        main?.refreshBottomNavVisibility()
                    }
                } else {
                    adapterWidgets.swapData(data)
                    main?.refreshBottomNavVisibility()
                }
                binding.refreshLayout.setRefreshing(false)
            }
        }
    }
}
