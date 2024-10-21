package com.bailout.stickk.ubi4.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem

class SelectedGesturesAdapter(
    private var selectedGesturesList: MutableList<SprGestureItem>,
    private val onCheckGestureSprListener: OnCheckSprGestureListener,
    private val onDotsClickListener: (Int) -> Unit
) : RecyclerView.Adapter<SelectedGesturesAdapter.SprGesturesViewHolder>() {

    inner class SprGesturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gestureName: TextView = view.findViewById(R.id.gestureNumber)
        var gestureImage: ImageView = view.findViewById(R.id.imageGesture)
        val dotsThreeBtnSpr: ImageView = itemView.findViewById(R.id.dotsThreeBtnSpr)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SprGesturesViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_gesture, parent, false)
        return SprGesturesViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return selectedGesturesList.size
    }

    override fun onBindViewHolder(holder: SprGesturesViewHolder, position: Int) {
        val gesture = selectedGesturesList[position]
        holder.gestureName.text = gesture.title
        holder.gestureImage.setImageResource(gesture.image)
        holder.dotsThreeBtnSpr.setOnClickListener {
            onDotsClickListener(position)
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateGestures(newGestures: List<SprGestureItem>) {
        selectedGesturesList.clear()
        selectedGesturesList.addAll(newGestures)
        notifyDataSetChanged()

    }

    interface OnCheckSprGestureListener {
        fun onGestureSprClicked(position: Int, title: String)
    }
}
