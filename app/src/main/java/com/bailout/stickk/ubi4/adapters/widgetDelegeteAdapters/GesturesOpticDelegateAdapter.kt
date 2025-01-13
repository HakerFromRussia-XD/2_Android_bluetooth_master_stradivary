package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Handler
import android.util.DisplayMetrics
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
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider.Companion.getCollectionGestures
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.GesturesItem
import com.bailout.stickk.ubi4.models.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class GesturesOpticDelegateAdapter(
    val gestureNameList: ArrayList<String>,
    val onSelectorClick: (selectedPage: Int) -> Unit,
    val onAddGesturesToSprScreen: (onSaveClickDialog: (MutableList<Pair<Int, Int>>) -> Unit, List<Pair<Int,Int>>) -> Unit,
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
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesOptic1Binding>(
        Ubi4WidgetGesturesOptic1Binding::inflate
    ) {

    private val ANIMATION_DURATION = 200
    private var listBindingGesture: MutableList<Pair<Int,Int>> = mutableListOf()
    private var parameterIDSet = mutableSetOf<Quadruple<Int, Int, Int, Int>>()
    private var deviceAddress = 0
    private var hideFactoryCollectionGestures = true
    private var gestureCollectionBtns: ArrayList<View> = ArrayList()
    private var gestureCustomBtns: ArrayList<View> = ArrayList()
    private lateinit var sprGestureItemsProvider: SprGestureItemsProvider
    private lateinit var _annotationTv: TextView
    private lateinit var _annotationIv: ImageView

    private var currentBindingGroup: BindingGestureGroup = BindingGestureGroup()

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
                // позиция изменяемой ячейки в listBindingGesture
                val position = listBindingGesture.indexOfFirst { it.first == bindingItem.first }
                listBindingGesture[position] = bindingItem
                fillCollectionGesturesInBindingGroup()
                onSendBLEBindingGroup(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number), currentBindingGroup)
            }, listBindingGesture[selectedPosition])
        }
    )


    @SuppressLint("ClickableViewAccessibility", "LogNotTimber", "SuspiciousIndentation")
    override fun Ubi4WidgetGesturesOptic1Binding.onBind(item: GesturesItem) {
        onDestroyParent { onDestroy() }
        _annotationTv = annotationTv
        _annotationIv = annotationIv
        collectActiveFlows()

        when (item.widget) {
            is BaseParameterWidgetEStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                parameterIDSet = item.widget.baseParameterWidgetStruct.parametersIDAndDataCodes
                Log.d("ParamInfo"," ParamInfoEStruct parameterIDSet: $parameterIDSet" )

            }
            is BaseParameterWidgetSStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                parameterIDSet = item.widget.baseParameterWidgetStruct.parametersIDAndDataCodes
                Log.d("ParamInfo"," ParamInfoSStruct parameterIDSet: $parameterIDSet" )
            }
        }

        collectionOfGesturesSelectBtn.setOnClickListener {
            moveFilterSelection(
                1,
                gesturesSelectV,
                collectionOfGesturesTv,
                rotationGroupTv,
                ubi4GesturesSelectorV,
                collectionGesturesCl,
                sprGestureGroupCl
            )
        }
        sprGesturesSelectBtn.setOnClickListener {
            moveFilterSelection(
                2,
                gesturesSelectV,
                collectionOfGesturesTv,
                rotationGroupTv,
                ubi4GesturesSelectorV,
                collectionGesturesCl,
                sprGestureGroupCl
            )
            onRequestBindingGroup(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number) )
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
        }

        gestureCollectionBtns.clear()
        gestureCustomBtns.clear()
        for (i in 0..14) {
            val gestureCollectionBtn = this::class.java.getDeclaredField("gestureCollection${i}Btn")
                .get(this) as? View
            val gestureCollectionTitle =
                this::class.java.getDeclaredField("gestureCollection${i}Tv")
                    .get(this) as? TextView
            val gestureCollectionImage =
                this::class.java.getDeclaredField("gestureCollection${i}Iv")
                    .get(this) as? ImageView

                gestureCollectionBtn?.let { btn ->
                    gestureCollectionBtns.add(btn)
                    btn.setOnClickListener {
                        Log.d("GesturesDelegateAdapter", "GestureCollectionBtn $i clicked")
                        setActiveButton(btn)
                        onSendBLEActiveGesture(i)
                        onRequestActiveGesture(deviceAddress,getParameterIDByCode(ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number))
                    }
                }
                gestureCollectionTitle?.text =
                    CollectionGesturesProvider.getCollectionGestures()[i].gestureName
                gestureCollectionImage?.setImageResource(CollectionGesturesProvider.getCollectionGestures()[i].gestureImage)



        }

        for (i in 1..8) {
            val gestureCustomTv = this::class.java.getDeclaredField("gesture${i}NameTv")
                .get(this) as? TextView
            val gestureCustomBtn = this::class.java.getDeclaredField("gestureCustom${i}Btn")
                .get(this) as? View
            val gestureSettingsBtn = this::class.java.getDeclaredField("gesture${i}SettingsBtn")
                .get(this) as? View
            gestureCustomTv?.text = gestureNameList[i - 1]
            gestureCustomBtn?.let { btn ->
                gestureCustomBtns.add(btn)
                btn.setOnClickListener {
                    Log.d("gestureCustomBtn", "gestureCustomBtn $i")
                    setActiveButton(btn)
                    onSendBLEActiveGesture(i)
                    onRequestGestureSettings(
                        deviceAddress,
                        getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number),
                        (0x40).toInt() + i
                    )

                }

            }
            gestureSettingsBtn?.setOnClickListener {
                Log.d("gestureCustomBtn", "gestureSettingsBtn $i")
                onShowGestureSettings(
                    deviceAddress,
                    getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number),
                    (0x40).toInt() + i
                )
                main.saveInt(PreferenceKeysUBI4.SELECT_GESTURE_SETTINGS_NUM, i)
            }
        }


        chooseLearningGesturesBtn1.setOnClickListener {
            val selectedGestures: (MutableList<Pair<Int, Int>>) -> Unit = { listBindingGestures ->
                listBindingGesture = listBindingGestures
                fillCollectionGesturesInBindingGroup()
                onSendBLEBindingGroup(deviceAddress, getParameterIDByCode(ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number), currentBindingGroup)

            }
            onAddGesturesToSprScreen(selectedGestures, listBindingGesture)
        }


        val gridLayoutManager = GridLayoutManager(root.context, 2)
        gridLayoutManager.orientation = LinearLayoutManager.VERTICAL
        selectedSprGesturesRv.layoutManager = gridLayoutManager
        selectedSprGesturesRv.adapter = adapter

        sprGestureItemsProvider = SprGestureItemsProvider(root.context)

    }

    private fun fillCollectionGesturesInBindingGroup(
    ) : BindingGestureGroup {
        currentBindingGroup = BindingGestureGroup()
        listBindingGesture.forEachIndexed { index, pair: Pair<Int, Int> ->
            currentBindingGroup.setGestureAt(index, pair)
        }
        Log.d("fillSprGesturesInBG", "$listBindingGesture")
        adapter.updateGestures(listBindingGesture)

        if (adapter.itemCount > 0) {
            _annotationTv.visibility = View.GONE
            _annotationIv.visibility = View.GONE
        } else {
            _annotationTv.visibility = View.VISIBLE
            _annotationIv.visibility = View.VISIBLE
        }

        return currentBindingGroup
    }


    private fun setActiveButton(activeBtn: View) {
        gestureCollectionBtns.forEach { btn ->
            btn.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray)
        }
        gestureCustomBtns.forEach { btn ->
            btn.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray)
        }
        activeBtn.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_active)
    }

    private fun collectActiveFlows () {
        scope.launch(Dispatchers.IO) {
            merge(
                MainActivityUBI4.activeGestureFlow.map { activeGestureParameterRef ->
                    val parameter =  ParameterProvider.getParameter(deviceAddress, activeGestureParameterRef.parameterID)
                },

                MainActivityUBI4.bindingGroupFlow.map { bindingGroupParameterRef ->
                    val parameter = ParameterProvider.getParameter(bindingGroupParameterRef.addressDevice, bindingGroupParameterRef.parameterID)
                    val bindingGroup = Json.decodeFromString<BindingGestureGroup>("\"${parameter.data}\"")
                    listBindingGesture.clear()
                    bindingGroup.toGestureList().forEach{
                        if (it.first != 0) { listBindingGesture.add(it) }
                    }
                    fillCollectionGesturesInBindingGroup()
                }
            ).collect()

        }
    }


