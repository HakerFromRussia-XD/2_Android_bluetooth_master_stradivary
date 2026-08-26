package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesOptic1Binding
import com.bailout.stickk.ubi4.adapters.dialog.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.data.state.WidgetState.rotationGroupGestures
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.GesturesItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.CollectionGesturePreviewController
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider.Companion.getCollectionGestures
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider.Companion.getGesture
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_BINDING_DATA
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListenerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.stream.Collectors
import kotlin.coroutines.cancellation.CancellationException

@Suppress("DEPRECATION")
class GesturesDelegateAdapterV3(
    val coroutineScope: CoroutineScope?,
    val gestureNameList: ArrayList<String>,
    val onDeleteClick: (resultCb: ((result: Int) -> Unit), gestureName: String) -> Unit,
    val onAddGesturesToRotationGroup: (onSaveDialogClick: ((selectedGestures: ArrayList<Gesture>) -> Unit)) -> Unit,
    val onAddGesturesToSprScreen: (onSaveClickDialog: (MutableList<Pair<Int, Int>>) -> Unit, List<Pair<Int, Int>>) -> Unit,
    val onSetCustomGesture: (
        onSaveDotsClick: (Pair<Int, Int>) -> Unit,
        bindingItem: Pair<Int, Int>
    ) -> Unit,
    val onSendBLERotationGroup: () -> Unit,
    val onSendBLEActiveGesture: (activeGesture: Int) -> Unit,
    val onSendBLEBindingGroup: (bindingGestureGroup: BindingGestureGroup) -> Unit,
    //  onShowGestureSettings колбек при нажатии на шестерёнку кастомного жеста
    val onShowGestureSettings: (subcommand: Int, gestureID: Int) -> Unit,
    val onRequestActiveGesture: () -> Unit,
    val onRequestRotationGroup: () -> Unit,
    val onRequestBindingGroup: () -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : RotationGroupItemAdapterV3.OnCopyClickRotationGroupListener,
    RotationGroupItemAdapterV3.OnDeleteClickRotationGroupListener,
    RotationGroupItemAdapterV3.OnSelectClickRotationGroupListener,
    ViewBindingDelegateAdapter<GesturesItemV3, Ubi4WidgetGesturesOptic1Binding>(Ubi4WidgetGesturesOptic1Binding::inflate) {

    private val ANIMATION_DURATION = 200
    private var itemsGesturesRotationArray: ArrayList<Pair<Long, String>>? = null
    private var listRotationGroupAdapter: RotationGroupItemAdapterV3? = null
    private var mRotationGroupDragLv: DragListView? = null
    private var hideFactoryCollectionGestures = true

    private lateinit var mRotationGroupExplanationTv: TextView
    private lateinit var mRotationGroupExplanation2Tv: TextView
    private lateinit var mRotationGroupExplanationIv: ImageView
    private lateinit var mRotationGroupExplanation2Iv: ImageView
    private lateinit var _ubi4GesturesSelectorV: View


    private lateinit var _rotationGroupTv: TextView
    private lateinit var _bindingGroupTv: TextView
    private lateinit var _collectionOfGesturesTv: TextView
    private lateinit var _gesturesSelectV: View
    private lateinit var _collectionGesturesCl: ConstraintLayout
    private lateinit var _rotationGroupCl: ConstraintLayout
    private lateinit var _sprGestureGroupCl: ConstraintLayout
    private lateinit var _annotationTv: TextView
    private lateinit var _annotationIv: ImageView


    private lateinit var mAddGestureToRotationGroupBtn: View
    private lateinit var mPlusIv: ImageView
    private var listBindingGesture: MutableList<Pair<Int, Int>> = mutableListOf()
    private var currentBindingGroup: BindingGestureGroup = BindingGestureGroup()
    private var parameterIDSet = mutableSetOf<ParameterInfo<Int, Int, Int, Int>>()
    private var deviceAddress = 0
    private var gestureCollectionBtns: ArrayList<Pair<View, Int>> = ArrayList()
    private var gestureCustomBtns: ArrayList<Pair<View, Int>> = ArrayList()

    private lateinit var _activeGestureNameCl: ConstraintLayout
    private lateinit var _activeGestureNameTv: TextView

    private lateinit var scope: CoroutineScope
    private var collectJob: Job? = null
    private var interactionJob: Job? = null
    private var currentActiveGestureId: Int? = null
    private var isRotationGroupResponseReceived = false
    private var isBindingGroupResponseReceived = false
    private var lastRenderedFilter: Int? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
    private var hideCollectionBtnView: View? = null
    private var addGestureToRotationGroupBtnView: View? = null
    private var chooseLearningGesturesBtnView: View? = null
    private val gestureSettingsBtns: ArrayList<View> = ArrayList()
    private val collectionPreviewController = CollectionGesturePreviewController()

    private companion object {
        private const val UNKNOWN_GESTURE_LABEL = "Unknow"
    }

    private val bindingAdapter = SelectedGesturesAdapter(
        selectedGesturesList = mutableListOf(),
        onCheckGestureSprListener = object : SelectedGesturesAdapter.OnCheckSprGestureListener {
            override fun onGestureSprClicked(position: Int, title: String, gestureId: Int) {
                if (!isInteractionEnabled || gestureId == 0) return
                currentActiveGestureId = gestureId
                listRotationGroupAdapter?.setActiveGestureId(gestureId)
                setActiveGesture(getGestureViewById(gestureId))
                updateActiveGestureHeader(gestureId)
                sendActiveGesture(gestureId)
            }
        },
        onDotsClickListener = dots@{ selectedPosition ->
            if (!isInteractionEnabled) return@dots
            val bindingItem = listBindingGesture.getOrNull(selectedPosition)
                ?: return@dots
            onSetCustomGesture({ updatedBindingItem ->
                val position = listBindingGesture.indexOfFirst { it.first == updatedBindingItem.first }
                if (position != -1) {
                    listBindingGesture[position] = updatedBindingItem
                    val bindingGroup = fillCollectionGesturesInBindingGroup()
                    sendBindingGroup(bindingGroup)
                }
            }, bindingItem)
        }
    )


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetGesturesOptic1Binding.onBind(item: GesturesItemV3) {
        platformLog("PWCE_GESTURES_WINDOW_V3", "запустился GesturesDelegateAdapterV3")
        mRotationGroupDragLv = rotationGroupDragLv
        onDestroyParent { onDestroy() }
        collectionPreviewController.release()


        // scope как в Optic
        scope = coroutineScope ?: main.lifecycleScope

        _rotationGroupTv = rotationGroupTv
        _bindingGroupTv = bindingGroupTv
        _collectionOfGesturesTv = collectionOfGesturesTv
        _gesturesSelectV = gesturesSelectV
        _ubi4GesturesSelectorV = ubi4GesturesSelectorV
        _collectionGesturesCl = collectionGesturesCl
        _rotationGroupCl = rotationGroupCl
        _sprGestureGroupCl = sprGestureGroupCl
        _annotationTv = annotationTv
        _annotationIv = annotationIv
        _activeGestureNameCl = activeGestureNameCl
        _activeGestureNameTv = activeGestureNameTv
        _gesturesSelectV = gesturesSelectV
        _collectionOfGesturesTv = collectionOfGesturesTv
        hideCollectionBtnView = hideCollectionBtn
        addGestureToRotationGroupBtnView = addGestureToRotationGroupBtn
        chooseLearningGesturesBtnView = chooseLearningGesturesBtn1


        when (val widget = item.widget) {
            is BaseParameterWidgetEStruct -> {
                deviceAddress = widget.baseParameterWidgetStruct.deviceId
                Log.d("ParamInfo deviceAddress", "deviceAddress1 : $deviceAddress")
                deviceAddress = widget.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).deviceAddress
                Log.d("ParamInfo deviceAddress", "deviceAddress2 : $deviceAddress")


                parameterIDSet = widget.baseParameterWidgetStruct.parameterInfoSet
                Log.d("ParamInfo", " ParamInfoEStruct parameterIDSet: $parameterIDSet")
            }

            is BaseParameterWidgetSStruct -> {
                deviceAddress = widget.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).deviceAddress
                parameterIDSet = widget.baseParameterWidgetStruct.parameterInfoSet
            }
        }
        val savedHideState = main.getInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, 1)
        hideFactoryCollectionGestures = savedHideState == 1
        if (hideFactoryCollectionGestures) {
            hideCollectionBtn.rotation = 0F
            collectionFactoryGesturesCl.visibility = View.VISIBLE
            collectionFactoryGesturesCl.alpha = 1.0f
        } else {
            hideCollectionBtn.rotation = 180F
            collectionFactoryGesturesCl.visibility = View.GONE
            collectionFactoryGesturesCl.alpha = 0.0f
        }

        // восстановить фильтр (1/2)
        val savedFilter = main.getInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1).coerceIn(1, 3)
        lastRenderedFilter = null
        renderFilterUI(savedFilter, animate = false)
        UiState.activeGestureFragmentFilterFlow.value = savedFilter // если у тебя этот flow доступен тут
        if (savedFilter == 2 && isInteractionEnabled) {
            requestRotationGroupWithRetry()
        }
        if (savedFilter == 3 && isInteractionEnabled) {
            requestBindingGroupWithRetry()
        }
        // запрос активного жеста — чтобы подсветка/текст пришли
        if (isInteractionEnabled) {
            onRequestActiveGesture()
        } else {
            updateActiveGestureHeader(null)
        }

        collectionOfGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1)
            UiState.activeGestureFragmentFilterFlow.value = 1
        }

        rotationGroupSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 2)
            UiState.activeGestureFragmentFilterFlow.value = 2
            if (isInteractionEnabled) {
                onRequestRotationGroup()
            }
        }
        sprGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 3)
            UiState.activeGestureFragmentFilterFlow.value = 3
            if (isInteractionEnabled) {
                onRequestBindingGroup()
            }
        }
        hideCollectionBtn.setOnClickListener {
            if (!isInteractionEnabled) return@setOnClickListener
            System.err.println("collectionFactoryGesturesCl.layoutParams.height = ${collectionFactoryGesturesCl.layoutParams.height}")

            if (hideFactoryCollectionGestures) {
                hideFactoryCollectionGestures = false
                hideCollectionBtn.animate().rotation(180F).duration = ANIMATION_DURATION.toLong()
                collectionUserGesturesCl.animate()
                    .translationY(-(collectionFactoryGesturesCl.height).toFloat()).duration =
                    ANIMATION_DURATION.toLong()
                collectionFactoryGesturesCl.animate()
                    .alpha(0.0f)
                    .setDuration(ANIMATION_DURATION.toLong())
                Handler().postDelayed({
                    collectionFactoryGesturesCl.visibility = View.GONE
                    collectionUserGesturesCl.animate().translationY(0F).duration = 0
                }, ANIMATION_DURATION.toLong())
            } else {
                hideFactoryCollectionGestures = true
                hideCollectionBtn.animate().rotation(0F).duration = ANIMATION_DURATION.toLong()
                collectionUserGesturesCl.animate()
                    .translationY(-(collectionFactoryGesturesCl.height).toFloat()).duration = 0
                collectionFactoryGesturesCl.visibility = View.VISIBLE
                Handler().postDelayed({
                    collectionUserGesturesCl.animate().translationY(0F).duration =
                        ANIMATION_DURATION.toLong()
                    collectionFactoryGesturesCl.animate()
                        .alpha(1.0f)
                        .setDuration(ANIMATION_DURATION.toLong())
                }, ANIMATION_DURATION.toLong())
            }
            main.saveInt(
                PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE,
                if (hideFactoryCollectionGestures) 1 else 0
            )
        }


        gestureCollectionBtns.clear()
        gestureCustomBtns.clear()
        gestureSettingsBtns.clear()

        for (i in 0..13) {
            val gestureCollectionBtn =
                this::class.java.getDeclaredField("gestureCollection${i}Btn")
                    .get(this) as? View
            val gestureCollectionTitle =
                this::class.java.getDeclaredField("gestureCollection${i}Tv")
                    .get(this) as? TextView
            val gestureCollectionImage =
                this::class.java.getDeclaredField("gestureCollection${i}Iv")
                    .get(this) as? ImageView

            if (i <= 10) {
                gestureCollectionTitle?.text = getCollectionGestures()[i].gestureName
                gestureCollectionImage?.setImageResource(getCollectionGestures()[i].gestureImage)

                val gestureId = i + 1
                gestureCollectionBtn?.let { gestureCollectionBtns.add(it to gestureId) }

                gestureCollectionBtn?.setOnClickListener {
                    if (!isInteractionEnabled) return@setOnClickListener
                    Log.d("GesturesDelegateAdapter", "GestureCollectionBtn $gestureId clicked")
                    currentActiveGestureId = gestureId
                    listRotationGroupAdapter?.setActiveGestureId(gestureId)
                    setActiveGesture(getGestureViewById(gestureId))
                    updateActiveGestureHeader(gestureId)
                    sendActiveGesture(gestureId)
                }
            } else {
                gestureCollectionTitle?.text = getCollectionGestures()[i + 1].gestureName
                gestureCollectionImage?.setImageResource(getCollectionGestures()[i + 1].gestureImage)

                val gestureId = i + 2
                gestureCollectionBtn?.let { gestureCollectionBtns.add(it to gestureId) }

                gestureCollectionBtn?.setOnClickListener {
                    if (!isInteractionEnabled) return@setOnClickListener
                    Log.d("GesturesDelegateAdapter", "GestureCollectionBtn $gestureId clicked")
                    currentActiveGestureId = gestureId
                    listRotationGroupAdapter?.setActiveGestureId(gestureId)
                    setActiveGesture(getGestureViewById(gestureId))
                    updateActiveGestureHeader(gestureId)
                    sendActiveGesture(gestureId)
                }
            }
        }
        collectionPreviewController.bind(root, { isInteractionEnabled }) { card -> card.performClick() }

        for (i in 1..PreferenceKeysUbi4.NUM_GESTURES) {
            val gestureCustomTv = this::class.java.getDeclaredField("gesture${i}NameTv")
                .get(this) as? TextView
            val gestureCustomBtn = this::class.java.getDeclaredField("gestureCustom${i}Btn")
                .get(this) as? View
            val gestureSettingsBtn = this::class.java.getDeclaredField("gesture${i}SettingsBtn")
                .get(this) as? View

            gestureCustomTv?.text = gestureNameList[i - 1]

            val gestureId = 63 + i
            gestureCustomBtn?.let { gestureCustomBtns.add(it to gestureId) }
            gestureSettingsBtn?.let { gestureSettingsBtns.add(it) }

            gestureCustomBtn?.setOnClickListener {
                if (!isInteractionEnabled) return@setOnClickListener
                Log.d("GesturesDelegateAdapter", "GestureCustomBtn $gestureId clicked")
                currentActiveGestureId = gestureId
                listRotationGroupAdapter?.setActiveGestureId(gestureId)
                setActiveGesture(getGestureViewById(gestureId))
                updateActiveGestureHeader(gestureId)
                sendActiveGesture(gestureId)
            }

            gestureSettingsBtn?.setOnClickListener {
                if (!isInteractionEnabled) return@setOnClickListener
                Log.d("gestureCustomBtn", "gestureSettingsBtn $i")
                onShowGestureSettings(
                    ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING).dataCode,
                    (0x3F).toInt() + i
                )
                main.saveInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, i)
            }
        }


        addGestureToRotationGroupBtn.setOnClickListener {
            if (!isInteractionEnabled) return@setOnClickListener
            val resultCb: ((selectedGestures: ArrayList<Gesture>) -> Unit) = { selectedGestures ->
                // проверка что элемент из selectedGestures содержится в rotationGroupGestures
                // если да, то не меняем его положение и добавляем новых в конец списка
                val notContainsList = selectedGestures.stream()
                    .filter { element -> !rotationGroupGestures.contains(element) }
                    .collect(Collectors.toList())//rotationGroupGestures.add())
                notContainsList.forEach { rotationGroupGestures.add(it) }
                // удаляем те элементы, которые были отчекнуты
                val finalList = rotationGroupGestures.stream()
                    .filter { element -> selectedGestures.contains(element) }
                    .collect(Collectors.toList())
                rotationGroupGestures = ArrayList(finalList)

                showIntroduction()
                setupListRecyclerView()
                synchronizeRotationGroup()
                sendRotationGroup()
                calculatingShowAddButton()
            }
            onAddGesturesToRotationGroup(resultCb)
        }
        rotationGroupDragLv.recyclerView.isVerticalScrollBarEnabled = false
        rotationGroupDragLv.setScrollingEnabled(false)
        rotationGroupDragLv.setOnClickListener {}
        rotationGroupDragLv.setDragListListener(object : DragListListenerAdapter() {
            override fun onItemDragStarted(position: Int) {}

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (!isInteractionEnabled) return
                if (fromPosition != toPosition) {
                    synchronizeRotationGroup()
                    sendRotationGroup()
                }
            }
        })

        chooseLearningGesturesBtn1.setOnClickListener {
            if (!isInteractionEnabled) return@setOnClickListener
            onAddGesturesToSprScreen({ selectedBindingGestures ->
                listBindingGesture = selectedBindingGestures
                val bindingGroup = fillCollectionGesturesInBindingGroup()
                sendBindingGroup(bindingGroup)
            }, listBindingGesture)
        }

        selectedSprGesturesRv.layoutManager = GridLayoutManager(root.context, 2).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        selectedSprGesturesRv.adapter = bindingAdapter

        mRotationGroupExplanationTv = rotationGroupExplanationTv
        mRotationGroupExplanation2Tv = rotationGroupExplanation2Tv
        mRotationGroupExplanationIv = rotationGroupExplanationIv
        mRotationGroupExplanation2Iv = rotationGroupExplanation2Iv
        mAddGestureToRotationGroupBtn = addGestureToRotationGroupBtn
        mPlusIv = plusIv
        showIntroduction()
        setupListRecyclerView()
        applyGesturesLockState(isInteractionEnabled)
        observeInteractionState()
        gestureFlowCollect()
    }

    private fun setActiveGesture(activeGesture: View?) {
        gestureCollectionBtns.forEach { it.first.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_outside) }
        gestureCustomBtns.forEach { it.first.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_outside) }
        activeGesture?.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_active)
    }

    private fun getGestureViewById(gestureId: Int?): View? {
        if (gestureId == null) return null
        gestureCollectionBtns.firstOrNull { it.second == gestureId }?.let { return it.first }
        gestureCustomBtns.firstOrNull { it.second == gestureId }?.let { return it.first }
        return null
    }

    private fun updateActiveGestureHeader(activeGestureId: Int?) {
        val name = when {
            activeGestureId == null -> UNKNOWN_GESTURE_LABEL
            activeGestureId in 1..62 -> getCollectionGestures().getOrNull(activeGestureId - 1)?.gestureName ?: UNKNOWN_GESTURE_LABEL
            else -> gestureNameList.getOrNull(activeGestureId - 64) ?: UNKNOWN_GESTURE_LABEL
        }
        _activeGestureNameTv.text = main.getString(R.string.active_gesture_is, name)
    }

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return

        interactionJob = scope.launch(Dispatchers.Main.immediate) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                applyGesturesLockState(enabled)
                if (enabled) {
                    onRequestActiveGesture()
                    if (UiState.activeGestureFragmentFilterFlow.value == 2) {
                        requestRotationGroupWithRetry()
                    }
                    if (UiState.activeGestureFragmentFilterFlow.value == 3) {
                        requestBindingGroupWithRetry()
                    }
                }
            }
        }
    }

    private fun applyGesturesLockState(enabled: Boolean) {
        hideCollectionBtnView?.isEnabled = enabled
        hideCollectionBtnView?.isClickable = enabled
        addGestureToRotationGroupBtnView?.isEnabled = enabled
        addGestureToRotationGroupBtnView?.isClickable = enabled
        chooseLearningGesturesBtnView?.isEnabled = enabled
        chooseLearningGesturesBtnView?.isClickable = enabled

        gestureCollectionBtns.forEach { (view, _) ->
            view.isEnabled = enabled
            view.isClickable = enabled
        }
        gestureCustomBtns.forEach { (view, _) ->
            view.isEnabled = enabled
            view.isClickable = enabled
        }
        gestureSettingsBtns.forEach { view ->
            view.isEnabled = enabled
            view.isClickable = enabled
        }

        mRotationGroupDragLv?.setCanDragVertically(enabled)
        listRotationGroupAdapter?.setInteractionEnabled(enabled)

        if (!enabled) {
            setActiveGesture(null)
            listRotationGroupAdapter?.setActiveGestureId(-1)
            updateActiveGestureHeader(null)
            return
        }

        val activeGestureId = currentActiveGestureId
        listRotationGroupAdapter?.setActiveGestureId(activeGestureId ?: -1)
        setActiveGesture(getGestureViewById(activeGestureId))
        updateActiveGestureHeader(activeGestureId)
    }



    private fun gestureFlowCollect() {
        collectJob?.cancel()

        collectJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                val currentGestureInfo = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
                val currentGestureMeta = ParameterInfoRegistry.getMeta(currentGestureInfo)
                val currentGestureKey = ParameterStoreV3.toKey(currentGestureInfo)

                val rotationGroupInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
                val rotationGroupMeta = ParameterInfoRegistry.getMeta(rotationGroupInfo)
                val rotationGroupKey = ParameterStoreV3.toKey(rotationGroupInfo)

                val bindingGroupInfo = ParameterInfoRegistry.require(P_KEY_BINDING_DATA)
                val bindingGroupMeta = ParameterInfoRegistry.getMeta(bindingGroupInfo)
                val bindingGroupKey = ParameterStoreV3.toKey(bindingGroupInfo)

                merge(
                    UiState.activeGestureFragmentFilterFlow.map{ filter ->
                        renderFilterUI(filter, animate = true)
                    },
                    ParameterStoreV3.updates.map { key ->
                        if (key != currentGestureKey) return@map
                        val typedValue = ParameterStoreV3.get(currentGestureInfo)
                            ?: run {
                                val serialized = ParameterProvider.getParameterV3(currentGestureInfo).data
                                val codecId = currentGestureMeta?.codecId ?: return@run null
                                ParameterCodecRegistryV3.decodeFromSerialized(codecId, serialized)
                            }
                        val currentGesture = (typedValue as? ParameterTypedValueV3.CurrentGesture)?.value
                            ?: return@map
                        currentActiveGestureId = currentGesture.currentGesture
                        setActiveGesture(getGestureViewById(currentGesture.currentGesture))
                        updateActiveGestureHeader(currentGesture.currentGesture)
                        bindingAdapter.setActiveGesture(currentGesture.currentGesture)
                        listRotationGroupAdapter?.setActiveGestureId(currentGesture.currentGesture)},
                    ParameterStoreV3.updates.map { key ->
                        if (key != rotationGroupKey) return@map
                        val typedValue = ParameterStoreV3.get(rotationGroupInfo)
                            ?: run {
                                val serialized = ParameterProvider.getParameterV3(rotationGroupInfo).data
                                val codecId = rotationGroupMeta?.codecId ?: return@run null
                                ParameterCodecRegistryV3.decodeFromSerialized(codecId, serialized)
                            }
                        val currentGesture = (typedValue as? ParameterTypedValueV3.RotationGroup)?.value
                            ?: return@map
                        val rotationGroupList = currentGesture.toGestureList()
                        val receivedGestures = rotationGroupList
                            .filter { item -> item.first != 0 }
                            .map { item -> getGesture(item.first) }
                        isRotationGroupResponseReceived = true
                        if (!hasSameRotationGroup(receivedGestures)) {
                            rotationGroupGestures.clear()
                            rotationGroupGestures.addAll(receivedGestures)

                            showIntroduction()
                            setupListRecyclerView()
                            synchronizeRotationGroup()
                            calculatingShowAddButton()
                        }

                        currentActiveGestureId?.let { id ->
                            setActiveGesture(getGestureViewById(id))
                            updateActiveGestureHeader(id)
                            bindingAdapter.setActiveGesture(id)
                        }
                        platformLog("requestRotationGroupV3", "приняли requestRotationGroupV3 $currentGesture")
                    },
                    ParameterStoreV3.updates.map { key ->
                        if (key != bindingGroupKey) return@map
                        val typedValue = ParameterStoreV3.get(bindingGroupInfo)
                            ?: run {
                                val serialized = ParameterProvider.getParameterV3(bindingGroupInfo).data
                                val codecId = bindingGroupMeta?.codecId ?: return@run null
                                ParameterCodecRegistryV3.decodeFromSerialized(codecId, serialized)
                            }
                        val bindingGroup = (typedValue as? ParameterTypedValueV3.BindingGroup)?.value
                            ?: return@map
                        currentBindingGroup = bindingGroup
                        listBindingGesture.clear()
                        bindingGroup.toGestureList().forEach { pair ->
                            if (pair.first != 0) {
                                listBindingGesture.add(pair)
                            }
                        }
                        isBindingGroupResponseReceived = true
                        fillCollectionGesturesInBindingGroup()
                        bindingAdapter.setActiveGesture(currentActiveGestureId)
                        platformLog("requestBindingGroupV3", "приняли requestBindingGroupV3 $bindingGroup")
                    },
                ).collect()
            } catch (e: CancellationException) {
                Log.d("gestureFlowCollect", "Job was cancelled: ${e.message}")
            } catch (e: Exception) {
                main.runOnUiThread {
                    main.showToast(main.getString(SharedRes.strings.plot_array_flow_collect_error.resourceId))
                }
                Log.e("gestureFlowCollect", "Exception: ${e.message}")
            }
        }
    }
    private fun calculatingShowAddButton() {
        if (rotationGroupGestures.size >= 8) {
            mAddGestureToRotationGroupBtn.visibility = View.GONE
            mPlusIv.visibility = View.GONE
        } else {
            mAddGestureToRotationGroupBtn.visibility = View.VISIBLE
            mPlusIv.visibility = View.VISIBLE
        }
    }

    private fun synchronizeRotationGroup() {
        rotationGroupGestures.clear()
        itemsGesturesRotationArray?.forEach {
            rotationGroupGestures.add(getGesture(it.second.split("™")[1].toInt()))
        }
    }

    private fun sendActiveGesture(gestureId: Int) {
        persistActiveGesture(gestureId)
        bindingAdapter.setActiveGesture(gestureId)
        onSendBLEActiveGesture(gestureId)
    }

    private fun sendRotationGroup() {
        persistRotationGroup()
        onSendBLERotationGroup()
    }

    private fun sendBindingGroup(bindingGroup: BindingGestureGroup) {
        persistBindingGroup(bindingGroup)
        onSendBLEBindingGroup(bindingGroup)
    }

    private fun persistActiveGesture(gestureId: Int) {
        val parameterInfo = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
        val typedValue = ParameterTypedValueV3.CurrentGesture(
            CurrentGestureV3(currentGesture = gestureId)
        )
        ParameterStoreV3.put(parameterInfo, typedValue)
        SettingsProfileManager.saveBleValue(parameterInfo, typedValue)

        val parameterMeta = ParameterInfoRegistry.getMeta(parameterInfo) ?: return
        ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, typedValue)?.let { encoded ->
            ParameterProvider.getParameterV3(parameterInfo).data = encoded
        }
    }

    private fun persistRotationGroup() {
        val parameterInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        val typedValue = ParameterTypedValueV3.RotationGroup(rotationGroupGestures.toRotationGroupV3())
        ParameterStoreV3.put(parameterInfo, typedValue)
        SettingsProfileManager.saveBleValue(parameterInfo, typedValue)

        val parameterMeta = ParameterInfoRegistry.getMeta(parameterInfo) ?: return
        ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, typedValue)?.let { encoded ->
            ParameterProvider.getParameterV3(parameterInfo).data = encoded
        }
    }

    private fun persistBindingGroup(bindingGroup: BindingGestureGroup) {
        val parameterInfo = ParameterInfoRegistry.require(P_KEY_BINDING_DATA)
        val typedValue = ParameterTypedValueV3.BindingGroup(bindingGroup)
        ParameterStoreV3.put(parameterInfo, typedValue)
        SettingsProfileManager.saveBleValue(parameterInfo, typedValue)

        val parameterMeta = ParameterInfoRegistry.getMeta(parameterInfo) ?: return
        ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, typedValue)?.let { encoded ->
            ParameterProvider.getParameterV3(parameterInfo).data = encoded
        }
    }

    private fun fillCollectionGesturesInBindingGroup(): BindingGestureGroup {
        currentBindingGroup = BindingGestureGroup()
        listBindingGesture.take(BindingGestureGroup.PAIR_COUNT).forEachIndexed { index, pair ->
            currentBindingGroup.setGestureAt(index, pair)
        }

        bindingAdapter.updateGestures(listBindingGesture)
        if (bindingAdapter.itemCount > 0) {
            _annotationTv.visibility = View.GONE
            _annotationIv.visibility = View.GONE
        } else {
            _annotationTv.visibility = View.VISIBLE
            _annotationIv.visibility = View.VISIBLE
        }

        return currentBindingGroup
    }

    private fun requestRotationGroupWithRetry() {
        if (!isInteractionEnabled) return
        isRotationGroupResponseReceived = false
        RetryUtils.sendRequestWithRetry(
            request = {
                onRequestRotationGroup()
                Log.d("GesturesDelegateAdapter", "Отправил onRequestRotationGroup")
            },
            isResponseReceived = { isRotationGroupResponseReceived },
            maxRetries = 5,
            delayMillis = 400,
            scope = scope
        )
    }

    private fun requestBindingGroupWithRetry() {
        if (!isInteractionEnabled) return
        isBindingGroupResponseReceived = false
        RetryUtils.sendRequestWithRetry(
            request = {
                onRequestBindingGroup()
                Log.d("GesturesDelegateAdapter", "Отправил onRequestBindingGroup")
            },
            isResponseReceived = { isBindingGroupResponseReceived },
            maxRetries = 5,
            delayMillis = 400,
            scope = scope
        )
    }

    private fun hasSameRotationGroup(newGestures: List<Gesture>): Boolean {
        if (rotationGroupGestures.size != newGestures.size) return false
        return rotationGroupGestures.zip(newGestures).all { (current, received) ->
            current.gestureId == received.gestureId
        }
    }

    private fun setupListRecyclerView() {
        val newItems = ArrayList(rotationGroupGestures.mapIndexed { index, gesture ->
            Pair(
                index.toLong(),
                gesture.gestureName + "™" + gesture.gestureId.toString()
            )
        })
        if (itemsGesturesRotationArray == newItems && listRotationGroupAdapter != null) {
            listRotationGroupAdapter?.setInteractionEnabled(isInteractionEnabled)
            if (isInteractionEnabled) {
                currentActiveGestureId?.let { id ->
                    listRotationGroupAdapter?.setActiveGestureId(id)
                } ?: listRotationGroupAdapter?.setActiveGestureId(-1)
            } else {
                listRotationGroupAdapter?.setActiveGestureId(-1)
            }
            return
        }

        mRotationGroupDragLv?.setLayoutManager(LinearLayoutManager(main.applicationContext))
        itemsGesturesRotationArray = newItems
        listRotationGroupAdapter =
            RotationGroupItemAdapterV3(
                itemsGesturesRotationArray,
                R.layout.ubi4_item_rotation_group,
                R.id.swapIv,
                false,
                this,
                this,
                this

            )
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        listRotationGroupAdapter?.setInteractionEnabled(isInteractionEnabled)
        if (isInteractionEnabled) {
            currentActiveGestureId?.let { id ->
                listRotationGroupAdapter?.setActiveGestureId(id)
            } ?: listRotationGroupAdapter?.setActiveGestureId(-1)
        } else {
            listRotationGroupAdapter?.setActiveGestureId(-1)
        }
        mRotationGroupDragLv?.setCanDragHorizontally(false)
        mRotationGroupDragLv?.setCanDragVertically(isInteractionEnabled)
        mRotationGroupDragLv?.setCustomDragItem(
            MyDragItem(
                main.applicationContext,
                R.layout.ubi4_item_rotation_group_drag
            )
        )
    }

    private fun renderFilterUI(activeFilter: Int, animate: Boolean = true, force: Boolean = false) {
        val normalizedFilter = activeFilter.coerceIn(1, 3)
        if (!force && lastRenderedFilter == normalizedFilter) return

        val collectionTargetColor = if (normalizedFilter == 1) {
            main.getColor(R.color.white)
        } else {
            main.getColor(R.color.ubi4_deactivate_text)
        }
        val rotationTargetColor = if (normalizedFilter == 2) {
            main.getColor(R.color.white)
        } else {
            main.getColor(R.color.ubi4_deactivate_text)
        }
        val bindingTargetColor = if (normalizedFilter == 3) {
            main.getColor(R.color.white)
        } else {
            main.getColor(R.color.ubi4_deactivate_text)
        }

        if (!_ubi4GesturesSelectorV.isLaidOut && !force) {
            _collectionOfGesturesTv.setTextColor(collectionTargetColor)
            _rotationGroupTv.setTextColor(rotationTargetColor)
            _bindingGroupTv.setTextColor(bindingTargetColor)
            showFilterContainers(normalizedFilter)
            if (_activeGestureNameCl.visibility != View.VISIBLE) {
                _activeGestureNameCl.visibility = View.VISIBLE
            }
            lastRenderedFilter = normalizedFilter
            _ubi4GesturesSelectorV.post {
                renderFilterUI(normalizedFilter, animate = false, force = true)
            }
            return
        }

        val density = main.resources.displayMetrics.density
        val selectorContainerWidth = _ubi4GesturesSelectorV.width.takeIf { it > 0 }
            ?: _ubi4GesturesSelectorV.measuredWidth
        val buttonWidth = selectorContainerWidth / 3f
        val extraOffsetPx = 3f * density
        val targetSelectorX = (normalizedFilter - 1) * buttonWidth + when (normalizedFilter) {
            2 -> extraOffsetPx - density
            3 -> extraOffsetPx * 2
            else -> 0f
        }

        if (animate && lastRenderedFilter != null) {
            ObjectAnimator.ofFloat(_gesturesSelectV, "translationX", targetSelectorX)
                .setDuration(ANIMATION_DURATION.toLong())
                .start()

            ObjectAnimator.ofInt(
                _collectionOfGesturesTv,
                "textColor",
                _collectionOfGesturesTv.currentTextColor,
                collectionTargetColor
            ).apply {
                setEvaluator(ArgbEvaluator())
                duration = ANIMATION_DURATION.toLong()
                start()
            }

            ObjectAnimator.ofInt(
                _rotationGroupTv,
                "textColor",
                _rotationGroupTv.currentTextColor,
                rotationTargetColor
            ).apply {
                setEvaluator(ArgbEvaluator())
                duration = ANIMATION_DURATION.toLong()
                start()
            }

            ObjectAnimator.ofInt(
                _bindingGroupTv,
                "textColor",
                _bindingGroupTv.currentTextColor,
                bindingTargetColor
            ).apply {
                setEvaluator(ArgbEvaluator())
                duration = ANIMATION_DURATION.toLong()
                start()
            }
        } else {
            _gesturesSelectV.translationX = targetSelectorX
            _collectionOfGesturesTv.setTextColor(collectionTargetColor)
            _rotationGroupTv.setTextColor(rotationTargetColor)
            _bindingGroupTv.setTextColor(bindingTargetColor)
        }

        showFilterContainers(normalizedFilter)
        if (_activeGestureNameCl.visibility != View.VISIBLE) {
            _activeGestureNameCl.visibility = View.VISIBLE
        }
        lastRenderedFilter = normalizedFilter
    }

    private fun showFilterContainers(activeFilter: Int) {
        when (activeFilter) {
            1 -> {
                if (_collectionGesturesCl.visibility != View.VISIBLE) _collectionGesturesCl.visibility = View.VISIBLE
                if (_rotationGroupCl.visibility != View.GONE) _rotationGroupCl.visibility = View.GONE
                if (_sprGestureGroupCl.visibility != View.GONE) _sprGestureGroupCl.visibility = View.GONE
            }
            2 -> {
                if (_rotationGroupCl.visibility != View.VISIBLE) _rotationGroupCl.visibility = View.VISIBLE
                if (_collectionGesturesCl.visibility != View.GONE) _collectionGesturesCl.visibility = View.GONE
                if (_sprGestureGroupCl.visibility != View.GONE) _sprGestureGroupCl.visibility = View.GONE
            }
            3 -> {
                if (_sprGestureGroupCl.visibility != View.VISIBLE) _sprGestureGroupCl.visibility = View.VISIBLE
                if (_collectionGesturesCl.visibility != View.GONE) _collectionGesturesCl.visibility = View.GONE
                if (_rotationGroupCl.visibility != View.GONE) _rotationGroupCl.visibility = View.GONE
            }
        }
    }

    private fun showIntroduction() {
        if (rotationGroupGestures.size == 0) {
            mRotationGroupExplanationTv.visibility = View.VISIBLE
            mRotationGroupExplanation2Tv.visibility = View.VISIBLE
            mRotationGroupExplanationIv.visibility = View.VISIBLE
            mRotationGroupExplanation2Iv.visibility = View.VISIBLE
        } else {
            mRotationGroupExplanationTv.visibility = View.GONE
            mRotationGroupExplanation2Tv.visibility = View.GONE
            mRotationGroupExplanationIv.visibility = View.GONE
            mRotationGroupExplanation2Iv.visibility = View.GONE
        }
    }

    override fun isForViewType(item: Any): Boolean = item is GesturesItemV3
    override fun GesturesItemV3.getItemId(): Any = when (val w = widget) {
        is BaseParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetStruct
            val paramsKey = s.parameterInfoSet
                .toList()
                .sortedWith(
                    compareBy<ParameterInfo<Int, Int, Int, Int>> { it.dataOffsets }
                        .thenBy { it.deviceAddress }
                        .thenBy { it.parameterID }
                        .thenBy { it.dataCode }
                )
                .joinToString("_") { p ->
                    "${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${p.dataOffsets}"
                }
            "gestures-${s.widgetPosition}-${paramsKey}"
        }
        is BaseParameterWidgetEStruct -> {
            val s = w.baseParameterWidgetStruct
            val paramsKey = s.parameterInfoSet
                .toList()
                .sortedWith(
                    compareBy<ParameterInfo<Int, Int, Int, Int>> { it.dataOffsets }
                        .thenBy { it.deviceAddress }
                        .thenBy { it.parameterID }
                        .thenBy { it.dataCode }
                )
                .joinToString("_") { p ->
                    "${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${p.dataOffsets}"
                }
            "gestures-${s.widgetPosition}-${paramsKey}"
        }
        else -> "gestures-$title"
    }

    class MyDragItem internal constructor(context: Context?, layoutId: Int) :
        DragItem(context, layoutId) {
        override fun onBindDragView(clickedView: View, dragView: View) {
            val text =
                (clickedView.findViewById<View>(R.id.gestureInRotationGroupTv) as TextView).text
            (dragView.findViewById<View>(R.id.gestureInRotationGroupTv) as TextView).text =
                text
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCopyClick(position: Int, gestureId: String) {
        if (!isInteractionEnabled) return
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        listRotationGroupAdapter?.notifyDataSetChanged()
        synchronizeRotationGroup()
        sendRotationGroup()
        calculatingShowAddButton()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onDeleteClickCb(position: Int) {
        if (!isInteractionEnabled) return
        val resultCb: ((result: Int) -> Unit) = {
            rotationGroupGestures.removeAt(position)
            showIntroduction()
            setupListRecyclerView()
            synchronizeRotationGroup()
            sendRotationGroup()
            calculatingShowAddButton()
        }
        onDeleteClick(resultCb, rotationGroupGestures.get(position).gestureName)
    }

    // Метод для завершения работы CoroutineScope, чтобы освободить ресурсы
    fun onDestroy() {
        Log.d("LifeCycele", "stopCollectingGestureFlow")
        collectJob?.cancel()
        interactionJob?.cancel()
        interactionJob = null
        collectionPreviewController.release()
    }

    override fun onRotationGestureClick(position: Int, gestureName: String?, gestureId: Int) {
        if (!isInteractionEnabled) return
        if (gestureId == 0) return

        currentActiveGestureId = gestureId
        listRotationGroupAdapter?.setActiveGestureId(gestureId)

        setActiveGesture(getGestureViewById(gestureId))
        updateActiveGestureHeader(gestureId)

        sendActiveGesture(gestureId)
    }
}

private fun List<Gesture>.toRotationGroupV3(): RotationGroupV3 {
    val pairs = take(8).map { it.gestureId to it.gestureId }
    fun id(index: Int) = pairs.getOrNull(index)?.first ?: 0
    fun image(index: Int) = pairs.getOrNull(index)?.second ?: 0
    return RotationGroupV3(
        gesture1Id = id(0),
        gesture1ImageId = image(0),
        gesture2Id = id(1),
        gesture2ImageId = image(1),
        gesture3Id = id(2),
        gesture3ImageId = image(2),
        gesture4Id = id(3),
        gesture4ImageId = image(3),
        gesture5Id = id(4),
        gesture5ImageId = image(4),
        gesture6Id = id(5),
        gesture6ImageId = image(5),
        gesture7Id = id(6),
        gesture7ImageId = image(6),
        gesture8Id = id(7),
        gesture8ImageId = image(7)
    )
}
