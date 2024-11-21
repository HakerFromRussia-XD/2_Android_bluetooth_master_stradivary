package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SwitcherDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.TrainingFragmentDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SliderDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.models.SprTrainingViewModel
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Suppress("DEPRECATION")
class SensorsFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    @SuppressLint("CheckResult", "LogNotTimber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }

        //фейковые данные принимаемого потока
        val mBLEParser = main?.let { BLEParser(it) }
        mBLEParser?.parseReceivedData(BLECommands.testDataTransfer())

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
    private fun refreshWidgetsList() {
        graphThreadFlag = false
        listWidgets.clear()
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
    }
    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
                    main?.runOnUiThread {
                        Log.d("widgetListUpdater","${mDataFactory.prepareData(1)}")
                        adapterWidgets.swapData(mDataFactory.prepareData(1))
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
        PlotDelegateAdapter(
            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") }
        ),
        OneButtonDelegateAdapter (
            onButtonPressed = { addressDevice, parameterID, command -> oneButtonPressed(addressDevice, parameterID, command) },
            onButtonReleased = { addressDevice, parameterID, command -> oneButtonReleased(addressDevice, parameterID, command) }
        ) ,
        TrainingFragmentDelegateAdapter(
            onConfirmClick = {},
            onGenerateClick = {},
            onShowFileClick = {},
            onDestroyParent = {onDestroyParent -> },
        ),
        SwitcherDelegateAdapter(
            onSwitchClick = {
                Log.d("SwitcherDelegateAdapter", "$it")
            }
        ),
        SliderDelegateAdapter(
        )
    )

    private fun oneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {
        Log.d("ButtonClick", "oneButtonPressed  addressDevice=$addressDevice  parameterID: $parameterID   command: $command")
        transmitter().bleCommand(BLECommands.sendOneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)
    }
    private fun oneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {
        Log.d("ButtonClick", "oneButtonReleased  addressDevice=$addressDevice  parameterID: $parameterID   command: $command")
        transmitter().bleCommand(BLECommands.sendOneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)


//        val stamp = Timestamp(System.currentTimeMillis())
//        val calendar: Calendar = Calendar.getInstance()
//        calendar.setTimeInMillis(System.currentTimeMillis())
//        val year = calendar.get(Calendar.YEAR)
//        val month = calendar.get(Calendar.MONTH) + 1
//        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
//        transmitter().bleCommand(BLECommands.sendTimestampInfo(7,1, year, month, dayOfMonth, Date(stamp.time).day, Date(stamp.time).hours, Date(stamp.time).minutes, Date(stamp.time).seconds), MAIN_CHANNEL, WRITE)


//        transmitter().bleCommand(BLECommands.requestTransferFlow(1), MAIN_CHANNEL, WRITE)

//        BLECommands.requestSubDeviceParametrs(6, 0, 2).forEach { i ->
//            // проверка правильности сформированной команды
//            System.err.println("oneButtonReleased ${castUnsignedCharToInt(i)}")
//        }
//        transmitter().bleCommand(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE)
//        transmitter().bleCommand(BLECommands.requestSubDeviceParametrs(6, 0, 1), MAIN_CHANNEL, WRITE)
//        transmitter().bleCommand(BLECommands.requestSubDeviceAdditionalParametrs(6, 0), MAIN_CHANNEL, WRITE)
    }
}
