package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.models.DialogGestureItem
import com.bailout.stickk.ubi4.models.Gesture
import com.bailout.stickk.ubi4.models.RotationGroup
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DEVICE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.GESTURE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.PARAMETER_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.gripper.with_encoders.UBI4GripperScreenWithEncodersActivity
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.util.Calendar
import java.util.Date


@Suppress("DEPRECATION")
class GesturesFragment : Fragment() {
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

        RxUpdateMainEventUbi4.getInstance().gestureStateWithEncodersObservable
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameters ->
                Log.d("gestureStateWithEncodersObservable", "parameters = ${parameters.gesture.openPosition1}")
                val gesture = Gesture(parameters.gesture.gestureId,
                    parameters.gesture.openPosition1, parameters.gesture.openPosition2,
                    parameters.gesture.openPosition3, parameters.gesture.openPosition4,
                    parameters.gesture.openPosition5, parameters.gesture.openPosition6,
                    parameters.gesture.closePosition1, parameters.gesture.closePosition2,
                    parameters.gesture.closePosition3, parameters.gesture.closePosition4,
                    parameters.gesture.closePosition5, parameters.gesture.closePosition6,
                    parameters.gesture.openToCloseTimeShift1, parameters.gesture.openToCloseTimeShift2,
                    parameters.gesture.openToCloseTimeShift3, parameters.gesture.openToCloseTimeShift4,
                    parameters.gesture.openToCloseTimeShift5, parameters.gesture.openToCloseTimeShift6,
                    parameters.gesture.closeToOpenTimeShift1, parameters.gesture.closeToOpenTimeShift2,
                    parameters.gesture.closeToOpenTimeShift3, parameters.gesture.closeToOpenTimeShift4,
                    parameters.gesture.closeToOpenTimeShift5, parameters.gesture.closeToOpenTimeShift6)
                transmitter().bleCommand(BLECommands.sendGestureInfo (parameters.deviceAddress, parameters.parameterID, gesture), MAIN_CHANNEL, WRITE)
            }

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
        return binding.root
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
    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
                    main?.runOnUiThread {
                        adapterWidgets.swapData(mDataFactory.prepareData(0))
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
        GesturesDelegateAdapter (
            onSelectorClick = {},
            onDeleteClick = { resultCb, gestureName -> showDeleteGestureFromRotationGroupDialog(resultCb, gestureName) },
            onAddGesturesToRotationGroup = { onSaveDialogClick -> showAddGestureToRotationGroupDialog(onSaveDialogClick) },
            onSendBLERotationGroup = {deviceAddress, parameterID -> sendBLERotationGroup(deviceAddress, parameterID) },
            onShowGestureSettings = { deviceAddress, parameterID, gestureID -> showGestureSettings(deviceAddress, parameterID, gestureID) },
            onRequestGestureSettings = {deviceAddress, parameterID, gestureID -> requestGestureSettings(deviceAddress, parameterID, gestureID)}
        )
    )

    private fun requestGestureSettings(deviceAddress: Int, parameterID: Int, gestureID: Int) {
        transmitter().bleCommand(BLECommands.requestGestureInfo(deviceAddress, parameterID, gestureID), MAIN_CHANNEL, WRITE)
    }
    private fun showGestureSettings (deviceAddress: Int, parameterID: Int, gestureID: Int) {
        val intent = Intent(context, UBI4GripperScreenWithEncodersActivity::class.java)
        intent.putExtra(DEVICE_ID_IN_SYSTEM_UBI4, deviceAddress)
        intent.putExtra(PARAMETER_ID_IN_SYSTEM_UBI4, parameterID)
        intent.putExtra(GESTURE_ID_IN_SYSTEM_UBI4, gestureID)
        startActivity(intent)
    }
    private fun sendBLERotationGroup(deviceAddress: Int, parameterID: Int) {
        Log.d("sendBLERotationGroup", "deviceAddress = $deviceAddress   parameterID = $parameterID")
        transmitter().bleCommand(BLECommands.sendRotationGroupInfo (deviceAddress, parameterID, RotationGroup(1,1,2,2,3,3,5,5,6,6,0,0,0,0,0,0)), MAIN_CHANNEL, WRITE)
    }
    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n")
    private fun showAddGestureToRotationGroupDialog(onSaveDialogClick: (()->Unit)) {
        System.err.println("showAddGestureToRotationGroupDialog")
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_rotation_group, null)
        val myDialog = Dialog(requireContext())
        val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesToGroupRv)
        val linearLayoutManager = LinearLayoutManager(context)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val listN: ArrayList<DialogGestureItem> = ArrayList()
        listN.add(DialogGestureItem("First profile", true))
        listN.add(DialogGestureItem("2 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("add", true))
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager

        val adapter = GesturesCheckAdapter(listN, object :
            OnCheckGestureListener {
            override fun onGestureClicked(position: Int, title: String) {
                System.err.println("onGestureClicked $position")
                if (listN[position].check) {
                    listN.removeAt(position)
                    listN.add(position, DialogGestureItem(title, false))
                } else {
                    listN.removeAt(position)
                    listN.add(position, DialogGestureItem(title, true))
                }
                gesturesRv.adapter?.notifyItemChanged(position)
            }
        })
        gesturesRv.adapter = adapter



        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToGroupCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToGroupSaveBtn)
        saveBtn.setOnClickListener {
            myDialog.dismiss()
            onSaveDialogClick.invoke()
        }
    }
    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n")
    private fun showDeleteGestureFromRotationGroupDialog(resultCb: ((result: Int)->Unit), gestureName: String) {
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_delete_gesture_from_rotation_group, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val ubi4DialogRotationGroupMessageTv = dialogBinding.findViewById<TextView>(R.id.ubi4DialogRotationGroupMessageTv)
        ubi4DialogRotationGroupMessageTv.text = getString(R.string.the_that_rocks_gesture_will_remain_available_in_the_gesture_collection_but_will_be_removed_from_the_rotation_group, "\"$gestureName\"")

        val cancelBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogRotationGroupCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val deleteBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogRotationGroupConfirmBtn)
        deleteBtn.setOnClickListener {
            myDialog.dismiss()
            resultCb.invoke(2)
        }
    }
    private fun oneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {
        Log.d("ButtonClick", "oneButtonPressed  addressDevice=$addressDevice  parameterID: $parameterID   command: $command")
        transmitter().bleCommand(BLECommands.sendOneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)
    }
    private fun oneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {
        Log.d("ButtonClick", "oneButtonReleased  addressDevice=$addressDevice  parameterID: $parameterID   command: $command")
//        transmitter().bleCommand(BLECommands.oneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)


        val stamp = Timestamp(System.currentTimeMillis())
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(System.currentTimeMillis())
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        transmitter().bleCommand(BLECommands.sendTimestampInfo(7,1, year, month, dayOfMonth, Date(stamp.time).day, Date(stamp.time).hours, Date(stamp.time).minutes, Date(stamp.time).seconds), MAIN_CHANNEL, WRITE)


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
