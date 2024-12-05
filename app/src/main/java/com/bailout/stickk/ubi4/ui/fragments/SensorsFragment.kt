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
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SliderDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SwitcherDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.TrainingFragmentDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion
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
    private fun refreshWidgetsList() {
        graphThreadFlag = false
        listWidgets.clear()
        onDestroyParentCallbacks.forEach { it.invoke() }
        onDestroyParentCallbacks.clear()
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect {
                main?.runOnUiThread {
                    Log.d("widgetListUpdater", "${mDataFactory.prepareData(display)}")
                    adapterWidgets.swapData(mDataFactory.prepareData(display))
                    binding.refreshLayout.setRefreshing(false)
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
        PlotDelegateAdapter(
            plotIsReadyToData = { numberOfCharts -> System.err.println("plotIsReadyToData $numberOfCharts") },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent)}
        ),
        OneButtonDelegateAdapter (
            onButtonPressed = { addressDevice, parameterID, command -> oneButtonPressed(addressDevice, parameterID, command) },
            onButtonReleased = { addressDevice, parameterID, command -> oneButtonReleased(addressDevice, parameterID, command) },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent)}
        ),
        TrainingFragmentDelegateAdapter(
            onConfirmClick = {},
            generateClick = {},
            showFileClick = {}
        ),
        SwitcherDelegateAdapter(
            onSwitchClick = { addressDevice, parameterID, switchState -> sendSwitcherState(addressDevice, parameterID, switchState) },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent)}
        ),
        SliderDelegateAdapter(
            onSetProgress = { addressDevice, parameterID, progress -> sendSliderProgress(addressDevice, parameterID, progress)},
            //TODO решение сильно под вопросом, потому что колбек будет перезаписываться и скорее всего вызовется только у одного виджета
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent)}
        ),
        TrainingFragmentDelegateAdapter(
            onConfirmClick = {
                if (isAdded) {
                    Log.d("StateCallBack", "onConfirmClick: Button clicked")

                } else {
                    Log.e("StateCallBack", "Fragment is not attached to activity")
                }
            },

            onShowFileClick = {
                Log.d("TestWidgetView", "onShowFileClick FRAGMENT OK")

            },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent)}
        )
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
        transmitter().bleCommand(BLECommands.requestSlider(8, 1), MAIN_CHANNEL, WRITE)
//        transmitter().bleCommand(BLECommands.sendOneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)
        Log.d("ButtonClick", "oneButtonPressed  addressDevice=$addressDevice  parameterID: $parameterID   command: $command")
//        transmitter().bleCommand(BLECommands.sendOneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)
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
    private fun sendSliderProgress(addressDevice: Int, parameterID: Int, progress: Int) {
        Log.d("sendSliderProgress", "addressDevice=$addressDevice  parameterID: $parameterID  progress = $progress")
        transmitter().bleCommand(BLECommands.sendSliderCommand(addressDevice, parameterID, progress), MAIN_CHANNEL, WRITE)
    }
    private fun sendSwitcherState(addressDevice: Int, parameterID: Int, switchState: Boolean) {
        Log.d("sendSwitcherCommand", "addressDevice=$addressDevice  parameterID: $parameterID  command = $switchState")
        transmitter().bleCommand(BLECommands.sendSwitcherCommand(addressDevice, parameterID, switchState), MAIN_CHANNEL, WRITE)

    }
}
