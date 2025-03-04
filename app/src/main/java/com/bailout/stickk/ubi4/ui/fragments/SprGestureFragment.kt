import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSprGesturesBinding
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesOpticDelegateAdapter
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.DialogCollectionGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.rotationGroupGestures
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.bailout.stickk.ubi4.utility.BorderAnimator
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.notifyAll
import java.util.stream.Collectors
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

@Suppress("DEPRECATION")
class SprGestureFragment: BaseWidgetsFragment() {
    private lateinit var binding: Ubi4FragmentSprGesturesBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()
    private var onDestroyParentCallbacks = mutableListOf<() -> Unit>()

    private val display = 0



    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        adapterWidgets.notifyDataSetChanged()
    }

    @SuppressLint("CutPasteId", "LogNotTimber")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentSprGesturesBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }

        //настоящие виджеты
        widgetListUpdater()
        adapterWidgets.swapData(mDataFactory.prepareData(display))
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())

        mDataFactory.prepareData(display).forEach {
            Log.d("DataType", "Element type: ${it::class.simpleName}")
        }

        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprGesturesRv.layoutManager = LinearLayoutManager(context)
        binding.sprGesturesRv.adapter = adapterWidgets
        return binding.root

    }

    override fun sendBLERotationGroup(deviceAddress: Int, parameterID: Int) {
        val rotationGroup = RotationGroup()
        rotationGroupGestures.forEachIndexed { index, item ->
            // Используем рефлексию, чтобы найти и изменить свойства
            val idProperty =
                RotationGroup::class.memberProperties.find { it.name == "gesture${index + 1}Id" } as? KMutableProperty1<RotationGroup, Int>
            val imageIdProperty =
                RotationGroup::class.memberProperties.find { it.name == "gesture${index + 1}ImageId" } as? KMutableProperty1<RotationGroup, Int>

            // Устанавливаем значения, если свойства найдены
            idProperty?.set(rotationGroup, item.gestureId)
            imageIdProperty?.set(rotationGroup, item.gestureId)
        }

        // Проверяем результат
        Log.d(
            "sendBLERotationGroup",
            "deviceAddress = $deviceAddress  parameterID = $parameterID   rotationGroup = $rotationGroup"
        )

        transmitter().bleCommandWithQueue(
            BLECommands.sendRotationGroupInfo(
                deviceAddress,
                parameterID,
                rotationGroup
            ), MAIN_CHANNEL, WRITE
        ){}
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
                val checkedElements = dialogCollectionGestures.stream().filter{element -> element.check}.collect(
                    Collectors.toList())
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

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect { _ ->
                val newData = mDataFactory.prepareData(display)
                Log.d("SprGestureFragment", "New data size: ${newData.size}")
                adapterWidgets.swapData(mDataFactory.prepareData(display))
                binding.refreshLayout.setRefreshing(false)
            }
        }
    }


}

