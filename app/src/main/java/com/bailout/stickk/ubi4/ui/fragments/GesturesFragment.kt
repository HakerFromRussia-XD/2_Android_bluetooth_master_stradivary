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
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.DialogCollectionGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DEVICE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.GESTURE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.PARAMETER_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.gripper.with_encoders.UBI4GripperScreenWithEncodersActivity
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.connectedDeviceAddress
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.rotationGroupGestures
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
import java.util.stream.Collectors
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties


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
//        val mBLEParser = main?.let { BLEParser(it) }
//        mBLEParser?.parseReceivedData(BLECommands.testDataTransfer())

        //настоящие виджеты
        widgetListUpdater()
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())

//        showGestureSettings(6,11,64)


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }

        RxUpdateMainEventUbi4.getInstance().gestureStateWithEncodersObservable
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameters ->
                Log.d("gestureStateWithEncodersObservable", "parameters = ${parameters.gesture.openPosition1}")
                transmitter().bleCommand(BLECommands.sendGestureInfo (parameters), MAIN_CHANNEL, WRITE)
            }
        RxUpdateMainEventUbi4.getInstance().readCharacteristicBLE
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameters ->
                requestGestureSettings(parameters.deviceAddress, parameters.parameterID, parameters.gestureID)
            }

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
            onButtonPressed = { _, _, _ ->  },
            onButtonReleased = { _, _, _ ->  }
        ) ,
        GesturesDelegateAdapter (
            onSelectorClick = {},
            onDeleteClick = { resultCb, gestureName -> showDeleteGestureFromRotationGroupDialog(resultCb, gestureName) },
            onAddGesturesToRotationGroup = { onSaveDialogClick -> showAddGestureToRotationGroupDialog(onSaveDialogClick) },
            onSendBLERotationGroup = {deviceAddress, parameterID -> sendBLERotationGroup(deviceAddress, parameterID) },
            onShowGestureSettings = { deviceAddress, parameterID, gestureID -> showGestureSettings(deviceAddress, parameterID, gestureID) },
            onRequestGestureSettings = {deviceAddress, parameterID, gestureID -> requestGestureSettings(deviceAddress, parameterID, gestureID)},
            onRequestRotationGroup = {deviceAddress, parameterID -> requestRotationGroup(deviceAddress, parameterID)}
        )
    )

    private fun requestRotationGroup(deviceAddress: Int, parameterID: Int) {
        Log.d("uiRotationGroupObservable", "считывание данных в фрагменте")
        transmitter().bleCommand(BLECommands.requestRotationGroup(deviceAddress, parameterID), MAIN_CHANNEL, WRITE)
    }
    private fun requestGestureSettings(deviceAddress: Int, parameterID: Int, gestureID: Int) {
        Log.d("requestGestureSettings", "считывание данных в фрагменте")
        transmitter().bleCommand(BLECommands.requestGestureInfo(deviceAddress, parameterID, gestureID), MAIN_CHANNEL, WRITE)
    }
    private fun showGestureSettings (deviceAddress: Int, parameterID: Int, gestureID: Int) {
        val intent = Intent(context, UBI4GripperScreenWithEncodersActivity::class.java)
        intent.putExtra(DEVICE_ID_IN_SYSTEM_UBI4, deviceAddress)
        intent.putExtra(PARAMETER_ID_IN_SYSTEM_UBI4, parameterID)
        intent.putExtra(GESTURE_ID_IN_SYSTEM_UBI4, gestureID)
        startActivity(intent)
    }
    private fun sendBLERotationGroup (deviceAddress: Int, parameterID: Int) {
        val rotationGroup = RotationGroup()
        rotationGroupGestures.forEachIndexed { index, item ->
            // Используем рефлексию, чтобы найти и изменить свойства
            val idProperty = RotationGroup::class.memberProperties.find { it.name == "gesture${index + 1}Id" } as? KMutableProperty1<RotationGroup, Int>
            val imageIdProperty = RotationGroup::class.memberProperties.find { it.name == "gesture${index + 1}ImageId" } as? KMutableProperty1<RotationGroup, Int>

            // Устанавливаем значения, если свойства найдены
            idProperty?.set(rotationGroup, item.gestureId)
            imageIdProperty?.set(rotationGroup, item.gestureId)
        }

        // Проверяем результат
        Log.d("sendBLERotationGroup", "deviceAddress = $deviceAddress  parameterID = $parameterID   rotationGroup = $rotationGroup")

        transmitter().bleCommand(BLECommands.sendRotationGroupInfo (deviceAddress, parameterID, rotationGroup), MAIN_CHANNEL, WRITE)
    }
    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n")
    private fun showAddGestureToRotationGroupDialog(onSaveDialogClick: ((selectedGestures: ArrayList<Gesture>)->Unit)) {
        System.err.println("showAddGestureToRotationGroupDialog")
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_rotation_group, null)
        val myDialog = Dialog(requireContext())
        val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesToGroupRv)
        val linearLayoutManager = LinearLayoutManager(context)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()


        val dialogCollectionGestures: ArrayList<DialogCollectionGestureItem> =
            ArrayList(CollectionGesturesProvider.getCollectionGestures().map { DialogCollectionGestureItem(it) })

        // установка галочек в списке соответственно текущей группе ротации
        for (dialogGesture in dialogCollectionGestures) {
            rotationGroupGestures.find { it.gestureId == dialogGesture.gesture.gestureId }?.let {
                dialogGesture.check = true
            }
        }


        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager

        // инвертация галочек в списке при клике на элементы
        val adapter = GesturesCheckAdapter(dialogCollectionGestures, object :
            OnCheckGestureListener {
            override fun onGestureClicked(position: Int, dialogGesture: DialogCollectionGestureItem) {
                System.err.println("onGestureClicked $position")
                if (dialogCollectionGestures[position].check) {
                    dialogCollectionGestures.removeAt(position)
                    dialogCollectionGestures.add(position, DialogCollectionGestureItem(dialogGesture.gesture, false))
                } else {
                    // в dialogCollectionGestures посчитать количество элементов с галочкой
                    val checkedElements = dialogCollectionGestures.stream().filter{element -> element.check}.collect(Collectors.toList())
                    if (checkedElements.size >= 8) {
                        main?.showToast("Нельзя добавить больше 8-ми жестов")
                    } else {
                        dialogCollectionGestures.removeAt(position)
                        dialogCollectionGestures.add(position, DialogCollectionGestureItem(dialogGesture.gesture, true))
                    }
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
            val selectedGestures = dialogCollectionGestures.filter { it.check }.map { dialogCollectionGestureItem ->
                dialogCollectionGestureItem.gesture
            }
            onSaveDialogClick.invoke(ArrayList(selectedGestures))
            myDialog.dismiss()
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
}
