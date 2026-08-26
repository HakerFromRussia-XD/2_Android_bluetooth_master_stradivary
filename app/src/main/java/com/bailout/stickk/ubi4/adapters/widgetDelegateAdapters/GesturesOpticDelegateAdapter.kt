package com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope // >>> changed <<<
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesOptic1Binding
import com.bailout.stickk.ubi4.adapters.dialog.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.data.state.UiState.activeGestureFragmentFilterFlow
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.bindingGroupFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.rotationGroupFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.rotationGroupGestures
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.GesturesItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.CollectionGesturePreviewController
import com.bailout.stickk.ubi4.utility.BorderAnimator
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider.Companion.getCollectionGestures
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider.Companion.getGesture
import com.bailout.stickk.ubi4.utility.ParameterInfoProvider.Companion.getParameterIDByCode
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListenerAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.stream.Collectors

@Suppress("DEPRECATION")
class GesturesOpticDelegateAdapter(
    private val coroutineScope: CoroutineScope?,
    val gestureNameList: ArrayList<String>,
    val onDeleteClick: (resultCb: ((result: Int) -> Unit), gestureName: String) -> Unit,
    val onAddGesturesToRotationGroup: (onSaveDialogClick: ((selectedGestures: ArrayList<Gesture>) -> Unit)) -> Unit,
    val onAddGesturesToSprScreen: (onSaveClickDialog: (MutableList<Pair<Int, Int>>) -> Unit, List<Pair<Int, Int>>) -> Unit,
    val onShowGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onRequestGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onSetCustomGesture: (
        onSaveDotsClick: (Pair<Int, Int>) -> Unit,
        bindingItem: Pair<Int, Int>
    ) -> Unit,
    val onSendBLEActiveGesture: (deviceAddress: Int, parameterID: Int, activeGesture: Int) -> Unit,
    val onRequestActiveGesture: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onSendBLERotationGroup: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onSendBLEBindingGroup: (deviceAddress: Int, parameterID: Int, bindingGestureGroup: BindingGestureGroup) -> Unit,
    val onRequestBindingGroup: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onRequestRotationGroup: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit
) : ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesOptic1Binding>(
    Ubi4WidgetGesturesOptic1Binding::inflate
), RotationGroupItemAdapter.OnDeleteClickRotationGroupListener,
    RotationGroupItemAdapter.OnCopyClickRotationGroupListener,
    RotationGroupItemAdapter.OnSelectClickRotationGroupListener
{

    private val ANIMATION_DURATION = 200
    private var listBindingGesture: MutableList<Pair<Int, Int>> = mutableListOf()
    private var deviceAddress = 0
    private var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf()
    private var itemsGesturesRotationArray: ArrayList<Pair<Long, String>>? = null
    private var listRotationGroupAdapter: RotationGroupItemAdapter? = null
    private var mRotationGroupDragLv: DragListView? = null
    private var hideFactoryCollectionGestures = true

    private var gestureCollectionBtns: ArrayList<Pair<View, Int>> = ArrayList()
    private var gestureCustomBtns: ArrayList<Pair<View, Int>> = ArrayList()

    private lateinit var sprGestureItemsProvider: SprGestureItemsProvider
    private lateinit var _annotationTv: TextView
    private lateinit var _annotationIv: ImageView
    private lateinit var _bindingGroupTv: TextView
    private lateinit var _rotationGroupTv: TextView
    private lateinit var _collectionOfGesturesTv: TextView
    private lateinit var _gesturesSelectV: View
    private lateinit var _ubi4GesturesSelectorV: View
    private lateinit var _collectionGesturesCl: ConstraintLayout
    private lateinit var _rotationGroupCl: ConstraintLayout
    private lateinit var _sprGestureGroupCl: ConstraintLayout
    private lateinit var _activeGestureNameCl: ConstraintLayout
    private lateinit var _activeGestureNameTv: TextView

    private lateinit var mRotationGroupExplanationTv: TextView
    private lateinit var mRotationGroupExplanation2Tv: TextView
    private lateinit var mRotationGroupExplanationIv: ImageView
    private lateinit var mRotationGroupExplanation2Iv: ImageView

    private lateinit var mAddGestureToRotationGroupBtn: View
    private lateinit var mPlusIv: ImageView

    private var currentBindingGroup: BindingGestureGroup = BindingGestureGroup()

    private var isBindingGroupResponseReceived = false
    private var isRotationGroupResponseReceived = false

    private lateinit var scope: CoroutineScope // >>> changed <<<

    private var borderAnimator: BorderAnimator? = null

    private var currentActiveGestureId: Int? = null
    private var lastRenderedFilter: Int? = null
    private val collectionPreviewController = CollectionGesturePreviewController()

    private var collectJob: Job? = null
    private var selectModeJob: Job? = null // >>> changed <<<

    @SuppressLint("LogNotTimber")
    private val adapter = SelectedGesturesAdapter(
        selectedGesturesList = mutableListOf(),
        onCheckGestureSprListener = object : SelectedGesturesAdapter.OnCheckSprGestureListener {
            override fun onGestureSprClicked(position: Int, title: String, gestureId: Int) {
                Log.d(
                    "GesturesOpticDelegateAdapter",
                    "SPR item clicked: title=$title, position=$position, gestureId=$gestureId"
                )

                // Жест 0 — пустой слот, ничего не делаем (на всякий случай)
                if (gestureId == 0) return

                // Подсветить соответствующую кнопку в Коллекции / кастомах
                setActiveGesture(getGestureViewById(gestureId))

                // Отправить команду активного жеста в протез
                onSendBLEActiveGesture(gestureId)
            }

        },
        onDotsClickListener = { selectedPosition ->
            Log.d(
                "GesturesOpticDelegateAdapter",
                "onDotsClickListener called with selectedPosition=$selectedPosition"
            )
            onSetCustomGesture({ bindingItem ->
                // позиция изменяемой ячейки
                val position = listBindingGesture.indexOfFirst { it.first == bindingItem.first }
                if (position != -1) {
                    listBindingGesture[position] = bindingItem
                }
                fillCollectionGesturesInBindingGroup()
                onSendBLEBindingGroup(
                    deviceAddress,
                    getParameterIDByCode(
                        ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
                        parameterInfoSet
                    ),
                    currentBindingGroup
                )
            }, listBindingGesture[selectedPosition])
        }
    )

    @SuppressLint("ClickableViewAccessibility", "LogNotTimber", "SuspiciousIndentation")
    override fun Ubi4WidgetGesturesOptic1Binding.onBind(item: GesturesItem) {
        mRotationGroupDragLv = rotationGroupDragLv
        onDestroyParent { onDestroy() }
        collectionPreviewController.release()

        // >>> changed <<< Инициализируем scope для RetryUtils / корутин
        scope = coroutineScope ?: main.lifecycleScope

        // Инициируем View-поля для удобства
        _annotationTv = annotationTv
        _annotationIv = annotationIv
        _bindingGroupTv = bindingGroupTv
        _rotationGroupTv = rotationGroupTv
        _collectionOfGesturesTv = collectionOfGesturesTv
        _gesturesSelectV = gesturesSelectV
        _ubi4GesturesSelectorV = ubi4GesturesSelectorV
        _collectionGesturesCl = collectionGesturesCl
        _rotationGroupCl = rotationGroupCl
        _sprGestureGroupCl = sprGestureGroupCl
        _activeGestureNameCl = activeGestureNameCl
        _activeGestureNameTv = activeGestureNameTv

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

        borderAnimator = BorderAnimator(view = _activeGestureNameCl)

        // Подписка на BLE-события
        borderAnimator?.checkStateSelectGestureMode()

        // Определяем deviceAddress/parameterInfoSet в зависимости от widget
        when (val widget = item.widget) {
            is BaseParameterWidgetEStruct -> {
                deviceAddress =
                    widget.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).deviceAddress
                widget.baseParameterWidgetStruct.parameterInfoSet.forEachIndexed { index, it ->
                    Log.d(
                        "ParamInfo deviceAddress",
                        "${widget.baseParameterWidgetStruct.parameterInfoSet.size} - $index deviceAddress2 : $it"
                    )
                }
                parameterInfoSet = widget.baseParameterWidgetStruct.parameterInfoSet
            }

            is BaseParameterWidgetSStruct -> {
                deviceAddress =
                    widget.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).deviceAddress
                parameterInfoSet = widget.baseParameterWidgetStruct.parameterInfoSet
            }
        }

        val savedFilter = main.getInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1).coerceIn(1, 3)
        lastRenderedFilter = null
        renderFilterUI(savedFilter, animate = false)
        activeGestureFragmentFilterFlow.value = savedFilter
        if (savedFilter == 2) {
            requestRotationGroupWithRetry(
                deviceAddress,
                getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number,
                    parameterInfoSet
                )
            )
        }
        if (savedFilter == 3) {
            requestBindingGroupWithRetry(
                deviceAddress,
                getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
                    parameterInfoSet
                )
            )
        }

        rotationGroupSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 2)
            activeGestureFragmentFilterFlow.value = 2
            onRequestRotationGroup(
                deviceAddress,
                getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number,
                    parameterInfoSet
                )
            )
        }
        collectionOfGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1)
            activeGestureFragmentFilterFlow.value = 1
        }
        sprGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 3)
            activeGestureFragmentFilterFlow.value = 3
            onRequestBindingGroup(
                deviceAddress, getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
                    parameterInfoSet
                )
            )
        }
        hideCollectionBtn.setOnClickListener {
            if (hideFactoryCollectionGestures) {
                hideFactoryCollectionGestures = false
                hideCollectionBtn.animate().rotation(180F).duration =
                    ANIMATION_DURATION.toLong()
                collectionUserGesturesCl.animate()
                    .translationY(-(collectionFactoryGesturesCl.height).toFloat())
                    .setDuration(ANIMATION_DURATION.toLong())
                collectionFactoryGesturesCl.animate()
                    .alpha(0.0f)
                    .setDuration(ANIMATION_DURATION.toLong())
                Handler().postDelayed({
                    collectionFactoryGesturesCl.visibility = View.GONE
                    collectionUserGesturesCl.animate().translationY(0F).duration = 0
                }, ANIMATION_DURATION.toLong())
            } else {
                hideFactoryCollectionGestures = true
                hideCollectionBtn.animate().rotation(0F).duration =
                    ANIMATION_DURATION.toLong()
                collectionUserGesturesCl.animate()
                    .translationY(-(collectionFactoryGesturesCl.height).toFloat())
                    .setDuration(0)
                collectionFactoryGesturesCl.visibility = View.VISIBLE
                Handler().postDelayed({
                    collectionUserGesturesCl.animate().translationY(0F)
                        .setDuration(ANIMATION_DURATION.toLong())
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
        for (i in 0..13) {
            val gestureCollectionBtn =
                this::class.java.getDeclaredField("gestureCollection${i}Btn").get(this) as? View
            val gestureCollectionTitle =
                this::class.java.getDeclaredField("gestureCollection${i}Tv").get(this) as? TextView
            val gestureCollectionImage =
                this::class.java.getDeclaredField("gestureCollection${i}Iv").get(this) as? ImageView

            if (i <= 10) {
                gestureCollectionBtn?.let { gestureCollectionBtns.add(Pair(it, i + 1)) }
                gestureCollectionTitle?.text = getCollectionGestures()[i].gestureName
                gestureCollectionImage?.setImageResource(getCollectionGestures()[i].gestureImage)

                gestureCollectionBtn?.setOnClickListener {
                    setActiveGesture(gestureCollectionBtn)
                    onSendBLEActiveGesture(i + 1)
                }
            } else {
                gestureCollectionBtn?.let { gestureCollectionBtns.add(Pair(it, i + 2)) }
                gestureCollectionTitle?.text =
                    getCollectionGestures()[i + 1].gestureName
                gestureCollectionImage?.setImageResource(
                    getCollectionGestures()[i + 1].gestureImage
                )

                gestureCollectionBtn?.setOnClickListener {
                    setActiveGesture(gestureCollectionBtn)
                    onSendBLEActiveGesture(i + 2)
                }
            }
        }
        collectionPreviewController.bind(root, { true }) { card -> card.performClick() }
        for (i in 1..8) {
            val gestureCustomTv = this::class.java.getDeclaredField("gesture${i}NameTv")
                .get(this) as? TextView
            val gestureCustomBtn = this::class.java.getDeclaredField("gestureCustom${i}Btn")
                .get(this) as? View
            val gestureSettingsContainer =
                this::class.java.getDeclaredField("gesture${i}SettingsContainer")
                    .get(this) as? View

            gestureCustomBtn?.let { gestureCustomBtns.add(Pair(it, 63 + i)) }
            gestureCustomTv?.text = gestureNameList[i - 1]
            gestureCustomBtn?.setOnClickListener {
                onSendBLEActiveGesture(63 + i)
                setActiveGesture(gestureCustomBtn)
                activeGestureNameTv.text =
                    "Active gesture is: ${gestureCustomTv?.text ?: "Unknown"}"
            }
            gestureSettingsContainer?.setOnClickListener {
                onShowGestureSettings(
                    deviceAddress,
                    getParameterIDByCode(
                        ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number,
                        parameterInfoSet
                    ),
                    63 + i
                )

                main.saveInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, i)
            }
        }

        addGestureToRotationGroupBtn.setOnClickListener {
            val resultCb: ((selectedGestures: ArrayList<Gesture>) -> Unit) = { selectedGestures ->
                val notContainsList = selectedGestures.stream()
                    .filter { element -> !rotationGroupGestures.contains(element) }
                    .collect(Collectors.toList())
                notContainsList.forEach { rotationGroupGestures.add(it) }

                val finalList = rotationGroupGestures.stream()
                    .filter { element -> selectedGestures.contains(element) }
                    .collect(Collectors.toList())
                rotationGroupGestures = ArrayList(finalList)

                showIntroduction()
                setupListRecyclerView()
                synchronizeRotationGroup()
                sendBLERotationGroup()
                calculatingShowAddButton()
            }
            onAddGesturesToRotationGroup(resultCb)
        }
        rotationGroupDragLv.recyclerView.isVerticalScrollBarEnabled = false
        rotationGroupDragLv.setScrollingEnabled(false)
        rotationGroupDragLv.setOnClickListener {}
        rotationGroupDragLv.setDragListListener(object : DragListListenerAdapter() {
            override fun onItemDragStarted(position: Int) { }

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (fromPosition != toPosition) {
                    synchronizeRotationGroup()
                    sendBLERotationGroup()
                }
            }
        })

        chooseLearningGesturesBtn1.setOnClickListener {
            val selectedGestures: (MutableList<Pair<Int, Int>>) -> Unit =
                { listBindingGestures ->
                    listBindingGesture = listBindingGestures
                    fillCollectionGesturesInBindingGroup()
                    onSendBLEBindingGroup(
                        deviceAddress,
                        getParameterIDByCode(
                            ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
                            parameterInfoSet
                        ),
                        currentBindingGroup
                    )

                }
            onAddGesturesToSprScreen(selectedGestures, listBindingGesture)
        }

        val gridLayoutManager = GridLayoutManager(root.context, 2)
        gridLayoutManager.orientation = LinearLayoutManager.VERTICAL
        selectedSprGesturesRv.layoutManager = gridLayoutManager
        sprGestureItemsProvider = SprGestureItemsProvider(root.context)
        selectedSprGesturesRv.adapter = adapter
        onRequestActiveGesture(
            deviceAddress,
            getParameterIDByCode(
                ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number,
                parameterInfoSet
            )
        )

        mRotationGroupExplanationTv = rotationGroupExplanationTv
        mRotationGroupExplanation2Tv = rotationGroupExplanation2Tv
        mRotationGroupExplanationIv = rotationGroupExplanationIv
        mRotationGroupExplanation2Iv = rotationGroupExplanation2Iv
        mAddGestureToRotationGroupBtn = addGestureToRotationGroupBtn
        mPlusIv = plusIv
        showIntroduction()
        setupListRecyclerView()

        collectActiveFlows()
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

        val density = main.resources.displayMetrics.density
        val extraOffsetPx = 3f * density
        val containerWidth = (_ubi4GesturesSelectorV.width.takeIf { it > 0 }
            ?: _ubi4GesturesSelectorV.measuredWidth).toFloat()
        if (normalizedFilter != 1 && containerWidth == 0f) {
            _collectionOfGesturesTv.setTextColor(collectionTargetColor)
            _rotationGroupTv.setTextColor(rotationTargetColor)
            _bindingGroupTv.setTextColor(bindingTargetColor)
            showFilterContainers(normalizedFilter)
            if (_activeGestureNameCl.visibility != View.VISIBLE) {
                _activeGestureNameCl.visibility = View.VISIBLE
            }
            _ubi4GesturesSelectorV.post {
                renderFilterUI(normalizedFilter, animate = false, force = true)
            }
            return
        }

        val buttonWidth = containerWidth / 3f
        val indicatorTranslationX = (normalizedFilter - 1) * buttonWidth + when (normalizedFilter) {
            2 -> extraOffsetPx - density
            3 -> extraOffsetPx * 2
            else -> 0f
        }

        if (animate && lastRenderedFilter != null) {
            ObjectAnimator.ofFloat(_gesturesSelectV, "translationX", indicatorTranslationX)
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
            _gesturesSelectV.translationX = indicatorTranslationX
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

    override fun Ubi4WidgetGesturesOptic1Binding.onAttachedToWindow() {
        Log.d("GesturesAdapter", "onAttachedToWindow run")
        loadSavedGestureNames(root.context)
        updateGestureButtonsUI(this)
    }

    private fun showIntroduction() {
        if (!::mRotationGroupExplanationTv.isInitialized ||
            !::mRotationGroupExplanation2Tv.isInitialized ||
            !::mRotationGroupExplanationIv.isInitialized ||
            !::mRotationGroupExplanation2Iv.isInitialized
        ) {
            Log.w(
                "GesturesOpticDelegateAdapter",
                "showIntroduction() called before views initialized, skip"
            )
            return
        }

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

    private fun fillCollectionGesturesInBindingGroup(): BindingGestureGroup {
        currentBindingGroup = BindingGestureGroup()
        listBindingGesture.forEachIndexed { index, pair ->
            currentBindingGroup.setGestureAt(index, pair)
        }
        main.runOnUiThread {
            adapter.updateGestures(listBindingGesture)
            if (adapter.itemCount > 0) {
                _annotationTv.visibility = View.GONE
                _annotationIv.visibility = View.GONE
            } else {
                _annotationTv.visibility = View.VISIBLE
                _annotationIv.visibility = View.VISIBLE
            }
        }
        return currentBindingGroup
    }

    private fun setActiveGesture(activeGesture: View?) {
        gestureCollectionBtns.forEach { btn ->
            btn.first.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_outside)
        }
        gestureCustomBtns.forEach { btn ->
            btn.first.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_outside)
        }
        activeGesture?.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_active)
    }

    private fun getGestureViewById(gestureId: Int?): View? {
        gestureCollectionBtns.forEach { gestureCollection ->
            if (gestureCollection.second == gestureId) return gestureCollection.first
        }
        gestureCustomBtns.forEach { gestureCustom ->
            if (gestureCustom.second == gestureId) return gestureCustom.first
        }
        return null
    }


    private fun collectActiveFlows() {
        collectJob?.cancel()

        collectJob = scope.launch {
            try {
                merge(
                    // 1) Активный жест (подсветка и текст)
                    WidgetState.activeGestureState.map { activeGestureId ->
                        Log.d("ACTIVE_TRACE", "activeGestureState → $activeGestureId")

                        // Remember the last active gesture id
                        currentActiveGestureId = activeGestureId

                        withContext(Dispatchers.Main) {
                            // Подсветка кнопки в коллекции / кастомах
                            setActiveGesture(getGestureViewById(activeGestureId))

                            // Имя жеста для заголовка
                            val gestureName = when {
                                activeGestureId == null -> "Unknown"
                                activeGestureId < 63 -> getCollectionGestures()
                                    .getOrNull(activeGestureId - 1)
                                    ?.gestureName ?: "Unknown"
                                else -> gestureNameList.getOrNull(activeGestureId - 64)
                                    ?: "Unknown"
                            }

                            _activeGestureNameTv.text =
                                main.getString(R.string.active_gesture_is, gestureName)

                            // Подсветить соответствующий SPR-айтем в биндинге
                            adapter.setActiveGesture(activeGestureId)

                            listRotationGroupAdapter?.setActiveGestureId(activeGestureId ?: -1)
                        }
                    },

                    // 2) BindingGroup (SPR / оптический биндинг)
                    bindingGroupFlow.map { bindingGroupParameterRef ->
                        Log.d(
                            "ACTIVE_TRACE",
                            "bindingGroupFlow → addr=${bindingGroupParameterRef.addressDevice}, paramId=${bindingGroupParameterRef.parameterID}"
                        )

                        val parameter = ParameterProvider.getParameter(
                            bindingGroupParameterRef.addressDevice,
                            bindingGroupParameterRef.parameterID
                        )

                        val raw = "\"${parameter.data}\""
                        val bindingGroup = runCatching {
                            Json.decodeFromString<BindingGestureGroup>(raw)
                        }.getOrElse {
                            BindingGestureGroup()
                        }

                        listBindingGesture.clear()
                        bindingGroup.toGestureList().forEach { pair ->
                            if (pair.first != 0) {
                                listBindingGesture.add(pair)
                            }
                        }
                        isBindingGroupResponseReceived = true

                        withContext(Dispatchers.Main) {
                            fillCollectionGesturesInBindingGroup()
                        }
                    },

                    // 3) RotationGroup (группа ротации)
                    rotationGroupFlow.map { rotationGroupParameterRef ->
                        val parameter = ParameterProvider.getParameter(
                            rotationGroupParameterRef.addressDevice,
                            rotationGroupParameterRef.parameterID
                        )

                        val raw = "\"${parameter.data}\""
                        val rotationGroup = runCatching {
                            Json.decodeFromString<RotationGroup>(raw)
                        }.getOrElse {
                            RotationGroup()
                        }

                        val rotationGroupList = rotationGroup.toGestureList()
                        val receivedGestures = rotationGroupList
                            .filter { item -> item.first != 0 }
                            .map { item -> getGesture(item.first) }

                        isRotationGroupResponseReceived = true

                        if (!hasSameRotationGroup(receivedGestures)) {
                            rotationGroupGestures.clear()
                            rotationGroupGestures.addAll(receivedGestures)

                            withContext(Dispatchers.Main) {
                                showIntroduction()
                                setupListRecyclerView()
                                synchronizeRotationGroup()
                                calculatingShowAddButton()
                            }
                        }
                    },

                    // 4) Переключение фильтра (коллекция / rotation / SPR)
                    activeGestureFragmentFilterFlow.map { newFilter ->
                        withContext(Dispatchers.Main) {
                            renderFilterUI(newFilter, animate = true)
                        }
                    }
                ).collect()
            } catch (e: CancellationException) {
                // игнор, job отменён
            } catch (e: Exception) {
                main.runOnUiThread {
                    main.showToast(main.getString(SharedRes.strings.collect_active_flows_error.resourceId, e.message ?: ""))
                }
            }
        }

        // Отдельный лисенер для режима выбора жеста (обводка BorderAnimator)
        selectModeJob?.cancel()
        selectModeJob = scope.launch(Dispatchers.Main.immediate) {
            WidgetState.selectGestureModeState.collect { isSelectMode ->
                borderAnimator?.toggle(isSelectMode)
            }
        }
    }
    private fun calculatingShowAddButton() {
        if (!::mAddGestureToRotationGroupBtn.isInitialized || !::mPlusIv.isInitialized) {
            Log.w(
                "GesturesOpticDelegateAdapter",
                "calculatingShowAddButton() called before views initialized, skip"
            )
            return
        }
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

    private fun requestBindingGroupWithRetry(deviceAddress: Int, parameterId: Int) {
        isBindingGroupResponseReceived = false
        RetryUtils.sendRequestWithRetry(
            request = {
                onRequestBindingGroup(deviceAddress, parameterId)
                Log.d("GesturesOpticDelegateAdapter", "Отправил onRequestBindingGroup")
            },
            isResponseReceived = {
                isBindingGroupResponseReceived
            },
            maxRetries = 5,
            delayMillis = 400L,
            scope = scope
        )
    }

    private fun requestRotationGroupWithRetry(deviceAddress: Int, parameterID: Int) {
        isRotationGroupResponseReceived = false
        RetryUtils.sendRequestWithRetry(
            request = {
                onRequestRotationGroup(deviceAddress, parameterID)
                Log.d("GesturesOpticDelegateAdapter", "Отправил onRequestRotationGroup")
            },
            isResponseReceived = { isRotationGroupResponseReceived },
            maxRetries = 5,
            delayMillis = 400,
            scope = scope
        )
    }

    private fun sendBLERotationGroup() {
        onSendBLERotationGroup(
            deviceAddress,
            getParameterIDByCode(
                ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number,
                parameterInfoSet
            )
        )
    }

    private fun onSendBLEActiveGesture(activeGesture: Int) {
        Log.d(
            "ActiveGestureTest",
            "i=${activeGesture - 0x3F}  ->  byte=0x%02X".format(activeGesture)
        )
        onSendBLEActiveGesture(
            deviceAddress,
            getParameterIDByCode(
                ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number,
                parameterInfoSet
            ),
            activeGesture
        )
        Log.d(
            "onSendBLEActiveGesture",
            "Sending active gesture command: deviceAddress=$deviceAddress, activeGesture=$activeGesture"
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
            Pair(index.toLong(), gesture.gestureName + "™" + gesture.gestureId.toString())
        })
        if (itemsGesturesRotationArray == newItems && listRotationGroupAdapter != null) {
            currentActiveGestureId?.let { id ->
                listRotationGroupAdapter?.setActiveGestureId(id)
            }
            return
        }

        mRotationGroupDragLv?.setLayoutManager(LinearLayoutManager(main.applicationContext))
        itemsGesturesRotationArray = newItems
        listRotationGroupAdapter =
            RotationGroupItemAdapter(
                itemsGesturesRotationArray,
                R.layout.ubi4_item_rotation_group,
                R.id.swapIv,
                false,
                this,
                this,
                this
            )
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        mRotationGroupDragLv?.setCanDragHorizontally(false)
        mRotationGroupDragLv?.setCanDragVertically(true)
        mRotationGroupDragLv?.setCustomDragItem(
            MyDragItem(
                main.applicationContext,
                R.layout.ubi4_item_rotation_group_drag
            )
        )
        // Restore last known active gesture highlight in new adapter if possible
        currentActiveGestureId?.let { id ->
            listRotationGroupAdapter?.setActiveGestureId(id)
        }
    }

    override fun isForViewType(item: Any): Boolean = item is GesturesItem

    override fun GesturesItem.getItemId(): Any = title

    private fun loadSavedGestureNames(context: Context) {
        val sp = context.getSharedPreferences(
            PreferenceKeysUbi4.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        val macKey =
            sp.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "default") ?: "default"
        gestureNameList.clear()
        for (i in 0 until 8) {
            val key = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + i
            val name = sp.getString(key, "Gesture ${i + 1}") ?: "Gesture ${i + 1}"
            gestureNameList.add(name)
        }
        Log.d("GesturesAdapter", "Final loaded gesture names: $gestureNameList")
    }

    private fun updateGestureButtonsUI(binding: Ubi4WidgetGesturesOptic1Binding) {
        for (i in 1..PreferenceKeysUbi4.NUM_GESTURES) {
            try {
                val field = binding::class.java.getDeclaredField("gesture${i}NameTv")
                field.isAccessible = true
                val gestureTv = field.get(binding) as? TextView
                gestureTv?.text = gestureNameList.getOrElse(i - 1) { "Gesture $i" }
            } catch (e: Exception) {
                Log.e(
                    "GesturesAdapter",
                    "Ошибка обновления имени жеста $i: ${e.localizedMessage}"
                )
            }
        }
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

    override fun onCopyClick(position: Int, gestureName: String?) {
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        listRotationGroupAdapter?.notifyDataSetChanged()
        synchronizeRotationGroup()
        sendBLERotationGroup()
        calculatingShowAddButton()
    }

    override fun onDeleteClickCb(position: Int) {
        val resultCb: ((result: Int) -> Unit) = {
            rotationGroupGestures.removeAt(position)
            showIntroduction()
            setupListRecyclerView()
            synchronizeRotationGroup()
            sendBLERotationGroup()
            calculatingShowAddButton()
        }
        onDeleteClick(resultCb, rotationGroupGestures[position].gestureName)
    }

    fun onDestroy() {
        collectJob?.cancel()
        selectModeJob?.cancel() // >>> changed <<<
        borderAnimator?.destroyCoroutines()
        collectionPreviewController.release()
    }

    override fun onRotationGestureClick(
        position: Int,
        gestureName: String?,
        gestureId: Int
    ) {
        if (gestureId == 0) return
        // Подсветить соответствующую кнопку в коллекции / кастомах
        setActiveGesture(getGestureViewById(gestureId))

        // Обновить текст активного жеста
        val safeName = gestureName?.ifBlank { "Unknown" }
        _activeGestureNameTv.text =
            main.getString(R.string.active_gesture_is, safeName)

        // Отправить команду активного жеста в протез
        onSendBLEActiveGesture(gestureId)

        // Immediately update highlight state for UI/adapter
        currentActiveGestureId = gestureId
        listRotationGroupAdapter?.setActiveGestureId(gestureId)
    }
}
