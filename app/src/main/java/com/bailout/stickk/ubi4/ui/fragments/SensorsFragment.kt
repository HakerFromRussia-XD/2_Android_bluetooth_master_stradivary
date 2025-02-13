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
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.OneButtonItem
import com.bailout.stickk.ubi4.models.SliderItem
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers.Main
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
        widgetListUpdater()
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())

        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
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
//    override fun refreshWidgetsList() {
//        graphThreadFlag = false
//        listWidgets.clear()
//        onDestroyParentCallbacks.forEach { it.invoke() }
//        onDestroyParentCallbacks.clear()
//        transmitter().bleCommandWithQueue(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE){}
//    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect {
                main?.runOnUiThread {
//                    val nonSliderPositions = widgets.filter { it !is SliderItem }.mapNotNull { widget ->
//                        when (widget) {
//                            is OneButtonItem -> (widget.widget as? BaseParameterWidgetStruct)?.widgetPosition
//                            // если у других типов есть доступ к widgetPosition, добавляем их тоже
//                            else -> null
//                        }
//                    }
//                    val offset = (nonSliderPositions.maxOrNull() ?: 0) + 1
//                    val widgets = mDataFactory.prepareData(display)
//                    widgets.filterIsInstance<SliderItem>()
//                        .forEachIndexed { index, sliderItem ->
//                            when (sliderItem.widget) {
//                                is SliderParameterWidgetEStruct -> {
//                                    sliderItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition = offset + index
//                                }
//                                is SliderParameterWidgetSStruct -> {
//                                    sliderItem.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition = offset + index
//                                }
//                            }
//                        }

                    Log.d("widgetListUpdater", "${mDataFactory.prepareData(display)}")
                    adapterWidgets.swapData(mDataFactory.prepareData(display))
                    binding.refreshLayout.setRefreshing(false)
                }
            }
        }
    }


}
