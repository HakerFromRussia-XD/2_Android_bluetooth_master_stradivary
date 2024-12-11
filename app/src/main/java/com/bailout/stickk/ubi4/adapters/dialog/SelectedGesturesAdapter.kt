package com.bailout.stickk.ubi4.adapters.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.models.BindingGestureItem
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider

class SelectedGesturesAdapter(
    private var selectedGesturesList: MutableList<BindingGestureItem>,
    private val onCheckGestureSprListener: OnCheckSprGestureListener,
    private val onDotsClickListener: (Int) -> Unit
) : RecyclerView.Adapter<SelectedGesturesAdapter.SprGesturesViewHolder>() {

    lateinit var myContext: Context
    inner class SprGesturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gestureName: TextView = view.findViewById(R.id.gestureNumber)
        var gestureAnimation: LottieAnimationView = view.findViewById(R.id.lottieAnimationGesture)
        val dotsThreeBtnSpr: ImageView = itemView.findViewById(R.id.dotsThreeBtnSpr)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SprGesturesViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_gesture, parent, false)
        myContext = parent.context
        return SprGesturesViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return selectedGesturesList.size
    }

    override fun onBindViewHolder(holder: SprGesturesViewHolder, position: Int) {
        val bindingGesture = selectedGesturesList[position]

        holder.gestureName.text = bindingGesture.nameOfUserGesture
        holder.gestureAnimation.setAnimation(SprGestureItemsProvider(myContext).getAnimationIdByKeyNameGesture(bindingGesture.sprGestureItem.keyNameGesture))
        holder.gestureAnimation.playAnimation()
        holder.dotsThreeBtnSpr.setOnClickListener {
            onDotsClickListener(position)
            holder.gestureName.text = bindingGesture.nameOfUserGesture
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateGestures(newGestures: List<BindingGestureItem>) {
        selectedGesturesList.clear()
        selectedGesturesList.addAll(newGestures)
        notifyDataSetChanged()
    }

    interface OnCheckSprGestureListener {
        fun onGestureSprClicked(position: Int, title: String)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeGestureByName(gestureTitle: String) {
        val index = selectedGesturesList.indexOfFirst { it.nameOfUserGesture == gestureTitle }
        if (index != -1) {
            selectedGesturesList.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
