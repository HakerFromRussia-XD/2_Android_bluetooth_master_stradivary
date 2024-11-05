package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesOptic1Binding
import com.bailout.stickk.ubi4.adapters.dialog.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.models.BindingGestureItem
import com.bailout.stickk.ubi4.models.GesturesItem
import com.bailout.stickk.ubi4.models.SprGestureItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class GesturesOpticDelegateAdapter(
    val onSelectorClick: (selectedPage: Int) -> Unit,
    val onAddGesturesToSprScreen: (onSaveClickDialog: (List<SprGestureItem>) -> Unit, List<SprGestureItem>, List<BindingGestureItem>) -> Unit,
    val onsetCustomGesture: (onSaveDotsClick: ((name: String, position: Int) -> Unit), selectedPosition: Int, name: String) -> Unit
) :
    ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesOptic1Binding>(
        Ubi4WidgetGesturesOptic1Binding::inflate
    ) {

    private val ANIMATION_DURATION = 200
    private var listBindingGesture: MutableList<BindingGestureItem> = mutableListOf()


    @SuppressLint("LogNotTimber")
    val adapter = SelectedGesturesAdapter(
        selectedGesturesList = ArrayList(),
        onCheckGestureSprListener = object : SelectedGesturesAdapter.OnCheckSprGestureListener {
            override fun onGestureSprClicked(position: Int, title: String) {
                System.err.println("Gesture clicked: $title at position: $position")
            }

        },
        onDotsClickListener = { selectedPosition ->
            onsetCustomGesture({ name, position ->
                val bindingGesture = listBindingGesture[position].copy(nameOfUserGesture = name)
                listBindingGesture[position] = bindingGesture
                updateGestureName(bindingGesture.position, bindingGesture.nameOfUserGesture)
                Log.d("GestureAdapter", "$bindingGesture")
            }, selectedPosition, listBindingGesture[selectedPosition].nameOfUserGesture)
        }


    )


    @SuppressLint("ClickableViewAccessibility", "LogNotTimber")
    override fun Ubi4WidgetGesturesOptic1Binding.onBind(item: GesturesItem) {

        var listSpr: List<SprGestureItem> = ArrayList()


        var parameterID = 0
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        when (item.widget) {
            is CommandParameterWidgetEStruct -> {
                parameterID =
                    item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parentParameterID
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
            }

            is CommandParameterWidgetSStruct -> {
                parameterID =
                    item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parentParameterID
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
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



        gesture1Btn.setOnClickListener { System.err.println("setOnClickListener gesture1Btn") }
        gesture1SettingsBtn.setOnClickListener { System.err.println("setOnClickListener gesture1SettingsBtn") }


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
                Log.d("GesturesDelegateAdapter", "$listBindingGesture")
                Log.d("GesturesDelegateAdapter", "$listSprItems")
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


    }


    @SuppressLint("LogNotTimber")
    private fun updateGestureName(position: Int, newName: String) {
        val newList = listBindingGesture.toMutableList()
        newList[position] = newList[position].copy(nameOfUserGesture = newName)
        listBindingGesture = newList
        adapter.updateGestures(listBindingGesture)
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
}