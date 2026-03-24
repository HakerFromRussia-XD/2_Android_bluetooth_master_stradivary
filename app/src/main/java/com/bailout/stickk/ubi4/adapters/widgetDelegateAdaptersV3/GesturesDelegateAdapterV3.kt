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
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.state.UiState
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
import kotlinx.serialization.json.Json
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

    private val json = Json { encodeDefaults = true }
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
    private var currentActiveGestureId: Int? = null
    private var isRotationGroupResponseReceived = false


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
        UiState.activeGestureFragmentFilterFlow.value = savedFilter // если у тебя этот flow доступен тут
        if (savedFilter == 2) {
            requestRotationGroupWithRetry()
        }
        // запрос активного жеста — чтобы подсветка/текст пришли
        onRequestActiveGesture()

        collectionOfGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1)
            UiState.activeGestureFragmentFilterFlow.value = 1
        }

        rotationGroupSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 2)
            UiState.activeGestureFragmentFilterFlow.value = 2
            onRequestRotationGroup()
        }
        hideCollectionBtn.setOnClickListener {
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
                    Log.d("GesturesDelegateAdapter", "GestureCollectionBtn $gestureId clicked")
                    setActiveGesture(getGestureViewById(gestureId))
                    updateActiveGestureHeader(gestureId)
                    onSendBLEActiveGesture(gestureId)
                }
            } else {
                gestureCollectionTitle?.text = getCollectionGestures()[i + 1].gestureName
                gestureCollectionImage?.setImageResource(getCollectionGestures()[i + 1].gestureImage)

                val gestureId = i + 2
                gestureCollectionBtn?.let { gestureCollectionBtns.add(it to gestureId) }

                gestureCollectionBtn?.setOnClickListener {
                    Log.d("GesturesDelegateAdapter", "GestureCollectionBtn $gestureId clicked")
                    setActiveGesture(getGestureViewById(gestureId))
                    updateActiveGestureHeader(gestureId)
                    onSendBLEActiveGesture(gestureId)
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

            gestureCustomBtn?.setOnClickListener {
                Log.d("GesturesDelegateAdapter", "GestureCustomBtn $gestureId clicked")
                setActiveGesture(getGestureViewById(gestureId))
                updateActiveGestureHeader(gestureId)
                onSendBLEActiveGesture(gestureId)
            }

            gestureSettingsBtn?.setOnClickListener {
                Log.d("gestureCustomBtn", "gestureSettingsBtn $i")
                onShowGestureSettings(
                    ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING).dataCode,
                    (0x3F).toInt() + i
                )
                main.saveInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, i)
            }
        }


        addGestureToRotationGroupBtn.setOnClickListener {
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
                onSendBLERotationGroup()
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
                if (fromPosition != toPosition) {
                    synchronizeRotationGroup()
                    onSendBLERotationGroup()
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
            activeGestureId == null -> "Unknown"
            activeGestureId in 1..62 -> getCollectionGestures().getOrNull(activeGestureId - 1)?.gestureName ?: "Unknown"
            else -> gestureNameList.getOrNull(activeGestureId - 64) ?: "Unknown"
        }
        _activeGestureNameTv.text = main.getString(R.string.active_gesture_is, name)
    }



    private fun gestureFlowCollect() {
        collectJob?.cancel()

        collectJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                merge(
                    UiState.activeGestureFragmentFilterFlow.map{ filter ->
                        renderFilterUI(filter)
                    },
                    WidgetState.currentGestureFlowV3.map { parameterInfo ->
                        val parameter = ParameterProvider.getParameterV3(parameterInfo)
                        val currentGesture = parseCurrentGestureSafely(parameter.data) ?: return@map
                        currentActiveGestureId = currentGesture.currentGesture
                        setActiveGesture(getGestureViewById(currentGesture.currentGesture))
                        updateActiveGestureHeader(currentGesture.currentGesture)
                        listRotationGroupAdapter?.setActiveGestureId(currentGesture.currentGesture)},
                    WidgetState.gestureGroupFlowV3.map { parameterInfo ->
                        val parameter = ParameterProvider.getParameterV3(parameterInfo)
                        platformLog("requestRotationGroupV3", "приняли requestRotationGroupV3 пришли по потоку ${parameter.data}")
                        val currentGesture = parseGestureGroupeSafely(parameter.data) ?: return@map
                        val rotationGroupList = currentGesture.toGestureList()
                        isRotationGroupResponseReceived = true
                        rotationGroupGestures.clear()
                        rotationGroupList.forEach { item ->
                            if (item.first != 0) rotationGroupGestures.add(getGesture(item.first))
                        }
                        showIntroduction()
                        setupListRecyclerView()
                        synchronizeRotationGroup()
                        calculatingShowAddButton()
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
                    main.showToast("ERROR plotArrayFlowCollect")
                }
                Log.e("gestureFlowCollect", "Exception: ${e.message}")
            }
        }
    }
    private fun parseGestureGroupeSafely(data: String): RotationGroupV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<RotationGroupV3>(data) }
            .getOrNull()
    }
    private fun parseCurrentGestureSafely(data: String): CurrentGestureV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<CurrentGestureV3>(data) }
            .getOrNull()
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
    private fun requestRotationGroupWithRetry() {
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

    private fun setupListRecyclerView() {
        mRotationGroupDragLv?.setLayoutManager(LinearLayoutManager(main.applicationContext))
        itemsGesturesRotationArray = ArrayList(rotationGroupGestures.mapIndexed { index, gesture ->
            Pair(
                index.toLong(),
                gesture.gestureName + "™" + gesture.gestureId.toString()
            )
        })
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
        currentActiveGestureId?.let { id ->
            listRotationGroupAdapter?.setActiveGestureId(id)
        }
        mRotationGroupDragLv?.setCanDragHorizontally(false)
        mRotationGroupDragLv?.setCanDragVertically(true)
        mRotationGroupDragLv?.setCustomDragItem(
            MyDragItem(
                main.applicationContext,
                R.layout.ubi4_item_rotation_group_drag
            )
        )
    }

    private fun renderFilterUI(activeFilter: Int) {
        val displayMetrics = main.resources.displayMetrics
        val filterWidth = (_ubi4GesturesSelectorV.width / displayMetrics.density).toInt()

        when (activeFilter) {
            1 -> {
                ObjectAnimator.ofFloat(_gesturesSelectV, "x", (18 * displayMetrics.density))
                    .setDuration(ANIMATION_DURATION.toLong()).start()

                ObjectAnimator.ofInt(
                    _collectionOfGesturesTv, "textColor",
                    main.getColor(R.color.ubi4_deactivate_text), main.getColor(R.color.white)
                ).apply { setEvaluator(ArgbEvaluator()); duration = ANIMATION_DURATION.toLong(); start() }

                ObjectAnimator.ofInt(
                    _rotationGroupTv, "textColor",
                    main.getColor(R.color.white), main.getColor(R.color.ubi4_deactivate_text)
                ).apply { setEvaluator(ArgbEvaluator()); duration = ANIMATION_DURATION.toLong(); start() }

                showCollectionGestures(true, _rotationGroupCl, _collectionGesturesCl)
            }
            2 -> {
                ObjectAnimator.ofFloat(
                    _gesturesSelectV,
                    "x",
                    ((filterWidth / 2) + 18) * displayMetrics.density
                ).setDuration(ANIMATION_DURATION.toLong()).start()

                ObjectAnimator.ofInt(
                    _collectionOfGesturesTv, "textColor",
                    main.getColor(R.color.white), main.getColor(R.color.ubi4_deactivate_text)
                ).apply { setEvaluator(ArgbEvaluator()); duration = ANIMATION_DURATION.toLong(); start() }

                ObjectAnimator.ofInt(
                    _rotationGroupTv, "textColor",
                    main.getColor(R.color.ubi4_deactivate_text), main.getColor(R.color.white)
                ).apply { setEvaluator(ArgbEvaluator()); duration = ANIMATION_DURATION.toLong(); start() }

                showCollectionGestures(false, _rotationGroupCl, _collectionGesturesCl)
            }
        }

        _activeGestureNameCl.visibility = View.VISIBLE
    }

    private fun showCollectionGestures(
        show: Boolean,
        rotationGroupCl: ConstraintLayout,
        collectionGesturesCl: ConstraintLayout
    ) {
        if (show) {
            collectionGesturesCl.visibility = View.VISIBLE
            rotationGroupCl.visibility = View.GONE
        } else {
            rotationGroupCl.visibility = View.VISIBLE
            collectionGesturesCl.visibility = View.GONE
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
    override fun GesturesItemV3.getItemId(): Any = title

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
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        listRotationGroupAdapter?.notifyDataSetChanged()
        synchronizeRotationGroup()
        onSendBLERotationGroup()
        calculatingShowAddButton()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onDeleteClickCb(position: Int) {
        val resultCb: ((result: Int) -> Unit) = {
            rotationGroupGestures.removeAt(position)
            showIntroduction()
            setupListRecyclerView()
            synchronizeRotationGroup()
            onSendBLERotationGroup()
            calculatingShowAddButton()
        }
        onDeleteClick(resultCb, rotationGroupGestures.get(position).gestureName)
    }

    // Метод для завершения работы CoroutineScope, чтобы освободить ресурсы
    fun onDestroy() {
        Log.d("LifeCycele", "stopCollectingGestureFlow")
        collectJob?.cancel()
    }

    override fun onRotationGestureClick(position: Int, gestureName: String?, gestureId: Int) {
        if (gestureId == 0) return

        currentActiveGestureId = gestureId
        listRotationGroupAdapter?.setActiveGestureId(gestureId)

        setActiveGesture(getGestureViewById(gestureId))
        updateActiveGestureHeader(gestureId)

        onSendBLEActiveGesture(gestureId)
    }
}