package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Pair
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesBinding
import com.bailout.stickk.ubi4.adapters.models.GesturesItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListenerAdapter

class GesturesDelegateAdapter(
    val onSelectorClick: (selectedPage: Int) -> Unit,
    val onDeleteClick: (resultCb: ((result: Int)->Unit), gestureName: String) -> Unit
) : RotationGroupItemAdapter.OnCopyClickRotationGroupListener,
    RotationGroupItemAdapter.OnDeleteClickRotationGroupListener,
    ViewBindingDelegateAdapter<GesturesItem, Ubi4WidgetGesturesBinding>(Ubi4WidgetGesturesBinding::inflate) {

    private val ANIMATION_DURATION = 200
    private var itemsGesturesRotationArray: ArrayList<Pair<Long, String>>? = null
    private var listRotationGroupAdapter: RotationGroupItemAdapter? = null
    private var mRotationGroupDragLv: DragListView? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetGesturesBinding.onBind(item: GesturesItem) {
        mRotationGroupDragLv = rotationGroupDragLv
        var parameterID = 0
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0



        when (item.widget) {
            is CommandParameterWidgetEStruct -> {
                parameterID = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parentParameterID
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
            }
            is CommandParameterWidgetSStruct -> {
                parameterID = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parentParameterID
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
            }
        }
        collectionOfGesturesSelectBtn.setOnClickListener { moveFilterSelection(1, gesturesSelectV, collectionOfGesturesTv, rotationGroupTv, ubi4GesturesSelectorV, collectionGesturesCl, rotationGroupCl) }
        rotationGroupSelectBtn.setOnClickListener { moveFilterSelection(2, gesturesSelectV, collectionOfGesturesTv, rotationGroupTv, ubi4GesturesSelectorV, collectionGesturesCl, rotationGroupCl) }

        Handler().postDelayed({
            moveFilterSelection(2, gesturesSelectV, collectionOfGesturesTv, rotationGroupTv, ubi4GesturesSelectorV, collectionGesturesCl, rotationGroupCl)
        }, 100)

        gesture1Btn.setOnClickListener { System.err.println("setOnClickListener gesture1Btn") }
        gesture1SettingsBtn.setOnClickListener { System.err.println("setOnClickListener gesture1SettingsBtn") }





        rotationGroupDragLv.recyclerView.isVerticalScrollBarEnabled = false
        rotationGroupDragLv.setScrollingEnabled(false)
        rotationGroupDragLv.setOnClickListener {  }
        rotationGroupDragLv.setDragListListener(object : DragListListenerAdapter() {
            override fun onItemDragStarted(position: Int) { }

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (fromPosition != toPosition) { }
            }
        })

        itemsGesturesRotationArray = ArrayList()
        for (i in 0..4) {
            itemsGesturesRotationArray!!.add(Pair<Long, String>(i.toLong(), "Item $i"))
        }
        setupListRecyclerView()
    }

    private fun setupListRecyclerView() {
        mRotationGroupDragLv?.setLayoutManager(LinearLayoutManager(main.applicationContext))
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
    override fun onCopyClick(position: Int) {
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        listRotationGroupAdapter?.notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onDeleteClickCb(position: Int) {
        val resultCb: ((result: Int)->Unit) = {
            itemsGesturesRotationArray?.removeAt(position)
            setupListRecyclerView()
        }
        onDeleteClick(resultCb, itemsGesturesRotationArray?.get(position)?.second.toString())
    }
}