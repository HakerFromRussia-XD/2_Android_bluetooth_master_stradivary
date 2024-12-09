import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSprGesturesBinding
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.SprGesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckSprGestureListener2
import com.bailout.stickk.ubi4.adapters.dialog.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesOpticDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.models.BindingGestureItem
import com.bailout.stickk.ubi4.models.DialogCollectionGestureItem
import com.bailout.stickk.ubi4.models.SprDialogCollectionGestureItem
import com.bailout.stickk.ubi4.models.SprGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DEVICE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.GESTURE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.PARAMETER_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.ui.gripper.with_encoders.UBI4GripperScreenWithEncodersActivity
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class SprGestureFragment() : Fragment() {
    private lateinit var binding: Ubi4FragmentSprGesturesBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val selectedGesturesSet = mutableSetOf<String>()

    private var gestureNameList = ArrayList<String>()
    private var onDestroyParent: (() -> Unit)? = null
    private var onDestroyParentCallbacks = mutableListOf<() -> Unit>()


    @SuppressLint("CutPasteId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentSprGesturesBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }
        loadGestureNameList()
        //настоящие виджеты
        widgetListUpdater()
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprGesturesRv.layoutManager = LinearLayoutManager(context)
        binding.sprGesturesRv.adapter = adapterWidgets
        return binding.root
    }

    private fun refreshWidgetsList() {
        graphThreadFlag = false
        listWidgets.clear()
        onDestroyParentCallbacks.forEach { it.invoke() }
        onDestroyParentCallbacks.clear()
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
        //TODO только для демонстрации
        Handler().postDelayed({
            binding.refreshLayout.setRefreshing(false)
        }, 1000)
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
                    main?.runOnUiThread {
                        Log.d("widgetListUpdater", "${mDataFactory.prepareData(0)}")
                        adapterWidgets.swapData(mDataFactory.prepareData(0))
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
        PlotDelegateAdapter(
            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent) }

        ),

        OneButtonDelegateAdapter(
            onButtonPressed = { addressDevice, parameterID, command ->
                oneButtonPressed(
                    addressDevice,
                    parameterID,
                    command
                )
            },
            onButtonReleased = { addressDevice, parameterID, command ->
                oneButtonReleased(
                    addressDevice,
                    parameterID,
                    command
                )
            },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent) }

        ),
        GesturesOpticDelegateAdapter(
            gestureNameList = gestureNameList,
            onSelectorClick = {},
            onAddGesturesToSprScreen = { onSaveClickDialog, listSprItem, bindingGestureList ->
                showControlGesturesDialog(onSaveClickDialog, listSprItem, bindingGestureList)
            },
            onShowGestureSettings = { deviceAddress, parameterID, gestureID ->
                showGestureSettings(
                    deviceAddress,
                    parameterID,
                    gestureID
                )
            },
            onRequestGestureSettings = { deviceAddress, parameterID, gestureID ->
                requestGestureSettings(
                    deviceAddress,
                    parameterID,
                    gestureID
                )
            },
            onSetCustomGesture = { onSaveDotsClick, position, name ->
                showCustomGesturesDialog(onSaveDotsClick, position, name)
            },
            onDestroyParent = { onDestroyParent -> this.onDestroyParent = onDestroyParent },
        ),
    )

    private fun requestGestureSettings(deviceAddress: Int, parameterID: Int, gestureID: Int) {
        Log.d("requestGestureSettings", "считывание данных в фрагменте")
        transmitter().bleCommand(
            BLECommands.requestGestureInfo(
                deviceAddress,
                parameterID,
                gestureID
            ), MAIN_CHANNEL, WRITE
        )
    }

    private fun showGestureSettings(deviceAddress: Int, parameterID: Int, gestureID: Int) {
        val intent = Intent(context, UBI4GripperScreenWithEncodersActivity::class.java)
        intent.putExtra(DEVICE_ID_IN_SYSTEM_UBI4, deviceAddress)
        intent.putExtra(PARAMETER_ID_IN_SYSTEM_UBI4, parameterID)
        intent.putExtra(GESTURE_ID_IN_SYSTEM_UBI4, gestureID)
        startActivity(intent)
    }

    @SuppressLint("MissingInflatedId", "LogNotTimber")
    private fun showCustomGesturesDialog(
        onSaveClick: ((name: String, position: Int) -> Unit),
        position: Int,
        name: String
    ) {
        System.err.println("showAddGestureToSprScreen")
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_spr_screen, null)
        val myDialog = Dialog(requireContext())
        val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesRv)
        val linearLayoutManager = LinearLayoutManager(context)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
        val titleText = dialogBinding.findViewById<TextView>(R.id.dialogTitleBindingTv)
        titleText.setText(R.string.assign_gesture)

