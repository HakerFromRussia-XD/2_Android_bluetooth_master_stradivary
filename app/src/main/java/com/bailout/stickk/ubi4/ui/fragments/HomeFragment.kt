package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.models.DataFactory
import com.bailout.stickk.ubi4.adapters.models.OneButtonItem
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OnePlotDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

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
        binding.refreshLayout.setOnRefreshListener {
//            System.err.println("TEST REFRESH")
            refreshWidgetsList()
        }

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
        return binding.root
    }
    private fun refreshWidgetsList() {
        graphThreadFlag = false
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
                    main?.runOnUiThread {
                        adapterWidgets.swapData(mDataFactory.prepareData(1))
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
        OnePlotDelegateAdapter(),
        OneButtonDelegateAdapter { oneButtonItem ->  buttonClick(oneButtonItem) }
    )

    private fun buttonClick(oneButtonItem: OneButtonItem) {
        System.err.println("buttonClick title: ${oneButtonItem.title}  description: ${oneButtonItem.description}" +
                "widget: ${oneButtonItem.widget}")
//        graphThreadFlag = false
//        GlobalScope.launch {
//            fakeUpdateWidgets()
//        }
//        listWidgets.clear()
//        adapterWidgets.swapData(DataFactory().prepareData())

//        mDataFactory = null
//        listWidgets.clear()
//        mDataFactory = DataFactory()
//        adapterWidgets.swapData(mDataFactory.prepareData())
//        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
//        transmitter().bleCommand(BLECommands.requestPlotFlow(), MAIN_CHANNEL, WRITE)
//        if (title.title.contains("Open 0")) {
//            System.err.println("buttonClick Open 0")
            transmitter().bleCommand(byteArrayOf(0x40, 0x88.toByte(), 0x00, 0x01, 0x00, 0x00, 0x00, 0x01), MAIN_CHANNEL, WRITE)
//        }
//        if (title.title.contains("Open 1")) {
//            System.err.println("buttonClick Open 1")
            transmitter().bleCommand(byteArrayOf(0x40, 0x88.toByte(), 0x00, 0x01, 0x00, 0x00, 0x00, 0x01), MAIN_CHANNEL, WRITE)
//        }
    }

    private suspend fun fakeUpdateWidgets() {
        main?.runOnUiThread {
            adapterWidgets.swapData(DataFactory().fakeDataClear())
        }

        delay(1000)

        main?.runOnUiThread {
            adapterWidgets.swapData(DataFactory().fakeData())
        }
    }
}