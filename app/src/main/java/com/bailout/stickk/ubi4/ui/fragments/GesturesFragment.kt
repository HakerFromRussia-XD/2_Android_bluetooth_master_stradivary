package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.rotationGroupGestures
import com.bailout.stickk.ubi4.models.dialog.DialogCollectionGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.resources.AndroidResourceProvider
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.stream.Collectors
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties


@Suppress("DEPRECATION")
class GesturesFragment : BaseWidgetsFragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()
    private val display = 0
    private var mSettings: SharedPreferences? = null
    private val collectionGesturesProvider: CollectionGesturesProvider by lazy {
        CollectionGesturesProvider(AndroidResourceProvider(requireContext()))
    }

    @SuppressLint("CheckResult", "LogNotTimber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        mSettings = context?.getSharedPreferences(PreferenceKeysUBI4.APP_PREFERENCES, Context.MODE_PRIVATE)
        if (activity != null) { main = activity as MainActivityUBI4? }
        Log.d("LifeCycele", "onCreateView")

        //настоящие виджеты
        widgetListUpdater()
        adapterWidgets.swapData(mDataFactory.prepareData(display))
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

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect {
                    main?.runOnUiThread {
                        Log.d("widgetListUpdater", "${mDataFactory.prepareData(display)}")
                        binding.homeRv.post {
                            adapterWidgets.swapData(mDataFactory.prepareData(display))
                        }
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    override fun sendBLERotationGroup (deviceAddress: Int, parameterID: Int) {
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
    override fun showAddGestureToRotationGroupDialog(onSaveDialogClick: ((selectedGestures: ArrayList<Gesture>)->Unit)) {
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
            ArrayList(collectionGesturesProvider.getCollectionGestures().map { DialogCollectionGestureItem(it) })

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
    override fun showDeleteGestureFromRotationGroupDialog(resultCb: ((result: Int)->Unit), gestureName: String) {
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