//    private fun bindingGroupFlowCollect() {
//        scope.launch(Dispatchers.Main) {
//            MainActivityUBI4.bindingGroupFlow.collect { parameterRef ->
//                val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
//                val bindingGroup = Json.decodeFromString<BindingGestureGroup>("\"${parameter.data}\"")
//                listBindingGesture.clear()
//                bindingGroup.toGestureList().forEach{
//                    if (it.first != 0) { listBindingGesture.add(it) }
//                }
//                fillCollectionGesturesInBindingGroup()
//            }
//        }
//    }




    private fun getParameterIDByCode(dataCode: Int): Int {
        parameterIDSet.forEach {
            if (it.dataCode == dataCode) {
                return it.parameterID
            }
        }
        return 0
    }



    private fun moveFilterSelection(
        position: Int,
        gesturesSelectV: View,
        collectionOfGesturesTv: TextView,
        rotationGroupTv: TextView,
        ubi4GesturesSelectorV: View,
        collectionGesturesCl: ConstraintLayout,
        sprGestureGroupCl: ConstraintLayout
    ) {
        System.err.println("moveFilterSelection")
        val displayMetrics: DisplayMetrics = main.getResources().displayMetrics
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
                showCollectionGestures(true, collectionGesturesCl)
                showBindingGroup(false, sprGestureGroupCl)
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
                showCollectionGestures(false, collectionGesturesCl)
                showBindingGroup(true, sprGestureGroupCl)
            }

            else -> throw IllegalStateException("Unexpected value: $position")
        }
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
            getParameterIDByCode(ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number),
            activeGesture
        )
    }


    override fun isForViewType(item: Any): Boolean = item is GesturesItem

    override fun GesturesItem.getItemId(): Any = title

    // Метод для завершения работы CoroutineScope, чтобы освободить ресурсы
    fun onDestroy() {
        Log.d("LifeCycele", "stopCollectingGestureFlow")
        scope.cancel()
    }
}