//        val gestureItemsProvider = GestureSprAndCustomItemsProvider()
//        val sprGestureItemList =
//            gestureItemsProvider.getSprAndCustomGestureItemList(requireContext())
        val sprGestureItemList: ArrayList<DialogCollectionGestureItem> =
            ArrayList(
                CollectionGesturesProvider.getCollectionGestures()
                    .map { DialogCollectionGestureItem(it) })

        sprGestureItemList.forEach { dialogGesture ->
            if (dialogGesture.gesture.gestureName == name) {
                dialogGesture.check = true
            }
        }


        var selectedGesturePosition = sprGestureItemList.indexOfFirst { it.check }
        Log.d("selectedGesturesSet", "Selected gesture: $selectedGesturePosition")


        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter = GesturesCheckAdapter(sprGestureItemList, object :
            OnCheckGestureListener {
            override fun onGestureClicked(
                clickedPosition: Int,
                dialogGesture: DialogCollectionGestureItem
            ) {

                if (selectedGesturePosition != -1 && selectedGesturePosition == clickedPosition) {
                    sprGestureItemList[selectedGesturePosition] =
                        sprGestureItemList[selectedGesturePosition].copy(check = false)
                    selectedGesturePosition = -1
                    gesturesRv.adapter?.notifyItemChanged(clickedPosition)

                    selectedGesturesSet.remove(dialogGesture.gesture.gestureName)
                    Log.d("showCustomGesturesDialog1", " remove1: $selectedGesturesSet")

                    return
                }

                if (selectedGesturesSet.contains(dialogGesture.gesture.gestureName) && selectedGesturePosition != clickedPosition) {
                    Toast.makeText(
                        context,
                        getString(R.string.toast_notification_gesture_in_use),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("selectedGesturesSet", "$selectedGesturesSet")
                    return
                }

                if (selectedGesturePosition != -1) {
                    sprGestureItemList[selectedGesturePosition] =
                        sprGestureItemList[selectedGesturePosition].copy(check = false)
                    selectedGesturesSet.remove(sprGestureItemList[selectedGesturePosition].gesture.gestureName)
                    Log.d("showCustomGesturesDialog1", " remove2: $selectedGesturesSet")

                    gesturesRv.adapter?.notifyItemChanged(selectedGesturePosition)
                }

                selectedGesturePosition = clickedPosition
                sprGestureItemList[clickedPosition] =
                    sprGestureItemList[clickedPosition].copy(check = true)
                gesturesRv.adapter?.notifyItemChanged(clickedPosition)

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
            val selectedGesture = if (selectedGesturePosition != -1) {
                sprGestureItemList[selectedGesturePosition].gesture.gestureName
            } else {
                null
            }
            if (selectedGesture != null) {
                selectedGesturesSet.add(selectedGesture)
                Log.d("showCustomGesturesDialog1", " add: $selectedGesturesSet")
            } else {

                name.let {
                    selectedGesturesSet.remove(it)
                    Log.d("showCustomGesturesDialog1", " remove3: $selectedGesturesSet")

                }

            }
            myDialog.dismiss()
            onSaveClick.invoke(selectedGesture ?: "", position)


        }
    }


    @SuppressLint("MissingInflatedId", "LogNotTimber", "CutPasteId")
    private fun showControlGesturesDialog(
        onSaveClick: (List<SprGestureItem>) -> Unit,
        selectedGestures: List<SprGestureItem>,
        bindingGestureList: List<BindingGestureItem>
    ) {
        System.err.println("showAddGestureToSprScreen")
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_spr_screen, null)
        val myDialog = Dialog(requireContext())
        val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesRv)


        val linearLayoutManager = LinearLayoutManager(context)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()


        val sprGestureItemList: ArrayList<SprDialogCollectionGestureItem> =
            ArrayList(
                SprGestureItemsProvider(requireContext()).getSprGestureItemList()
                    .map { SprDialogCollectionGestureItem(it) })



        for (dialogGesture in sprGestureItemList) {
            selectedGestures.find { it.title == dialogGesture.gesture.title }?.let {
                dialogGesture.check = true
            }
        }
        Log.d("showControlGesturesDialog", "$sprGestureItemList")

        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter =
            SprGesturesCheckAdapter(sprGestureItemList, object : OnCheckSprGestureListener2 {
                override fun onSprGestureClicked2(
                    position: Int,
                    dialogGesture: SprDialogCollectionGestureItem
                ) {
                    System.err.println("onGestureClicked $position")
                    Log.d("onGestureClicked", "$sprGestureItemList")

                    val checkedItems = sprGestureItemList.mapIndexedNotNull { index, item ->
                        if (item.check) index else null
                    }
                    var unselectedPosition =
                        bindingGestureList.indexOfFirst { it.sprGestureItem.title == dialogGesture.gesture.title }
                    sprGestureItemList[position] =
                        sprGestureItemList[position].copy(check = !sprGestureItemList[position].check)



                    if (!sprGestureItemList[position].check) {
                        unselectedPosition = checkedItems.indexOf(position)
                        Log.d(
                            "GestureUnselected",
                            "Position of unselected gesture: $unselectedPosition"
                        )
                    }

                    // Удаление безопасным способом
                    if (unselectedPosition >= 0 && unselectedPosition < bindingGestureList.size) {
                        val iterator = selectedGesturesSet.iterator()
                        while (iterator.hasNext()) {
                            val gesture = iterator.next()
                            if (gesture == bindingGestureList[unselectedPosition].nameOfUserGesture) {
                                iterator.remove()
                            }
                        }
                    } else {
                        Log.e(
                            "onGestureClicked1",
                            "Index $unselectedPosition is out of bounds for bindingGestureList with size ${bindingGestureList.size}"
                        )
                    }

                    Log.d("onGestureClicked", "$sprGestureItemList")
                    gesturesRv.adapter?.notifyItemChanged(position)
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
            val provider = SprGestureItemsProvider(requireContext())
            val selectedGestures =
                sprGestureItemList.filter { it.check }.mapNotNull { dialogGesture ->
                    val gestureName = dialogGesture.gesture.title
                    val keyNameGesture = provider.getKeyNameGestureByGestureName(gestureName)
                    if (keyNameGesture != null) {
                        val animationId = provider.getAnimationIdByKeyNameGesture(keyNameGesture)
                        if (animationId != R.drawable.sleeping)
                            SprGestureItem(
                                title = gestureName,
                                animationId = animationId,
                                check = true,
                                keyNameGesture = keyNameGesture
                            )
                        else {
                            Log.e(
                                "SprGestureFragment",
                                "Animation not found for gesture: $gestureName"
                            )
                            null
                        }
                    } else {
                        Log.e(
                            "SprGestureFragment",
                            "KeyNameGesture not found for gesture: $gestureName"
                        )
                        null
                    }
                    // SprGestureItem(dialogGesture.gesture.gestureName, dialogGesture.gesture.gestureImage, true, "")
                }



            myDialog.dismiss()
            onSaveClick.invoke(selectedGestures)


        }
    }

    private fun oneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {
        System.err.println("oneButtonPressed    parameterID: $parameterID   command: $command")
        transmitter().bleCommand(
            BLECommands.sendOneButtonCommand(addressDevice, parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

    private fun oneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {


        transmitter().bleCommand(
            BLECommands.sendOneButtonCommand(addressDevice, parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

    private fun loadGestureNameList() {
        val macKey = navigator().getString(PreferenceKeysUBI4.LAST_CONNECTION_MAC)
        gestureNameList.clear()
        for (i in 0 until PreferenceKeysUBI4.NUM_GESTURES) {
            System.err.println("loadGestureNameList: " + PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM + macKey + i)
            gestureNameList.add(
                navigator().getString(PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM + macKey + i)
                    .toString()
            )
//            System.err.println("loadGestureNameList: ${gestureNameList[i]}")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        onDestroyParentCallbacks.forEach { it.invoke() }


    }

}
