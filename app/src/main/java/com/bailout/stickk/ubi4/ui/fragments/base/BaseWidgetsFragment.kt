package com.bailout.stickk.ubi4.ui.fragments.base

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckSprGestureListener2
import com.bailout.stickk.ubi4.adapters.dialog.SprGesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesOpticDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SliderDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SwitcherDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.TrainingFragmentDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.models.DialogCollectionGestureItem
import com.bailout.stickk.ubi4.models.SprDialogCollectionGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DEVICE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.GESTURE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.PARAMETER_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.ui.gripper.with_encoders.UBI4GripperScreenWithEncodersActivity
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter

abstract class BaseWidgetsFragment : Fragment() {
    private var gestureNameList = ArrayList<String>()
    private val onDestroyParentCallbacks = mutableListOf<() -> Unit>()
    private var main: MainActivityUBI4? = null
    private var loadingCurrentDialog: Dialog? = null
    private lateinit var bleController: BLEController




    protected val adapterWidgets by lazy {
        CompositeDelegateAdapter(
            PlotDelegateAdapter(
                onDestroyParent = { onDestroyParent ->
                    onDestroyParentCallbacks.add(onDestroyParent)
                }
            ),
            OneButtonDelegateAdapter(
                onButtonPressed = { device, param, command ->
                    oneButtonPressed(device, param, command)
                },
                onButtonReleased = { device, param, command ->
                    oneButtonReleased(device, param, command)
                },
                onDestroyParent = { onDestroyParent ->
                    onDestroyParentCallbacks.add(onDestroyParent)
                }
            ),
            //TODO Сделать ячейки GesturesDelegateAdapter и GesturesOpticDelegateAdapter разными
//            GesturesDelegateAdapter (
//                gestureNameList = gestureNameList,
//                onDeleteClick = { resultCb, gestureName -> showDeleteGestureFromRotationGroupDialog(resultCb, gestureName) },
//                onAddGesturesToRotationGroup = { onSaveDialogClick -> showAddGestureToRotationGroupDialog(onSaveDialogClick) },
//                onSendBLERotationGroup = {deviceAddress, parameterID -> sendBLERotationGroup(deviceAddress, parameterID) },
//                onSendBLEActiveGesture = {deviceAddress, parameterID, activeGesture -> sendBLEActiveGesture(deviceAddress, parameterID, activeGesture) },
//                onShowGestureSettings = { deviceAddress, parameterID, gestureID -> showGestureSettings(deviceAddress, parameterID, gestureID) },
//                onRequestGestureSettings = {deviceAddress, parameterID, gestureID -> requestGestureSettings(deviceAddress, parameterID, gestureID)},
//                onRequestRotationGroup = {deviceAddress, parameterID -> requestRotationGroup(deviceAddress, parameterID)},
//                onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent)}
//            ),
            GesturesOpticDelegateAdapter(
                gestureNameList = gestureNameList,
                onAddGesturesToSprScreen = { onSaveClickDialog, bindingGestureList ->
                    showControlGesturesDialog(onSaveClickDialog, bindingGestureList)
                },
                onShowGestureSettings = { device, param, gestureID ->
                    showGestureSettings(device, param, gestureID)
                },
                onRequestGestureSettings = { device, param, gestureID ->
                    requestGestureSettings(device, param, gestureID)
                },
                onSetCustomGesture = { onSaveDotsClick, bindingItem ->
                    showCustomGesturesDialog(onSaveDotsClick, bindingItem)
                },
                onSendBLEActiveGesture = { deviceAddress, parameterID, activeGesture ->
                    sendBLEActiveGesture(deviceAddress, parameterID, activeGesture)
                },
                onRequestActiveGesture = { deviceAddress, parameterID ->
                    requestActiveGesture(deviceAddress, parameterID)
                },
                onSendBLEBindingGroup = { deviceAddress, parameterID, bindingGestureGroup ->
                    sendBLEBindingGroup(deviceAddress, parameterID, bindingGestureGroup)
                },
                onRequestBindingGroup = { deviceAddress, parameterID ->
                    requestBindingGroup(deviceAddress, parameterID)
                },
                onDestroyParent = { onDestroyParent ->
                    onDestroyParentCallbacks.add(onDestroyParent)
                }
            ),
            TrainingFragmentDelegateAdapter(
                onConfirmClick = {
                    if (isAdded) {
                        Log.d("StateCallBack", "onConfirmClick: Button clicked")
                        showConfirmTrainingDialog {
                            navigator().showMotionTrainingScreen {
                                main?.manageTrainingLifecycle()
                            }
                        }
                    } else {
                        Log.e("StateCallBack", "Fragment is not attached to activity")
                    }
                },
                onShowFileClick = { addressDevice -> showFilesDialog(addressDevice,6) },
                onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent) },
            ),
            SwitcherDelegateAdapter(
                onSwitchClick = { addressDevice, parameterID, switchState ->
                    sendSwitcherState(addressDevice, parameterID, switchState)
                },
                onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent) }
            ),
            SliderDelegateAdapter(
                onSetProgress = { addressDevice, parameterID, progress ->
                    sendSliderProgress(
                        addressDevice,
                        parameterID,
                        progress
                    )
                },
                onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent) }
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadGestureNameList()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }
        bleController = (requireActivity() as MainActivityUBI4).getBLEController()

    }
    override fun onDestroy() {
        super.onDestroy()
        onDestroyParentCallbacks.forEach { it.invoke() }
    }

    //CallBacks
