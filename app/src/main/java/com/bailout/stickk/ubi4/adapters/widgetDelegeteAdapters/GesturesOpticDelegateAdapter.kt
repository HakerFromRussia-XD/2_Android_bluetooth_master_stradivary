package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

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
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesOptic1Binding
import com.bailout.stickk.ubi4.adapters.dialog.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider.Companion.getCollectionGestures
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider.Companion.getGesture
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.data.local.SprGestureItemsProvider
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.GesturesItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.rotationGroupGestures
import com.bailout.stickk.ubi4.utility.BorderAnimator
import com.bailout.stickk.ubi4.utility.ParameterInfoProvider.Companion.getParameterIDByCode
import com.bailout.stickk.ubi4.utility.RetryUtils
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
    val onAddGesturesToSprScreen: (onSaveClickDialog: (MutableList<kotlin.Pair<Int, Int>>) -> Unit, List<kotlin.Pair<Int, Int>>) -> Unit,
    val onShowGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onRequestGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onSetCustomGesture: (
        onSaveDotsClick: (kotlin.Pair<Int, Int>) -> Unit,
        bindingItem: kotlin.Pair<Int, Int>
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
),RotationGroupItemAdapter.OnDeleteClickRotationGroupListener,RotationGroupItemAdapter.OnCopyClickRotationGroupListener {

    private val ANIMATION_DURATION = 200
    private var listBindingGesture: MutableList<Pair<Int, Int>> = mutableListOf()
    private var deviceAddress = 0
    private var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf()
    private var itemsGesturesRotationArray: ArrayList<Pair<Long, String>>? = null
    private var listRotationGroupAdapter: RotationGroupItemAdapter? = null
    private var mRotationGroupDragLv: DragListView? = null
    private var hideFactoryCollectionGestures = true

    private var gestureCollectionBtns: ArrayList<kotlin.Pair<View, Int>> = ArrayList()
    private var gestureCustomBtns: ArrayList<kotlin.Pair<View, Int>> = ArrayList()

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

    private var parameterIDSet = mutableSetOf<ParameterInfo<Int, Int, Int, Int>>()

    private lateinit var mRotationGroupExplanationTv: TextView
    private lateinit var mRotationGroupExplanation2Tv: TextView
    private lateinit var mRotationGroupExplanationIv: ImageView
    private lateinit var mRotationGroupExplanation2Iv: ImageView

    private lateinit var mAddGestureToRotationGroupBtn: View
    private lateinit var mPlusIv: ImageView

    private var currentBindingGroup: BindingGestureGroup = BindingGestureGroup()

    private var isBindingGroupResponseReceived = false
    private var isRotationGroupResponseReceived = false


    private var borderAnimator: BorderAnimator? = null


    private var collectJob: Job? = null

    @SuppressLint("LogNotTimber")
    val adapter = SelectedGesturesAdapter(
        selectedGesturesList = ArrayList(),
        onCheckGestureSprListener = object : SelectedGesturesAdapter.OnCheckSprGestureListener {
            override fun onGestureSprClicked(position: Int, title: String) {
                Log.d("GesturesDelegateAdapter", "Gesture clicked: $title at position: $position")
            }
        },
        onDotsClickListener = { selectedPosition ->
            Log.d("GesturesOpticDelegateAdapter", "onDotsClickListener called with selectedPosition=$selectedPosition")
            onSetCustomGesture({ bindingItem ->
                // позиция изменяемой ячейки
                val position = listBindingGesture.indexOfFirst { it.first == bindingItem.first }
                listBindingGesture[position] = bindingItem
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


        val savedHideState = main.getInt(PreferenceKeysUBI4.LAST_HIDE_COLLECTION_BTN_STATE, 1)
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
        collectActiveFlows()

        // Определяем deviceAddress/parameterInfoSet в зависимости от widget
        when (val widget = item.widget) {
            is BaseParameterWidgetEStruct -> {
                deviceAddress = widget.baseParameterWidgetStruct.deviceId
                parameterInfoSet = widget.baseParameterWidgetStruct.parameterInfoSet
            }
            is BaseParameterWidgetSStruct -> {
                deviceAddress = widget.baseParameterWidgetStruct.deviceId
                parameterInfoSet = widget.baseParameterWidgetStruct.parameterInfoSet
            }
        }


        val savedFilter = main.getInt(PreferenceKeysUBI4.LAST_ACTIVE_GESTURE_FILTER, 1)
        if (savedFilter == 2) {
            // Устанавливаем активный фильтр (если это необходимо для UI)
            MainActivityUBI4.activeGestureFragmentFilterFlow.value = 2
            requestRotationGroupWithRetry(
                deviceAddress,
                getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number,
                    parameterInfoSet
                )
            )
        }
        if (savedFilter == 3){
            MainActivityUBI4.activeGestureFragmentFilterFlow.value = 3
            requestBindingGroupWithRetry(
                deviceAddress,
                getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
                    parameterInfoSet
                )
            )
        }

        rotationGroupSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUBI4.LAST_ACTIVE_GESTURE_FILTER, 2)
            MainActivityUBI4.activeGestureFragmentFilterFlow.value = 2
            activeGestureNameCl.visibility = View.GONE
            onRequestRotationGroup(
                deviceAddress,
                getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number,
                    parameterInfoSet
                )
            )
        }
        collectionOfGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUBI4.LAST_ACTIVE_GESTURE_FILTER, 1)
            MainActivityUBI4.activeGestureFragmentFilterFlow.value = 1
            activeGestureNameCl.visibility = View.VISIBLE

        }
        sprGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUBI4.LAST_ACTIVE_GESTURE_FILTER, 3)
            MainActivityUBI4.activeGestureFragmentFilterFlow.value = 3
            activeGestureNameCl.visibility = View.GONE
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
                hideCollectionBtn.animate().rotation(180F).duration = ANIMATION_DURATION.toLong()
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
                hideCollectionBtn.animate().rotation(0F).duration = ANIMATION_DURATION.toLong()
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
            main.saveInt(PreferenceKeysUBI4.LAST_HIDE_COLLECTION_BTN_STATE, if (hideFactoryCollectionGestures) 1 else 0)

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
//                    activeGestureNameTv.text = "Active gesture is: ${gestureCollectionTitle?.text ?: "Unknown"}"
                }
            } else {
                gestureCollectionBtn?.let { gestureCollectionBtns.add(Pair(it, i + 2)) }
                gestureCollectionTitle?.text = getCollectionGestures()[i + 1].gestureName
                gestureCollectionImage?.setImageResource(getCollectionGestures()[i + 1].gestureImage)

                gestureCollectionBtn?.setOnClickListener {
                    setActiveGesture(gestureCollectionBtn)
                    onSendBLEActiveGesture(i + 2)
//                    activeGestureNameTv.text = "Active gesture is: ${gestureCollectionTitle?.text ?: "Unknown"}"
                }
            }
        }

        for (i in 1..8) {
            val gestureCustomTv = this::class.java.getDeclaredField("gesture${i}NameTv")
                .get(this) as? TextView
            val gestureCustomBtn = this::class.java.getDeclaredField("gestureCustom${i}Btn")
                .get(this) as? View
            val gestureSettingsContainer = this::class.java.getDeclaredField("gesture${i}SettingsContainer")
                .get(this) as? View


            gestureCustomBtn?.let { gestureCustomBtns.add(Pair(it, 63 + i)) }
            gestureCustomTv?.text = gestureNameList[i - 1]
            gestureCustomBtn?.setOnClickListener {
                onSendBLEActiveGesture(63 + i)
                setActiveGesture(gestureCustomBtn)
                activeGestureNameTv.text = "Active gesture is: ${gestureCustomTv?.text ?: "Unknown"}"
            }
            gestureSettingsContainer?.setOnClickListener {
                onShowGestureSettings(
                    deviceAddress,
                    getParameterIDByCode(
                        ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number,
                        parameterInfoSet
                    ),
                    (0x40).toInt() + i
                )
                main.saveInt(PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM, i)
            }
        }

        addGestureToRotationGroupBtn.setOnClickListener {
            val resultCb: ((selectedGestures: ArrayList<Gesture>)->Unit) = { selectedGestures ->
                // проверка что элемент из selectedGestures содержится в rotationGroupGestures
                // если да, то не меняем его положение и добавляем новых в конец списка
                val notContainsList = selectedGestures.stream().filter{element -> !rotationGroupGestures.contains(element)}.collect(
                    Collectors.toList())//rotationGroupGestures.add())
                notContainsList.forEach { rotationGroupGestures.add(it) }
                // удаляем те элементы, которые были отчекнуты
                val finalList = rotationGroupGestures.stream().filter{ element -> selectedGestures.contains(element)}.collect(
                    Collectors.toList())
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
            val selectedGestures: (MutableList<kotlin.Pair<Int, Int>>) -> Unit = { listBindingGestures ->
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
        selectedSprGesturesRv.adapter = adapter
        sprGestureItemsProvider = SprGestureItemsProvider(root.context)
        onRequestActiveGesture(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number, parameterInfoSet))

        mRotationGroupExplanationTv = rotationGroupExplanationTv
        mRotationGroupExplanation2Tv = rotationGroupExplanation2Tv
        mRotationGroupExplanationIv = rotationGroupExplanationIv
        mRotationGroupExplanation2Iv = rotationGroupExplanation2Iv
        mAddGestureToRotationGroupBtn = addGestureToRotationGroupBtn
        mPlusIv = plusIv
        showIntroduction()
        setupListRecyclerView()

    }

    private fun renderFilterUI(activeFilter: Int) {
    // Параметры для индикатора
    val density = main.resources.displayMetrics.density
    val marginPx = 18 * density
    val extraOffsetPx = 3 * density
    val containerWidth = _ubi4GesturesSelectorV.width.toFloat()
    val buttonWidth = containerWidth / 3f

    // Расчёт финальной координаты X для анимации индикатора
    val indicatorX = (activeFilter - 1) * buttonWidth + marginPx + when (activeFilter) {
        2 -> extraOffsetPx - (1 * density)
        3 -> extraOffsetPx * 2
        else -> 0f
    }
    // Анимируем движение индикатора
    ObjectAnimator.ofFloat(_gesturesSelectV, "x", indicatorX)
        .setDuration(ANIMATION_DURATION.toLong())
        .start()
    // Соберём все TextView, которые мы хотим «обнулять» цветом при любом переключении
    val allTextViews = listOf(
        _collectionOfGesturesTv,
        _rotationGroupTv,
        _bindingGroupTv
    )
    // Карта: какому фильтру соответствуют какие TextView (которые будут активироваться)
    val filterToTextViews = mapOf(
        1 to listOf(_collectionOfGesturesTv),
        2 to listOf(_rotationGroupTv),
        3 to listOf(_bindingGroupTv)
    )
    // Сброс: переводим все текстовые элементы в неактивный цвет
    allTextViews.forEach { textView ->
        ObjectAnimator.ofInt(
            textView,
            "textColor",
            textView.currentTextColor,
            main.getColor(R.color.ubi4_deactivate_text)
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = ANIMATION_DURATION.toLong()
            start()
        }
    }
    // Активируем цвет только тем текстовым элементам, что соответствуют выбранному фильтру
    filterToTextViews[activeFilter]?.forEach { textView ->
        ObjectAnimator.ofInt(
            textView,
            "textColor",
            main.getColor(R.color.ubi4_deactivate_text),
            main.getColor(R.color.white)
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = ANIMATION_DURATION.toLong()
            start()
        }
    }

    // Настраиваем видимость групп в зависимости от выбранного фильтра
    when (activeFilter) {
        1 -> {
            // Фильтр "Коллекция жестов"
            _activeGestureNameCl.visibility = View.VISIBLE
            _collectionGesturesCl.visibility = View.VISIBLE
            _rotationGroupCl.visibility = View.GONE
            _sprGestureGroupCl.visibility = View.GONE
        }
        2 -> {
            // Фильтр "Rotation Group"

            _activeGestureNameCl.visibility = View.GONE
            _rotationGroupCl.visibility = View.VISIBLE
            _collectionGesturesCl.visibility = View.GONE
            _sprGestureGroupCl.visibility = View.GONE
        }
        3 -> {
            // Фильтр "SPR (Binding Group)"
            _activeGestureNameCl.visibility = View.GONE
            _sprGestureGroupCl.visibility = View.VISIBLE
            _collectionGesturesCl.visibility = View.GONE
            _rotationGroupCl.visibility = View.GONE

        }
    }
}
    override fun Ubi4WidgetGesturesOptic1Binding.onAttachedToWindow() {
        Log.d("GesturesAdapter", "onAttachedToWindow run")
        loadSavedGestureNames(root.context)
        updateGestureButtonsUI(this)
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
        Log.d("BorderAnimator", "collectActiveFlows() started")
        collectJob?.cancel()
        collectJob = coroutineScope?.launch {
            try {
                merge(
                    MainActivityUBI4.activeGestureFlow.map { activeGestureParameterRef ->
                        val parameter = ParameterProvider.getParameter(
                            deviceAddress,
                            activeGestureParameterRef.parameterID
                        )
                        val activeGestureIdHex = parameter.data.takeLast(2)
                        val activeGestureId = activeGestureIdHex.toIntOrNull(16)
                        withContext(Dispatchers.Main) {
                            // Обновляем визуальный индикатор активного жеста
                            setActiveGesture(getGestureViewById(activeGestureId))

                            // Определяем имя жеста по его идентификатору
                            val gestureName = activeGestureId?.let { id ->
                                if (id < 63) {
                                    // Для коллекционных жестов: индекс = id - 1
                                    getCollectionGestures().getOrNull(id - 1)?.gestureName ?: "Unknown"
                                } else {
                                    gestureNameList.getOrNull(id - 64) ?: "Unknown"
                                }
                            } ?: "Unknown"

                            _activeGestureNameTv.text = main.getString(R.string.active_gesture_is, gestureName) ?: "Unknown"
                        }
                    },
                    MainActivityUBI4.bindingGroupFlow.map { bindingGroupParameterRef ->
                        val parameter = ParameterProvider.getParameter(
                            bindingGroupParameterRef.addressDevice,
                            bindingGroupParameterRef.parameterID
                        )
                        val bindingGroup = Json.decodeFromString<BindingGestureGroup>("\"${parameter.data}\"")
                        listBindingGesture.clear()
                        bindingGroup.toGestureList().forEach {
                            if (it.first != 0) {
                                listBindingGesture.add(it)
                            }
                        }
                        isBindingGroupResponseReceived = true
                        withContext(Dispatchers.Main) {
                            fillCollectionGesturesInBindingGroup()
                        }
                    },
                    MainActivityUBI4.rotationGroupFlow.map { rotationGroupParameterRef ->
                        val parameter = ParameterProvider.getParameter(
                            rotationGroupParameterRef.addressDevice,
                            rotationGroupParameterRef.parameterID
                        )
                        val rotationGroup = Json.decodeFromString<RotationGroup>("\"${parameter.data}\"")
                        val rotationGroupList = rotationGroup.toGestureList()
                        rotationGroupGestures.clear()
                        rotationGroupList.forEach { item ->
                            if (item.first != 0) {
                                rotationGroupGestures.add(getGesture(item.first))
                            }
                        }
                        isRotationGroupResponseReceived = true
                        withContext(Dispatchers.Main) {
                            showIntroduction()
                            setupListRecyclerView()
                            synchronizeRotationGroup()
                            calculatingShowAddButton()
                        }
                    },
                    MainActivityUBI4.activeGestureFragmentFilterFlow.map { newFilter ->
                        withContext(Dispatchers.Main) {
                            // При любом изменении фильтра - рендерим UI
                            renderFilterUI(newFilter)
                        }
                    }
                ).collect()
            } catch (e: CancellationException) {
                Log.d("collectActiveFlows", "Job was cancelled: ${e.message}")
            } catch (e: Exception) {
                main.runOnUiThread {
                    main.showToast("ERROR collectActiveFlows")
                    Log.e("collectActiveFlows", "ERROR collectActiveFlows: $e")
                }
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
            delayMillis = 400L
        )
    }
    private fun requestRotationGroupWithRetry(deviceAddress: Int, parameterID: Int){
        isRotationGroupResponseReceived = false
        RetryUtils.sendRequestWithRetry(
            request = {
                onRequestRotationGroup(deviceAddress, parameterID)
                Log.d("GesturesOpticDelegateAdapter", "Отправил onRequestRotationGroup")

            },
            isResponseReceived = { isRotationGroupResponseReceived },
            maxRetries = 5,
            delayMillis = 400L
        )
    }

    private fun sendBLERotationGroup() {
        onSendBLERotationGroup(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number, parameterIDSet))
    }

    private fun onSendBLEActiveGesture(activeGesture: Int) {
        onSendBLEActiveGesture(deviceAddress,getParameterIDByCode(ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number, parameterInfoSet), activeGesture)
        Log.d(
            "onSendBLEActiveGesture",
            "Sending active gesture command: deviceAddress=$deviceAddress, activeGesture=$activeGesture"
        )
    }

    private fun setupListRecyclerView() {
        mRotationGroupDragLv?.setLayoutManager(LinearLayoutManager(main.applicationContext))
        itemsGesturesRotationArray = ArrayList(rotationGroupGestures.mapIndexed { index, gesture -> Pair(index.toLong(), gesture.gestureName+"™"+gesture.gestureId.toString()) })
        listRotationGroupAdapter =
            RotationGroupItemAdapter(
                itemsGesturesRotationArray,
                R.layout.ubi4_item_rotation_group,
                R.id.swapIv,
                false,
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
    }

    override fun isForViewType(item: Any): Boolean = item is GesturesItem

    override fun GesturesItem.getItemId(): Any = title

    private fun loadSavedGestureNames(context: Context) {
        val sp = context.getSharedPreferences(PreferenceKeysUBI4.APP_PREFERENCES, Context.MODE_PRIVATE)
        val macKey = sp.getString(PreferenceKeysUBI4.LAST_CONNECTION_MAC_UBI4, "default") ?: "default"
        gestureNameList.clear()
        for (i in 0 until 8) {
            val key = PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM + macKey + i
            val name = sp.getString(key, "Gesture ${i + 1}") ?: "Gesture ${i + 1}"
            gestureNameList.add(name)
        }
        Log.d("GesturesAdapter", "Final loaded gesture names: $gestureNameList")
    }

    private fun updateGestureButtonsUI(binding: Ubi4WidgetGesturesOptic1Binding) {
        for (i in 1..PreferenceKeysUBI4.NUM_GESTURES) {
            try {
                // Получаем ссылку на TextView через рефлексию (как уже делается в onBind)
                val field = binding::class.java.getDeclaredField("gesture${i}NameTv")
                field.isAccessible = true
                val gestureTv = field.get(binding) as? TextView
                gestureTv?.text = gestureNameList.getOrElse(i - 1) { "Gesture $i" }
            } catch (e: Exception) {
                Log.e("GesturesAdapter", "Ошибка обновления имени жеста $i: ${e.localizedMessage}")
            }
        }
    }

    class MyDragItem internal constructor(context: Context?, layoutId: Int) :
        DragItem(context, layoutId) {
        override fun onBindDragView(clickedView: View, dragView: View) {
            val text = (clickedView.findViewById<View>(R.id.gestureInRotationGroupTv) as TextView).text
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
        val resultCb: ((result: Int)->Unit) = {
            rotationGroupGestures.removeAt(position)
            showIntroduction()
            setupListRecyclerView()
            synchronizeRotationGroup()
            sendBLERotationGroup()
            calculatingShowAddButton()
        }
        onDeleteClick(resultCb, rotationGroupGestures.get(position).gestureName)    }

    fun onDestroy() {
        collectJob?.cancel() // Явная отмена при уничтожении
        borderAnimator?.destroyCoroutines()
    }
}