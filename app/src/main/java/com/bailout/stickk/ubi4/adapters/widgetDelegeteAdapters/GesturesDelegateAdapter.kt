package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesBinding
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.GesturesItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListenerAdapter
import android.util.Pair
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider.Companion.getGesture
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.MyViewModel
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.rotationGroupGestures
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.stream.Collectors

class GesturesDelegateAdapter(
//    private val viewModel: MyViewModel,
    val onSelectorClick: (selectedPage: Int) -> Unit,
    val onDeleteClick: (resultCb: ((result: Int) -> Unit), gestureName: String) -> Unit,
    val onAddGesturesToRotationGroup: (onSaveDialogClick: ((selectedGestures: ArrayList<Gesture>) -> Unit)) -> Unit,
    val onSendBLERotationGroup: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onShowGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onRequestGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onRequestRotationGroup: (deviceAddress: Int, parameterID: Int) -> Unit,
    val onDestroyParrent: (onDestroyParrent: (() -> Unit)) -> Unit,
) : RotationGroupItemAdapter.OnCopyClickRotationGroupListener,
    RotationGroupItemAdapter.OnDeleteClickRotationGroupListener,
    ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesBinding>(Ubi4WidgetGesturesBinding::inflate) {

    private lateinit var binding: Ubi4WidgetGesturesBinding
    private val ANIMATION_DURATION = 200
    private var itemsGesturesRotationArray: ArrayList<Pair<Long, String>>? = null
    private var listRotationGroupAdapter: RotationGroupItemAdapter? = null
    private var mRotationGroupDragLv: DragListView? = null
    private var hideFactoryCollectionGestures = true

    private lateinit var mRotationGroupExplanationTv: TextView
    private lateinit var mRotationGroupExplanation2Tv: TextView
    private lateinit var mRotationGroupExplanationIv: ImageView
    private lateinit var mRotationGroupExplanation2Iv: ImageView

    private lateinit var mAddGestureToRotationGroupBtn: View
    private lateinit var mPlusIv: ImageView
    private var parameterIDSet = mutableSetOf<Pair<Int, Int>>()
    private var deviceAddress = 0

    // Создаем единственный CoroutineScope с диспетчером, который будет использоваться для потока
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetGesturesBinding.onBind(item: GesturesItem) {
        mRotationGroupDragLv = rotationGroupDragLv
        onDestroyParrent{ stopCollectingGestureFlow() }

        when (item.widget) {
            is BaseParameterWidgetEStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                //TODO добавить здесь сортировку parameterID по датакоду
                parameterIDSet = item.widget.baseParameterWidgetStruct.parametersIDAndDataCodes
            }
            is BaseParameterWidgetSStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                parameterIDSet = item.widget.baseParameterWidgetStruct.parametersIDAndDataCodes
            }
        }
        collectionOfGesturesSelectBtn.setOnClickListener { moveFilterSelection(1, gesturesSelectV, collectionOfGesturesTv, rotationGroupTv, ubi4GesturesSelectorV, collectionGesturesCl, rotationGroupCl) }
        rotationGroupSelectBtn.setOnClickListener {
            moveFilterSelection(2, gesturesSelectV, collectionOfGesturesTv, rotationGroupTv, ubi4GesturesSelectorV, collectionGesturesCl, rotationGroupCl)
            onRequestRotationGroup(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number))
        }
        hideCollectionBtn.setOnClickListener {
            System.err.println("collectionFactoryGesturesCl.layoutParams.height = ${collectionFactoryGesturesCl.layoutParams.height}")

            if (hideFactoryCollectionGestures) {
                hideFactoryCollectionGestures = false
                hideCollectionBtn.animate().rotation(180F).duration = ANIMATION_DURATION.toLong()
                collectionUserGesturesCl.animate().translationY(-(collectionFactoryGesturesCl.height).toFloat()).duration = ANIMATION_DURATION.toLong()
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
                collectionUserGesturesCl.animate().translationY(-(collectionFactoryGesturesCl.height).toFloat()).duration = 0
                collectionFactoryGesturesCl.visibility = View.VISIBLE
                Handler().postDelayed({
                    collectionUserGesturesCl.animate().translationY(0F).duration = ANIMATION_DURATION.toLong()
                    collectionFactoryGesturesCl.animate()
                        .alpha(1.0f)
                        .setDuration(ANIMATION_DURATION.toLong())
                }, ANIMATION_DURATION.toLong())
            }
        }

        gestureCollection1Btn.setOnClickListener { System.err.println("setOnClickListener gestureCollection1Btn") }
        gestureCollection2Btn.setOnClickListener { System.err.println("setOnClickListener gestureCollection2Btn") }
        gestureCollection3Btn.setOnClickListener { System.err.println("setOnClickListener gestureCollection3Btn") }
        gestureCollection4Btn.setOnClickListener { System.err.println("setOnClickListener gestureCollection4Btn") }



        gesture1Btn.setOnClickListener {
            System.err.println("setOnClickListener gesture1Btn")
            onRequestGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x40).toInt())
        }
        gesture1SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x40).toInt())
        }
        gesture2SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x41).toInt())
        }
        gesture3SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x42).toInt())
        }
        gesture4SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x43).toInt())
        }
        gesture5SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x44).toInt())
        }
        gesture6SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x45).toInt())
        }
        gesture7SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x46).toInt())
        }
        gesture8SettingsBtn.setOnClickListener {
            onShowGestureSettings(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number), (0x47).toInt())
        }



        addGestureToRotationGroupBtn.setOnClickListener {
            val resultCb: ((selectedGestures: ArrayList<Gesture>)->Unit) = { selectedGestures ->
                // проверка что элемент из selectedGestures содержится в rotationGroupGestures
                // если да, то не меняем его положение и добавляем новых в конец списка
                val notContainsList = selectedGestures.stream().filter{element -> !rotationGroupGestures.contains(element)}.collect(Collectors.toList())//rotationGroupGestures.add())
                notContainsList.forEach { rotationGroupGestures.add(it) }
                // удаляем те элементы, которые были отчекнуты
                val finalList = rotationGroupGestures.stream().filter{element -> selectedGestures.contains(element)}.collect(Collectors.toList())
                rotationGroupGestures = ArrayList(finalList)

                showIntroduction()
                setupListRecyclerView()
                synhronizeRotationGroup()
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
                    synhronizeRotationGroup()
                    sendBLERotationGroup()
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
    private fun gestureFlowCollect() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                MainActivityUBI4.rotationGroupFlow.collect { _ ->
                    val parameter = ParameterProvider.getParameter(ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number)
                    val rotationGroup = Json.decodeFromString<RotationGroup>("\"${parameter.data}\"")
                    val testList = rotationGroup.toGestureList()
                    Log.d("uiRotationGroupObservable", "InAdapter testList = $testList  size = ${testList.size}")
                    rotationGroupGestures.clear()
                    testList.forEach{ item ->
                        if (item.first != 0 )
                            rotationGroupGestures.add(CollectionGesturesProvider.getGesture(item.first))
                    }

                    showIntroduction()
                    setupListRecyclerView()
                    synhronizeRotationGroup()
                    calculatingShowAddButton()
                }
            }
        }
    }
    // Метод для завершения работы CoroutineScope, чтобы освободить ресурсы
    fun stopCollectingGestureFlow() {
        Log.d("LifeCycele", "stopCollectingGestureFlow")
        scope.cancel()
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
    private fun synhronizeRotationGroup() {
        rotationGroupGestures.clear()
        itemsGesturesRotationArray?.forEach {
            rotationGroupGestures.add(getGesture(it.second.split("™")[1].toInt()))
        }
    }
    private fun sendBLERotationGroup() {
        onSendBLERotationGroup(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number))
    }
    private fun getParameterIDByCode(dataCode: Int): Int {
        parameterIDSet.forEach {
            if (it.second == dataCode) {
                return it.first
            }
        }
        return 0
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


    private fun moveFilterSelection(
        position: Int,
        gesturesSelectV: View,
        collectionOfGesturesTv: TextView,
        rotationGroupTv: TextView,
        ubi4GesturesSelectorV: View,
        collectionGesturesCl: ConstraintLayout,
        rotationGroupCl: ConstraintLayout,
    ) {
        System.err.println("moveFilterSelection")
        val displayMetrics: DisplayMetrics = main.resources.displayMetrics
        val filterWidth = (ubi4GesturesSelectorV.width / displayMetrics.density).toInt()
        when (position) {
            1 -> {
                ObjectAnimator.ofFloat(gesturesSelectV, "x", (18 * displayMetrics.density))
                    .setDuration(ANIMATION_DURATION.toLong()).start()
                val colorAnim: ObjectAnimator = ObjectAnimator.ofInt(
                    collectionOfGesturesTv, "textColor",
                    main.getColor(R.color.ubi4_deactivate_text), main.getColor(R.color.white)
                )
                colorAnim.setEvaluator(ArgbEvaluator())
                colorAnim.start()
                val colorAnim3: ObjectAnimator = ObjectAnimator.ofInt(
                    rotationGroupTv, "textColor",
                    main.getColor(R.color.white), main.getColor(R.color.ubi4_deactivate_text)
                )
                colorAnim3.setEvaluator(ArgbEvaluator())
                colorAnim3.start()
                showCollectionGestures(true, rotationGroupCl, collectionGesturesCl)
            }
            2 -> {
                ObjectAnimator.ofFloat(
                    gesturesSelectV,
                    "x",
                    ((filterWidth / 2) + 18) * displayMetrics.density
                ).setDuration(ANIMATION_DURATION.toLong()).start() //546
                val colorAnim2: ObjectAnimator = ObjectAnimator.ofInt(
                    collectionOfGesturesTv, "textColor",
                    main.getColor(R.color.white), main.getColor(R.color.ubi4_deactivate_text)
                )
                colorAnim2.setEvaluator(ArgbEvaluator())
                colorAnim2.start()
                val colorAnim4: ObjectAnimator = ObjectAnimator.ofInt(
                    rotationGroupTv, "textColor",
                    main.getColor(R.color.ubi4_deactivate_text), main.getColor(R.color.white)
                )
                colorAnim4.setEvaluator(ArgbEvaluator())
                colorAnim4.start()
                showCollectionGestures(false, rotationGroupCl, collectionGesturesCl)
            }

            else -> throw IllegalStateException("Unexpected value: $position")
        }
    }

    private fun showCollectionGestures(show: Boolean, rotationGroupCl: ConstraintLayout, collectionGesturesCl: ConstraintLayout) {
        if (show) {
            collectionGesturesCl.visibility = View.VISIBLE
            rotationGroupCl.visibility = View.GONE
        } else {
            rotationGroupCl.visibility = View.VISIBLE
            collectionGesturesCl.visibility = View.GONE
        }
    }
    private fun showIntroduction () {
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

    override fun isForViewType(item: Any): Boolean = item is GesturesItem
    override fun GesturesItem.getItemId(): Any = title
    class MyDragItem internal constructor(context: Context?, layoutId: Int) :
        DragItem(context, layoutId) {
        override fun onBindDragView(clickedView: View, dragView: View) {
            val text = (clickedView.findViewById<View>(R.id.gestureInRotationGroupTv) as TextView).text
            (dragView.findViewById<View>(R.id.gestureInRotationGroupTv) as TextView).text =
                text
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onCopyClick(position: Int, gestureId: String) {
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        listRotationGroupAdapter?.notifyDataSetChanged()
        synhronizeRotationGroup()
        sendBLERotationGroup()
        calculatingShowAddButton()
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onDeleteClickCb(position: Int) {
        val resultCb: ((result: Int)->Unit) = {
            rotationGroupGestures.removeAt(position)
            showIntroduction()
            setupListRecyclerView()
            synhronizeRotationGroup()
            sendBLERotationGroup()
            calculatingShowAddButton()
        }
        onDeleteClick(resultCb, rotationGroupGestures.get(position).gestureName)
    }
}