//    open fun onPlotReady(num: Int) {}
    open fun oneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {
        transmitter().bleCommand(BLECommands.sendOneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)
    }
    open fun oneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {
        transmitter().bleCommand(BLECommands.sendOneButtonCommand(addressDevice, parameterID, command), MAIN_CHANNEL, WRITE)
    }
    open fun showControlGesturesDialog(onSaveClickDialog: (MutableList<Pair<Int, Int>>) -> Unit, bindingGestureList:  List<Pair<Int, Int>>) {
    System.err.println("showAddGestureToSprScreen")
    val dialogBinding =
        layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_spr_screen, null)
    val myDialog = Dialog(requireContext())
    val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesRv)

    var checkedItems = bindingGestureList.map { it.first }

    val linearLayoutManager = LinearLayoutManager(context)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val sprGestureDialogList: ArrayList<SprDialogCollectionGestureItem> =
        ArrayList(
            SprGestureItemsProvider(requireContext()).getSprGestureItemList()
                .map { SprDialogCollectionGestureItem(it) })

    for (sprDialogCollectionGestureItem in sprGestureDialogList) {
        bindingGestureList.find { it.first == sprDialogCollectionGestureItem.gesture.sprGestureId }?.let {
            sprDialogCollectionGestureItem.check = true
        }
    }
    Log.d("showControlGesturesDialog", "$sprGestureDialogList")

    linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
    gesturesRv.layoutManager = linearLayoutManager
    val adapter =
        SprGesturesCheckAdapter(sprGestureDialogList, object : OnCheckSprGestureListener2 {
            override fun onSprGestureClicked2(
                position: Int,
                sprDialogCollectionGestureItem: SprDialogCollectionGestureItem
            ) {
                // происходит изменение состояние check на противоположное
                sprGestureDialogList[position] =
                    sprGestureDialogList[position].copy(check = !sprGestureDialogList[position].check)
                //происходит создание листа в котором позиции выбраных элементов отображаются числами, а остальные null
                checkedItems = sprGestureDialogList.mapIndexedNotNull { index, item ->
                    if (item.check) item.gesture.sprGestureId else null
                }.toMutableList()
                Log.d("DialogGestureTest", "checkedItems: $checkedItems")

                gesturesRv.adapter?.notifyItemChanged(position)
            }
        })
    gesturesRv.adapter = adapter


    val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToCancelBtn)
    cancelBtn.setOnClickListener {
        myDialog.dismiss()
    }

    val saveBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToSaveBtn)
    saveBtn.setOnClickListener {

        myDialog.dismiss()

        val listBindingGesture: MutableList<Pair<Int, Int>> = checkedItems.map { item ->
            // Ищем пару с first == item
            val existingPair = bindingGestureList.find { it.first == item }
            // Если нашли, используем существующее second, иначе 0
            if (existingPair != null) {
                Pair(item, existingPair.second)
            } else {
                Pair(item, 0)
            }
        }.toMutableList()


        onSaveClickDialog.invoke(listBindingGesture)
    }
}
    open fun showGestureSettings(deviceAddress: Int, parameterID: Int, gestureID: Int) {
        val intent = Intent(context, UBI4GripperScreenWithEncodersActivity::class.java)
        intent.putExtra(DEVICE_ID_IN_SYSTEM_UBI4, deviceAddress)
        intent.putExtra(PARAMETER_ID_IN_SYSTEM_UBI4, parameterID)
        intent.putExtra(GESTURE_ID_IN_SYSTEM_UBI4, gestureID)
        startActivity(intent)
    }
    open fun requestGestureSettings(deviceAddress: Int, parameterID: Int, gestureID: Int) {
        if (!isAdded) { return }
        transmitter().bleCommandWithQueue(BLECommands.requestGestureInfo(deviceAddress, parameterID, gestureID), MAIN_CHANNEL, WRITE) {}
    }
    open fun showCustomGesturesDialog(onSaveDotsClick: (Pair<Int, Int>) -> Unit, bindingItem: Pair<Int, Int>) {
    val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_spr_screen, null)
    val myDialog = Dialog(requireContext())
    val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesRv)
    val linearLayoutManager = LinearLayoutManager(context)

    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val titleText = dialogBinding.findViewById<TextView>(R.id.dialogTitleBindingTv)
    titleText.setText(R.string.assign_gesture)

    val collectionGestureDialogList: ArrayList<DialogCollectionGestureItem> = ArrayList(
        CollectionGesturesProvider.getCollectionGestures().map { gesture ->
            DialogCollectionGestureItem(
                gesture = gesture,
                check = (gesture.gestureId == bindingItem.second)
            )
        }
    )

    var selectedGesturePosition = collectionGestureDialogList.indexOfFirst { it.check }
    var selectedGestureId = bindingItem.second
    Log.d("DialogGestureTest", "selectedGesturePosition $selectedGesturePosition")
    linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
    gesturesRv.layoutManager = linearLayoutManager

    val adapter = GesturesCheckAdapter(
        collectionGestureDialogList,
        object : OnCheckGestureListener {
            override fun onGestureClicked(
                clickedPosition: Int,
                dialogGesture: DialogCollectionGestureItem
            ) {
                if (selectedGesturePosition == clickedPosition) {
                    // снимаем выделение

                    collectionGestureDialogList[clickedPosition] = collectionGestureDialogList[clickedPosition].copy(
                        check = false
                    )
                    selectedGesturePosition = -1
                    selectedGestureId = -1
                    gesturesRv.adapter?.notifyItemChanged(clickedPosition)
                    Log.d("DialogGestureTest", "selectedGesturePosition $selectedGesturePosition")

                    return
                }
                if (selectedGesturePosition != -1) {
                    collectionGestureDialogList[selectedGesturePosition] =
                        collectionGestureDialogList[selectedGesturePosition].copy(check = false)
                    gesturesRv.adapter?.notifyItemChanged(selectedGesturePosition)

                }

                // Устанавливаем текущий жест как выбранный
                selectedGesturePosition = clickedPosition
                selectedGestureId = dialogGesture.gesture.gestureId
                collectionGestureDialogList[clickedPosition] =
                    collectionGestureDialogList[clickedPosition].copy(check = true)
                gesturesRv.adapter?.notifyItemChanged(clickedPosition)
                Log.d("DialogGestureTest", "selectedGestureId $selectedGestureId")

            }

        }
    )
    gesturesRv.adapter = adapter

    val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToCancelBtn)
    cancelBtn.setOnClickListener {
        myDialog.dismiss()
    }

    val saveBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToSaveBtn)
    saveBtn.setOnClickListener {
        val sendBindingItem = if (selectedGestureId != -1) { Pair(bindingItem.first, selectedGestureId)
        } else { Pair(bindingItem.first, 0) }

        onSaveDotsClick.invoke(sendBindingItem)
        myDialog.dismiss()
    }
}
    open fun sendBLEActiveGesture(deviceAddress: Int, parameterID: Int, activeGesture: Int) {
        transmitter().bleCommand(BLECommands.sendActiveGesture(deviceAddress, parameterID, activeGesture), MAIN_CHANNEL, WRITE)
    }
    open fun requestActiveGesture(deviceAddress: Int, parameterID: Int) {
        if (!isAdded) {return}
        transmitter().bleCommandWithQueue(BLECommands.requestActiveGesture(deviceAddress, parameterID), MAIN_CHANNEL, WRITE){}
    }
    open fun sendBLEBindingGroup(deviceAddress: Int, parameterID: Int, bindingGestureGroup: BindingGestureGroup) {
        if (!isAdded) { return }
        transmitter().bleCommandWithQueue(BLECommands.sendBindingGroupInfo (deviceAddress, parameterID, bindingGestureGroup), MAIN_CHANNEL, WRITE){}
    }
    open fun requestBindingGroup(deviceAddress: Int, parameterID: Int) {
        if (!isAdded) { return }
        transmitter().bleCommandWithQueue(BLECommands.requestBindingGroup(deviceAddress, parameterID), MAIN_CHANNEL, WRITE){}
    }
    @SuppressLint("MissingInflatedId")
    open fun showConfirmTrainingDialog(confirmClick: () -> Unit) {
        main?.showToast("Виджет отображается вне своего экрана")
    }
    open fun showAddGestureToRotationGroupDialog(onSaveDialogClick: ((selectedGestures: ArrayList<Gesture>)->Unit)) {
        main?.showToast("Виджет отображается вне своего экрана")
    }
    open fun sendBLERotationGroup (deviceAddress: Int, parameterID: Int) {
        main?.showToast("Виджет отображается вне своего экрана")
    }
    private fun requestRotationGroup(deviceAddress: Int, parameterID: Int) {
        transmitter().bleCommandWithQueue(BLECommands.requestRotationGroup(deviceAddress, parameterID), MAIN_CHANNEL, WRITE){}
    }

    open fun showConfirmLoadingDialog(onConfirm: () -> Unit) {
        if (loadingCurrentDialog != null && loadingCurrentDialog?.isShowing == true) {
            return
        }
        closeCurrentDialog()
        val dialogFileBinding = layoutInflater.inflate(R.layout.ubi4_dialog_confirm_loading, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogFileBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingCurrentDialog = myDialog
        myDialog.show()

        val confirmBtn = dialogFileBinding.findViewById<View>(R.id.ubi4DialogConfirmLoadingBtn)
        confirmBtn.setOnClickListener {
            closeCurrentDialog()
            onConfirm()
        }
        val cancelBtn = dialogFileBinding.findViewById<View>(R.id.ubi4DialogLoadingCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    open fun showFilesDialog(addressDevice: Int, parameterID: Int) {
        main?.showToast("Виджет отображается вне своего экрана")
    }
    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n")
    open fun showDeleteGestureFromRotationGroupDialog(resultCb: ((result: Int)->Unit), gestureName: String) {
        main?.showToast("Виджет отображается вне своего экрана")
    }


    private fun sendSwitcherState(addressDevice: Int, parameterID: Int, switchState: Boolean) {
        transmitter().bleCommandWithQueue(BLECommands.sendSwitcherCommand(addressDevice, parameterID, switchState), MAIN_CHANNEL, WRITE){}
    }
    private fun sendSliderProgress(addressDevice: Int, parameterID: Int, progress: ArrayList<Int>) {
        transmitter().bleCommandWithQueue(BLECommands.sendSliderCommand(addressDevice, parameterID, progress), MAIN_CHANNEL, WRITE){}
    }


    //Others fun
    private fun loadGestureNameList() {
        val macKey = navigator().getString(PreferenceKeysUBI4.LAST_CONNECTION_MAC)
        gestureNameList.clear()
        for (i in 0 until PreferenceKeysUBI4.NUM_GESTURES) {
            System.err.println("loadGestureNameList: " + PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM + macKey + i)
            gestureNameList.add(
                navigator().getString(PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM + macKey + i)
            )
        }
    }
    open fun closeCurrentDialog() {
        loadingCurrentDialog?.dismiss()
        loadingCurrentDialog = null
    }
    open suspend fun sendFileInChunks(byteArray: ByteArray, name: String, addressDevice: Int, parameterID: Int, progressBar: ProgressBar) {}
}