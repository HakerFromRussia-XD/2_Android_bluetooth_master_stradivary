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
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider.Companion.getCollectionGestures
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.BindingGestureItem
import com.bailout.stickk.ubi4.models.GesturesItem
import com.bailout.stickk.ubi4.models.SprGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GesturesOpticDelegateAdapter(
    val gestureNameList:  ArrayList<String>,
    val onSelectorClick: (selectedPage: Int) -> Unit,
    val onAddGesturesToSprScreen: (onSaveClickDialog: (List<SprGestureItem>) -> Unit, List<SprGestureItem>, List<BindingGestureItem>) -> Unit,
    val onShowGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onRequestGestureSettings: (deviceAddress: Int, parameterID: Int, gestureID: Int) -> Unit,
    val onSetCustomGesture: (onSaveDotsClick: ((name: String, position: Int) -> Unit), selectedPosition: Int, name: String) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesOptic1Binding>(
        Ubi4WidgetGesturesOptic1Binding::inflate
    ) {

    private val ANIMATION_DURATION = 200
    private var listBindingGesture: MutableList<BindingGestureItem> = mutableListOf()
    private var parameterIDSet = mutableSetOf<Pair<Int, Int>>()
    private var deviceAddress = 0
    private var hideFactoryCollectionGestures = true

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
            onSetCustomGesture({ name, position ->
                val bindingGesture = listBindingGesture[position].copy(nameOfUserGesture = name)
                listBindingGesture[position] = bindingGesture
                updateGestureName(bindingGesture.position, bindingGesture.nameOfUserGesture)
            }, selectedPosition, listBindingGesture[selectedPosition].nameOfUserGesture)
        }


    )



    @SuppressLint("ClickableViewAccessibility", "LogNotTimber", "SuspiciousIndentation")
    override fun Ubi4WidgetGesturesOptic1Binding.onBind(item: GesturesItem) {
        onDestroyParent{ onDestroy() }
        var listSpr: List<SprGestureItem> = ArrayList()


        var parameterID = 0
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        when (item.widget) {
            is BaseParameterWidgetEStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                parameterIDSet = item.widget.baseParameterWidgetStruct.parametersIDAndDataCodes
            }
            is BaseParameterWidgetSStruct -> {
                deviceAddress = item.widget.baseParameterWidgetStruct.deviceId
                parameterIDSet = item.widget.baseParameterWidgetStruct.parametersIDAndDataCodes
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

        for (i in 1..14) {
            val gestureCollectionTitle = this::class.java.getDeclaredField("gestureCollection${i}Tv")
                .get(this) as? TextView
            val gestureCollectionImage = this::class.java.getDeclaredField("gestureCollection${i}Iv")
                .get(this) as? ImageView
            gestureCollectionTitle?.text = getCollectionGestures().get(i).gestureName
            gestureCollectionImage?.setImageResource(getCollectionGestures().get(i).gestureImage)
        }

        for (i in 1..8) {
                val gestureCustomTv = this::class.java.getDeclaredField("gesture${i}NameTv")
                    .get(this) as? TextView
                val gestureCustomBtn = this::class.java.getDeclaredField("gestureCustom${i}Btn")
                    .get(this) as? View
                val gestureSettingsBtn = this::class.java.getDeclaredField("gesture${i}SettingsBtn")
                    .get(this) as? View
                gestureCustomTv?.text = gestureNameList[i - 1]
                gestureCustomBtn?.setOnClickListener {
                    Log.d("gestureCustomBtn", "gestureCustomBtn $i")
                    onRequestGestureSettings(
                        deviceAddress,
                        getParameterIDByCode(ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number),
                        (0x40).toInt() + i
                    )
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
            val selectedGestures: (List<SprGestureItem>) -> Unit = { listSprItems ->
                listSpr = listSprItems

                listBindingGesture = listSprItems.mapIndexed { position, sprGestureItem ->
                    //проверяем уже выбранные жесты
                    val existingBindingGesture = listBindingGesture.find { it.position == position }
                    BindingGestureItem(
                        position = position,
                        nameOfUserGesture = existingBindingGesture?.nameOfUserGesture ?: "",
                        sprGestureItem = sprGestureItem
                    )
                }.toMutableList()
                adapter.updateGestures(listBindingGesture)
                if (adapter.itemCount > 0) {
                    annotationTv.visibility = View.GONE
                    annotationIv.visibility = View.GONE
                } else {
                    annotationTv.visibility = View.VISIBLE
                    annotationIv.visibility = View.VISIBLE
                }

            }
            onAddGesturesToSprScreen(selectedGestures, listSpr, listBindingGesture)
        }


        val gridLayoutManager = GridLayoutManager(root.context, 2)
        gridLayoutManager.orientation = LinearLayoutManager.VERTICAL
        selectedSprGesturesRv.layoutManager = gridLayoutManager
        selectedSprGesturesRv.adapter = adapter

            bindingGroupFlowCollect()

    }


    private fun bindingGroupFlowCollect() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                MainActivityUBI4.bindingGroupFlow.collect {
                    //TODO реализовать обновление UI, сделай новый список: на вход будет список Pair<Int,Int>,а
                    //  на выходе получить список listBindingGesture
                }
            }
        }
    }


    @SuppressLint("LogNotTimber")
    private fun updateGestureName(position: Int, newName: String) {
        val newList = listBindingGesture.toMutableList()
        newList[position] = newList[position].copy(nameOfUserGesture = newName)
        listBindingGesture = newList
        adapter.updateGestures(listBindingGesture)
    }
    private fun getParameterIDByCode(dataCode: Int): Int {
        parameterIDSet.forEach {
            if (it.second == dataCode) {
                return it.first
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
                showRotationGroup(false, sprGestureGroupCl)
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
                showRotationGroup(true, sprGestureGroupCl)
            }

            else -> throw IllegalStateException("Unexpected value: $position")
        }
    }


    private fun showRotationGroup(show: Boolean, collectionGesturesCl: ConstraintLayout) {
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


    override fun isForViewType(item: Any): Boolean = item is GesturesItem

    override fun GesturesItem.getItemId(): Any = title

    // Метод для завершения работы CoroutineScope, чтобы освободить ресурсы
    fun onDestroy() {
        Log.d("LifeCycele", "stopCollectingGestureFlow")
        scope.cancel()
    }
}