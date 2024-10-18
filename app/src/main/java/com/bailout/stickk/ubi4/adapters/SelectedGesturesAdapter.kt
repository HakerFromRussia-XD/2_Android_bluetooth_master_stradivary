package com.bailout.stickk.ubi4.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.models.DialogGestureItem
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem

class SelectedGesturesAdapter(
    private val selectedGesturesList: List<SprGestureItem>,
    private val onCheckGestureSprListener: OnCheckSprGestureListener,
    private val onDotsClickListener: (Int) -> Unit
) : RecyclerView.Adapter<SelectedGesturesAdapter.SprGesturesViewHolder>() {

    inner class SprGesturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gestureName: TextView = view.findViewById(R.id.gestureNumber)
        val gestureImage: ImageView = view.findViewById(R.id.imageGesture)
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
        holder.dotsThreeBtnSpr.setOnClickListener {
            onDotsClickListener(position)
        }

        when (gesture.title) {

            "Gesture №1" -> holder.gestureImage.setImageResource(R.drawable.ok)
            "Gesture №2" -> holder.gestureImage.setImageResource(R.drawable.koza)
            "Gesture №3" -> holder.gestureImage.setImageResource(R.drawable.kulak)
            "Gesture №4" -> holder.gestureImage.setImageResource(R.drawable.grip_the_ball)
            "Gesture №5" -> holder.gestureImage.setImageResource(R.drawable.ok)
            "Gesture №6" -> holder.gestureImage.setImageResource(R.drawable.koza)
            "Gesture №7" -> holder.gestureImage.setImageResource(R.drawable.kulak)
            "Gesture №8" -> holder.gestureImage.setImageResource(R.drawable.grip_the_ball)

        }
    }


    interface OnCheckSprGestureListener {
        fun onGestureSprClicked(position : Int, title: String)
    }
}
