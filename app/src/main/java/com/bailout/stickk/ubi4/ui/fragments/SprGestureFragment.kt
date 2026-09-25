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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSprGesturesBinding
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.rotationGroupGestures
import com.bailout.stickk.ubi4.models.dialog.DialogCollectionGestureItem
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.UBI4GripperScreenWithEncodersActivityV3
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.UBI4GripperScreenWithEncodersActivityV3.Companion.EXTRA_USE_V3_GESTURE_PROTOCOL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.PARAMETER_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GESTURE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider
import com.bailout.stickk.ubi4.ui.gestures.GestureCollectionFactory
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.simform.refresh.SSPullToRefreshLayout
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets.V3GesturesWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets.V3GesturesWidgetMapper
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.V3GesturesAction
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.V3GesturesViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.V3GesturesUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.V3GestureSettingsUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.V3RotationGroupSelectionDialogHost
import com.bailout.stickk.ubi4.versions.v3.di.V3GesturesViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.GesturesTwoSectionDelegateAdapterV3
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.util.stream.Collectors
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

@Suppress("DEPRECATION")
class SprGestureFragment: BaseWidgetsFragment() {
    private var _binding: Ubi4FragmentSprGesturesBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val display = 0
    private val v3GesturesAdapter by lazy {
        GesturesTwoSectionDelegateAdapterV3(
            onAction = ::onV3GesturesAction,
            onDestroyParent = ::registerDelegateCleanup,
        )
    }
    protected override val adapterWidgets: CompositeDelegateAdapter by lazy {
        if (UiState.isInterfaceV3Activated) CompositeDelegateAdapter(v3GesturesAdapter)
        else super.adapterWidgets
    }
    private var gesturesViewModel: V3GesturesViewModel? = null
    private var gestureStateJob: Job? = null
    private var pendingRender: Runnable? = null
    private val v3WidgetMapper = V3GesturesWidgetMapper()
    private var renderedV3Widgets: List<V3GesturesWidget>? = null
    private var renderedV3WidgetsUpdateId: Long? = null
    private var rotationGestureRemovalDialog: Dialog? = null
    private var renderedRemovalRequestId: Long? = null
    private val rotationGroupSelectionDialog = V3RotationGroupSelectionDialogHost()



    override fun loadGestureNameList() {
        if (!UiState.isInterfaceV3Activated) super.loadGestureNameList()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        updateFlow.tryEmit(0)
    }

