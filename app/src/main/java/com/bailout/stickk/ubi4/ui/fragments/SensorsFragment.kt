package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapterMy
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Suppress("DEPRECATION")
class SensorsFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val disposables = CompositeDisposable()
    private val rxUpdateMainEvent = RxUpdateMainEventUbi4.getInstance()

    private var count = 0
    private val display = 1

    @SuppressLint("CheckResult", "LogNotTimber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }

        //фейковые данные принимаемого потока
//        val mBLEParser = main?.let { BLEParser(it) }
//        mBLEParser?.parseReceivedData(BLECommands.testDataTransfer())

        //настоящие виджеты
//        widgetListUpdater()
//        widgetListUpdaterRx()
//        if (binding.homeRv.isComputingLayout.not()) {
//            if (Looper.myLooper() != Looper.getMainLooper()) {
//                // If BG thread,then post task to recycler view
//                Log.d("parseWidgets", "1 приём команды Rx  listWidgets = $listWidgets")
//                binding.homeRv.post { adapterWidgets.swapData(mDataFactory.prepareData(display)) }
//            } else {
//                Log.d("parseWidgets", "2 приём команды Rx  listWidgets = $listWidgets")
//                adapterWidgets.swapData(mDataFactory.prepareData(display))
//            }
//        } else {
//            Log.d("parseWidgets", "3 приём команды Rx  isComputingLayout = ${binding.homeRv.isComputingLayout}   scrollState  = ${binding.homeRv.scrollState}$")
//        }
        //фейковые виджеты
        adapterWidgets.swapData(mDataFactory.fakeData())

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
    }

    private fun refreshWidgetsList() {
        graphThreadFlag = false
        listWidgets.clear()
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
        //TODO только для демонстрации
//        Handler().postDelayed({
//            binding.refreshLayout.setRefreshing(false)
//        }, 1000)
    }

    private fun widgetListUpdaterRx() {
        adapterWidgets.swapData(mDataFactory.prepareData(1))
        val sensorsFragmentStreamDisposable = rxUpdateMainEvent.allFragmentUiObservable
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _ ->
//                if ( count < 4 ) {
////                    Log.d("parseWidgets", "приём команды Rx  listWidgets = $listWidgets")
//                    Log.d("parseWidgets_rx", "приём команды Rx  listWidgets = ${mDataFactory.prepareData(1)}")
//                    count += 1
//                    adapterWidgets.swapData(mDataFactory.prepareData(1))
//                    binding.refreshLayout.setRefreshing(false)
//                }

                if (binding.homeRv.isComputingLayout.not()) {
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        // If BG thread,then post task to recycler view
                        Log.d("parseWidgets", "1 приём команды Rx  listWidgets = $listWidgets")
                        binding.homeRv.post { adapterWidgets.swapData(mDataFactory.prepareData(display)) }
                    } else {
                        Log.d("parseWidgets", "2 приём команды Rx  listWidgets = $listWidgets")
                        adapterWidgets.swapData(mDataFactory.prepareData(display))
                    }
                } else {
                    Log.d("parseWidgets", "3 приём команды Rx  isComputingLayout = ${binding.homeRv.isComputingLayout}   scrollState  = ${binding.homeRv.scrollState}$")
                }
                binding.refreshLayout.setRefreshing(false)
            }
        disposables.add(sensorsFragmentStreamDisposable)
    }
    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            withContext(Main) {
                updateFlow.collect { value ->
//                    main?.runOnUiThread (Runnable {
                    adapterWidgets.swapData(mDataFactory.prepareData(1))
//                        adapterWidgets.swapData(mDataFactory.fakeData())
                    binding.refreshLayout.setRefreshing(false)
//                    })
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
//        PlotDelegateAdapter(
//            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") }
//        ),
        PlotDelegateAdapterMy(
            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") }
        ),
        OneButtonDelegateAdapter (
            onButtonPressed = { addressDevice, parameterID, command -> oneButtonPressed(addressDevice, parameterID, command) },
            onButtonReleased = { addressDevice, parameterID, command -> oneButtonReleased(addressDevice, parameterID, command) }
        ) ,
//        GesturesDelegateAdapter (
//            onSelectorClick = {},
//            onDeleteClick = { resultCb, gestureName -> },
//            onAddGesturesToRotationGroup = { onSaveDialogClick -> },
//            onSendBLERotationGroup = {deviceAddress, parameterID -> },
//            onShowGestureSettings = { deviceAddress, parameterID, gestureID -> },
//            onRequestGestureSettings = {deviceAddress, parameterID, gestureID -> },
//            onRequestRotationGroup = {deviceAddress, parameterID -> }
//        )
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
