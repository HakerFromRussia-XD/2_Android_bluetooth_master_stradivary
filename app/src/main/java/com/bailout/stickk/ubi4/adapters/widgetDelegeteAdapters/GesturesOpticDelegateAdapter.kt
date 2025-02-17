package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Handler
import android.util.Log
import android.util.Pair
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
import com.bailout.stickk.ubi4.data.local.SprGestureItemsProvider
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.GesturesItem
import com.bailout.stickk.ubi4.models.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ParameterInfoProvider
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GesturesOpticDelegateAdapter(
    val gestureNameList: ArrayList<String>,
    val onAddGesturesToSprScreen: (onSaveClickDialog: (MutableList<Pair<Int, Int>>) -> Unit, List<Pair<Int, Int>>) -> Unit,
    val onShowGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onRequestGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onSetCustomGesture: (
        onSaveDotsClick: (Pair<Int, Int>) -> Unit,
        bindingItem: Pair<Int, Int>
    ) -> Unit,
    val onSendBLEActiveGesture: (deviceAddress: Int, parameterID: Int, activeGesture: Int) -> Unit,
    val onRequestActiveGesture: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onSendBLEBindingGroup: (deviceAddress: Int, parameterID: Int, bindingGestureGroup: BindingGestureGroup) -> Unit,
    val onRequestBindingGroup: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit
) : ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesOptic1Binding>(
    Ubi4WidgetGesturesOptic1Binding::inflate
) {

    private val ANIMATION_DURATION = 200
    private var listBindingGesture: MutableList<Pair<Int, Int>> = mutableListOf()
    private var deviceAddress = 0
    private var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf()
    private var hideFactoryCollectionGestures = true

    private var gestureCollectionBtns: ArrayList<Pair<View, Int>> = ArrayList()
    private var gestureCustomBtns: ArrayList<Pair<View, Int>> = ArrayList()

    private lateinit var sprGestureItemsProvider: SprGestureItemsProvider
    private lateinit var _annotationTv: TextView
    private lateinit var _annotationIv: ImageView
    private lateinit var _rotationGroupTv: TextView
    private lateinit var _collectionOfGesturesTv: TextView
    private lateinit var _gesturesSelectV: View
    private lateinit var _ubi4GesturesSelectorV: View
    private lateinit var _collectionGesturesCl: ConstraintLayout
    private lateinit var _sprGestureGroupCl: ConstraintLayout
    private lateinit var _activeGestureNameCl: ConstraintLayout
    private lateinit var _activeGestureNameTv: TextView

    private var currentBindingGroup: BindingGestureGroup = BindingGestureGroup()

    private var isBindingGroupResponseReceived = false


    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
                    ParameterInfoProvider.getParameterIDByCode(
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
        onDestroyParent { onDestroy() }

        // Инициируем View-поля для удобства
        _annotationTv = annotationTv
        _annotationIv = annotationIv
        _rotationGroupTv = rotationGroupTv
        _collectionOfGesturesTv = collectionOfGesturesTv
        _gesturesSelectV = gesturesSelectV
        _ubi4GesturesSelectorV = ubi4GesturesSelectorV
        _collectionGesturesCl = collectionGesturesCl
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

        // Подписка на BLE-события
        collectActiveFlows()

        // Определяем deviceAddress/parameterInfoSet в зависимости от widget
        when (item.widget) {
            is BaseParameterWidgetEStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                parameterInfoSet = item.widget.baseParameterWidgetStruct.parameterInfoSet
            }
            is BaseParameterWidgetSStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                parameterInfoSet = item.widget.baseParameterWidgetStruct.parameterInfoSet
            }
        }


        val savedFilter = main.getInt(PreferenceKeysUBI4.LAST_ACTIVE_GESTURE_FILTER, 1)
        if (savedFilter == 2) {
            // Устанавливаем активный фильтр (если это необходимо для UI)
            MainActivityUBI4.activeFilterFlow.value = 2

            // Сразу вызываем команду запроса binding group
            requestBindingGroupWithRetry(
                deviceAddress,
                ParameterInfoProvider.getParameterIDByCode(
                    ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
                    parameterInfoSet
                )
            )
        }


        collectionOfGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUBI4.LAST_ACTIVE_GESTURE_FILTER, 1)
            MainActivityUBI4.activeFilterFlow.value = 1
            activeGestureNameCl.visibility = View.VISIBLE

        }
        sprGesturesSelectBtn.setOnClickListener {
            main.saveInt(PreferenceKeysUBI4.LAST_ACTIVE_GESTURE_FILTER, 2)
            MainActivityUBI4.activeFilterFlow.value = 2
            activeGestureNameCl.visibility = View.GONE
            onRequestBindingGroup(
                    deviceAddress,
                    ParameterInfoProvider.getParameterIDByCode(
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
                    activeGestureNameTv.text = "Active gesture is: ${gestureCollectionTitle?.text ?: "Unknown"}"
                }
            } else {
                gestureCollectionBtn?.let { gestureCollectionBtns.add(Pair(it, i + 2)) }
                gestureCollectionTitle?.text = getCollectionGestures()[i + 1].gestureName
                gestureCollectionImage?.setImageResource(getCollectionGestures()[i + 1].gestureImage)

                gestureCollectionBtn?.setOnClickListener {
                    setActiveGesture(gestureCollectionBtn)
                    onSendBLEActiveGesture(i + 2)
                    activeGestureNameTv.text = "Active gesture is: ${gestureCollectionTitle?.text ?: "Unknown"}"
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

            gestureCustomBtn?.let { gestureCustomBtns.add(Pair(it, 63 + i)) }
            gestureCustomTv?.text = gestureNameList[i - 1]
            gestureCustomBtn?.setOnClickListener {
                onSendBLEActiveGesture(63 + i)
                setActiveGesture(gestureCustomBtn)
                activeGestureNameTv.text = "Active gesture is: ${gestureCustomTv?.text ?: "Unknown"}"
            }
            gestureSettingsBtn?.setOnClickListener {
                onShowGestureSettings(
                    deviceAddress,
                    ParameterInfoProvider.getParameterIDByCode(
                        ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number,
                        parameterInfoSet
                    ),
                    (0x40).toInt() + i
                )
                main.saveInt(PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM, i)
            }
        }

        chooseLearningGesturesBtn1.setOnClickListener {
            val selectedGestures: (MutableList<Pair<Int, Int>>) -> Unit = { listBindingGestures ->
                listBindingGesture = listBindingGestures
                fillCollectionGesturesInBindingGroup()
                onSendBLEBindingGroup(
                    deviceAddress,
                    ParameterInfoProvider.getParameterIDByCode(
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



        onRequestActiveGesture(deviceAddress, ParameterInfoProvider.getParameterIDByCode(ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number, parameterInfoSet))

        scope.launch(Dispatchers.IO) {
            MainActivityUBI4.activeFilterFlow.collect { newFilter ->
                withContext(Dispatchers.Main) {
                    // При любом изменении фильтра - рендерим UI
                    renderFilterUI(newFilter)
                }
            }
        }
    }

    /**
     * Единый метод: ставит в нужное положение индикатор, меняет цвета текста,
     * показывает или скрывает нужные блоки (collection vs SPR).
     */
    private fun renderFilterUI(activeFilter: Int) {
        val density = main.resources.displayMetrics.density
        val filterWidth = (_ubi4GesturesSelectorV.width / density).toInt()

        when (activeFilter) {
            1 -> {
                _activeGestureNameCl.visibility = View.VISIBLE
                // Остальная логика для фильтра 1
                // Анимация индикатора в левую позицию
                ObjectAnimator.ofFloat(_gesturesSelectV, "x", 18 * density)
                    .setDuration(ANIMATION_DURATION.toLong()).start()

                // Анимация цвета текста
                val colorAnim = ObjectAnimator.ofInt(
                    _collectionOfGesturesTv, "textColor",
                    main.getColor(R.color.ubi4_deactivate_text), main.getColor(R.color.white)
                )
                colorAnim.setEvaluator(ArgbEvaluator())
                colorAnim.start()

                val colorAnim3 = ObjectAnimator.ofInt(
                    _rotationGroupTv, "textColor",
                    main.getColor(R.color.white), main.getColor(R.color.ubi4_deactivate_text)
                )
                colorAnim3.setEvaluator(ArgbEvaluator())
                colorAnim3.start()

                // Показываем collection, скрываем SPR
                showCollectionGestures(true, _collectionGesturesCl)
                showBindingGroup(false, _sprGestureGroupCl)
            }

            2 -> {
                _activeGestureNameCl.visibility = View.GONE
                // Анимация индикатора вправо
                ObjectAnimator.ofFloat(
                    _gesturesSelectV,
                    "x",
                    ((filterWidth / 2) + 18) * density
                ).setDuration(ANIMATION_DURATION.toLong()).start()

                // Анимация цвета текста
                val colorAnim2 = ObjectAnimator.ofInt(
                    _collectionOfGesturesTv, "textColor",
                    main.getColor(R.color.white), main.getColor(R.color.ubi4_deactivate_text)
                )
                colorAnim2.setEvaluator(ArgbEvaluator())
                colorAnim2.start()

                val colorAnim4 = ObjectAnimator.ofInt(
                    _rotationGroupTv, "textColor",
                    main.getColor(R.color.ubi4_deactivate_text), main.getColor(R.color.white)
                )
                colorAnim4.setEvaluator(ArgbEvaluator())
                colorAnim4.start()

                // Скрываем collection, показываем SPR
                showCollectionGestures(false, _collectionGesturesCl)
                showBindingGroup(true, _sprGestureGroupCl)
                onRequestBindingGroup(
                    deviceAddress,
                    ParameterInfoProvider.getParameterIDByCode(
                        ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
                        parameterInfoSet
                    )
                )
            }
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
        scope.launch(Dispatchers.IO) {
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
                                    // Для кастомных жестов: индекс = id - 63
                                    gestureNameList.getOrNull(id - 63) ?: "Unknown"
                                }
                            } ?: "Unknown"

                            // Устанавливаем текст с использованием строкового ресурса
                            _activeGestureNameTv.text = main.getString(R.string.active_gesture_is, gestureName)
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
                        withContext(Dispatchers.Main) {
                            fillCollectionGesturesInBindingGroup()
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

    private fun showBindingGroup(show: Boolean, collectionGesturesCl: ConstraintLayout) {
        if (show) {
            collectionGesturesCl.visibility = View.VISIBLE
        } else {
            collectionGesturesCl.visibility = View.GONE
        }
    }

    private fun showCollectionGestures(show: Boolean, rotationGroupCl: ConstraintLayout) {
        if (show) {
            rotationGroupCl.visibility = View.VISIBLE
        } else {
            rotationGroupCl.visibility = View.GONE
        }
    }

    private fun onSendBLEActiveGesture(activeGesture: Int) {
        onSendBLEActiveGesture(
            deviceAddress,
            ParameterInfoProvider.getParameterIDByCode(
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

    override fun isForViewType(item: Any): Boolean = item is GesturesItem
    override fun GesturesItem.getItemId(): Any = title

    fun onDestroy() {
        scope.cancel()
    }
}