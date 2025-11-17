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
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.UiState.widgetsLoadingProgressFlow
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import com.bailout.stickk.ubi4.persistence.preference.WidgetBootstrapHydrator
import com.bailout.stickk.ubi4.persistence.preference.WidgetRepoProvider
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@Suppress("DEPRECATION")
class SensorsFragment : BaseWidgetsFragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val disposables = CompositeDisposable()
    private var onDestroyParentCallbacks = mutableListOf<() -> Unit>()

    private var count = 0
    private val display = 1


    @SuppressLint("CheckResult", "LogNotTimber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }

        //настоящие виджеты

//        adapterWidgets.swapData(mDataFactory.prepareData(display))
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())

        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }
        widgetListUpdater()
        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets


        val initialData = mDataFactory.prepareData(display)
        platformLog("BOOTSTRAP_UI", "apply initial widgets in SensorsFragment: size=${initialData.size}")
        adapterWidgets.swapData(initialData)
        main?.refreshBottomNavVisibility()

//        bootstrapWhenMacReady()

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        Log.d("onDestroyParentCallbacks", "========================")
        onDestroyParentCallbacks.forEach {
            Log.d("onDestroyParentCallbacks", " считаем сколько раз")
            it.invoke() }
    }

//    private suspend fun waitUntilMacIsReady() {
//        repeat(50) { // 5 секунд максимум
//            val mac = WidgetRepoProvider.mac()
//            if (mac.isNotBlank()) return
//            delay(100)
//        }
//    }


    @SuppressLint("NotifyDataSetChanged")
    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            //viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){}
            updateFlow.collect { updateEvent->
                Log.d("WidgetUpdater", "updateFlow event received: $updateEvent")
                main?.runOnUiThread {
                    Log.d("widgetListUpdater", "${mDataFactory.prepareData(display)}")
                    platformLog("sendWidgetsArray", "▶️▶\uFE0F widgetListUpdater(), mDataFactory.prepareData=${mDataFactory.prepareData(display)}")
                    binding.homeRv.post {
                        adapterWidgets.swapData(mDataFactory.prepareData(display))
                        main?.refreshBottomNavVisibility()
                    }
                    binding.refreshLayout.setRefreshing(false)
                }
            }
        }
    }
}


