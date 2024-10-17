package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.Fragment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.models.DataFactory
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Suppress("DEPRECATION")
class SprGestureFragment(): Fragment() {

    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }
        //настоящие виджеты
//        widgetListUpdater()
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

    private fun refreshWidgetsList() {
        graphThreadFlag = false
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
        //TODO только для демонстрации
        Handler().postDelayed({
            binding.refreshLayout.setRefreshing(false)
        }, 1000)
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
        PlotDelegateAdapter(
            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") }
        ),
        OneButtonDelegateAdapter(
            onButtonPressed = { parameterID, command -> oneButtonPressed(parameterID, command) },
//            onButtonReleased = { parameterID, command -> oneButtonReleased(parameterID, command) }
            onButtonReleased = { parameterID, command -> oneButtonReleased(parameterID, command) }
        ),
        GesturesDelegateAdapter(
            onSelectorClick = {},
            onDeleteClick = { position, resultCb -> onDeletePressedTest(position, resultCb) }
        )
    )

    private fun onDeletePressedTest(position: Int, resultCb: ((result: Int) -> Unit)) {
        System.err.println("onDeletePressed $position")
        resultCb.invoke(1)
        showDeleteGestureFromRotationGroupDialog()
    }

    private fun onDeletePressed(position: Int) {
        System.err.println("onDeletePressed $position")
        showDeleteGestureFromRotationGroupDialog()
    }

    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n")
    private fun showDeleteGestureFromRotationGroupDialog() {
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_delete_gesture_from_rotation_group, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()


        //заполнение текстовых данных диалога
        val cancelBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogRotationGroupCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val deleteBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogRotationGroupConfirmBtn)
        deleteBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }

    private fun oneButtonPressed(parameterID: Int, command: Int) {
        System.err.println("oneButtonPressed    parameterID: $parameterID   command: $command")
        transmitter().bleCommand(
            BLECommands.oneButtonCommand(parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

    private fun oneButtonReleased(parameterID: Int, command: Int) {
        System.err.println("oneButtonReleased    parameterID: $parameterID   command: $command")
        BLECommands.requestSubDevices().forEach { i ->
            System.err.println("oneButtonReleased ${castUnsignedCharToInt(i)}")
        }
//        transmitter().bleCommand(BLECommands.oneButtonCommand(parameterID, command), MAIN_CHANNEL, WRITE)
//        transmitter().bleCommand(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE)
        transmitter().bleCommand(
            BLECommands.requestSubDeviceParametrs(6, 0, 2),
            MAIN_CHANNEL,
            WRITE
        )
    }
}