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
import com.bailout.stickk.ubi4.adapters.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.adapters.models.GesturesItem
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class GesturesDelegateAdapter(
    val onSelectorClick: (selectedPage: Int) -> Unit,
    val onAddGesturesToSprScreen: (onSaveClickDialog: (List<SprGestureItem>) -> Unit, List<SprGestureItem>) -> Unit,
    val onsetCustomGesture: (onSaveClick: (() -> Unit)) -> Unit
) :
    ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesOptic1Binding>(
        Ubi4WidgetGesturesOptic1Binding::inflate
    ) {
    private val ANIMATION_DURATION = 200


    private val listSprItems: ArrayList<SprGestureItem> = ArrayList()

    val adapter = SelectedGesturesAdapter(
        selectedGesturesList = listSprItems,
        onCheckGestureSprListener = object : SelectedGesturesAdapter.OnCheckSprGestureListener {
            override fun onGestureSprClicked(position: Int, title: String) {
                System.err.println("Gesture clicked: $title at position: $position")
            }

        },
        onDotsClickListener = { position ->
            System.err.println("Dots clicked at position: $position")
            // Здесь  вызвать диалог или выполнить нужное действие
            onsetCustomGesture {
                // Обработать сохранение данных из диалога
            }
        }
    )

    @SuppressLint("ClickableViewAccessibility", "LogNotTimber")
    override fun Ubi4WidgetGesturesOptic1Binding.onBind(item: GesturesItem) {
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
                adapter.updateGestures(listSprItems)
                Log.d("GesturesDelegateAdapter", "$listSprItems")
            }
            onAddGesturesToSprScreen(selectedGestures,listSprItems)
        }



        val gridLayoutManager = GridLayoutManager(root.context, 2)
        gridLayoutManager.orientation = LinearLayoutManager.VERTICAL
        selectedSprGesturesRv.layoutManager = gridLayoutManager



        selectedSprGesturesRv.adapter = adapter


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