    @SuppressLint("CutPasteId", "LogNotTimber")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentSprGesturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        main = activity as? MainActivityUBI4
        val ubi4InitialData = if (UiState.isInterfaceV3Activated) null else mDataFactory.prepareData(display).also { data ->
            widgetListUpdater()
            data.forEach { Log.d("DataType", "Element type: ${it::class.simpleName}") }
        }
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.isEnabled = false

//        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprGesturesRv.layoutManager = LinearLayoutManager(context)
        binding.sprGesturesRv.adapter = adapterWidgets
        if (ubi4InitialData == null) {
            bindV3Gestures()
        } else {
            adapterWidgets.swapData(ubi4InitialData)
        }
    }

    private fun bindV3Gestures() {
        val viewModel = ViewModelProvider(this, V3GesturesViewModelFactory.create(requireContext()))[V3GesturesViewModel::class.java]
        gesturesViewModel = viewModel
        renderV3GesturesScreen(viewModel.uiState.value)
        gestureStateJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onAction(V3GesturesAction.ViewAttached)
                try {
                    viewModel.uiState.collect { state ->
                        renderV3GesturesScreen(state)
                        openV3GestureSettings(state.gestureSettings)
                    }
                } finally {
                    viewModel.onAction(V3GesturesAction.ViewDetached)
                    dismissRotationGestureRemovalDialog()
                    rotationGroupSelectionDialog.dismiss()
                }
            }
        }
    }

    private fun onV3GesturesAction(action: V3GesturesAction) {
        gesturesViewModel?.onAction(action)
    }

    private fun renderV3GesturesScreen(state: V3GesturesUiState) {
        val currentBinding = _binding ?: return
        val recyclerView = currentBinding.sprGesturesRv
        pendingRender?.let(recyclerView::removeCallbacks)
        pendingRender = null
        if (recyclerView.isComputingLayout) {
            pendingRender = Runnable {
                if (_binding === currentBinding) gesturesViewModel?.uiState?.value?.let(::renderV3GesturesScreen)
            }.also(recyclerView::post)
            return
        }
        v3GesturesAdapter.render(state)
        if (renderedV3Widgets != state.widgets || renderedV3WidgetsUpdateId != state.widgetsUpdateId) {
            adapterWidgets.swapData(v3WidgetMapper.toItems(state.widgets))
            renderedV3Widgets = state.widgets
            renderedV3WidgetsUpdateId = state.widgetsUpdateId
            main?.refreshBottomNavVisibility()
        }
        rotationGroupSelectionDialog.render(
            requireContext(), state.rotationGroupSelection,
            createItems = { ids ->
                val collection = GestureCollectionFactory.create(requireContext()) { index, fallback ->
                    state.customGestureNames.collectionNames.getOrNull(index) ?: fallback
                }.associateBy { it.gestureId }
                ids.map { DialogCollectionGestureItem(collection.getValue(it)) }
            },
            onAction = ::onV3GesturesAction,
        )
        val removal = state.rotationGestureRemoval
        if (renderedRemovalRequestId == removal?.requestId) return
        dismissRotationGestureRemovalDialog()
        if (removal == null) return
        renderedRemovalRequestId = removal.requestId
        rotationGestureRemovalDialog = createRotationGestureRemovalDialog(
            gestureName = (GestureCollectionFactory.create(requireContext()) { index, fallback ->
                state.customGestureNames.collectionNames.getOrNull(index) ?: fallback
            }.firstOrNull { it.gestureId == removal.gestureId } ?: Gesture(0)).gestureName,
            onConfirm = { onV3GesturesAction(V3GesturesAction.RotationGestureRemovalConfirmed(removal.requestId)) },
            onCancel = { onV3GesturesAction(V3GesturesAction.RotationGestureRemovalCancelled(removal.requestId)) },
        )
    }

    private fun openV3GestureSettings(request: V3GestureSettingsUiState?) {
        if (request == null || gesturesViewModel?.uiState?.value?.gestureSettings != request) return
        val intent = Intent(requireContext(), UBI4GripperScreenWithEncodersActivityV3::class.java).apply {
            putExtra(EXTRA_USE_V3_GESTURE_PROTOCOL, true)
            putExtra(PARAMETER_ID_IN_SYSTEM_UBI4, ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING).dataCode)
            putExtra(GESTURE_ID_IN_SYSTEM_UBI4, request.gestureId)
        }
        startActivity(intent)
        // Keep this acknowledgement in the same main-thread call: the editor reads the saved number in onCreate.
        onV3GesturesAction(V3GesturesAction.GestureSettingsOpened(request.requestId))
    }

    private fun dismissRotationGestureRemovalDialog() {
        rotationGestureRemovalDialog?.dismiss()
        rotationGestureRemovalDialog = null
        renderedRemovalRequestId = null
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
            ), MAIN_CHANNEL_CHARACTERISTIC, WRITE
        ){}
    }

    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n", "SuspiciousIndentation")
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
                    main?.showToast(getString(SharedRes.strings.rotation_dialog_limit_message.resourceId))
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
    override fun showDeleteGestureFromRotationGroupDialog(resultCb: ((result: Int)->Unit), gestureName: String) {
        createRotationGestureRemovalDialog(gestureName, onConfirm = { resultCb(2) })
    }

    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n")
    private fun createRotationGestureRemovalDialog(
        gestureName: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {},
    ): Dialog {
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
            onCancel()
        }

        val deleteBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogRotationGroupConfirmBtn)
        deleteBtn.setOnClickListener {
            myDialog.dismiss()
            onConfirm()
        }
        return myDialog
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect { updateEvent ->
                Log.d("WidgetUpdater", "updateFlow event received: $updateEvent")

                val newData = mDataFactory.prepareData(display)
                Log.d("SprGestureFragment", "New data size: ${newData.size}")
                val currentBinding = _binding ?: return@collect
                val recyclerView = currentBinding.sprGesturesRv
                pendingRender?.let(recyclerView::removeCallbacks)
                pendingRender = null
                if (recyclerView.isComputingLayout) {
                    pendingRender = Runnable {
                        if (_binding === currentBinding) {
                            adapterWidgets.swapData(newData)
                            main?.refreshBottomNavVisibility()
                        }
                    }.also(recyclerView::post)
                } else {
                    adapterWidgets.swapData(newData)
                    main?.refreshBottomNavVisibility()
                }
//                binding.refreshLayout.setRefreshing(false)
            }
        }





    }

    override fun onDestroyView() {
        gesturesViewModel?.onAction(V3GesturesAction.ViewDetached)
        gestureStateJob?.cancel()
        gestureStateJob = null
        rotationGroupSelectionDialog.dismiss()
        dismissRotationGestureRemovalDialog()
        gesturesViewModel = null
        renderedV3Widgets = null
        renderedV3WidgetsUpdateId = null
        pendingRender?.let { _binding?.sprGesturesRv?.removeCallbacks(it) }
        pendingRender = null
        _binding?.sprGesturesRv?.adapter = null
        main = null
        _binding = null
        super.onDestroyView()
    }


}
