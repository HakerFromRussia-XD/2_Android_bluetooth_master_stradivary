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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesBinding
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
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
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider.Companion.getCollectionGestures
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider.Companion.getGesture
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
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
    val onSendBLERotationGroup: () -> Unit,
    val onSendBLEActiveGesture: (activeGesture: Int) -> Unit,
    //  onShowGestureSettings колбек при нажатии на шестерёнку кастомного жеста
    val onShowGestureSettings: (subcommand: Int, gestureID: Int) -> Unit,
    val onRequestActiveGesture: () -> Unit,
    val onRequestRotationGroup: () -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : RotationGroupItemAdapterV3.OnCopyClickRotationGroupListener,
    RotationGroupItemAdapterV3.OnDeleteClickRotationGroupListener,
    RotationGroupItemAdapterV3.OnSelectClickRotationGroupListener,
    ViewBindingDelegateAdapter<GesturesItemV3, Ubi4WidgetGesturesBinding>(Ubi4WidgetGesturesBinding::inflate) {

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
    private lateinit var _collectionOfGesturesTv: TextView
    private lateinit var _gesturesSelectV: View
    private lateinit var _collectionGesturesCl: ConstraintLayout
    private lateinit var _rotationGroupCl: ConstraintLayout


    private lateinit var mAddGestureToRotationGroupBtn: View
    private lateinit var mPlusIv: ImageView
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
    private var lastRenderedFilter: Int? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
    private var hideCollectionBtnView: View? = null
    private var addGestureToRotationGroupBtnView: View? = null
    private val gestureSettingsBtns: ArrayList<View> = ArrayList()

    private companion object {
        private const val UNKNOWN_GESTURE_LABEL = "Unknow"
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetGesturesBinding.onBind(item: GesturesItemV3) {
        platformLog("PWCE_GESTURES_WINDOW_V3", "запустился GesturesDelegateAdapterV3")
        mRotationGroupDragLv = rotationGroupDragLv
        onDestroyParent { onDestroy() }


        // scope как в Optic
        scope = coroutineScope ?: main.lifecycleScope

        _rotationGroupTv = rotationGroupTv
        _collectionOfGesturesTv = collectionOfGesturesTv
        _gesturesSelectV = gesturesSelectV
        _ubi4GesturesSelectorV = ubi4GesturesSelectorV
        _collectionGesturesCl = collectionGesturesCl
        _rotationGroupCl = rotationGroupCl
        _activeGestureNameCl = activeGestureNameCl
        _activeGestureNameTv = activeGestureNameTv
        _gesturesSelectV = gesturesSelectV
        _collectionOfGesturesTv = collectionOfGesturesTv
        hideCollectionBtnView = hideCollectionBtn
        addGestureToRotationGroupBtnView = addGestureToRotationGroupBtn


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
        val savedFilter = main.getInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1)
        lastRenderedFilter = null
        renderFilterUI(savedFilter, animate = false)
        UiState.activeGestureFragmentFilterFlow.value = savedFilter // если у тебя этот flow доступен тут
        if (savedFilter == 2 && isInteractionEnabled) {
            requestRotationGroupWithRetry()
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

        for (i in 1..8) {
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
                }
            }
        }
    }

    private fun applyGesturesLockState(enabled: Boolean) {
        hideCollectionBtnView?.isEnabled = enabled
        hideCollectionBtnView?.isClickable = enabled
        addGestureToRotationGroupBtnView?.isEnabled = enabled
        addGestureToRotationGroupBtnView?.isClickable = enabled

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
                        }
                        platformLog("requestRotationGroupV3", "приняли requestRotationGroupV3 $currentGesture")
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
        onSendBLEActiveGesture(gestureId)
    }

    private fun sendRotationGroup() {
        persistRotationGroup()
        onSendBLERotationGroup()
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
        if (!force && lastRenderedFilter == activeFilter) return

        val showCollection = activeFilter == 1
        val collectionTargetColor = if (showCollection) {
            main.getColor(R.color.white)
        } else {
            main.getColor(R.color.ubi4_deactivate_text)
        }
        val rotationTargetColor = if (showCollection) {
            main.getColor(R.color.ubi4_deactivate_text)
        } else {
            main.getColor(R.color.white)
        }

        if (!_ubi4GesturesSelectorV.isLaidOut && !force) {
            _collectionOfGesturesTv.setTextColor(collectionTargetColor)
            _rotationGroupTv.setTextColor(rotationTargetColor)
            showCollectionGestures(showCollection, _rotationGroupCl, _collectionGesturesCl)
            if (_activeGestureNameCl.visibility != View.VISIBLE) {
                _activeGestureNameCl.visibility = View.VISIBLE
            }
            lastRenderedFilter = activeFilter
            _ubi4GesturesSelectorV.post {
                renderFilterUI(activeFilter, animate = false, force = true)
            }
            return
        }

        val displayMetrics = main.resources.displayMetrics
        val selectorContainerWidth = _ubi4GesturesSelectorV.width.takeIf { it > 0 }
            ?: _ubi4GesturesSelectorV.measuredWidth
        val selectorStart = 18f * displayMetrics.density
        val targetSelectorX = if (activeFilter == 1) {
            selectorStart
        } else {
            (selectorContainerWidth / 2f) + selectorStart
        }

        if (animate && lastRenderedFilter != null) {
            ObjectAnimator.ofFloat(_gesturesSelectV, "x", targetSelectorX)
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
        } else {
            _gesturesSelectV.x = targetSelectorX
            _collectionOfGesturesTv.setTextColor(collectionTargetColor)
            _rotationGroupTv.setTextColor(rotationTargetColor)
        }

        showCollectionGestures(showCollection, _rotationGroupCl, _collectionGesturesCl)
        if (_activeGestureNameCl.visibility != View.VISIBLE) {
            _activeGestureNameCl.visibility = View.VISIBLE
        }
        lastRenderedFilter = activeFilter
    }

    private fun showCollectionGestures(
        show: Boolean,
        rotationGroupCl: ConstraintLayout,
        collectionGesturesCl: ConstraintLayout
    ) {
        if (show) {
            if (collectionGesturesCl.visibility != View.VISIBLE) {
                collectionGesturesCl.visibility = View.VISIBLE
            }
            if (rotationGroupCl.visibility != View.GONE) {
                rotationGroupCl.visibility = View.GONE
            }
        } else {
            if (rotationGroupCl.visibility != View.VISIBLE) {
                rotationGroupCl.visibility = View.VISIBLE
            }
            if (collectionGesturesCl.visibility != View.GONE) {
                collectionGesturesCl.visibility = View.GONE
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
